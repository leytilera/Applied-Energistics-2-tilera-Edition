package appeng.tile.legacy;

import java.util.ArrayList;

import appeng.api.config.Actionable;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IAssemblerCache;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.networking.events.MENetworkCraftingPatternChange;
import appeng.api.networking.security.MachineSource;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.util.WorldCoord;
import appeng.block.legacy.BlockAssemblerHeatVent;
import appeng.block.legacy.BlockAssemblerWall;
import appeng.container.ContainerNull;
import appeng.items.misc.ItemEncodedPattern;
import appeng.me.GridAccessException;
import appeng.me.cache.AssemblerGridCache;
import appeng.me.cluster.IAssemblerCluster;
import appeng.me.cluster.IAssemblerMB;
import appeng.me.cluster.implementations.AssemblerCluster;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.AENetworkProxyMultiblock;
import appeng.tile.TileEvent;
import appeng.tile.events.TileEventType;
import appeng.tile.grid.AENetworkTile;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.tile.inventory.IAEAppEngInventory;
import appeng.tile.inventory.InvOperation;
import appeng.util.InventoryAdaptor;
import appeng.util.Platform;
import appeng.util.inv.AdaptorIInventory;
import appeng.util.inv.IInventoryDestination;
import appeng.util.item.AEItemStack;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public class TileAssembler extends AENetworkTile
    implements IAssemblerMB, IAEAppEngInventory, IInventory, ICraftingProvider {
    AssemblerCluster ac = null;
    AppEngInternalInventory inv = new AppEngInternalInventory(this, 54);
    public Job[] jobs;

    public TileAssembler() {
        super();
        // TODO: WTF
        //super.updatesOnPower = false;
        this.getProxy().setFlags(GridFlags.REQUIRE_CHANNEL, GridFlags.MULTIBLOCK);
    }

    @Override
    protected AENetworkProxy createProxy() {
        return new AENetworkProxyMultiblock(
            this, "proxy", this.getItemFromTile(this), true
        );
    }

    @Override
    public boolean canBeRotated() {
        return false;
    }

    // TODO: WTF
    //protected void terminate() {
    //    super.terminate();
    //    if (this.ac != null) {
    //        this.ac.destroy();
    //    }

    //    this.ac = null;
    //}

    @Override
    public void setInventorySlotContents(int var1, ItemStack var2) {
        this.inv.setInventorySlotContents(var1, var2);
        try {
            this.getProxy().getGrid().postEvent(
                new MENetworkCraftingPatternChange(this, this.getProxy().getNode())
            );
        } catch (GridAccessException kek) {}
    }

    public ItemStack decrStackSize(int var1, int var2) {
        ItemStack is = this.inv.decrStackSize(var1, var2);
        try {
            this.getProxy().getGrid().postEvent(
                new MENetworkCraftingPatternChange(this, this.getProxy().getNode())
            );
        } catch (GridAccessException kek) {}
        return is;
    }

    public IAssemblerCluster getCluster() {
        return this.ac;
    }

    // TODO: WTF
    //@Override
    public boolean isComplete() {
        return this.ac != null;
    }

    public static void
    completeRegion(IBlockAccess w, WorldCoord min, WorldCoord max, AssemblerCluster ac) {
        TileAssemblerMB lastMB = null;

        for (int x = min.x; x <= max.x; ++x) {
            for (int y = min.y; y <= max.y; ++y) {
                for (int z = min.z; z <= max.z; ++z) {
                    TileEntity te = w.getTileEntity(x, y, z);
                    AssemblerCluster acc;
                    if (te instanceof TileAssembler) {
                        if (ac == null) {
                            acc = ((TileAssembler) te).ac;
                            if (acc != null) {
                                acc.destroy();
                            }
                        }

                        ((TileAssembler) te).updateStatus(ac);
                    } else if (te instanceof TileAssemblerMB) {
                        if (ac == null) {
                            acc = ((TileAssemblerMB) te).ac;
                            if (acc != null) {
                                acc.destroy();
                            }
                        }

                        lastMB = (TileAssemblerMB) te;
                        ((TileAssemblerMB) te).updateStatus(ac);
                    }
                }
            }
        }

        if (ac != null && lastMB != null) {
            lastMB.sendUpdate(true, null);
            ac.initMaster();
            try {
                IAssemblerCache cache
                    = lastMB.getProxy().getGrid().getCache(IAssemblerCache.class);
                ((AssemblerGridCache) cache).addCluster(ac);
            } catch (GridAccessException kek) {}
            // TODO: WTF
            //MinecraftForge.EVENT_BUS.post(new GridTileConnectivityEvent(
            //    lastMB, lastMB.field_70331_k, lastMB.getLocation()
            //));
        }
    }

    public boolean requiresTickingUpdates() {
        return false;
    }

    public static AssemblerCluster
    verifyOwnedRegion(IBlockAccess w, WorldCoord min, WorldCoord max) {
        AssemblerCluster ac = new AssemblerCluster(min, max);
        ac.assemblers = new ArrayList<>();
        ac.mb = new ArrayList<>();
        ac.accelerators = 1;

        for (int x = min.x; x <= max.x; ++x) {
            for (int y = min.y; y <= max.y; ++y) {
                for (int z = min.z; z <= max.z; ++z) {
                    TileEntity te = w.getTileEntity(x, y, z);
                    if (x != min.x && x != max.x && y != min.y && y != max.y && z != min.z
                        && z != max.z && te instanceof TileAssembler) {
                        ac.assemblers.add((TileAssembler) te);
                    } else {
                        if (!(te instanceof TileAssemblerMB)) {
                            return null;
                        }

                        Block mb = te.getBlockType();
                        if (mb == null) {
                            return null;
                        }

                        ac.mb.add((TileAssemblerMB) te);
                        if (te instanceof TileAssemblerCraftingAccelerator) {
                            ++ac.accelerators;
                        } else if (x != min.x && x != max.x && y != min.y && y != max.y && z != min.z && z != max.z) {
                            return null;
                        }

                        if (x == min.x && z == min.z || x == min.x && z == max.z
                            || x == max.x && z == min.z || x == max.x && z == max.z
                            || x == min.x && y == min.y || x == min.x && y == max.y
                            || x == max.x && y == min.y || x == max.x && y == max.y
                            || z == min.z && y == min.y || z == min.z && y == max.y
                            || z == max.z && y == min.y || z == max.z && y == max.y
                            || y == min.y && x == min.x || y == min.y && x == max.x
                            || y == max.y && z == min.z || y == max.y && z == max.z) {
                            if (!(mb instanceof BlockAssemblerWall)) {
                                return null;
                            }
                        } else if ((x == max.x || x == min.x || y == min.y || y == max.y || z == min.z || z == max.z) && !(mb instanceof BlockAssemblerHeatVent)) {
                            return null;
                        }
                    }
                }
            }
        }

        if (ac.assemblers.size() > 0) {
            return ac;
        } else {
            return null;
        }
    }

    public boolean canCraft() {
        for (Job j : this.jobs)
            if (j == null)
                return true;

        return false;
    }

    public static boolean verifyUnownedRegionInner(
        IBlockAccess w,
        int minx,
        int miny,
        int minz,
        int maxx,
        int maxy,
        int maxz,
        ForgeDirection side
    ) {
        switch (side) {
            case WEST:
                --minx;
                maxx = minx;
                break;
            case EAST:
                ++maxx;
                minx = maxx;
                break;
            case DOWN:
                --miny;
                maxy = miny;
                break;
            case NORTH:
                ++maxz;
                minz = maxz;
                break;
            case SOUTH:
                --minz;
                maxz = minz;
                break;
            case UP:
                ++maxy;
                miny = maxy;
                break;
            case UNKNOWN:
                return false;
        }

        for (int x = minx; x <= maxx; ++x) {
            for (int y = miny; y <= maxy; ++y) {
                for (int z = minz; z <= maxz; ++z) {
                    TileEntity te = w.getTileEntity(x, y, z);
                    if (te instanceof TileAssemblerMB) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static boolean
    verifyUnownedRegion(IBlockAccess w, WorldCoord min, WorldCoord max) {
        ForgeDirection[] arr$ = ForgeDirection.VALID_DIRECTIONS;
        int len$ = arr$.length;

        for (int i$ = 0; i$ < len$; ++i$) {
            ForgeDirection side = arr$[i$];
            if (verifyUnownedRegionInner(
                    w, min.x, min.y, min.z, max.x, max.y, max.z, side
                )) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void calculateMultiblock() {
        calculateMultiblockR(this);
    }

    public static boolean isAnAssembler(IBlockAccess w, int x, int y, int z) {
        TileEntity TE = w.getTileEntity(x, y, z);
        return TE instanceof IAssemblerMB;
    }

    public static void calculateMultiblockR(IAssemblerMB start) {
        if (!Platform.isClient()) {
            try {
                WorldCoord min = start.getLocation();
                WorldCoord max = start.getLocation();

                World w;
                for (w = ((TileEntity) start).getWorldObj();
                     isAnAssembler(w, min.x - 1, min.y, min.z);
                     --min.x) {}

                while (isAnAssembler(w, min.x, min.y - 1, min.z)) {
                    --min.y;
                }

                while (isAnAssembler(w, min.x, min.y, min.z - 1)) {
                    --min.z;
                }

                while (isAnAssembler(w, max.x + 1, max.y, max.z)) {
                    ++max.x;
                }

                while (isAnAssembler(w, max.x, max.y + 1, max.z)) {
                    ++max.y;
                }

                while (isAnAssembler(w, max.x, max.y, max.z + 1)) {
                    ++max.z;
                }

                if (max.x - min.x >= 2 && max.y - min.y >= 2 && max.z - min.z >= 2) {
                    AssemblerCluster ac = verifyOwnedRegion(w, min, max);
                    if (ac != null && verifyUnownedRegion(w, min, max)) {
                        if (start.getCluster() != null)
                            ac = (AssemblerCluster) start.getCluster();
                        completeRegion(w, min, max, ac);
                        return;
                    }
                }

                WorldCoord pos = start.getLocation();
                ForgeDirection[] arr$ = ForgeDirection.values();
                int len$ = arr$.length;

                for (int i$ = 0; i$ < len$; ++i$) {
                    ForgeDirection d = arr$[i$];
                    TileEntity TE = w.getTileEntity(
                        pos.x + d.offsetX, pos.y + d.offsetY, pos.z + d.offsetZ
                    );
                    if (TE instanceof IAssemblerMB) {
                        AssemblerCluster ac
                            = (AssemblerCluster) ((IAssemblerMB) TE).getCluster();
                        if (ac != null) {
                            ac.destroy();
                        }
                    }
                }
            } catch (Throwable var18) {}
        }
    }

    public String getInventoryName() {
        return "ME Pattern Provider";
    }

    public float getPowerDrainPerTick() {
        return this.ac != null ? 1.0F : 0.0F;
    }

    public void updateStatus(IAssemblerCluster _ac) {
        boolean wasComplete = this.ac != null;
        this.ac = (AssemblerCluster) _ac;
        if (wasComplete != (this.ac != null)) {
            this.markForUpdate();
        }
    }

    public boolean isEnabled() {
        return this.ac != null;
    }

    public int addItem(ItemStack stack, boolean doAdd, ForgeDirection from) {
        if (stack.getItem() instanceof ICraftingPatternItem) {
            InventoryAdaptor ia = new AdaptorIInventory(this);
            ItemStack used = null;
            int originalStackSize = stack.stackSize;
            if (doAdd) {
                used = ia.addItems(stack);
            } else {
                used = ia.simulateAdd(stack);
            }

            return used == null ? originalStackSize : originalStackSize - used.stackSize;
        } else {
            return 0;
        }
    }

    public ItemStack[] extractItem(
        boolean doRemove, ForgeDirection from, int maxItemCount
    ) {
        ItemStack[] output = new ItemStack[1];
        InventoryAdaptor ia = new AdaptorIInventory(this);
        if (doRemove) {
            output[0] = ia.removeItems(
                maxItemCount, (ItemStack) null, (IInventoryDestination) null
            );
        } else {
            output[0] = ia.simulateRemove(
                maxItemCount, (ItemStack) null, (IInventoryDestination) null
            );
        }

        return output[0] == null ? new ItemStack[0] : output;
    }

    public boolean hasCustomInventoryName() {
        return true;
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack itemstack) {
        if (itemstack.getItem() instanceof ICraftingPatternItem) {
            ICraftingPatternDetails pat
                = ((ICraftingPatternItem) itemstack.getItem())
                      .getPatternForItem(itemstack, this.worldObj);

            return pat != null;
        }
        return false;
    }

    public boolean isSeperated() {
        return !this.isEnabled();
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer entityplayer) {
        return this.worldObj.getTileEntity(this.xCoord, this.yCoord, this.zCoord) != this
            ? false
            : entityplayer.getDistanceSq(
                  (double) this.xCoord + 0.5,
                  (double) this.yCoord + 0.5,
                  (double) this.zCoord + 0.5
              ) <= 32.0;
    }

    @Override
    public void onChangeInventory(
        IInventory inv, int slot, InvOperation mc, ItemStack removed, ItemStack added
    ) {}

    @Override
    public int getSizeInventory() {
        return this.inv.getSizeInventory();
    }

    @Override
    public ItemStack getStackInSlot(int idx) {
        return this.inv.getStackInSlot(idx);
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int idx) {
        return this.inv.getStackInSlotOnClosing(idx);
    }

    @Override
    public int getInventoryStackLimit() {
        return this.inv.getInventoryStackLimit();
    }

    @Override
    public void openInventory() {
        this.inv.openInventory();
    }

    @Override
    public void closeInventory() {
        this.inv.closeInventory();
    }

    @Override
    public void onChunkLoad() {
        super.onChunkLoad();
        if (this.ac != null)
            return;

        this.calculateMultiblock();
    }

    @Override
    public boolean
    pushPattern(ICraftingPatternDetails patternDetails, InventoryCrafting table) {
        if (this.ac == null)
            return false;

        for (int i = 0; i < this.getSizeInventory(); i++) {
            ItemStack is = this.getStackInSlot(i);
            if (is == null || !(is.getItem() instanceof ICraftingPatternItem))
                continue;

            if (((ICraftingPatternItem) is.getItem())
                    .getPatternForItem(is, this.worldObj)
                    .equals(patternDetails)) {
                return this.ac.getMaster().addCraft(new Job(patternDetails, table));
            }
        }
        return false;
    }

    public void onOperation() {
        for (int i = 0; i < this.jobs.length; i++) {
            if (this.jobs[i] == null)
                continue;

            ItemStack out = this.jobs[i].det.getOutput(this.jobs[i].inv, this.worldObj);

            if (out != null) {
                try {
                    IMEMonitor<IAEItemStack> inv
                        = this.getProxy().getStorage().getInventory(StorageChannel.ITEMS);

                    inv.injectItems(
                        AEItemStack.create(out),
                        Actionable.MODULATE,
                        new MachineSource(this)
                    );

                } catch (GridAccessException kek) {}
            }

            this.jobs[i] = null;
        }
    }

    public boolean addCraft(Job job) {
        for (int i = 0; i < this.jobs.length; i++) {
            if (this.jobs[i] == null) {
                this.jobs[i] = job;
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isBusy() {
        if (this.ac == null)
            return true;

        return !this.ac.canCraft();
    }

    @Override
    public void provideCrafting(ICraftingProviderHelper craftingTracker) {
        if (this.ac == null)
            return;

        for (int i = 0; i < this.getSizeInventory(); i++) {
            ItemStack is = this.getStackInSlot(i);
            if (is == null || !(is.getItem() instanceof ICraftingPatternItem))
                continue;

            ICraftingPatternDetails det = ((ICraftingPatternItem) is.getItem())
                                              .getPatternForItem(is, this.worldObj);

            craftingTracker.addCraftingOption(this, det);
        }
    }

    public boolean isMaster() {
        return this.jobs != null;
    }

    @TileEvent(TileEventType.WORLD_NBT_WRITE)
    public void writeToNbtTileAssembler(NBTTagCompound nbt) {
        this.inv.writeToNBT(nbt, "inv");

        if (this.isMaster()) {
            NBTTagList jobs = new NBTTagList();
            for (Job j : this.jobs) {
                if (j == null)
                    continue;

                jobs.appendTag(j.writeToNBT(new NBTTagCompound()));
            }

            nbt.setTag("jobs", jobs);
            nbt.setInteger("jobc", this.jobs.length);
        }
    }

    @TileEvent(TileEventType.WORLD_NBT_READ)
    public void readFromNbtTileAssembler(NBTTagCompound nbt) {
        this.inv.readFromNBT(nbt, "inv");

        if (nbt.hasKey("jobs")) {
            NBTTagList jobs = nbt.getTagList("jobs", 10);

            this.jobs = new Job[nbt.getInteger("jobc")];

            for (int i = 0; i < jobs.tagCount(); i++) {
                this.jobs[i] = Job.readFromNBT(jobs.getCompoundTagAt(i), this.worldObj);
            }
        }
    }

    public static class Job {
        public ICraftingPatternDetails det;
        public InventoryCrafting inv;

        public Job(ICraftingPatternDetails det, InventoryCrafting inv) {
            this.det = det;
            this.inv = inv;
        }

        public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
            nbt.setTag("pattern", this.det.getPattern().writeToNBT(new NBTTagCompound()));
            NBTTagList input = new NBTTagList();

            for (int i = 0; i < this.inv.getSizeInventory(); i++) {
                if (this.inv.getStackInSlot(i) == null) {
                    input.appendTag(new NBTTagCompound());
                } else {
                    input.appendTag(
                        this.inv.getStackInSlot(i).writeToNBT(new NBTTagCompound())
                    );
                }
            }

            nbt.setTag("input", input);

            return nbt;
        }

        public static Job readFromNBT(NBTTagCompound nbt, World w) {
            ItemStack patternStack
                = ItemStack.loadItemStackFromNBT(nbt.getCompoundTag("pattern"));
            ICraftingPatternDetails det = ((ItemEncodedPattern) patternStack.getItem())
                                              .getPatternForItem(patternStack, w);

            NBTTagList input = nbt.getTagList("input", 10);
            InventoryCrafting inv = new InventoryCrafting(new ContainerNull(), 3, 3);

            for (int i = 0; i < input.tagCount(); i++) {
                if (input.getCompoundTagAt(i).hasNoTags()) {
                    inv.setInventorySlotContents(i, null);
                } else {
                    inv.setInventorySlotContents(
                        i, ItemStack.loadItemStackFromNBT(input.getCompoundTagAt(i))
                    );
                }
            }

            return new Job(det, inv);
        }
    }

    @Override
    public void disconnect(boolean b) {
        // TODO Auto-generated method stub
    }

    @Override
    public boolean isValid() {
        return this.isComplete();
    }
}
