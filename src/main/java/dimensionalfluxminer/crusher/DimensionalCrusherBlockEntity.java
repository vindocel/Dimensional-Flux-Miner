package dimensionalfluxminer.crusher;

import dimensionalfluxminer.DfmConfig;
import dimensionalfluxminer.advancement.DfmAdvancements;
import dimensionalfluxminer.machine.AbstractMachineBlockEntity;
import dimensionalfluxminer.machine.ThroughputStreak;
import dimensionalfluxminer.machine.ItemQueue;
import dimensionalfluxminer.menu.MachineMenu;
import dimensionalfluxminer.machine.NeighborTransfer;
import dimensionalfluxminer.machine.QueueItemHandler;
import dimensionalfluxminer.machine.math.RateScale;
import dimensionalfluxminer.machine.math.WorkAccumulator;
import dimensionalfluxminer.machine.side.RelativeFace;
import dimensionalfluxminer.machine.side.SideRoute;
import dimensionalfluxminer.machine.side.SideRules;
import dimensionalfluxminer.machine.side.Transmission;
import dimensionalfluxminer.registry.DfmBlockEntities;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.IItemHandler;

public class DimensionalCrusherBlockEntity extends AbstractMachineBlockEntity {
    /** Throughput endpoints: Rate 1 = 1 operation / 200 ticks, Rate 10 = 64 operations per logical tick. */
    public static final double MIN_OPERATIONS_PER_TICK = 0.005;
    public static final double MAX_OPERATIONS_PER_TICK = 64.0;
    /** One input slot and four output slots (one item kind each), 64 items per slot. */
    public static final int SLOT_CAPACITY = ItemQueue.SLOT_LIMIT;
    public static final int OUTPUT_KINDS = 4;

    /** Left click with an item: insert the carried stack; right click: insert one item. */
    public static final int BUTTON_INSERT_CARRIED = 10;
    public static final int BUTTON_INSERT_ONE = 11;
    /** {@code BUTTON_TAKE_OUTPUT + kind}, add {@link #BUTTON_SHIFT} to send to the inventory. */
    public static final int BUTTON_TAKE_OUTPUT = 20;
    public static final int BUTTON_TAKE_INPUT = 30;
    /** Right click with an empty hand: take half (rounded up). */
    public static final int BUTTON_TAKE_INPUT_HALF = 31;
    /** {@code BUTTON_TAKE_OUTPUT_HALF + kind} */
    public static final int BUTTON_TAKE_OUTPUT_HALF = 50;
    /** Creative middle click: a full stack of the input's item to the cursor. */
    public static final int BUTTON_PICK_INPUT = 32;
    /** {@code BUTTON_PICK_OUTPUT + kind}, creative middle click on an output. */
    public static final int BUTTON_PICK_OUTPUT = 60;
    public static final int BUTTON_SHIFT = 100;

    private final ItemQueue input = new ItemQueue(1);
    private final ItemQueue output = new ItemQueue(OUTPUT_KINDS);
    private final WorkAccumulator work = new WorkAccumulator();
    private final ThroughputStreak throughput = new ThroughputStreak(1000.0, 5);

    private ItemStack cachedInput = ItemStack.EMPTY;
    private boolean cachedUpgrade;
    private RecipeManager cachedRecipes;
    private Optional<CrusherProcessing.Resolution> cachedResolution = Optional.empty();

    /** Item currently being ground; synced to clients only for the grinding particles. */
    private ItemStack visualItem = ItemStack.EMPTY;

    public DimensionalCrusherBlockEntity(BlockPos pos, BlockState state) {
        super(DfmBlockEntities.DIMENSIONAL_CRUSHER.get(), pos, state, SideRules.CRUSHER);
        input.setOnChange(this::setChanged);
        output.setOnChange(this::setChanged);
    }

    public ItemQueue input() {
        return input;
    }

    public ItemQueue output() {
        return output;
    }

    @Override
    public long effectivePowerLimit() {
        double[] range = DfmConfig.range(DfmConfig.CRUSHER_MIN_FE, DfmConfig.CRUSHER_MAX_FE);
        return RateScale.powerLimit(rate, range[0], range[1], hasEfficiencyUpgrade() ? 0.5 : 1.0);
    }

    public double targetOperationsPerTick() {
        return RateScale.value(rate, MIN_OPERATIONS_PER_TICK, MAX_OPERATIONS_PER_TICK);
    }

