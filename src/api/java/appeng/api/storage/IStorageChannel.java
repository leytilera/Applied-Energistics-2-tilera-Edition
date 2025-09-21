package appeng.api.storage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

public interface IStorageChannel<T extends IAEStack<T>> {

    @Nonnull
    Class<T> getType();

    /**
	 * Create a new {@link IItemList} of the specific type.
	 * 
	 * @return
	 */
	@Nonnull
    IItemList<T> createList();

    /**
	 * Create a new {@link IAEStack} subtype of the specific object.
	 * 
	 * The parameter is unbound to allow a slightly more flexible approach.
	 * But the general intention is about converting an {@link ItemStack} or {@link FluidStack} into the corresponding
	 * {@link IAEStack}.
	 * Another valid case might be to use it instead of {@link IAEStack#copy()}, but this might not be supported by all
	 * types.
	 * IAEStacks that use custom items for {@link IAEStack#asItemStackRepresentation()} must also be able to convert
	 * these.
	 * 
	 * @param input The object to turn into an {@link IAEStack}
	 * @return The converted stack or null
	 */
	@Nullable
	T createStack( @Nonnull Object input );
    
}
