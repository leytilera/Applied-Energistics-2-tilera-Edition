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

package appeng.core.api;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import com.google.common.base.Preconditions;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;

import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.IStorageHelper;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.channels.IFluidStorageChannel;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.crafting.CraftingLink;
import appeng.util.Platform;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import appeng.util.item.FluidList;
import appeng.util.item.ItemList;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

public class ApiStorage implements IStorageHelper {

    private final ClassToInstanceMap<IStorageChannel<?>> channels;

    public ApiStorage() {
		this.channels = MutableClassToInstanceMap.create();
		this.registerStorageChannel( IItemStorageChannel.class, (IItemStorageChannel)(IStorageChannel)StorageChannel.ITEMS);
		this.registerStorageChannel( IFluidStorageChannel.class, (IFluidStorageChannel)(IStorageChannel)StorageChannel.FLUIDS);
	}

    @Override
    public ICraftingLink
    loadCraftingLink(final NBTTagCompound data, final ICraftingRequester req) {
        return new CraftingLink(data, req);
    }

    @Override
    public IAEItemStack createItemStack(final ItemStack is) {
        return AEItemStack.create(is);
    }

    @Override
    public IAEFluidStack createFluidStack(final FluidStack is) {
        return AEFluidStack.create(is);
    }

    @Override
    public IItemList<IAEItemStack> createItemList() {
        return new ItemList();
    }

    @Override
    public IItemList<IAEFluidStack> createFluidList() {
        return new FluidList();
    }

    @Override
    public IAEItemStack readItemFromPacket(final ByteBuf input) throws IOException {
        return AEItemStack.loadItemStackFromPacket(input);
    }

    @Override
    public IAEFluidStack readFluidFromPacket(final ByteBuf input) throws IOException {
        return AEFluidStack.loadFluidStackFromPacket(input);
    }

    @Override
    public IAEItemStack poweredExtraction(
        final IEnergySource energy,
        final IMEInventory<IAEItemStack> cell,
        final IAEItemStack request,
        final BaseActionSource src
    ) {
        return Platform.poweredExtraction(energy, cell, request, src);
    }

    @Override
    public IAEItemStack poweredInsert(
        final IEnergySource energy,
        final IMEInventory<IAEItemStack> cell,
        final IAEItemStack input,
        final BaseActionSource src
    ) {
        return Platform.poweredInsert(energy, cell, input, src);
    }

    @Override
    public <T extends IAEStack<T>, C extends IStorageChannel<T>> void registerStorageChannel(Class<C> channel,
            C factory) {
        Preconditions.checkNotNull(channel);
		Preconditions.checkNotNull(factory);
		Preconditions.checkArgument(channel.isInstance(factory));
		Preconditions.checkArgument(!this.channels.containsKey(channel));

		this.channels.putInstance(channel, factory);
    }

    @Override
    public <T extends IAEStack<T>, C extends IStorageChannel<T>> C getStorageChannel(Class<C> channel) {
        Preconditions.checkNotNull(channel);

		final C type = this.channels.getInstance(channel);

		Preconditions.checkNotNull(type);

		return type;
    }

    @Override
    public Collection<IStorageChannel<? extends IAEStack<?>>> storageChannels() {
        return Collections.unmodifiableCollection(this.channels.values());
    }
}