    public Optional<CrusherProcessing.Resolution> resolve(ItemStack stack) {
        if (level == null || stack.isEmpty()) {
            return Optional.empty();
        }
        boolean upgrade = hasEfficiencyUpgrade();
        RecipeManager recipes = level.getRecipeManager();
        if (!ItemStack.isSameItemSameComponents(stack, cachedInput) || upgrade != cachedUpgrade || recipes != cachedRecipes) {
            cachedInput = stack.copyWithCount(1);
            cachedUpgrade = upgrade;
            cachedRecipes = recipes;
            cachedResolution = CrusherProcessing.resolve(level, cachedInput, upgrade);
        }
        return cachedResolution;
    }

    /** Insertion strategy shared by pipes, Auto-Input, top pickup and the GUI: only valid process inputs. */
    public ItemStack insertInput(ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || resolve(stack).isEmpty()) {
            return stack;
        }
        return input.insert(stack, simulate);
    }

    @Override
    protected boolean processCycle() {
        if (input.isEmpty()) {
            status = "no_input";
            setVisual(ItemStack.EMPTY);
            return false;
        }
        ItemStack in = input.firstPrototype();
        Optional<CrusherProcessing.Resolution> resolution = resolve(in);
        if (resolution.isEmpty()) {
            status = "invalid_input";
            setVisual(ItemStack.EMPTY);
            return false;
        }
        ItemStack perOperation = resolution.get().result();
        long fit = output.operationsThatFit(perOperation);
        if (fit <= 0) {
            status = "output_blocked";
            setVisual(ItemStack.EMPTY);
            return false;
        }
        double power = effectivePowerLimit();
        double rateTarget = targetOperationsPerTick();
        long stored = energyBuffer.stored();
        if (stored <= 0 || power <= 0) {
            status = "no_energy";
            return false;
        }
        double energyFraction = Math.min(1.0, stored / power);
        double energyPerOperation = power / rateTarget;
        WorkAccumulator.Plan plan = work.plan(rateTarget * energyFraction, Math.min(input.firstCount(), fit));
        if (plan.operations() > 0) {
            input.remove(0, plan.operations(), false);
            output.insert(perOperation, plan.operations() * perOperation.getCount(), false);
        }
        // Energy follows the work really performed: a bottleneck never bills the full Rate.
        energy.consume(plan.workDone() * energyPerOperation);
        work.commit(plan);
        if (level != null && throughput.record(level.getGameTime(), plan.operations())) {
            reportNearby(DfmAdvancements.THOUSAND_PER_SECOND);
        }
        boolean worked = plan.workDone() > 0;
        status = energyFraction < 1.0 ? "low_energy" : "running";
        setVisual(worked ? in : ItemStack.EMPTY);
        return worked;
    }

    @Override
    protected void logicalCycle(boolean normalCall) {
        pickUpItemsAbove();
        super.logicalCycle(normalCall);
    }

    /** The top face works as an internal funnel when its ITEM route accepts insertion. */
    private void pickUpItemsAbove() {
        if (level == null || !route(RelativeFace.UP, Transmission.ITEM).insertsItems()) {
            return;
        }
        AABB area = new AABB(worldPosition.getX(), worldPosition.getY() + 1.0, worldPosition.getZ(),
                worldPosition.getX() + 1.0, worldPosition.getY() + 1.5, worldPosition.getZ() + 1.0);
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, area, e -> e.isAlive() && !e.getItem().isEmpty())) {
            ItemStack stack = entity.getItem();
            ItemStack remainder = insertInput(stack.copy(), false);
            if (remainder.getCount() != stack.getCount()) {
                if (remainder.isEmpty()) {
                    entity.discard();
                } else {
                    entity.setItem(remainder);
                }
            }
        }
    }

    @Override
    protected void autoInputCycle() {
        if (level == null) {
            return;
        }
        for (Direction side : itemSides(SideRoute::insertsItems)) {
            NeighborTransfer.pull(level, worldPosition, side, this::insertInput, input.free());
        }
    }

    @Override
    protected void autoEjectCycle() {
        if (level == null) {
            return;
        }
        for (Direction side : itemSides(SideRoute::extractsItems)) {
            NeighborTransfer.push(level, worldPosition, side, output, Long.MAX_VALUE);
        }
    }

    @Override
    protected IItemHandler createItemHandler(SideRoute route) {
        List<ItemQueue> inputs = route.insertsItems() ? List.of(input) : List.of();
        List<ItemQueue> outputs = route.extractsItems() ? List.of(output) : List.of();
        return new QueueItemHandler(inputs, this::insertInput, outputs, this::setChanged);
    }

    private void setVisual(ItemStack stack) {
        if (!ItemStack.isSameItemSameComponents(stack, visualItem)) {
            visualItem = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
            syncToClients();
        }
    }

    @Override
    protected void clientEffects() {
        if (level == null || visualItem.isEmpty() || machineState() != dimensionalfluxminer.content.MachineState.ACTIVE) {
            return;
        }
        // Bounded visual budget, independent from the logical throughput.
        if (level.random.nextInt(3) != 0) {
            return;
        }
        Direction front = facing();
        double x = worldPosition.getX() + 0.5 + front.getStepX() * 0.3 + (level.random.nextDouble() - 0.5) * 0.4;
        double y = worldPosition.getY() + 0.75 + level.random.nextDouble() * 0.2;
        double z = worldPosition.getZ() + 0.5 + front.getStepZ() * 0.3 + (level.random.nextDouble() - 0.5) * 0.4;
        level.addParticle(new ItemParticleOption(ParticleTypes.ITEM, visualItem), x, y, z,
                (level.random.nextDouble() - 0.5) * 0.08, 0.06, (level.random.nextDouble() - 0.5) * 0.08);
    }

    public ItemStack visualItem() {
        return visualItem;
    }

    @Override
    protected void writeClientData(CompoundTag tag, HolderLookup.Provider registries) {
        if (!visualItem.isEmpty()) {
            tag.put("visual", visualItem.save(registries));
        }
    }

    @Override
    protected void readClientData(CompoundTag tag, HolderLookup.Provider registries) {
        visualItem = tag.contains("visual") ? ItemStack.parseOptional(registries, tag.getCompound("visual")) : ItemStack.EMPTY;
    }

    // ---------------------------------------------------------------- menu

    @Override
    public boolean handleButton(Player player, int id) {
        if (super.handleButton(player, id)) {
            return true;
        }
        AbstractContainerMenu menu = player.containerMenu;
        boolean shift = id >= BUTTON_SHIFT;
        int base = shift ? id - BUTTON_SHIFT : id;
        if (base == BUTTON_INSERT_CARRIED || base == BUTTON_INSERT_ONE) {
            return MachineMenu.insertCarried(menu, base == BUTTON_INSERT_ONE, stack -> insertInput(stack, false));
        }
        if (base >= BUTTON_TAKE_OUTPUT && base < BUTTON_TAKE_OUTPUT + OUTPUT_KINDS) {
            return MachineMenu.take(player, output, base - BUTTON_TAKE_OUTPUT, false, shift);
        }
        if (base >= BUTTON_TAKE_OUTPUT_HALF && base < BUTTON_TAKE_OUTPUT_HALF + OUTPUT_KINDS) {
            return MachineMenu.take(player, output, base - BUTTON_TAKE_OUTPUT_HALF, true, shift);
        }
        if (base == BUTTON_TAKE_INPUT || base == BUTTON_TAKE_INPUT_HALF) {
            return MachineMenu.take(player, input, 0, base == BUTTON_TAKE_INPUT_HALF, shift);
        }
        if (id == BUTTON_PICK_INPUT) {
            return MachineMenu.pick(player, input, 0);
        }
        if (id >= BUTTON_PICK_OUTPUT && id < BUTTON_PICK_OUTPUT + OUTPUT_KINDS) {
            return MachineMenu.pick(player, output, id - BUTTON_PICK_OUTPUT);
        }
        return false;
    }

    @Override
    public void writeMenuData(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeMenuData(tag, registries);
        tag.put("input", input.save(registries));
        tag.put("output", output.save(registries));
        tag.putDouble("progress", work.progress());
        tag.putDouble("ops", targetOperationsPerTick());
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.dimensionalfluxminer.dimensional_crusher");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new CrusherMenu(containerId, inventory, this);
    }

    // ---------------------------------------------------------------- persistence

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("input", input.save(registries));
        tag.put("output", output.save(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        input.load(tag.getList("input", Tag.TAG_COMPOUND), registries);
        output.load(tag.getList("output", Tag.TAG_COMPOUND), registries);
    }

    @Override
    protected void collectDrops(List<ItemStack> drops) {
        drops.addAll(input.toStacks());
        drops.addAll(output.toStacks());
    }
}
