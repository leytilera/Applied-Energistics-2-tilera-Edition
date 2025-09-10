/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.me.cache;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import appeng.api.AEApi;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridStorage;
import appeng.api.networking.events.MENetworkCellArrayUpdate;
import appeng.api.networking.events.MENetworkEventSubscribe;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.ISecurityGrid;
import appeng.api.networking.security.MachineSource;
import appeng.api.networking.storage.IStackWatcher;
import appeng.api.networking.storage.IStackWatcherHost;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.*;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.me.helpers.GenericInterestManager;
import appeng.me.storage.ItemWatcher;
import appeng.me.storage.NetworkInventoryHandler;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

public class GridStorageCache implements IStorageGrid {
    private final IGrid myGrid;
    private final HashSet<ICellProvider> activeCellProviders
        = new HashSet<ICellProvider>();
    private final HashSet<ICellProvider> inactiveCellProviders
        = new HashSet<ICellProvider>();
    private final SetMultimap<IAEStack, ItemWatcher> interests = HashMultimap.create();
    private final GenericInterestManager<ItemWatcher> interestManager
        = new GenericInterestManager<ItemWatcher>(this.interests);
    /*private final NetworkMonitor<IAEItemStack> itemMonitor
        = new NetworkMonitor<IAEItemStack>(this, StorageChannel.ITEMS);
    private final NetworkMonitor<IAEFluidStack> fluidMonitor
        = new NetworkMonitor<IAEFluidStack>(this, StorageChannel.FLUIDS);*/
    private final Map<IStorageChannel, NetworkMonitor> monitors = new HashMap<>();
    private final HashMap<IGridNode, IStackWatcher> watchers
        = new HashMap<IGridNode, IStackWatcher>();
    private Map<IStorageChannel, NetworkInventoryHandler> myNetworks = new HashMap<>();

    public GridStorageCache(final IGrid g) {
        this.myGrid = g;
        monitors.put(StorageChannel.ITEMS, new NetworkMonitor<IAEItemStack>(this, StorageChannel.ITEMS));
        monitors.put(StorageChannel.FLUIDS, new NetworkMonitor<IAEItemStack>(this, StorageChannel.FLUIDS));
    }

    @Override
    public void onUpdateTick() {
        this.monitors.forEach((k, v) -> v.onTick());
    }

    @Override
    public void removeNode(final IGridNode node, final IGridHost machine) {
        if (machine instanceof ICellContainer) {
            final ICellContainer cc = (ICellContainer) machine;
            final CellChangeTracker tracker = new CellChangeTracker();

            this.removeCellProvider(cc, tracker);
            this.inactiveCellProviders.remove(cc);
            this.getGrid().postEvent(new MENetworkCellArrayUpdate());

            tracker.applyChanges();
        }

        if (machine instanceof IStackWatcherHost) {
            final IStackWatcher myWatcher = this.watchers.get(machine);

            if (myWatcher != null) {
                myWatcher.clear();
                this.watchers.remove(machine);
            }
        }
    }

    @Override
    public void addNode(final IGridNode node, final IGridHost machine) {
        if (machine instanceof ICellContainer) {
            final ICellContainer cc = (ICellContainer) machine;
            this.inactiveCellProviders.add(cc);

            this.getGrid().postEvent(new MENetworkCellArrayUpdate());

            if (node.isActive()) {
                final CellChangeTracker tracker = new CellChangeTracker();

                this.addCellProvider(cc, tracker);
                tracker.applyChanges();
            }
        }

        if (machine instanceof IStackWatcherHost) {
            final IStackWatcherHost swh = (IStackWatcherHost) machine;
            final ItemWatcher iw = new ItemWatcher(this, swh);
            this.watchers.put(node, iw);
            swh.updateWatcher(iw);
        }
    }

    @Override
    public void onSplit(final IGridStorage storageB) {}

    @Override
    public void onJoin(final IGridStorage storageB) {}

    @Override
    public void populateGridStorage(final IGridStorage storage) {}

    private CellChangeTracker
    addCellProvider(final ICellProvider cc, final CellChangeTracker tracker) {
        if (this.inactiveCellProviders.contains(cc)) {
            this.inactiveCellProviders.remove(cc);
            this.activeCellProviders.add(cc);

            BaseActionSource actionSrc = new BaseActionSource();
            if (cc instanceof IActionHost) {
                actionSrc = new MachineSource((IActionHost) cc);
            }

            for (final IMEInventoryHandler<IAEItemStack> h :
                 cc.getCellArray(StorageChannel.ITEMS)) {
                tracker.postChanges(StorageChannel.ITEMS, 1, h, actionSrc);
            }

            for (final IMEInventoryHandler<IAEFluidStack> h :
                 cc.getCellArray(StorageChannel.FLUIDS)) {
                tracker.postChanges(StorageChannel.FLUIDS, 1, h, actionSrc);
            }
        }

        return tracker;
    }

    private CellChangeTracker
    removeCellProvider(final ICellProvider cc, final CellChangeTracker tracker) {
        if (this.activeCellProviders.contains(cc)) {
            this.activeCellProviders.remove(cc);
            this.inactiveCellProviders.add(cc);

            BaseActionSource actionSrc = new BaseActionSource();

            if (cc instanceof IActionHost) {
                actionSrc = new MachineSource((IActionHost) cc);
            }

            for (final IMEInventoryHandler<IAEItemStack> h :
                 cc.getCellArray(StorageChannel.ITEMS)) {
                tracker.postChanges(StorageChannel.ITEMS, -1, h, actionSrc);
            }

            for (final IMEInventoryHandler<IAEFluidStack> h :
                 cc.getCellArray(StorageChannel.FLUIDS)) {
                tracker.postChanges(StorageChannel.FLUIDS, -1, h, actionSrc);
            }
        }

        return tracker;
    }

