package appeng.tile.legacy;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGrid;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.MachineSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.core.sync.packets.PacketTransitionEffect;
import appeng.me.GridAccessException;
import appeng.me.cluster.IAECluster;
import appeng.me.cluster.IAEMultiBlock;
import appeng.me.cluster.implementations.TransitionPlaneCalculator;
import appeng.me.cluster.implementations.TransitionPlaneCluster;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.AENetworkProxyMultiblock;
import appeng.server.ServerHelper;
import appeng.tile.TileEvent;
import appeng.tile.events.TileEventType;
import appeng.tile.grid.AENetworkTile;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.util.ForgeDirection;

public class TileTransitionPlane extends AENetworkTile implements IAEMultiBlock {
    List<IAEItemStack> buffer = new ArrayList<>();
    int delay = 0;
    int trys = 0;
    boolean wasPulse = false;
    static Set<Block> unbreakables = new HashSet<>();
    private final BaseActionSource mySrc = new MachineSource(this);

    public RenderMode renderMode = RenderMode.INVALID;

    public TransitionPlaneCluster cluster;
    public TransitionPlaneCalculator calc = new TransitionPlaneCalculator(this);

    public TileTransitionPlane() {
        // TODO: WTF
        //super.hasPower = false;
        this.getProxy().setValidSides(EnumSet.allOf(ForgeDirection.class));
        this.getProxy().setFlags(GridFlags.REQUIRE_CHANNEL, GridFlags.MULTIBLOCK);
        this.getProxy().setIdlePowerUsage(1.0);
    }

    @Override
    protected AENetworkProxy createProxy() {
        return new AENetworkProxyMultiblock(
            this, "proxy", this.getItemFromTile(this), true
        );
    }

    @Override
    public boolean requiresTESR() {
        return true;
    }

    @Override
    public void invalidate() {
        this.disconnect(false);
        super.invalidate();
    }

    @Override
    public void onChunkUnload() {
        this.disconnect(false);
        super.onChunkUnload();
    }

    @Override
    public void onReady() {
        super.onReady();
        this.calc.calculateMultiblock(this.worldObj, this.getLocation());
    }

    public void updateStatus(TransitionPlaneCluster c) {
        this.cluster = c;
        this.getProxy().setValidSides(
            c == null ? EnumSet.noneOf(ForgeDirection.class)
                      : EnumSet.allOf(ForgeDirection.class)
        );
        this.markForUpdate();
    }

    // TODO: WTF
    //public void setOrientationBySide(
    //    EntityPlayer player, int side, float hitX, float hitY, float hitZ
    //) {
    //    super.setOrientationBySide(player, side, hitX, hitY, hitZ);
    //    ForgeDirection getForward() = ForgeDirection.getOrientation(side);
    //    WorldCoord pos
    //        = new WorldCoord(this.xCoord, this.yCoord, this.zCoord);
    //    pos.add(getForward(), -1);
    //    this.worldObj.getTileEntity(pos.x, pos.y, pos.z);
    //    MinecraftForge.EVENT_BUS.post(
    //        new GridTileConnectivityEvent(this, this.worldObj, this.getLocation())
    //    );
    //}

    // TODO: WTF
    //public boolean syncStyle(IAppEngNetworkTile.SyncTime st) {
    //    return true;
    //}

