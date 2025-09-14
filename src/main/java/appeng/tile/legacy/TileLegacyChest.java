package appeng.tile.legacy;

import java.util.EnumSet;

import appeng.api.AEApi;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.tile.storage.TileChest;
import net.minecraftforge.common.util.ForgeDirection;

public class TileLegacyChest extends TileChest {
    
    public TileLegacyChest() {
        super();
        this.getProxy().setIdlePowerUsage(0.5);
        this.getProxy().setValidSides(EnumSet.allOf(ForgeDirection.class));
    }

    @Override
    public void
    setOrientation(final ForgeDirection inForward, final ForgeDirection inUp) {
        ForgeDirection forward = inForward;
        ForgeDirection up = inUp;
        if (up == ForgeDirection.DOWN) {
            up = ForgeDirection.UP;
        } else if (up != ForgeDirection.UP) {
            forward = up.getOpposite();
            up = ForgeDirection.UP;
        }
        super.setOrientation(forward, up);
    }

    @Override
    public int[] getAccessibleSlotsBySide(ForgeDirection whichSide) {
        if (this.isPowered()) {
            try {
                if (this.getHandler(AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class)) != null) {
                    return SIDES;
                }
            } catch (final ChestNoHandler e) {
                // nope!
            }
        }
        return NO_SLOTS;
    }

}
