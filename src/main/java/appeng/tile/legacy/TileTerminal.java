package appeng.tile.legacy;

import java.util.EnumSet;

import appeng.api.config.Settings;
import appeng.api.config.SortDir;
import appeng.api.config.SortOrder;
import appeng.api.config.ViewItems;
import appeng.api.networking.GridFlags;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.data.IAEStack;
import appeng.api.util.IConfigManager;
import appeng.me.GridAccessException;
import appeng.util.ConfigManager;
import appeng.util.IConfigManagerHost;
import net.minecraftforge.common.util.ForgeDirection;

public class TileTerminal
    extends TileLegacyDisplay implements ITerminalHost, IConfigManagerHost {
    private final IConfigManager cm = new ConfigManager(this);

    public TileTerminal() {
        this.getProxy().setFlags(GridFlags.REQUIRE_CHANNEL);
        this.getProxy().setIdlePowerUsage(0.5);
        this.getProxy().setValidSides(EnumSet.allOf(ForgeDirection.class));

        this.cm.registerSetting(Settings.SORT_BY, SortOrder.NAME);
        this.cm.registerSetting(Settings.VIEW_MODE, ViewItems.ALL);
        this.cm.registerSetting(Settings.SORT_DIRECTION, SortDir.ASCENDING);
    }

    @Override
    public <T extends IAEStack<T>> IMEMonitor<T> getInventory(IStorageChannel<T> channel) {
        try {
            return this.getProxy().getStorage().getInventory(channel);
        } catch (final GridAccessException e) {
            // err nope?
        }
        return null;
    }

    @Override
    public IConfigManager getConfigManager() {
        return this.cm;
    }

    @Override
    public void updateSetting(IConfigManager manager, Enum settingName, Enum newValue) {}
}