    @TileEvent(TileEventType.TICK)
    public void updateTileEntity() {
        if (this.getProxy().isActive() && this.delay++ > 320) {
            if (this.eatBlock()) {
                this.delay = 0;
            }

            try {
                IGrid gi = this.getProxy().getGrid();

                if (gi != null) {
                    while (!this.buffer.isEmpty()) {
                        IAEItemStack aeitem = this.buffer.get(0);
                        this.buffer.remove(0);
                        IStorageGrid storage = this.getProxy().getStorage();
                        IEnergyGrid energy = this.getProxy().getEnergy();
                        IAEItemStack overflow = Platform.poweredInsert(
                            energy, storage.getInventory(AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class)), aeitem, this.mySrc
                        );
                        if (overflow != null) {
                            this.buffer.add(overflow);
                            break;
                        }

                        if (this.buffer.isEmpty()) {
                            this.markForUpdate();
                        }
                    }
                    if (this.buffer.isEmpty()) {
                        this.reqEat();
                    }
                }
            } catch (GridAccessException e) {
                // :P
            }
        }
    }

    // TODO: WTF
    //public void ddItem(EntityItem entitiy) {
    //    if (this.isAccepting()) {
    //        ItemStack is = entitiy.getEntityItem();
    //        if (this.Buffer.isEmpty() && this.getProxy().isActive() &&
    //        Platform.isServer()
    //            && is != null) {
    //            IGrid gi = this.getProxy().getGrid();
    //            if (gi != null && !entitiy.isDead) {
    //                int howMany = gi.usePowerForAddition(is.stackSize, 1);
    //                if (howMany > 0) {
    //                    IMEInventory ca = gi.getCellArray();
    //                    if (ca != null) {
    //                        ItemStack toAdd = is.splitStack(howMany);
    //                        toAdd = Platform.refundEnergy(
    //                            gi, Platform.addItems(ca, toAdd), "Transiton Plane"
    //                        );
    //                        if (toAdd != null) {
    //                            is.stackSize += toAdd.stackSize;
    //                        }

    //                        try {
    //                            float var10009 = (float) entitiy.posX;
    //                            AppEng.getInstance().SideProxy.sendToAllNearExcept(
    //                                (EntityPlayer) null,
    //                                entitiy.posX,
    //                                entitiy.posY,
    //                                entitiy.posZ,
    //                                64.0,
    //                                this.field_70331_k,
    //                                (new PacketTransitionPlane(
    //                                     var10009,
    //                                     (float) entitiy.posY,
    //                                     (float) entitiy.posZ,
    //                                     this.getForward(),
    //                                     false
    //                                 ))
    //                                    .getPacket()
    //                            );
    //                        } catch (IOException var8) {}

    //                        if (is.stackSize > 0) {
    //                            this.Buffer.add(is.copy());
    //                            this.markForUpdate();
    //                        }

    //                        entitiy.setDead();
    //                    }
    //                }
    //            }
    //        }
    //    }
    //}

    public boolean isAccepting() {
        this.wasPulse = false;
        return true;
    }

    // TODO: WTF
    //public static void blacklist(int itemID, int damageValue) {
    //    unbreakables.add(damageValue << 16 | itemID);
    //}

    public boolean notUnbreakable(Block block, int damageValue) {
        if (block != Blocks.air && block.getMaterial() != Material.air
            && block.getMaterial() != Material.lava
            && block.getMaterial() != Material.water) {
            return !unbreakables.contains(block);
        } else {
            return false;
        }
    }

    public boolean eatBlock() {
        if (this.isAccepting() && this.getProxy().isActive() && Platform.isServer()
            // TODO: WTF
            /*&& this.isValid()*/) {
            int x = this.xCoord + this.getForward().offsetX;
            int y = this.yCoord + this.getForward().offsetY;
            int z = this.zCoord + this.getForward().offsetZ;
            Block bid = this.worldObj.getBlock(x, y, z);
            int meta = this.worldObj.getBlockMetadata(x, y, z);
            if (!this.worldObj.isAirBlock(x, y, z) && this.worldObj.blockExists(x, y, z)
                && this.buffer.isEmpty() && bid != Blocks.air
                && this.notUnbreakable(bid, meta) && bid != Blocks.bedrock
                && this.worldObj.canMineBlock(
                    FakePlayerFactory.getMinecraft((WorldServer) this.worldObj), x, y, z
                )) {
                float hardness = bid.getBlockHardness(this.worldObj, x, y, z);
                if ((double) hardness >= 0.0) {
                    ItemStack[] out = Platform.getBlockDrops(this.worldObj, x, y, z);
                    float total = 1.0F + hardness;
                    ItemStack[] arr$ = out;
                    int len$ = out.length;
                    for (len$ = 0; len$ < len$; ++len$) {
                        ItemStack is = arr$[len$];
                        total += (float) is.stackSize;
                    }

                    boolean hasPower = false;
                    try {
                        hasPower = this.getProxy().getEnergy().extractAEPower(
                                       total, Actionable.MODULATE, PowerMultiplier.CONFIG
                                   )
                            > total - 0.1;
                    } catch (GridAccessException e1) {
                        // :P
                    }
                    if (hasPower) {
                        this.worldObj.setBlockToAir(x, y, z);
                        this.trys = 0;

                        ServerHelper.proxy.sendToAllNearExcept(
                            null,
                            this.xCoord,
                            this.yCoord,
                            this.zCoord,
                            64,
                            this.getTile().getWorldObj(),
                            new PacketTransitionEffect(
                                this.xCoord + this.getForward().offsetX + 0.5,
                                this.yCoord + this.getForward().offsetY + 0.5,
                                this.zCoord + this.getForward().offsetZ + 0.5,
                                this.getForward(),
                                false
                            )
                        );

                        arr$ = out;
                        len$ = out.length;

                        for (int i$ = 0; i$ < len$; ++i$) {
                            //ItemStack is = arr$[i$];
                            //is = Platform.addItems(ca, is);
                            //if (is != null) {
                            //    this.Buffer.add(is);
                            //    this.markForUpdate();
                            //}
                            if (arr$[i$] == null)
                                continue;

                            this.buffer.add(AEItemStack.create(arr$[i$]));
                        }

                        try {
                            IGrid gi = this.getProxy().getGrid();

                            if (gi != null) {
                                while (!this.buffer.isEmpty()) {
                                    IAEItemStack aeitem = this.buffer.get(0);
                                    this.buffer.remove(0);
                                    IStorageGrid storage = this.getProxy().getStorage();
                                    IEnergyGrid energy = this.getProxy().getEnergy();
                                    IAEItemStack overflow = Platform.poweredInsert(
                                        energy,
                                        storage.getInventory(AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class)),
                                        aeitem,
                                        this.mySrc
                                    );
                                    if (overflow != null) {
                                        this.buffer.add(overflow);
                                        break;
                                    }

                                    if (this.buffer.isEmpty()) {
                                        this.markForUpdate();
                                    }
                                }
                                if (this.buffer.isEmpty()) {
                                    this.reqEat();
                                }
                            }
                        } catch (GridAccessException e) {
                            // :P
                        }
                    }
                }
            }
        }

        return true;
    }

    public void reqEat() {
        this.delay += 9999;
    }

    // TODO: WTF
    //public void onNeighborBlockChange() {
    //    this.reqEat();
    //    this.trys = 0;
    //}

    //public void setPowerStatus(boolean _hasPower) {
    //    super.setPowerStatus(_hasPower);
    //    this.reqEat();
    //}

    //public void onUpdateRedstone() {
    //    super.onUpdateRedstone();
    //    this.reqEat();
    //}

    //public void pulseRedStone() {
    //    super.pulseRedStone();
    //    this.wasPulse = true;
    //    this.reqEat();
    //}

    // TODO: WTF
    //public void placedBy(EntityLivingBase entityliving) {
    //    byte rotation = (byte
    //    ) (MathHelper.floor_double(
    //           (double) (entityliving.rotationYaw * 4.0F / 360.0F) + 2.5
    //       )
    //       & 3);
    //    if (entityliving.rotationPitch > 65.0F) {
    //        rotation = 4;
    //    } else if (entityliving.rotationPitch < -65.0F) {
    //        rotation = 5;
    //    }

    //    //this.getForward() = this.getDirectionFromAERotation(rotation);
    //    this.getForward() = ForgeDirection.getOrientation(rotation);
    //}

    // TODO: WTF
    //private Icon defTextures(ForgeDirection side) {
    //    if (side == this.getForward().getOpposite()) {
    //        return AppEngTextureRegistry.Blocks.GenericBottom.get();
    //    } else if (side == ForgeDirection.DOWN) {
    //        return AppEngTextureRegistry.Blocks.GenericTop.get();
    //    } else {
    //        return side == ForgeDirection.UP
    //            ? AppEngTextureRegistry.Blocks.GenericTop.get()
    //            : AppEngTextureRegistry.Blocks.GenericSide.get();
    //    }
    //}

    //public Icon getBlockTextureFromSide(ForgeDirection side) {
    //    Icon frontFace = this.isMachineActive()
    //        ? AppEngTextureRegistry.Blocks.BlockTransPlane.get()
    //        : AppEngTextureRegistry.Blocks.BlockTransPlaneOff.get();
    //    return this.getForward() == side ? frontFace : this.defTextures(side);
    //}

    @TileEvent(TileEventType.WORLD_NBT_WRITE)
    public void worldNbtWriteTileTransitionPlane(NBTTagCompound nbt) {
        NBTTagList items = new NBTTagList();

        for (int x = 0; x < this.buffer.size(); ++x) {
            NBTTagCompound item = new NBTTagCompound();
            this.buffer.get(x).writeToNBT(item);
            items.appendTag(item);
        }

        nbt.setTag("items", items);
    }

    @TileEvent(TileEventType.WORLD_NBT_READ)
    public void worldNbtReadTileTransitionPlane(NBTTagCompound nbt) {
        NBTTagList items = nbt.getTagList("items", 10);

        for (int x = 0; x < items.tagCount(); ++x) {
            this.buffer.add(AEItemStack.loadItemStackFromNBT(items.getCompoundTagAt(x)));
        }
    }

    @TileEvent(TileEventType.NETWORK_WRITE)
    public void writeToStreamTileTransitionPlane(ByteBuf buf) {
        if (this.cluster == null) {
            buf.writeByte(RenderMode.INVALID.ordinal());
        } else if (this.cluster.tiles.size() == 1) {
            buf.writeByte(RenderMode.SINGLE.ordinal());
        } else {
            buf.writeByte(RenderMode.MULTI.ordinal());
        }
    }

    @TileEvent(TileEventType.NETWORK_READ)
    public void readFromStreamTileTransitionPlane(ByteBuf buf) {
        this.renderMode = RenderMode.values()[buf.readByte()];

        this.worldObj.markBlockRangeForRenderUpdate(
            this.xCoord, this.yCoord, this.zCoord, this.xCoord, this.yCoord, this.zCoord
        );
    }

    @Override
    public void disconnect(boolean b) {
        if (this.cluster != null)
            this.cluster.destroy();
        this.updateStatus(null);
    }

    @Override
    public IAECluster getCluster() {
        return this.cluster;
    }

    @Override
    public boolean isValid() {
        return this.cluster != null;
    }

    //public boolean handleTilePacket(DataInputStream stream) throws IOException {
    //    ForgeDirection oldOrientation = this.getForward();
    //    boolean oldHasPower = super.hasPower;
    //    byte rotation = stream.readByte();
    //    this.getForward() = this.getDirectionFromAERotation(rotation);
    //    byte flags = stream.readByte();
    //    super.hasPower = (flags & 1) == 1;
    //    if ((flags & 2) == 2) {
    //        if (this.Buffer.isEmpty()) {
    //            this.Buffer.add(new ItemStack(Item.field_77669_D));
    //        }
    //    } else {
    //        this.Buffer.clear();
    //    }

    //    return oldHasPower != super.hasPower || oldOrientation != this.getForward();
    //}

    //public void configureTilePacket(DataOutputStream data) throws IOException {
    //    data.writeByte(this.getAERotationFromDirection(this.getForward()));
    //    byte flags = 0;
    //    byte flags = (byte) (flags | (super.hasPower ? 1 : 0));
    //    flags = (byte) (flags | (this.Buffer.isEmpty() ? 0 : 2));
    //    data.writeByte(flags);
    //}

    // TODO: WTF
    //public void addCollidingBlockToList(
    //    World world,
    //    int x,
    //    int y,
    //    int z,
    //    AxisAlignedBB axisalignedbb,
    //    List arraylist,
    //    Entity par7Entity
    //) {
    //    AppEngMultiBlock blk = (AppEngMultiBlock) this.getBlockType();
    //    if (par7Entity instanceof EntityItem && this.isMachineActive()
    //        && this.Buffer.isEmpty()) {
    //        blk.func_71905_a(
    //            this.getForward() == ForgeDirection.WEST ? 0.005F : 0.0F,
    //            this.getForward() == ForgeDirection.DOWN ? 0.005F : 0.0F,
    //            this.getForward() == ForgeDirection.NORTH ? 0.005F : 0.0F,
    //            this.getForward() == ForgeDirection.EAST ? 0.995F : 1.0F,
    //            this.getForward() == ForgeDirection.UP ? 0.995F : 1.0F,
    //            this.getForward() == ForgeDirection.SOUTH ? 0.995F : 1.0F
    //        );
    //        blk.configureCollidingBlockToList(
    //            world, x, y, z, axisalignedbb, arraylist, par7Entity
    //        );
    //        blk.func_71905_a(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
    //    } else {
    //        blk.func_71905_a(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
    //        blk.configureCollidingBlockToList(
    //            world, x, y, z, axisalignedbb, arraylist, par7Entity
    //        );
    //    }
    //}

    // TODO: this is to avoid Z-fighting. Put into renderer?
    //@Override
    //public AxisAlignedBB[] getSelectedBoundingBoxsFromPool(
    //    World world, int x, int y, int z
    //) {
    //    return new AxisAlignedBB[] { AxisAlignedBB.getBoundingBox(
    //        this.getForward() == ForgeDirection.WEST ? 0.004999999888241291 : 0.0,
    //        this.getForward() == ForgeDirection.DOWN ? 0.004999999888241291 : 0.0,
    //        this.getForward() == ForgeDirection.NORTH ? 0.004999999888241291 : 0.0,
    //        this.getForward() == ForgeDirection.EAST ? 0.9950000047683716 : 1.0,
    //        this.getForward() == ForgeDirection.UP ? 0.9950000047683716 : 1.0,
    //        this.getForward() == ForgeDirection.SOUTH ? 0.9950000047683716 : 1.0
    //    ) };
    //}

    //public void setPrimaryOrientation(ForgeDirection s) {
    //    this.orientation = s;
    //    MinecraftForge.EVENT_BUS.post(
    //        new GridTileConnectivityEvent(this, this.getWorld(), this.getLocation())
    //    );
    //}

    public static enum RenderMode {
        INVALID,
        SINGLE,
        MULTI;
    }
}
