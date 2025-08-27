package appeng.api.storage;

import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;

public interface IStorageChannel<T extends IAEStack<T>> {

    Class<T> getType();

    IItemList<T> createList();
    
}
