package appeng.block.legacy;

import java.util.EnumSet;

import appeng.api.networking.security.PlayerSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.block.AEBaseBlock;
import appeng.client.render.BaseBlockRender;
import appeng.client.render.blocks.RenderBlockStorageMonitor;
import appeng.client.texture.ExtraBlockTextures;
import appeng.core.Api;
import appeng.core.features.AEFeature;
import appeng.me.GridAccessException;
import appeng.tile.AEBaseTile;
import appeng.tile.legacy.TileStorageMonitor;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;

public class BlockStorageMonitor extends BlockLegacyDisplay {
    public BlockStorageMonitor() {
        super(Material.iron);
        this.setTileEntity(TileStorageMonitor.class);
        this.setFeature(EnumSet.of(AEFeature.Legacy));
    }

    @SideOnly(Side.CLIENT)
    @Override
    protected BaseBlockRender<? extends AEBaseBlock, ? extends AEBaseTile> getRenderer() {
        return new RenderBlockStorageMonitor();
    }

    @Override
    public boolean onBlockActivated(
        World w,
        int x,
        int y,
        int z,
        EntityPlayer player,
        int side,
        float hitX,
        float hitY,
        float hitZ
    ) {
        TileStorageMonitor tile = (TileStorageMonitor) w.getTileEntity(x, y, z);

        if (player.getHeldItem() != null) {
            if (Platform.isWrench(player, player.getHeldItem(), x, y, z)
                && player.isSneaking()) {
                tile.isLocked = !tile.isLocked;
                tile.markForUpdate();
                return true;
            }

            if (player.getHeldItem().getItem()
                    == Api.INSTANCE.definitions()
                           .materials()
                           .conversionMatrix()
                           .maybeItem()
                           .get()
                && !tile.upgraded) {
                if (!w.isRemote) {
                    player.inventory.decrStackSize(player.inventory.currentItem, 1);
                    tile.upgraded = true;
                    tile.markForUpdate();
                }
                return true;
            }

            if (side == tile.getForward().ordinal() && !tile.isLocked) {
                if (!w.isRemote) {
                    tile.myItem = AEItemStack.create(new ItemStack(
                        player.getHeldItem().getItem(),
                        0,
                        player.getHeldItem().getItemDamage()
                    ));
                    tile.configureWatchers();
                    tile.markForUpdate();
                }
                return true;
            }

            if (side == tile.getForward().ordinal() && tile.upgraded) {
                try {
                    IStorageGrid storage
                        = tile.getProxy().getGrid().getCache(IStorageGrid.class);

                    IAEItemStack remaining = Api.INSTANCE.storage().poweredInsert(
                        tile.getProxy().getEnergy(),
                        storage.getInventory(StorageChannel.ITEMS),
                        AEItemStack.create(player.getHeldItem()),
                        new PlayerSource(player, tile)
                    );

                    player.inventory.setInventorySlotContents(
                        player.inventory.currentItem,
                        remaining == null ? null : remaining.getItemStack()
                    );

                    if (!(player instanceof FakePlayer)) {
                        ((EntityPlayerMP) player)
                            .sendContainerToPlayer(player.inventoryContainer);
                    }
                } catch (GridAccessException kek) {}
                return true;
            }
        } else if (side == tile.getForward().ordinal() && tile.upgraded) {
            if (player.isSneaking()) {
                try {
                    IStorageGrid storage
                        = tile.getProxy().getGrid().getCache(IStorageGrid.class);

                    for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
                        ItemStack it = player.inventory.getStackInSlot(i);
                        if (it == null || it.getItem() != tile.myItem.getItem())
                            continue;

                        IAEItemStack remaining = Api.INSTANCE.storage().poweredInsert(
                            tile.getProxy().getEnergy(),
                            storage.getInventory(StorageChannel.ITEMS),
                            AEItemStack.create(it),
                            new PlayerSource(player, tile)
                        );

                        player.inventory.setInventorySlotContents(
                            i, remaining == null ? null : remaining.getItemStack()
                        );
                    }

                    if (!(player instanceof FakePlayer)) {
                        ((EntityPlayerMP) player)
                            .sendContainerToPlayer(player.inventoryContainer);
                    }
                } catch (GridAccessException kek) {}
            } else {
                if (tile.myItem.getStackSize() == 0)
                    return true;

                try {
                    IStorageGrid storage
                        = tile.getProxy().getGrid().getCache(IStorageGrid.class);

                    IAEItemStack request = tile.myItem.copy();
                    request.setStackSize(Math.min(
                        request.getStackSize(), request.getItemStack().getMaxStackSize()
                    ));

                    IAEItemStack extracted = Api.INSTANCE.storage().poweredExtraction(
                        tile.getProxy().getEnergy(),
                        storage.getInventory(StorageChannel.ITEMS),
                        request,
                        new PlayerSource(player, tile)
                    );

                    if (extracted == null)
                        return true;

                    Entity itemEntity = new EntityItem(
                        tile.getWorldObj(),
                        tile.xCoord + tile.getForward().offsetX + 0.5,
                        tile.yCoord + tile.getForward().offsetY + 0.5,
                        tile.zCoord + tile.getForward().offsetZ + 0.5,
                        extracted.getItemStack()
                    );

                    tile.getWorldObj().spawnEntityInWorld(itemEntity);
                } catch (GridAccessException kek) {}
            }
            return true;
        }

        return super.onBlockActivated(w, x, y, z, player, side, hitX, hitY, hitZ);
    }

    @Override
    public IIcon getIcon(IBlockAccess w, int x, int y, int z, int s) {
        TileStorageMonitor te = (TileStorageMonitor) w.getTileEntity(x, y, z);

        if (te != null && te.upgraded && s == te.getForward().ordinal()) {
            return ExtraBlockTextures.BlockStorageMonitorFrontMatrix.getIcon();
        }

        return super.getIcon(w, x, y, z, s);
    }
}
