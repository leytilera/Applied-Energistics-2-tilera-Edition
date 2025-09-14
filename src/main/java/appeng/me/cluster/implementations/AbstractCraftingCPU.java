package appeng.me.cluster.implementations;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.config.PowerMultiplier;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.CraftingItemList;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingMedium;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IMEMonitorHandlerReceiver;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.container.ContainerNull;
import appeng.core.AELog;
import appeng.crafting.CraftBranchFailure;
import appeng.crafting.CraftingJob;
import appeng.crafting.CraftingLink;
import appeng.crafting.CraftingWatcher;
import appeng.crafting.MECraftingInventory;
import appeng.me.cache.CraftingGridCache;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public abstract class AbstractCraftingCPU implements ICraftingCPU {
    private static final String LOG_MARK_AS_COMPLETE = "Completed job for %s.";

    protected final HashMap<IMEMonitorHandlerReceiver<IAEItemStack>, Object> listeners = new HashMap<IMEMonitorHandlerReceiver<IAEItemStack>, Object>();
    protected final Map<ICraftingPatternDetails, TaskProgress> tasks = new HashMap<ICraftingPatternDetails, TaskProgress>();
    protected MECraftingInventory inventory = new MECraftingInventory();
    protected IItemList<IAEItemStack> waitingFor = AEApi.instance().storage().createItemList();
    protected ICraftingLink myLastLink;

    /**
     * crafting job info
     */
    protected IAEItemStack finalOutput;
    protected boolean waiting = false;
    protected boolean isComplete = true;
    protected int remainingOperations;
    protected boolean somethingChanged;

    protected long lastTime;
    protected long elapsedTime;
    protected long startItemCount;
    protected long remainingItemCount;

    @Override
    public void addListener(IMEMonitorHandlerReceiver<IAEItemStack> l, Object verificationToken) {
        this.listeners.put(l, verificationToken);
    }

    @Override
    public void removeListener(IMEMonitorHandlerReceiver<IAEItemStack> l) {
        this.listeners.remove(l);
    }

    @Override
    public ICraftingLink getLastCraftingLink() {
        return this.myLastLink;
    }

    @Override
    public boolean isBusy() {
        final Iterator<Entry<ICraftingPatternDetails, TaskProgress>> i
            = this.tasks.entrySet().iterator();

        while (i.hasNext()) {
            if (i.next().getValue().value <= 0) {
                i.remove();
            }
        }

        return !this.tasks.isEmpty() || !this.waitingFor.isEmpty();
    }

    @Override
    public void getListOfItem(IItemList<IAEItemStack> list, CraftingItemList whichList) {
        switch (whichList) {
            case ACTIVE:
                for (final IAEItemStack ais : this.waitingFor) {
                    list.add(ais);
                }
                break;
            case PENDING:
                for (final Entry<ICraftingPatternDetails, TaskProgress> t :
                     this.tasks.entrySet()) {
                    for (IAEItemStack ais : t.getKey().getCondensedOutputs()) {
                        ais = ais.copy();
                        ais.setStackSize(ais.getStackSize() * t.getValue().value);
                        list.add(ais);
                    }
                }
                break;
            case STORAGE:
                this.inventory.getAvailableItems(list);
                break;
            default:
            case ALL:
                this.inventory.getAvailableItems(list);

                for (final IAEItemStack ais : this.waitingFor) {
                    list.add(ais);
                }

                for (final Entry<ICraftingPatternDetails, TaskProgress> t :
                     this.tasks.entrySet()) {
                    for (IAEItemStack ais : t.getKey().getCondensedOutputs()) {
                        ais = ais.copy();
                        ais.setStackSize(ais.getStackSize() * t.getValue().value);
                        list.add(ais);
                    }
                }
                break;
        }
    }

    @Override
    public IAEItemStack getItemStack(IAEItemStack what, CraftingItemList storage2) {
        IAEItemStack is;

        switch (storage2) {
            case STORAGE:
                is = this.inventory.getItemList().findPrecise(what);
                break;
            case ACTIVE:
                is = this.waitingFor.findPrecise(what);
                break;
            case PENDING:

                is = what.copy();
                is.setStackSize(0);

                for (final Entry<ICraftingPatternDetails, TaskProgress> t :
                     this.tasks.entrySet()) {
                    for (final IAEItemStack ais : t.getKey().getCondensedOutputs()) {
                        if (ais.equals(is)) {
                            is.setStackSize(
                                is.getStackSize()
                                + ais.getStackSize() * t.getValue().value
                            );
                        }
                    }
                }

                break;
            default:
            case ALL:
                throw new IllegalStateException("Invalid Operation");
        }

        if (is != null) {
            return is.copy();
        }

        is = what.copy();
        is.setStackSize(0);
        return is;
    }

    @Override
    public long getElapsedTime() {
        return this.elapsedTime;
    }

    @Override
    public long getRemainingItemCount() {
        return this.remainingItemCount;
    }

    @Override
    public long getStartItemCount() {
        return this.startItemCount;
    }

    @Override
    public void addCrafting(ICraftingPatternDetails details, long crafts) {
        TaskProgress i = this.tasks.get(details);

        if (i == null) {
            this.tasks.put(details, i = new TaskProgress());
        }

        i.value += crafts;
    }

    @Override
    public void addStorage(IAEItemStack extractItems) {
        this.inventory.injectItems(extractItems, Actionable.MODULATE, null);
        
    }

    @Override
    public void addEmitable(IAEItemStack i) {
        this.waitingFor.add(i);
        this.postCraftingStatusChange(i);
    }

    @Override
    public boolean canAccept(IAEStack<?> input) {
        if (input instanceof IAEItemStack) {
            final IAEItemStack is = this.waitingFor.findPrecise((IAEItemStack) input);
            if (is != null && is.getStackSize() > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isMaking(IAEItemStack what) {
        final IAEItemStack wat = this.waitingFor.findPrecise(what);
        return wat != null && wat.getStackSize() > 0;
    }

    protected abstract IGrid getGrid();

    protected void postCraftingStatusChange(final IAEItemStack diff) {
        if (this.getGrid() == null) {
            return;
        }

        final CraftingGridCache sg = this.getGrid().getCache(ICraftingGrid.class);

        if (sg.getInterestManager().containsKey(diff)) {
            final Collection<CraftingWatcher> list = sg.getInterestManager().get(diff);

            if (!list.isEmpty()) {
                for (final CraftingWatcher iw : list)

                {
                    iw.getHost().onRequestChange(sg, diff);
                }
            }
        }
    }

    protected void updateElapsedTime(final IAEItemStack is) {
        final long nextStartTime = System.nanoTime();
        this.elapsedTime = this.getElapsedTime() + nextStartTime - this.lastTime;
        this.lastTime = nextStartTime;
        this.remainingItemCount = this.getRemainingItemCount() - is.getStackSize();
    }

    protected Iterator<Entry<IMEMonitorHandlerReceiver<IAEItemStack>, Object>> getListeners() {
        return this.listeners.entrySet().iterator();
    }

    protected void postChange(final IAEItemStack diff, final BaseActionSource src) {
        final Iterator<Entry<IMEMonitorHandlerReceiver<IAEItemStack>, Object>> i
            = this.getListeners();

        // protect integrity
        if (i.hasNext()) {
            final ImmutableList<IAEItemStack> single = ImmutableList.of(diff.copy());

            while (i.hasNext()) {
                final Entry<IMEMonitorHandlerReceiver<IAEItemStack>, Object> o = i.next();
                final IMEMonitorHandlerReceiver<IAEItemStack> receiver = o.getKey();

                if (receiver.isValid(o.getValue())) {
                    receiver.postChange(null, single, src);
                } else {
                    i.remove();
                }
            }
        }
    }

    protected void completeJob() {
        if (this.myLastLink != null) {
            ((CraftingLink) this.myLastLink).markDone();
        }

        if (AELog.isCraftingLogEnabled()) {
            final IAEItemStack logStack = this.finalOutput.copy();
            logStack.setStackSize(this.startItemCount);
            AELog.crafting(LOG_MARK_AS_COMPLETE, logStack);
        }

        this.remainingItemCount = 0;
        this.startItemCount = 0;
        this.lastTime = 0;
        this.elapsedTime = 0;
        this.isComplete = true;
    }

    protected abstract void markDirty();

    protected abstract void updateCPU();

    protected abstract World getWorld();

    @Override
    public IAEStack
    injectItems(final IAEStack input, final Actionable type, final BaseActionSource src) {
        if (!(input instanceof IAEItemStack)) {
            return input;
        }

        final IAEItemStack what = (IAEItemStack) input.copy();
        final IAEItemStack is = this.waitingFor.findPrecise(what);

        if (type == Actionable.SIMULATE) // causes crafting to lock up?
        {
            if (is != null && is.getStackSize() > 0) {
                if (is.getStackSize() >= what.getStackSize()) {
                    if (this.finalOutput.equals(what)) {
                        if (this.myLastLink != null) {
                            return ((CraftingLink) this.myLastLink)
                                .injectItems(what.copy(), type);
                        }

                        return what; // ignore it.
                    }

                    return null;
                }

                final IAEItemStack leftOver = what.copy();
                leftOver.decStackSize(is.getStackSize());

                final IAEItemStack used = what.copy();
                used.setStackSize(is.getStackSize());

                if (this.finalOutput.equals(what)) {
                    if (this.myLastLink != null) {
                        leftOver.add(((CraftingLink) this.myLastLink)
                                         .injectItems(used.copy(), type));
                        return leftOver;
                    }

                    return what; // ignore it.
                }

                return leftOver;
            }
        } else if (type == Actionable.MODULATE) {
            if (is != null && is.getStackSize() > 0) {
                this.waiting = false;

                this.postChange(what, src);

                if (is.getStackSize() >= what.getStackSize()) {
                    is.decStackSize(what.getStackSize());

                    this.updateElapsedTime(what);
                    this.markDirty();
                    this.postCraftingStatusChange(is);

                    if (this.finalOutput.equals(what)) {
                        IAEStack leftover = what;

                        this.finalOutput.decStackSize(what.getStackSize());

                        if (this.myLastLink != null) {
                            leftover = ((CraftingLink) this.myLastLink)
                                           .injectItems(what, type);
                        }

                        if (this.finalOutput.getStackSize() <= 0) {
                            this.completeJob();
                        }

                        this.updateCPU();

                        return leftover; // ignore it.
                    }

                    // 2000
                    return this.inventory.injectItems(what, type, src);
                }

                final IAEItemStack insert = what.copy();
                insert.setStackSize(is.getStackSize());
                what.decStackSize(is.getStackSize());

                is.setStackSize(0);

                if (this.finalOutput.equals(insert)) {
                    IAEStack leftover = input;

                    this.finalOutput.decStackSize(insert.getStackSize());

                    if (this.myLastLink != null) {
                        what.add(((CraftingLink) this.myLastLink)
                                     .injectItems(insert.copy(), type));
                        leftover = what;
                    }

                    if (this.finalOutput.getStackSize() <= 0) {
                        this.completeJob();
                    }

                    this.updateCPU();
                    this.markDirty();

                    return leftover; // ignore it.
                }

                this.inventory.injectItems(insert, type, src);
                this.markDirty();

                return what;
            }
        }

        return input;
    }

    protected boolean canCraft(
        final ICraftingPatternDetails details, final IAEItemStack[] condensedInputs
    ) {
        for (IAEItemStack g : condensedInputs) {
            if (details.isCraftable()) {
                boolean found = false;

                for (IAEItemStack fuzz :
                     this.inventory.getItemList().findFuzzy(g, FuzzyMode.IGNORE_ALL)) {
                    fuzz = fuzz.copy();
                    fuzz.setStackSize(g.getStackSize());
                    final IAEItemStack ais = this.inventory.extractItems(
                        fuzz, Actionable.SIMULATE, this.getActionSource()
                    );
                    final ItemStack is = ais == null ? null : ais.getItemStack();

                    if (is != null && is.stackSize == g.getStackSize()) {
                        found = true;
                        break;
                    } else if (is != null) {
                        g = g.copy();
                        g.decStackSize(is.stackSize);
                    }
                }

                if (!found) {
                    return false;
                }
            } else {
                final IAEItemStack ais = this.inventory.extractItems(
                    g.copy(), Actionable.SIMULATE, this.getActionSource()
                );
                final ItemStack is = ais == null ? null : ais.getItemStack();

                if (is == null || is.stackSize < g.getStackSize()) {
                    return false;
                }
            }
        }

        return true;
    }

    protected void executeCrafting(final IEnergyGrid eg, final CraftingGridCache cc) {
        final Iterator<Entry<ICraftingPatternDetails, TaskProgress>> i
            = this.tasks.entrySet().iterator();

        while (i.hasNext()) {
            final Entry<ICraftingPatternDetails, TaskProgress> e = i.next();

            if (e.getValue().value <= 0) {
                i.remove();
                continue;
            }

            final ICraftingPatternDetails details = e.getKey();

            if (this.canCraft(details, details.getCondensedInputs())) {
                InventoryCrafting ic = null;

                for (final ICraftingMedium m : cc.getMediums(e.getKey())) {
                    if (e.getValue().value <= 0) {
                        continue;
                    }

                    if (!m.isBusy()) {
                        if (ic == null) {
                            final IAEItemStack[] input = details.getInputs();
                            double sum = 0;

                            for (final IAEItemStack anInput : input) {
                                if (anInput != null) {
                                    sum += anInput.getStackSize();
                                }
                            }

                            // power...
                            if (eg.extractAEPower(
                                    sum, Actionable.MODULATE, PowerMultiplier.CONFIG
                                )
                                < sum - 0.01) {
                                continue;
                            }

                            ic = new InventoryCrafting(new ContainerNull(), 3, 3);
                            boolean found = false;

                            for (int x = 0; x < input.length; x++) {
                                if (input[x] != null) {
                                    found = false;

                                    if (details.isCraftable()) {
                                        for (IAEItemStack fuzz :
                                             this.inventory.getItemList().findFuzzy(
                                                 input[x], FuzzyMode.IGNORE_ALL
                                             )) {
                                            fuzz = fuzz.copy();
                                            fuzz.setStackSize(input[x].getStackSize());

                                            if (details.isValidItemForSlot(
                                                    x,
                                                    fuzz.getItemStack(),
                                                    this.getWorld()
                                                )) {
                                                final IAEItemStack ais
                                                    = this.inventory.extractItems(
                                                        fuzz,
                                                        Actionable.MODULATE,
                                                        this.getActionSource()
                                                    );
                                                final ItemStack is = ais == null
                                                    ? null
                                                    : ais.getItemStack();

                                                if (is != null) {
                                                    this.postChange(
                                                        AEItemStack.create(is),
                                                        this.getActionSource()
                                                    );
                                                    ic.setInventorySlotContents(x, is);
                                                    found = true;
                                                    break;
                                                }
                                            }
                                        }
                                    } else {
                                        final IAEItemStack ais
                                            = this.inventory.extractItems(
                                                input[x].copy(),
                                                Actionable.MODULATE,
                                                this.getActionSource()
                                            );
                                        final ItemStack is
                                            = ais == null ? null : ais.getItemStack();

                                        if (is != null) {
                                            this.postChange(input[x], this.getActionSource());
                                            ic.setInventorySlotContents(x, is);
                                            if (is.stackSize == input[x].getStackSize()) {
                                                found = true;
                                                continue;
                                            }
                                        }
                                    }

                                    if (!found) {
                                        break;
                                    }
                                }
                            }

                            if (!found) {
                                // put stuff back..
                                for (int x = 0; x < ic.getSizeInventory(); x++) {
                                    final ItemStack is = ic.getStackInSlot(x);
                                    if (is != null) {
                                        this.inventory.injectItems(
                                            AEItemStack.create(is),
                                            Actionable.MODULATE,
                                            this.getActionSource()
                                        );
                                    }
                                }
                                ic = null;
                                break;
                            }
                        }

                        if (m.pushPattern(details, ic)) {
                            this.somethingChanged = true;
                            this.remainingOperations--;

                            for (final IAEItemStack out : details.getCondensedOutputs()) {
                                this.postChange(out, this.getActionSource());
                                this.waitingFor.add(out.copy());
                                this.postCraftingStatusChange(out.copy());
                            }

                            if (details.isCraftable()) {
                                FMLCommonHandler.instance().firePlayerCraftingEvent(
                                    Platform.getPlayer((WorldServer) this.getWorld()),
                                    details.getOutput(ic, this.getWorld()),
                                    ic
                                );

                                for (int x = 0; x < ic.getSizeInventory(); x++) {
                                    final ItemStack output
                                        = Platform.getContainerItem(ic.getStackInSlot(x));
                                    if (output != null) {
                                        final IAEItemStack cItem
                                            = AEItemStack.create(output);
                                        this.postChange(cItem, this.getActionSource());
                                        this.waitingFor.add(cItem);
                                        this.postCraftingStatusChange(cItem);
                                    }
                                }
                            }

                            ic = null; // hand off complete!
                            this.markDirty();

                            e.getValue().value--;
                            if (e.getValue().value <= 0) {
                                continue;
                            }

                            if (this.remainingOperations == 0) {
                                return;
                            }
                        }
                    }
                }

                if (ic != null) {
                    // put stuff back..
                    for (int x = 0; x < ic.getSizeInventory(); x++) {
                        final ItemStack is = ic.getStackInSlot(x);
                        if (is != null) {
                            this.inventory.injectItems(
                                AEItemStack.create(is),
                                Actionable.MODULATE,
                                this.getActionSource()
                            );
                        }
                    }
                }
            }
        }
    }

    protected String generateCraftingID() {
        final long now = System.currentTimeMillis();
        final int hash = System.identityHashCode(this);
        final int hmm = this.finalOutput == null ? 0 : this.finalOutput.hashCode();

        return Long.toString(now, Character.MAX_RADIX) + '-'
            + Integer.toString(hash, Character.MAX_RADIX) + '-'
            + Integer.toString(hmm, Character.MAX_RADIX);
    }

    protected NBTTagCompound generateLinkData(
        final String craftingID, final boolean standalone, final boolean req
    ) {
        final NBTTagCompound tag = new NBTTagCompound();

        tag.setString("CraftID", craftingID);
        tag.setBoolean("canceled", false);
        tag.setBoolean("done", false);
        tag.setBoolean("standalone", standalone);
        tag.setBoolean("req", req);

        return tag;
    }

    protected void prepareElapsedTime() {
        this.lastTime = System.nanoTime();
        this.elapsedTime = 0;

        final IItemList<IAEItemStack> list = AEApi.instance().storage().createItemList();

        this.getListOfItem(list, CraftingItemList.ACTIVE);
        this.getListOfItem(list, CraftingItemList.PENDING);

        int itemCount = 0;
        for (final IAEItemStack ge : list) {
            itemCount += ge.getStackSize();
        }

        this.startItemCount = itemCount;
        this.remainingItemCount = itemCount;
    }

    protected void storeItems() {
        final IGrid g = this.getGrid();

        if (g == null) {
            return;
        }

        final IStorageGrid sg = g.getCache(IStorageGrid.class);
        final IMEInventory<IAEItemStack> ii = sg.getInventory(AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class));

        for (IAEItemStack is : this.inventory.getItemList()) {
            is = this.inventory.extractItems(
                is.copy(), Actionable.MODULATE, this.getActionSource()
            );

            if (is != null) {
                this.postChange(is, this.getActionSource());
                is = ii.injectItems(is, Actionable.MODULATE, this.getActionSource());
            }

            if (is != null) {
                this.inventory.injectItems(is, Actionable.MODULATE, this.getActionSource());
            }
        }

        if (this.inventory.getItemList().isEmpty()) {
            this.inventory = new MECraftingInventory();
        }

        this.markDirty();
    }

    @Override
    public void cancel() {
        if (this.myLastLink != null) {
            this.myLastLink.cancel();
        }

        final IItemList<IAEItemStack> list;
        this.getListOfItem(
            list = AEApi.instance().storage().createItemList(), CraftingItemList.ALL
        );
        for (final IAEItemStack is : list) {
            this.postChange(is, this.getActionSource());
        }

        this.isComplete = true;
        this.myLastLink = null;
        this.tasks.clear();

        final ImmutableSet<IAEItemStack> items = ImmutableSet.copyOf(this.waitingFor);

        this.waitingFor.resetStatus();

        for (final IAEItemStack is : items) {
            this.postCraftingStatusChange(is);
        }

        this.finalOutput = null;
        this.updateCPU();

        this.storeItems(); // marks dirty
    }

    @Override
    public ICraftingLink submitJob(
        final IGrid g,
        final ICraftingJob job,
        final BaseActionSource src,
        final ICraftingRequester requestingMachine
    ) {
        if (!this.tasks.isEmpty() || !this.waitingFor.isEmpty()) {
            return null;
        }

        if (!(job instanceof CraftingJob)) {
            return null;
        }

        if (this.isBusy() || !this.isActive()
            || this.getAvailableStorage() < job.getByteTotal()) {
            return null;
        }

        final IStorageGrid sg = g.getCache(IStorageGrid.class);
        final IMEInventory<IAEItemStack> storage = sg.getInventory(AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class));
        final MECraftingInventory ci
            = new MECraftingInventory(storage, true, false, false);

        try {
            this.waitingFor.resetStatus();
            ((CraftingJob) job).getTree().setJob(ci, this, src);
            if (ci.commit(src)) {
                this.finalOutput = job.getOutput();
                this.waiting = false;
                this.isComplete = false;
                this.markDirty();

                this.updateCPU();
                final String craftID = this.generateCraftingID();

                this.myLastLink = new CraftingLink(
                    this.generateLinkData(craftID, requestingMachine == null, false), this
                );

                this.prepareElapsedTime();

                if (requestingMachine == null) {
                    return this.myLastLink;
                }

                final ICraftingLink whatLink = new CraftingLink(
                    this.generateLinkData(craftID, false, true), requestingMachine
                );

                this.submitLink(this.myLastLink);
                this.submitLink(whatLink);

                final IItemList<IAEItemStack> list
                    = AEApi.instance().storage().createItemList();
                this.getListOfItem(list, CraftingItemList.ALL);
                for (final IAEItemStack ge : list) {
                    this.postChange(ge, this.getActionSource());
                }

                return whatLink;
            } else {
                this.tasks.clear();
                this.inventory.getItemList().resetStatus();
            }
        } catch (final CraftBranchFailure e) {
            this.tasks.clear();
            this.inventory.getItemList().resetStatus();
            // AELog.error( e );
        }

        return null;
    }

    protected void submitLink(final ICraftingLink myLastLink2) {
        if (this.getGrid() != null) {
            final CraftingGridCache cc = this.getGrid().getCache(ICraftingGrid.class);
            cc.addLink((CraftingLink) myLastLink2);
        }
    }

    public void readFromNBT(final NBTTagCompound data) {
        this.finalOutput = AEItemStack.loadItemStackFromNBT((NBTTagCompound
        ) data.getTag("finalOutput"));
        for (final IAEItemStack ais :
             this.readList((NBTTagList) data.getTag("inventory"))) {
            this.inventory.injectItems(ais, Actionable.MODULATE, this.getActionSource());
        }

        this.waiting = data.getBoolean("waiting");
        this.isComplete = data.getBoolean("isComplete");

        if (data.hasKey("link")) {
            final NBTTagCompound link = data.getCompoundTag("link");
            this.myLastLink = new CraftingLink(link, this);
            this.submitLink(this.myLastLink);
        }

        final NBTTagList list = data.getTagList("tasks", 10);
        for (int x = 0; x < list.tagCount(); x++) {
            final NBTTagCompound item = list.getCompoundTagAt(x);
            final IAEItemStack pattern = AEItemStack.loadItemStackFromNBT(item);
            if (pattern != null && pattern.getItem() instanceof ICraftingPatternItem) {
                final ICraftingPatternItem cpi = (ICraftingPatternItem) pattern.getItem();
                final ICraftingPatternDetails details
                    = cpi.getPatternForItem(pattern.getItemStack(), this.getWorld());
                if (details != null) {
                    final TaskProgress tp = new TaskProgress();
                    tp.value = item.getLong("craftingProgress");
                    this.tasks.put(details, tp);
                }
            }
        }

        this.waitingFor = this.readList((NBTTagList) data.getTag("waitingFor"));
        for (final IAEItemStack is : this.waitingFor) {
            this.postCraftingStatusChange(is.copy());
        }

        this.lastTime = System.nanoTime();
        this.elapsedTime = data.getLong("elapsedTime");
        this.startItemCount = data.getLong("startItemCount");
        this.remainingItemCount = data.getLong("remainingItemCount");
    }

    public void writeToNBT(final NBTTagCompound data) {
        data.setTag("finalOutput", this.writeItem(this.finalOutput));
        data.setTag("inventory", this.writeList(this.inventory.getItemList()));
        data.setBoolean("waiting", this.waiting);
        data.setBoolean("isComplete", this.isComplete);

        if (this.myLastLink != null) {
            final NBTTagCompound link = new NBTTagCompound();
            this.myLastLink.writeToNBT(link);
            data.setTag("link", link);
        }

        final NBTTagList list = new NBTTagList();
        for (final Entry<ICraftingPatternDetails, TaskProgress> e :
             this.tasks.entrySet()) {
            final NBTTagCompound item
                = this.writeItem(AEItemStack.create(e.getKey().getPattern()));
            item.setLong("craftingProgress", e.getValue().value);
            list.appendTag(item);
        }
        data.setTag("tasks", list);

        data.setTag("waitingFor", this.writeList(this.waitingFor));

        data.setLong("elapsedTime", this.getElapsedTime());
        data.setLong("startItemCount", this.getStartItemCount());
        data.setLong("remainingItemCount", this.getRemainingItemCount());
    }

    protected IItemList<IAEItemStack> readList(final NBTTagList tag) {
        final IItemList<IAEItemStack> out = AEApi.instance().storage().createItemList();

        if (tag == null) {
            return out;
        }

        for (int x = 0; x < tag.tagCount(); x++) {
            final IAEItemStack ais
                = AEItemStack.loadItemStackFromNBT(tag.getCompoundTagAt(x));
            if (ais != null) {
                out.add(ais);
            }
        }

        return out;
    }

    protected NBTTagCompound writeItem(final IAEItemStack finalOutput2) {
        final NBTTagCompound out = new NBTTagCompound();

        if (finalOutput2 != null) {
            finalOutput2.writeToNBT(out);
        }

        return out;
    }

    protected NBTTagList writeList(final IItemList<IAEItemStack> myList) {
        final NBTTagList out = new NBTTagList();

        for (final IAEItemStack ais : myList) {
            out.appendTag(this.writeItem(ais));
        }

        return out;
    }

    protected static class TaskProgress { protected long value; }
    
}
