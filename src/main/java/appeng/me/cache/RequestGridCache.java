package appeng.me.cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridStorage;
import appeng.api.networking.events.MENetworkEventSubscribe;
import appeng.api.networking.events.MENetworkPostCacheConstruction;
import appeng.api.networking.events.MENetworkRequestProviderChange;
import appeng.api.networking.request.IRequestGrid;
import appeng.api.networking.request.IRequestProvider;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.ICellProvider;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;

public class RequestGridCache
    implements IRequestGrid, IMEInventoryHandler<IAEItemStack>, ICellProvider {
    private IGrid grid;
    private IStorageGrid storageGrid;
    private Map<IAEItemStack, Requestable> requestable = new HashMap<>();
    private Set<IRequestProvider> requestProviders = new HashSet<>();
    private int tickSinceRefresh = 0;

    public RequestGridCache(IGrid grid) {
        this.grid = grid;
    }

    @Override
    public void onUpdateTick() {
        if (requestProviders.isEmpty()) return;
        if (tickSinceRefresh % 10 == 0) {
            tickSinceRefresh = 0;
            recalcRequestable();
        }
        tickSinceRefresh++;
    }

    @Override
    public void removeNode(IGridNode gridNode, IGridHost machine) {
        if (machine instanceof IRequestProvider) {
            requestProviders.remove(machine);
            recalcRequestable();
        }
    }

    @Override
    public void addNode(IGridNode gridNode, IGridHost machine) {
        if (machine instanceof IRequestProvider) {
            if (((IRequestProvider)machine).isActive()) {
                requestProviders.add((IRequestProvider) machine);
                recalcRequestable();
            }
        }
    }

    @Override
    public void onSplit(IGridStorage destinationStorage) {}

    @Override
    public void onJoin(IGridStorage sourceStorage) {}

    @Override
    public void populateGridStorage(IGridStorage destinationStorage) {}

    @MENetworkEventSubscribe
    public void requestProviderChange(MENetworkRequestProviderChange event) {
        if (event.provider.isActive()) {
            requestProviders.add(event.provider);
        } else {
            requestProviders.remove(event.provider);
        }
        recalcRequestable();
    }

    public void recalcRequestable() {
        requestable.clear();
        for (IRequestProvider provider : requestProviders) {
            Set<IAEItemStack> stacks = provider.getRequestableItems();
            for (IAEItemStack stack : stacks) {
                if (requestable.containsKey(stack)) {
                    Requestable r = requestable.get(stack);
                    r.addProvider(provider);
                    r.increaseAmount(stack.getCountRequestable());
                    if (stack.isCraftable()) {
                        r.setCraftable();
                    }
                } else {
                    Requestable r = new Requestable(stack);
                    r.addProvider(provider);
                    requestable.put(stack, r);
                }
            }
        }
        storageGrid.postAlterationOfStoredItems(
            StorageChannel.ITEMS, requestable.keySet(), new BaseActionSource()
        );
    }

    @Override
    public Set<IAEItemStack> getRequestableItems() {
        Set<IAEItemStack> list = new HashSet<>();
        for (IAEItemStack stack : requestable.keySet()) {
            list.add(stack.copy());
        }
        return list;
    }

    @Override
    public IAEItemStack requestItems(IAEItemStack stack) {
        if (!requestable.containsKey(stack))
            return stack;

        Requestable r = requestable.get(stack);
        IAEItemStack toRequest = stack;
        for (IRequestProvider provider : r.providers) {
            if (toRequest == null)
                break;
            toRequest = provider.requestStack(toRequest, Actionable.MODULATE);
        }

        return toRequest;
    }

    @Override
    public IAEItemStack
    injectItems(IAEItemStack input, Actionable type, BaseActionSource src) {
        return input;
    }

    @Override
    public IAEItemStack
    extractItems(IAEItemStack request, Actionable mode, BaseActionSource src) {
        return null;
    }

    @Override
    public IItemList<IAEItemStack> getAvailableItems(IItemList<IAEItemStack> out) {
        Set<IAEItemStack> items = requestable.keySet();
        for (IAEItemStack s : items) {
            IAEItemStack stack = s.copy();
            stack.reset();
            Requestable r = requestable.get(s);
            stack.setCountRequestable(r.amount);
            stack.setCraftable(r.craftable);
            out.add(stack);
        }
        return out;
    }

    @Override
    public StorageChannel getChannel() {
        return StorageChannel.ITEMS;
    }

    @Override
    public AccessRestriction getAccess() {
        return AccessRestriction.WRITE;
    }

    @Override
    public boolean isPrioritized(IAEItemStack input) {
        return true;
    }

    @Override
    public boolean canAccept(IAEItemStack input) {
        return false;
    }

    @Override
    public int getPriority() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getSlot() {
        return 0;
    }

    @Override
    public boolean validForPass(int i) {
        return i == 1;
    }

    @MENetworkEventSubscribe
    public void
    afterCacheConstruction(final MENetworkPostCacheConstruction cacheConstruction) {
        this.storageGrid = this.grid.getCache(IStorageGrid.class);
        this.storageGrid.registerCellProvider(this);
    }

    @Override
    public List<IMEInventoryHandler> getCellArray(IStorageChannel channel) {
        final List<IMEInventoryHandler> list = new ArrayList<>(1);

        if (channel == StorageChannel.ITEMS) {
            list.add(this);
        }

        return list;
    }

    static class Requestable {
        public Set<IRequestProvider> providers;
        public long amount;
        public boolean craftable;

        public Requestable(IAEItemStack stack) {
            this.providers = new HashSet<>();
            this.amount = stack.getCountRequestable();
            this.craftable = stack.isCraftable();
        }

        public void addProvider(IRequestProvider provider) {
            this.providers.add(provider);
        }

        public void increaseAmount(long amount) {
            this.amount += amount;
        }

        public void setCraftable() {
            this.craftable = true;
        }
    }
}