    @MENetworkEventSubscribe
    public void cellUpdate(final MENetworkCellArrayUpdate ev) {
        //this.myItemNetwork = null;
        //this.myFluidNetwork = null;
        this.myNetworks.clear();

        final LinkedList<ICellProvider> ll = new LinkedList();
        ll.addAll(this.inactiveCellProviders);
        ll.addAll(this.activeCellProviders);

        final CellChangeTracker tracker = new CellChangeTracker();

        for (final ICellProvider cc : ll) {
            boolean active = true;

            if (cc instanceof IActionHost) {
                final IGridNode node = ((IActionHost) cc).getActionableNode();
                active = node != null && node.isActive();
            }

            if (active) {
                this.addCellProvider(cc, tracker);
            } else {
                this.removeCellProvider(cc, tracker);
            }
        }

        this.monitors.forEach((k, v) -> v.forceUpdate());

        tracker.applyChanges();
    }

    private void postChangesToNetwork(
        final IStorageChannel chan,
        final int upOrDown,
        final IItemList availableItems,
        final BaseActionSource src
    ) {
        if (this.monitors.containsKey(chan)) {
            this.monitors.get(chan).postChange(upOrDown > 0, availableItems, src);
        }
    }

    IMEInventoryHandler<IAEItemStack> getItemInventoryHandler() {
        if (!this.myNetworks.containsKey(StorageChannel.ITEMS)) {
            this.buildNetworkStorage(StorageChannel.ITEMS);
        }
        return this.myNetworks.get(StorageChannel.ITEMS);
    }

    private void buildNetworkStorage(final IStorageChannel chan) {
        final SecurityCache security = this.getGrid().getCache(ISecurityGrid.class);
        NetworkInventoryHandler myNetwork = new NetworkInventoryHandler(chan, security);
        myNetworks.put(chan, myNetwork);
        for (final ICellProvider cc : this.activeCellProviders) {
            for (final IMEInventoryHandler<IAEFluidStack> h :
                cc.getCellArray(chan)) {
                myNetwork.addNewStorage(h);
            }
        }
    }

    IMEInventoryHandler<IAEFluidStack> getFluidInventoryHandler() {
        if (!this.myNetworks.containsKey(StorageChannel.FLUIDS)) {
            this.buildNetworkStorage(StorageChannel.FLUIDS);
        }
        return this.myNetworks.get(StorageChannel.FLUIDS);
    }

    @Override
    public void postAlterationOfStoredItems(
        final StorageChannel chan,
        final Iterable<? extends IAEStack> input,
        final BaseActionSource src
    ) {
        if (this.monitors.containsKey(chan)) {
            this.monitors.get(chan).postChange(true, (Iterable<IAEItemStack>) input, src);
        }
    }

    @Override
    public void registerCellProvider(final ICellProvider provider) {
        this.inactiveCellProviders.add(provider);
        this.addCellProvider(provider, new CellChangeTracker()).applyChanges();
    }

    @Override
    public void unregisterCellProvider(final ICellProvider provider) {
        this.removeCellProvider(provider, new CellChangeTracker()).applyChanges();
        this.inactiveCellProviders.remove(provider);
    }

    @Override
    public IMEMonitor<IAEItemStack> getItemInventory() {
        return getInventory(StorageChannel.ITEMS);
    }

    @Override
    public IMEMonitor<IAEFluidStack> getFluidInventory() {
        return getInventory(StorageChannel.FLUIDS);
    }

    @Override
    public <T extends IAEStack<T>> IMEMonitor<T> getInventory(IStorageChannel<T> channel) {
        return this.monitors.get(channel);
    }

    public GenericInterestManager<ItemWatcher> getInterestManager() {
        return this.interestManager;
    }

    IGrid getGrid() {
        return this.myGrid;
    }

    private class CellChangeTrackerRecord {
        final IStorageChannel channel;
        final int up_or_down;
        final IItemList list;
        final BaseActionSource src;

        public CellChangeTrackerRecord(
            final IStorageChannel channel,
            final int i,
            final IMEInventoryHandler<? extends IAEStack> h,
            final BaseActionSource actionSrc
        ) {
            this.channel = channel;
            this.up_or_down = i;
            this.src = actionSrc;
            this.list = h.getAvailableItems(channel.createList());
        }

        public void applyChanges() {
            GridStorageCache.this.postChangesToNetwork(
                this.channel, this.up_or_down, this.list, this.src
            );
        }
    }

    private class CellChangeTracker {
        final List<CellChangeTrackerRecord> data
            = new LinkedList<CellChangeTrackerRecord>();

        public void postChanges(
            final IStorageChannel channel,
            final int i,
            final IMEInventoryHandler<? extends IAEStack> h,
            final BaseActionSource actionSrc
        ) {
            this.data.add(new CellChangeTrackerRecord(channel, i, h, actionSrc));
        }

        public void applyChanges() {
            for (final CellChangeTrackerRecord rec : this.data) {
                rec.applyChanges();
            }
        }
    }
}
