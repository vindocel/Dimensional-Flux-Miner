package dimensionalfluxminer.furnace;

import dimensionalfluxminer.advancement.DfmAdvancements;
import dimensionalfluxminer.machine.AbstractMachineBlockEntity;
import dimensionalfluxminer.machine.ItemQueue;
import dimensionalfluxminer.machine.NeighborTransfer;
import dimensionalfluxminer.machine.QueueItemHandler;
import dimensionalfluxminer.machine.math.RateScale;
import dimensionalfluxminer.machine.math.WorkAccumulator;
import dimensionalfluxminer.machine.side.RelativeFace;
import dimensionalfluxminer.machine.side.SideRoute;
import dimensionalfluxminer.machine.side.SideRules;
import dimensionalfluxminer.machine.side.Transmission;
import dimensionalfluxminer.menu.MachineMenu;
import dimensionalfluxminer.recipe.AlloyRecipeInput;
import dimensionalfluxminer.recipe.AlloyingRecipe;
import dimensionalfluxminer.registry.DfmBlockEntities;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

public class ElectricFurnaceBlockEntity extends AbstractMachineBlockEntity {
    public static final int LANES = 3;
    /** Rate 10: operations of one lane per logical tick (furnace items, or standard alloy executions). */
    public static final int RATE_TEN_OPERATIONS_PER_LANE = 64;
    /** Rate 10: the three furnace lanes together. */
    public static final int RATE_TEN_OPERATIONS_TOTAL = LANES * RATE_TEN_OPERATIONS_PER_LANE;
    /** Cooking time of a vanilla furnace recipe (10 s): the standard recipe of both energy profiles. */
    public static final int STANDARD_COOKING_TICKS = 200;
    /** Every input and output is a single slot of 64 items; a full output stops its lane. */
    public static final int SLOT_CAPACITY = ItemQueue.SLOT_LIMIT;

    public static final int BUTTON_AUTO_SPLIT = 4;
    /** Next mode (furnace only -> alloy + furnace -> alloy only); {@link #BUTTON_MODE_BACK} goes back. */
    public static final int BUTTON_MODE = 5;
    public static final int BUTTON_MODE_BACK = 40;
    /** {@code + lane} */
    public static final int BUTTON_INSERT_LANE = 10;
    /** {@code + lane} (NORMAL outputs 0..2, ALLOY main 3, byproduct 4) */
    public static final int BUTTON_TAKE_OUTPUT = 20;
    /** {@code + lane} */
    public static final int BUTTON_TAKE_INPUT = 30;
    /** Right click: {@code + lane} insert one carried item / take half of the input. */
    public static final int BUTTON_INSERT_ONE_LANE = 50;
    public static final int BUTTON_TAKE_INPUT_HALF = 60;
    /** Right click: {@code + output index} (as {@link #BUTTON_TAKE_OUTPUT}) take half. */
    public static final int BUTTON_TAKE_OUTPUT_HALF = 70;
    /** Creative middle click: {@code + lane} a full stack of the input's item to the cursor. */
    public static final int BUTTON_PICK_INPUT = 80;
    /** Creative middle click: {@code + output index}. */
    public static final int BUTTON_PICK_OUTPUT = 90;
    public static final int BUTTON_SHIFT = 100;
    /** Output slots used by alloy results (main, byproduct). */
    public static final int ALLOY_MAIN_OUTPUT = 0;
    public static final int ALLOY_BYPRODUCT_OUTPUT = 1;

    private final ItemQueue[] inputs = new ItemQueue[LANES];
    private final ItemQueue[] outputs = new ItemQueue[LANES];
    private final WorkAccumulator[] laneWork = new WorkAccumulator[LANES];
    private final WorkAccumulator alloyWork = new WorkAccumulator();
    private final StoredExperience experience = new StoredExperience();
    private final ExperienceFluidHandler experienceHandler = new ExperienceFluidHandler(experience, this::setChanged);

    private ElectricFurnaceMode mode = ElectricFurnaceMode.ALL;
    private boolean autoSplit;

    private final ElectricFurnaceRecipes recipes = new ElectricFurnaceRecipes(LANES);

    /** Ingredients shown inside the chamber (client render data). */
    private List<ItemStack> chamber = List.of();
    private double[] laneProgress = new double[LANES];

