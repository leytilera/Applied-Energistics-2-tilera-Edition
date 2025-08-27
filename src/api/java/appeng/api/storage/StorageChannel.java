/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2013 AlgorithmX2
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package appeng.api.storage;

import java.util.function.Consumer;
import java.util.function.Function;

import appeng.api.AEApi;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;

@Deprecated
public enum StorageChannel implements IStorageChannel {
    /**
     * AE2's Default Storage.
     */
    ITEMS(IAEItemStack.class),

    /**
     * AE2's Fluid Based Storage ( mainly added to better support ExtraCells )
     */
    FLUIDS(IAEFluidStack.class);

    public final Class<? extends IAEStack> type;

    StorageChannel(final Class<? extends IAEStack> t) {
        this.type = t;
    }

    @Override
    public IItemList createList() {
        if (this == ITEMS) {
            return AEApi.instance().storage().createItemList();
        } else {
            return AEApi.instance().storage().createFluidList();
        }
    }

    @Override
    public Class getType() {
        return type;
    }

    public static StorageChannel get(IStorageChannel channel) {
        if (channel instanceof StorageChannel) {
            return (StorageChannel) channel;
        } else {
            return null;
        }
    }

    public static void call(IStorageChannel channel, Consumer<StorageChannel> action) {
        if (channel instanceof StorageChannel) {
            action.accept((StorageChannel)channel);
        }
    }

    public static <T> T call(IStorageChannel channel, Function<StorageChannel, T> action, T def) {
        if (channel instanceof StorageChannel) {
            return action.apply((StorageChannel)channel);
        }
        return def;
    }

}
