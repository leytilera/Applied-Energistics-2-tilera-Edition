package appeng.tile.legacy;

import java.io.IOException;
import java.util.EnumSet;

import appeng.api.AEApi;
import appeng.api.networking.GridFlags;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.storage.IStackWatcher;
import appeng.api.networking.storage.IStackWatcherHost;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.me.GridAccessException;
import appeng.tile.TileEvent;
import appeng.tile.events.TileEventType;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

public class TileStorageMonitor extends TileLegacyDisplay implements IStackWatcherHost {
    public boolean isLocked;
    public boolean upgraded;
    public IAEItemStack myItem;
    public boolean updateDisplayList;

    public int dspList;

    private IStackWatcher watcher;

    public TileStorageMonitor() {
        this.getProxy().setFlags(GridFlags.REQUIRE_CHANNEL);
        this.getProxy().setIdlePowerUsage(0.5);
        this.getProxy().setValidSides(EnumSet.allOf(ForgeDirection.class));

        if (Platform.isClient()) {
            this.dspList = GLAllocation.generateDisplayLists(1);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();

        if (Platform.isClient()) {
            GLAllocation.deleteDisplayLists(this.dspList);
        }
    }

    @Override
    public boolean requiresTESR() {
        return true;
    }

    @TileEvent(TileEventType.NETWORK_WRITE)
    public void writeToStreamTileStorageMonitor(ByteBuf data) {
        try {
            super.writeToStreamTileLegacyDisplay(data);
            int flags = (this.isLocked ? 1 : 0) | (this.upgraded ? 0b10 : 0)
                | (this.myItem != null ? 0b100 : 0);

            data.writeByte(flags);
            if (this.myItem != null)
                this.myItem.writeToPacket(data);
        } catch (IOException kek) {
            throw new RuntimeException(kek);
        }
    }

    @TileEvent(TileEventType.NETWORK_READ)
    public void readFromStreamTileStorageMonitor(ByteBuf data) {
        try {
            super.readFromStreamTileLegacyDisplay(data);
            byte flags = data.readByte();
            this.isLocked = (flags & 0b1) > 0;
            this.upgraded = (flags & 0b10) > 0;

            // 3rd flag means that there's an item set
            if ((flags & 0b100) > 0) {
                this.myItem = AEItemStack.loadItemStackFromPacket(data);
            }

            this.updateDisplayList = true;
        } catch (IOException kek) {
            throw new RuntimeException(kek);
        }
    }

    @TileEvent(TileEventType.WORLD_NBT_WRITE)
    public void writeNbt(NBTTagCompound nbt) {
        nbt.setBoolean("isLocked", this.isLocked);
        nbt.setBoolean("upgraded", this.upgraded);
        if (this.myItem != null) {
            NBTTagCompound item = new NBTTagCompound();
            this.myItem.writeToNBT(item);
            nbt.setTag("item", item);
        }
    }

    @TileEvent(TileEventType.WORLD_NBT_READ)
    public void readNbt(NBTTagCompound nbt) {
        this.isLocked = nbt.getBoolean("isLocked");
        this.upgraded = nbt.getBoolean("upgraded");
        if (nbt.hasKey("item")) {
            this.myItem = AEItemStack.loadItemStackFromNBT(nbt.getCompoundTag("item"));
        } else {
            this.myItem = null;
        }
    }

    public void configureWatchers() {
        if (this.watcher == null)
            return;

        this.watcher.clear();

        if (this.myItem != null) {
            this.watcher.add(this.myItem);

            try {
                IAEItemStack meitem = this.getProxy()
                                          .getStorage()
                                          .getInventory(AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class))
                                          .getStorageList()
                                          .findPrecise(this.myItem);

                this.myItem.setStackSize(meitem == null ? 0 : meitem.getStackSize());
            } catch (GridAccessException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void updateWatcher(IStackWatcher newWatcher) {
        this.watcher = newWatcher;
        this.configureWatchers();
    }

    @Override
    public void onStackChange(
        IItemList o,
        IAEStack fullStack,
        IAEStack diffStack,
        BaseActionSource src,
        IStorageChannel chan
    ) {
        if (this.myItem == null)
            return;

        if (fullStack == null) {
            this.myItem.setStackSize(0);
        } else {
            this.myItem.setStackSize(fullStack.getStackSize());
        }

        this.markForUpdate();
    }
}