    public ElectricFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(DfmBlockEntities.ELECTRIC_FURNACE.get(), pos, state, SideRules.ELECTRIC_FURNACE);
        for (int i = 0; i < LANES; i++) {
            inputs[i] = new ItemQueue(1);
            outputs[i] = new ItemQueue(1);
            laneWork[i] = new WorkAccumulator();
            inputs[i].setOnChange(this::setChanged);
            outputs[i].setOnChange(this::setChanged);
        }
    }

    public ElectricFurnaceMode mode() {
        return mode;
    }

    public ItemQueue input(int lane) {
        return inputs[lane];
    }

    public ItemQueue output(int lane) {
        return outputs[lane];
    }

    /** Alloy main result: first output slot. */
    public ItemQueue alloyMain() {
        return outputs[ALLOY_MAIN_OUTPUT];
    }

    /** Alloy byproduct: second output slot. */
    public ItemQueue alloyByproduct() {
        return outputs[ALLOY_BYPRODUCT_OUTPUT];
    }

    public StoredExperience experience() {
        return experience;
    }

    public boolean autoSplit() {
        return autoSplit;
    }

    public void setAutoSplit(boolean autoSplit) {
        this.autoSplit = autoSplit;
        setChanged();
    }

    @Override
    public int itemProfile() {
        return mode.profile();
    }

    /**
     * FE/t of the selected Rate for standard recipes (200 ticks): the larger of what the profiles the mode can run ask for.
     * The FE really charged follows the work done, one profile per job (see {@link #processNormal} and {@link #processAlloy}).
     */
    @Override
    public long effectivePowerLimit() {
        double alloy = ElectricFurnaceEnergy.alloyStandardPower(rate, hasEfficiencyUpgrade());
        double furnace = mode.canSmelt() ? ElectricFurnaceEnergy.furnaceStandardPower(rate, hasEfficiencyUpgrade()) : 0.0;
        return RateScale.powerLimit(Math.max(alloy, furnace));
    }

    /** Progress of a furnace lane towards its next item (0..1). */
    public double laneWorkProgress(int lane) {
        return laneWork[lane].progress();
    }

    /** Progress of the alloy towards its next execution (0..1). */
    public double alloyWorkProgress() {
        return alloyWork.progress();
    }

    /**
     * The three output slots, shared by every mode: lane {@code n} smelts into output {@code n}; an alloy puts its
     * main result in the first output and its byproduct in the second. A slot holding another item blocks that result until
     * it is emptied.
     */
    private List<ItemQueue> currentOutputs() {
        return List.of(outputs);
    }

    /** Changes the mode; outputs keep their items and stay extractable in every mode. */
    public boolean setMode(ElectricFurnaceMode next) {
        if (next == mode) {
            return false;
        }
        mode = next;
        for (WorkAccumulator w : laneWork) {
            w.reset();
        }
        alloyWork.reset();
        onSidesChanged();
        return true;
    }

    // ---------------------------------------------------------------- recipes

    private Optional<AbstractCookingRecipe> cooking(int lane, ItemStack stack) {
        return recipes.cooking(level, lane, stack);
    }

    /** Blast furnace recipe of the item, otherwise its furnace recipe. */
    private Optional<AbstractCookingRecipe> cooking(ItemStack stack) {
        return recipes.cooking(level, stack);
    }

    private Optional<AlloyingRecipe> alloy(AlloyRecipeInput input) {
        return recipes.alloy(level, input);
    }

    /** Raw -> Ingot of the same material with a single result, the only case eligible for the bulk yield. */
    public static boolean isRawToIngot(ItemStack input, ItemStack result) {
        return ElectricFurnaceRecipes.isRawToIngot(input, result);
    }
    private boolean validForMode(ItemStack stack) {
        if (stack.isEmpty() || stack.is(dimensionalfluxminer.registry.DfmItems.EFFICIENCY_UPGRADE.get())) {
            return false;
        }
        return (mode.canSmelt() && cooking(stack).isPresent()) || (mode.canAlloy() && isAlloyIngredient(stack));
    }

    private boolean isAlloyIngredient(ItemStack stack) {
        return recipes.isAlloyIngredient(level, stack);
    }

    /**
     * ALL mode: an alloy ingredient stays in a single lane (so the combined recipe keeps one lane per ingredient) when
     * it cannot be smelted, or while another lane holds a different ingredient of one of its alloys. A smeltable
     * ingredient with no alloy partner in the lanes is handled as a furnace item, so Auto-Split spreads it.
     */
    private boolean keepForAlloy(ItemStack stack) {
        if (!isAlloyIngredient(stack)) {
            return false;
        }
        if (cooking(stack).isEmpty()) {
            return true;
        }
        for (ItemQueue lane : inputs) {
            ItemStack other = lane.firstPrototype();
            if (!other.isEmpty() && !ItemStack.isSameItemSameComponents(other, stack) && ElectricFurnaceRecipes.alloyPartners(level, stack, other)) {
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- insertion strategies

    /** Dedicated lane insertion (ALLOY ITEM_INPUT_1/2/3 and GUI lane clicks). Bypasses Auto-Split. */
    public ItemStack insertLane(int lane, ItemStack stack, boolean simulate) {
        if (level == null || !validForMode(stack)) {
            return stack;
        }
        return inputs[lane].insert(stack, simulate);
    }

    /** Aggregate insertion (ITEM_INPUT_ALL / ITEM_INPUT_OUTPUT_ALL, Auto-Input, shift-click). */
    public ItemStack insertAggregate(ItemStack stack, boolean simulate) {
        if (level == null || !validForMode(stack)) {
            return stack;
        }
        List<Integer> same = new ArrayList<>();
        List<Integer> empty = new ArrayList<>();
        for (int i = 0; i < LANES; i++) {
            if (inputs[i].isEmpty()) {
                empty.add(i);
            } else if (ItemStack.isSameItemSameComponents(inputs[i].firstPrototype(), stack)) {
                same.add(i);
            }
        }
        if (mode == ElectricFurnaceMode.ALLOY || (mode == ElectricFurnaceMode.ALL && keepForAlloy(stack))) {
            // An alloy ingredient stays in a single lane so the combined recipe keeps one lane per ingredient.
            int lane = !same.isEmpty() ? same.getFirst() : !empty.isEmpty() ? empty.getFirst() : -1;
            return lane < 0 ? stack : inputs[lane].insert(stack, simulate);
        }
        List<Integer> candidates = new ArrayList<>(same);
        candidates.addAll(empty);
        if (candidates.isEmpty()) {
            return stack;
        }
        if (!autoSplit) {
            ItemStack left = stack.copy();
            for (int lane : candidates) {
                left = inputs[lane].insert(left, simulate);
                if (left.isEmpty()) {
                    break;
                }
            }
            return left;
        }
        long[] counts = new long[candidates.size()];
        long[] free = new long[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) {
            ItemQueue q = inputs[candidates.get(i)];
            counts[i] = q.isEmpty() ? 0 : q.firstCount();
            free[i] = q.acceptable(stack);
        }
        boolean pairAware = !hasEfficiencyUpgrade() && cooking(stack)
                .map(r -> isRawToIngot(stack, r.getResultItem(level.registryAccess())))
                .orElse(false);
        long[] add = AutoSplit.distribute(counts, free, stack.getCount(), pairAware);
        long placed = 0;
        for (int i = 0; i < candidates.size(); i++) {
            if (add[i] > 0) {
                placed += inputs[candidates.get(i)].insert(stack, add[i], simulate);
            }
        }
        return placed >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount((int) (stack.getCount() - placed));
    }

    // ---------------------------------------------------------------- processing

    @Override
    protected void logicalCycle(boolean normalCall) {
        super.logicalCycle(normalCall);
        updateChamber();
    }

    @Override
    protected boolean processCycle() {
        rebalanceLanes();
        // ALL: the lanes run an alloy when together they hold one; otherwise every lane smelts on its own.
        if (mode.canAlloy() && (!mode.canSmelt() || alloyMatches())) {
            return processAlloy();
        }
        return processNormal();
    }

    /**
     * Auto-Split ON forces the distribution, whatever put the items in the lanes (GUI, automation or an earlier
     * state): every smeltable kind is spread evenly over the lanes it occupies plus the empty lanes (the empty lanes
     * go to the kind with most items), keeping Raw pairs together without the upgrade. Lanes holding an alloy recipe,
     * and in ALL mode the alloy ingredients kept for an alloy ({@link #keepForAlloy}), are never split.
     */
    private void rebalanceLanes() {
        if (!autoSplit || !mode.canSmelt() || (mode == ElectricFurnaceMode.ALL && alloyMatches())) {
            return;
        }
        List<ItemStack> kinds = new ArrayList<>();
        List<List<Integer>> kindLanes = new ArrayList<>();
        List<Integer> empty = new ArrayList<>();
        for (int lane = 0; lane < LANES; lane++) {
            if (inputs[lane].isEmpty()) {
                empty.add(lane);
                continue;
            }
            ItemStack prototype = inputs[lane].firstPrototype();
            int k = 0;
            while (k < kinds.size() && !ItemStack.isSameItemSameComponents(kinds.get(k), prototype)) {
                k++;
            }
            if (k == kinds.size()) {
                kinds.add(prototype);
                kindLanes.add(new ArrayList<>());
            }
            kindLanes.get(k).add(lane);
        }
        int biggest = -1;
        long biggestTotal = -1;
        for (int k = 0; k < kinds.size(); k++) {
            ItemStack prototype = kinds.get(k);
            if (cooking(prototype).isEmpty() || (mode == ElectricFurnaceMode.ALL && keepForAlloy(prototype))) {
                kindLanes.set(k, List.of());
                continue;
            }
            long total = 0;
            for (int lane : kindLanes.get(k)) {
                total += inputs[lane].firstCount();
            }
            if (total > biggestTotal) {
                biggestTotal = total;
                biggest = k;
            }
        }
        if (biggest >= 0) {
            kindLanes.get(biggest).addAll(empty);
        }
        boolean upgrade = hasEfficiencyUpgrade();
        for (int k = 0; k < kinds.size(); k++) {
            List<Integer> lanes = kindLanes.get(k);
            if (lanes.size() < 2) {
                continue;
            }
            ItemStack prototype = kinds.get(k);
            long[] current = new long[lanes.size()];
            long[] free = new long[lanes.size()];
            long total = 0;
            for (int i = 0; i < lanes.size(); i++) {
                ItemQueue in = inputs[lanes.get(i)];
                current[i] = in.isEmpty() ? 0 : in.firstCount();
                free[i] = ItemQueue.slotLimit(prototype);
                total += current[i];
            }
            boolean pairAware = !upgrade && cooking(prototype)
                    .map(r -> isRawToIngot(prototype, r.getResultItem(level.registryAccess())))
                    .orElse(false);
            long[] target = AutoSplit.distribute(new long[lanes.size()], free, total, pairAware);
            if (java.util.Arrays.equals(target, current) || java.util.Arrays.stream(target).sum() != total) {
                continue;
            }
            for (int i = 0; i < lanes.size(); i++) {
                ItemQueue in = inputs[lanes.get(i)];
                if (current[i] > 0) {
                    in.remove(0, current[i], false);
                }
                if (target[i] > 0) {
                    in.insert(prototype, target[i], false);
                }
            }
        }
    }

    /** True while the lanes run (or wait for) an alloy instead of smelting each lane. */
    private boolean alloyRunning() {
        if (mode == ElectricFurnaceMode.ALLOY) {
            return true;
        }
        return mode == ElectricFurnaceMode.ALL && level != null && alloyMatches();
    }

    private boolean alloyMatches() {
        AlloyRecipeInput input = new AlloyRecipeInput(inputs[0].firstPrototype(), inputs[1].firstPrototype(), inputs[2].firstPrototype());
        if (input.isEmpty()) {
            return false;
        }
        return alloy(input).map(r -> r.laneAssignment(input) != null).orElse(false);
    }

    private boolean processNormal() {
        boolean upgrade = hasEfficiencyUpgrade();
        double energyPerItem = ElectricFurnaceEnergy.furnaceEnergyPerItem(rate, upgrade);
        record LanePlan(int lane, int target, AbstractCookingRecipe recipe, int unitItems, ItemStack unitOutput,
                        double unitsPerTick, long limit) {
        }
        // First pass: what every lane would produce (its result also reserves the output slot under it).
        AbstractCookingRecipe[] recipes = new AbstractCookingRecipe[LANES];
        ItemStack[] units = new ItemStack[LANES];
        int[] unitItems = new int[LANES];
        boolean anyInput = false;
        for (int lane = 0; lane < LANES; lane++) {
            ItemQueue in = inputs[lane];
            laneProgress[lane] = laneWork[lane].progress();
            units[lane] = ItemStack.EMPTY;
            if (in.isEmpty()) {
                continue;
            }
            anyInput = true;
            Optional<AbstractCookingRecipe> found = cooking(lane, in.firstPrototype());
            if (found.isEmpty()) {
                continue;
            }
            ItemStack result = found.get().getResultItem(level.registryAccess());
            if (result.isEmpty()) {
                continue;
            }
            boolean bulk = isRawToIngot(in.firstPrototype(), result);
            int items = 1;
            int unitCount = result.getCount();
            if (bulk && upgrade) {
                unitCount = 2;
            } else if (bulk && in.firstCount() >= 2) {
                items = 2;
                unitCount = 3;
            }
            recipes[lane] = found.get();
            units[lane] = result.copyWithCount(unitCount);
            unitItems[lane] = items;
        }
        // Second pass: pick an output slot per lane, keeping track of what this cycle already planned per slot.
        ElectricFurnaceOutputPlanner planner = new ElectricFurnaceOutputPlanner(outputs, units);
        List<LanePlan> plans = new ArrayList<>();
        boolean blocked = false;
        for (int lane = 0; lane < LANES; lane++) {
            if (recipes[lane] == null) {
                continue;
            }
            ItemStack unit = units[lane];
            int target = planner.choose(lane, unit);
            if (target < 0) {
                blocked = true;
                continue;
            }
            long fit = planner.room(target, unit) / unit.getCount();
            long limit = Math.min(inputs[lane].firstCount() / unitItems[lane], fit);
            planner.reserve(target, unit, limit * unit.getCount());
            double itemsPerTick = ElectricFurnaceEnergy.laneOperationsPerTick(rate, recipes[lane].getCookingTime());
            plans.add(new LanePlan(lane, target, recipes[lane], unitItems[lane], unit, itemsPerTick / unitItems[lane], limit));
        }
        if (plans.isEmpty()) {
            status = blocked ? "output_blocked" : anyInput ? "invalid_input" : "no_input";
            return false;
        }
        // Furnace profile: every item smelted costs the same FE, so only the lanes that really work are billed.
        double demand = 0;
        for (LanePlan p : plans) {
            demand += RateScale.power(p.unitsPerTick * p.unitItems, energyPerItem);
        }
        double available = energyBuffer.available();
        if (available <= 0 || demand <= 0) {
            status = "no_energy";
            return false;
        }
        double fraction = Math.min(1.0, available / demand);
        boolean worked = false;
        for (LanePlan p : plans) {
            WorkAccumulator.Plan plan = laneWork[p.lane].plan(p.unitsPerTick * fraction, p.limit);
            if (plan.operations() > 0) {
                long consumed = plan.operations() * p.unitItems;
                inputs[p.lane].remove(0, consumed, false);
                outputs[p.target].insert(p.unitOutput, plan.operations() * p.unitOutput.getCount(), false);
                // XP follows the base inputs processed; the bulk bonus multiplies items, not XP.
                experience.add(p.recipe.getExperience() * consumed);
            }
            // Billed per input item: a Raw pair (2 items -> 3 ingots) costs two items.
            energy.consume(plan.workDone() * p.unitItems * energyPerItem);
            laneWork[p.lane].commit(plan);
            laneProgress[p.lane] = laneWork[p.lane].progress();
            worked |= plan.workDone() > 0;
        }
        status = fraction < 1.0 ? "low_energy" : "running";
        return worked;
    }

    /** Slag Aggregate turned back into steel ("Nothing Is Lost"). */
    private static boolean recoversSlag(AlloyingRecipe recipe) {
        ItemStack aggregate = new ItemStack(dimensionalfluxminer.registry.DfmItems.SLAG_AGGREGATE.get());
        return recipe.result().is(dimensionalfluxminer.registry.DfmItems.INGOT_STEEL.get())
                && recipe.ingredients().stream().anyMatch(sized -> sized.ingredient().test(aggregate));
    }

    private boolean processAlloy() {
        AlloyRecipeInput input = new AlloyRecipeInput(inputs[0].firstPrototype(), inputs[1].firstPrototype(), inputs[2].firstPrototype());
        if (input.isEmpty()) {
            status = "no_input";
            return false;
        }
        Optional<AlloyingRecipe> found = alloy(input);
        if (found.isEmpty()) {
            status = "invalid_input";
            return false;
        }
        AlloyingRecipe recipe = found.get();
        int[] assignment = recipe.laneAssignment(input);
        if (assignment == null) {
            status = "invalid_input";
            return false;
        }
        long limit = Long.MAX_VALUE;
        for (int i = 0; i < assignment.length; i++) {
            int q = recipe.ingredients().get(i).count();
            limit = Math.min(limit, inputs[assignment[i]].firstCount() / q);
        }
        if (limit <= 0) {
            status = "no_input";
            return false;
        }
        long fit = alloyMain().operationsThatFit(recipe.result());
        if (recipe.byproduct().isPresent()) {
            fit = Math.min(fit, alloyByproduct().operationsThatFit(recipe.byproduct().get().stack()));
        }
        if (fit <= 0) {
            status = "output_blocked";
            return false;
        }
        double opsPerTick = ElectricFurnaceEnergy.alloyExecutionsPerTick(rate, recipe);
        // Alloy profile: an execution costs its units of work, whatever the recipe's duration.
        double energyPerExecution = ElectricFurnaceEnergy.alloyExecutionEnergy(recipe, rate, hasEfficiencyUpgrade());
        double demand = RateScale.power(opsPerTick, energyPerExecution);
        double available = energyBuffer.available();
        if (available <= 0 || demand <= 0) {
            status = "no_energy";
            return false;
        }
        double fraction = Math.min(1.0, available / demand);
        WorkAccumulator.Plan plan = alloyWork.plan(opsPerTick * fraction, Math.min(limit, fit));
        if (plan.operations() > 0) {
            long ops = plan.operations();
            for (int i = 0; i < assignment.length; i++) {
                inputs[assignment[i]].remove(0, ops * recipe.ingredients().get(i).count(), false);
            }
            alloyMain().insert(recipe.result(), ops * recipe.result().getCount(), false);
            recipe.byproduct().ifPresent(b -> {
                long successes = 0;
                for (long op = 0; op < ops; op++) {
                    if (b.chance() >= 1.0F || level.random.nextFloat() < b.chance()) {
                        successes++;
                    }
                }
                if (successes > 0) {
                    alloyByproduct().insert(b.stack(), successes * b.stack().getCount(), false);
                }
            });
            experience.add(recipe.experience() * ops);
            if (recoversSlag(recipe)) {
                reportNearby(DfmAdvancements.SLAG_RECOVERED);
            }
        }
        energy.consume(plan.workDone() * energyPerExecution);
        alloyWork.commit(plan);
        for (int i = 0; i < LANES; i++) {
            laneProgress[i] = alloyWork.progress();
        }
        status = fraction < 1.0 ? "low_energy" : "running";
        return plan.workDone() > 0;
    }

    /**
     * Chamber display policy: visible while a batch is processing; frozen while an active batch waits for energy
     * or is halted by redstone; never created when the output blocks a new batch; hidden without a batch.
     */
    private void updateChamber() {
        List<ItemStack> next;
        if (workedThisTick) {
            next = new ArrayList<>();
            for (ItemQueue in : inputs) {
                if (!in.isEmpty()) {
                    next.add(in.firstPrototype());
                }
            }
        } else if ((redstoneHalt || "no_energy".equals(status) || "low_energy".equals(status)) && hasActiveBatch()) {
            next = chamber;
        } else {
            next = List.of();
        }
        if (!sameStacks(next, chamber)) {
            chamber = List.copyOf(next);
            syncToClients();
        }
    }

    private boolean hasActiveBatch() {
        if (alloyWork.progress() > 0) {
            return true;
        }
        for (WorkAccumulator w : laneWork) {
            if (w.progress() > 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean sameStacks(List<ItemStack> a, List<ItemStack> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!ItemStack.isSameItemSameComponents(a.get(i), b.get(i))) {
                return false;
            }
        }
        return true;
    }

    public List<ItemStack> chamber() {
        return chamber;
    }

    // ---------------------------------------------------------------- automation

    @Override
    protected void autoInputCycle() {
        for (RelativeFace face : RelativeFace.VALUES) {
            SideRoute route = route(face, Transmission.ITEM);
            if (!route.insertsItems()) {
                continue;
            }
            Direction side = face.toDirection(facing());
            int lane = route.dedicatedLane();
            QueueItemHandler.Inserter inserter = lane >= 0 ? (s, sim) -> insertLane(lane, s, sim) : this::insertAggregate;
            NeighborTransfer.pull(level, worldPosition, side, inserter, Long.MAX_VALUE);
        }
    }

    @Override
    protected void autoEjectCycle() {
        for (Direction side : itemSides(SideRoute::extractsItems)) {
            for (ItemQueue out : currentOutputs()) {
                NeighborTransfer.push(level, worldPosition, side, out, Long.MAX_VALUE);
            }
        }
    }

    /** Liquid XP is output-only: it has Auto Eject of its own and no Auto Input. */
    @Override
    protected void autoEjectFluidCycle() {
        for (RelativeFace face : RelativeFace.VALUES) {
            if (route(face, Transmission.EXPERIENCE) != SideRoute.XP_OUTPUT || experience.availableMillibuckets() <= 0) {
                continue;
            }
            Direction side = face.toDirection(facing());
            BlockPos target = worldPosition.relative(side);
            if (!level.isLoaded(target)) {
                continue;
            }
            IFluidHandler destination = level.getCapability(Capabilities.FluidHandler.BLOCK, target, side.getOpposite());
            if (destination != null) {
                FluidUtil.tryFluidTransfer(destination, experienceHandler, Integer.MAX_VALUE, true);
            }
        }
    }

    @Override
    protected IItemHandler createItemHandler(SideRoute route) {
        int lane = route.dedicatedLane();
        List<ItemQueue> in;
        QueueItemHandler.Inserter inserter;
        if (lane >= 0) {
            in = List.of(inputs[lane]);
            inserter = (s, sim) -> insertLane(lane, s, sim);
        } else {
            in = route.insertsItems() ? List.of(inputs) : List.of();
            inserter = this::insertAggregate;
        }
        List<ItemQueue> out = route.extractsItems() ? currentOutputs() : List.of();
        return new QueueItemHandler(in, inserter, out, this::setChanged);
    }

    /** Liquid XP on XP_OUTPUT faces only (Jade shows the stored XP in points through its own provider). */
    public IFluidHandler experienceFor(Direction side) {
        if (side == null || route(side, Transmission.EXPERIENCE) != SideRoute.XP_OUTPUT) {
            return null;
        }
        return experienceHandler;
    }

    // ---------------------------------------------------------------- menu

    @Override
    public boolean handleButton(Player player, int id) {
        if (super.handleButton(player, id)) {
            return true;
        }
        if (id == BUTTON_AUTO_SPLIT) {
            autoSplit = !autoSplit;
            setChanged();
            return true;
        }
        if (id == BUTTON_MODE || id == BUTTON_MODE_BACK) {
            return setMode(mode.cycle(id == BUTTON_MODE));
        }
        AbstractContainerMenu menu = player.containerMenu;
        boolean shift = id >= BUTTON_SHIFT;
        int base = shift ? id - BUTTON_SHIFT : id;
        if (base >= BUTTON_INSERT_LANE && base < BUTTON_INSERT_LANE + LANES) {
            int lane = base - BUTTON_INSERT_LANE;
            return MachineMenu.insertCarried(menu, false, stack -> insertLane(lane, stack, false));
        }
        if (base >= BUTTON_INSERT_ONE_LANE && base < BUTTON_INSERT_ONE_LANE + LANES) {
            int lane = base - BUTTON_INSERT_ONE_LANE;
            return MachineMenu.insertCarried(menu, true, stack -> insertLane(lane, stack, false));
        }
        if (base >= BUTTON_TAKE_INPUT && base < BUTTON_TAKE_INPUT + LANES) {
            return MachineMenu.take(player, inputs[base - BUTTON_TAKE_INPUT], 0, false, shift);
        }
        if (base >= BUTTON_TAKE_INPUT_HALF && base < BUTTON_TAKE_INPUT_HALF + LANES) {
            return MachineMenu.take(player, inputs[base - BUTTON_TAKE_INPUT_HALF], 0, true, shift);
        }
        if (base >= BUTTON_TAKE_OUTPUT && base < BUTTON_TAKE_OUTPUT + LANES) {
            return takeResult(player, outputQueue(base - BUTTON_TAKE_OUTPUT), false, shift);
        }
        if (base >= BUTTON_TAKE_OUTPUT_HALF && base < BUTTON_TAKE_OUTPUT_HALF + LANES) {
            return takeResult(player, outputQueue(base - BUTTON_TAKE_OUTPUT_HALF), true, shift);
        }
        if (id >= BUTTON_PICK_INPUT && id < BUTTON_PICK_INPUT + LANES) {
            return MachineMenu.pick(player, inputs[id - BUTTON_PICK_INPUT], 0);
        }
        if (id >= BUTTON_PICK_OUTPUT && id < BUTTON_PICK_OUTPUT + LANES) {
            return MachineMenu.pick(player, outputQueue(id - BUTTON_PICK_OUTPUT), 0);
        }
        return false;
    }

    /** Taking results by hand also collects the stored experience, as from a vanilla furnace. */
    private boolean takeResult(Player player, ItemQueue queue, boolean half, boolean toInventory) {
        if (!MachineMenu.take(player, queue, 0, half, toInventory)) {
            return false;
        }
        awardExperience(player);
        return true;
    }

    /** Pays out all stored experience as orbs at the player (the fraction of a point is rolled like vanilla). */
    void awardExperience(Player player) {
        if (player == null || !(level instanceof ServerLevel serverLevel) || experience.milli() <= 0) {
            return;
        }
        int xp = StoredExperience.payout(experience.milli(), level.random.nextDouble());
        experience.setMilli(0);
        setChanged();
        if (xp > 0) {
            ExperienceOrb.award(serverLevel, player.position(), xp);
        }
    }

    /** Output by GUI index (0..2). */
    private ItemQueue outputQueue(int index) {
        return outputs[Math.min(index, LANES - 1)];
    }

    @Override
    public void writeMenuData(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeMenuData(tag, registries);
        tag.putString("mode", mode.name());
        tag.putBoolean("auto_split", autoSplit);
        ListTag lanes = new ListTag();
        for (int i = 0; i < LANES; i++) {
            CompoundTag lane = new CompoundTag();
            lane.put("in", inputs[i].save(registries));
            lane.put("out", outputs[i].save(registries));
            lane.putDouble("progress", laneProgress[i]);
            lanes.add(lane);
        }
        tag.put("lanes", lanes);
        tag.putDouble("alloy_progress", alloyWork.progress());
        tag.putBoolean("alloy_running", alloyRunning());
        tag.putLong("xp_milli", experience.milli());
        // Lane speed of the current rate for a standard furnace recipe, shown on the rate tab.
        tag.putDouble("lane_speed", ElectricFurnaceEnergy.laneOperationsPerTick(rate, STANDARD_COOKING_TICKS));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.dimensionalfluxminer.electric_furnace");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ElectricFurnaceMenu(containerId, inventory, this);
    }

    // ---------------------------------------------------------------- client data

    @Override
    protected void writeClientData(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (ItemStack stack : chamber) {
            list.add(stack.save(registries));
        }
        tag.put("chamber", list);
    }

    @Override
    protected void readClientData(CompoundTag tag, HolderLookup.Provider registries) {
        List<ItemStack> list = new ArrayList<>();
        ListTag items = tag.getList("chamber", Tag.TAG_COMPOUND);
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = ItemStack.parseOptional(registries, items.getCompound(i));
            if (!stack.isEmpty()) {
                list.add(stack);
            }
        }
        chamber = List.copyOf(list);
    }

    // ---------------------------------------------------------------- persistence

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("mode", mode.name());
        tag.putBoolean("auto_split", autoSplit);
        for (int i = 0; i < LANES; i++) {
            tag.put("in" + i, inputs[i].save(registries));
            tag.put("out" + i, outputs[i].save(registries));
            // Progress already paid for survives a save and reload.
            tag.putDouble("lane_progress" + i, laneWork[i].progress());
        }
        tag.putDouble("alloy_progress", alloyWork.progress());
        tag.putLong("xp_milli", experience.milli());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        try {
            mode = ElectricFurnaceMode.valueOf(tag.getString("mode"));
        } catch (IllegalArgumentException e) {
            mode = ElectricFurnaceMode.ALL;
        }
        autoSplit = tag.getBoolean("auto_split");
        for (int i = 0; i < LANES; i++) {
            inputs[i].load(tag.getList("in" + i, Tag.TAG_COMPOUND), registries);
            outputs[i].load(tag.getList("out" + i, Tag.TAG_COMPOUND), registries);
            laneWork[i].setProgress(tag.getDouble("lane_progress" + i));
            laneProgress[i] = laneWork[i].progress();
        }
        alloyWork.setProgress(tag.getDouble("alloy_progress"));
        // Saves from before the shared outputs kept alloy results apart: move them into the output slots.
        mergeLegacyOutput(tag.getList("alloy_main", Tag.TAG_COMPOUND), registries);
        mergeLegacyOutput(tag.getList("alloy_byproduct", Tag.TAG_COMPOUND), registries);
        experience.setMilli(tag.getLong("xp_milli"));
    }

    /** Items of old separate alloy outputs that found no room in the output slots; dropped with the block. */
    private final List<ItemStack> legacyOverflow = new ArrayList<>();

    private void mergeLegacyOutput(ListTag list, HolderLookup.Provider registries) {
        ItemQueue legacy = new ItemQueue(1);
        legacy.load(list, registries);
        for (ItemStack stack : legacy.toStacks()) {
            ItemStack left = stack;
            for (ItemQueue out : outputs) {
                left = out.insert(left, false);
                if (left.isEmpty()) {
                    break;
                }
            }
            if (!left.isEmpty()) {
                legacyOverflow.add(left);
            }
        }
    }

    @Override
    protected void collectDrops(List<ItemStack> drops) {
        for (int i = 0; i < LANES; i++) {
            drops.addAll(inputs[i].toStacks());
            drops.addAll(outputs[i].toStacks());
        }
        drops.addAll(legacyOverflow);
    }

    @Override
    public void dropContents() {
        super.dropContents();
        if (level instanceof ServerLevel serverLevel) {
            int xp = StoredExperience.payout(experience.milli(), level.random.nextDouble());
            if (xp > 0) {
                // Vanilla splits the award into a bounded number of orbs, never one entity per point.
                ExperienceOrb.award(serverLevel, Vec3.atCenterOf(worldPosition), xp);
            }
            experience.setMilli(0);
        }
    }
}
