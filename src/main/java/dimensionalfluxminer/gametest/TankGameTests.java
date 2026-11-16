package dimensionalfluxminer.gametest;

//? if =26.1.2 {
import net.minecraft.world.entity.EntityType;
import dimensionalfluxminer.machine.transfer.FluidTanks;
import dimensionalfluxminer.machine.transfer.Transfer;
//?} else if >=26.3 {
/*import net.minecraft.world.entity.EntityTypes;
import dimensionalfluxminer.machine.transfer.FluidTanks;
import dimensionalfluxminer.machine.transfer.Transfer;
*///?}
import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.machine.AbstractMachineBlockEntity;
import dimensionalfluxminer.machine.side.RelativeFace;
import dimensionalfluxminer.machine.side.SideRoute;
import dimensionalfluxminer.registry.DfmBlocks;
import dimensionalfluxminer.registry.DfmDataComponents;
import dimensionalfluxminer.tank.DimensionalTankBlockEntity;
import dimensionalfluxminer.tank.TankChannels;
import dimensionalfluxminer.tank.TankCode;
import dimensionalfluxminer.tank.TankKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
//? if <26.1 {
/*import net.minecraft.gametest.framework.GameTest;
*///?}
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
//? if <26.1 {
/*import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

/**
 * Dimensional Tanks. Channels belong to the whole save and the tests run together, so every test uses a code of its
 * own (only the first one reads the default white code).
 */
//? if <26.1 {
/*@GameTestHolder(DimensionalFluxMiner.MOD_ID)
@PrefixGameTestTemplate(false)
*///?}
public final class TankGameTests {
    private static final BlockPos FIRST = new BlockPos(1, 1, 1);
    private static final BlockPos SECOND = new BlockPos(3, 1, 3);
    private static final int BUCKET = 1000;

    private TankGameTests() {
    }

    //? if <26.1 {
    /*private static DimensionalTankBlockEntity place(GameTestHelper helper, BlockPos pos, TankKind kind, Direction facing, TankCode code) {
        Block block = kind == TankKind.DUAL ? DfmBlocks.DIMENSIONAL_DUAL_TANK.get() : DfmBlocks.DIMENSIONAL_TANK.get();
        helper.setBlock(pos, block.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing));
        DimensionalTankBlockEntity be = helper.getBlockEntity(pos);
        if (code != null) {
            be.setCode(code);
            // A test world kept from an earlier run may still hold liquid in this channel.
            TankChannels.Channel channel = be.channel();
            for (int i = 0; i < channel.chambers(); i++) {
                channel.set(i, FluidStack.EMPTY);
            }
        }
        return be;
    }

    private static DimensionalTankBlockEntity place(GameTestHelper helper, BlockPos pos, TankKind kind, TankCode code) {
        return place(helper, pos, kind, Direction.NORTH, code);
    }

    private static IFluidHandler face(GameTestHelper helper, DimensionalTankBlockEntity be, RelativeFace face) {
        return helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, be.getBlockPos(), face.toDirection(be.facing()));
    }

    private static FluidStack water(int amount) {
        return new FluidStack(Fluids.WATER, amount);
    }

    private static FluidStack lava(int amount) {
        return new FluidStack(Fluids.LAVA, amount);
    }

    private static TankCode code(DyeColor a, DyeColor b, DyeColor c) {
        return new TankCode(a, b, c);
    }

    private static IFluidHandler itemFluids(net.minecraft.world.item.ItemStack stack) {
        return stack.getCapability(Capabilities.FluidHandler.ITEM);
    }

    private static void load(DimensionalTankBlockEntity be, CompoundTag tag, GameTestHelper helper) {
        be.loadWithComponents(tag, helper.getLevel().registryAccess());
    }

    private static net.neoforged.neoforge.items.IItemHandler items(GameTestHelper helper, BlockPos absolute, Direction side) {
        return helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, absolute, side);
    }

    private static TankCode uniqueCode(DyeColor a, DyeColor b, DyeColor c) {
        return new TankCode(a, b, c, java.util.Optional.of(java.util.UUID.randomUUID()), java.util.Optional.empty());
    }

    *///?}
    @GameTest(template = "empty_small")
    public static void tankItemDisplayFollowsChannelWithoutOwningFluid(GameTestHelper helper) {
        //? if >=26.1 {
        var code = new TankCode(DyeColor.LIME, DyeColor.RED, DyeColor.BLUE,
                java.util.Optional.of(java.util.UUID.randomUUID()), java.util.Optional.empty());
        //?} else {
        /*var code = uniqueCode(DyeColor.LIME, DyeColor.RED, DyeColor.BLUE);
        *///?}
        var stack = new net.minecraft.world.item.ItemStack(DfmBlocks.DIMENSIONAL_TANK.get());
        stack.set(DfmDataComponents.TANK_CODE.get(), code);
        var channel = TankChannels.get(helper.getLevel().getServer()).channel(TankKind.SINGLE, code);
        channel.set(0, water(30000));
        dimensionalfluxminer.tank.TankItemDisplay.update(stack, helper.getLevel().getServer());
        helper.assertTrue(dimensionalfluxminer.tank.TankItemDisplay.fluid(stack, TankKind.SINGLE, 0).getAmount() == 30000,
                "item reading preserves the finite fill amount");
        stack.set(DfmDataComponents.TANK_DISPLAY_A.get(), net.neoforged.neoforge.fluids.SimpleFluidContent.copyOf(lava(128000)));
        helper.assertTrue(itemFluids(stack).getFluidInTank(0).is(Fluids.WATER)
                && itemFluids(stack).getFluidInTank(0).getAmount() == 30000, "a display snapshot is never fluid storage");
        channel.set(0, water(1000));
        dimensionalfluxminer.tank.TankItemDisplay.update(stack, helper.getLevel().getServer());
        helper.assertTrue(dimensionalfluxminer.tank.TankItemDisplay.fluid(stack, TankKind.SINGLE, 0).getAmount() == 1000,
                "channel changes refresh an existing item");
        channel.set(0, FluidStack.EMPTY);
        dimensionalfluxminer.tank.TankItemDisplay.update(stack, helper.getLevel().getServer());
        helper.assertTrue(!stack.has(DfmDataComponents.TANK_DISPLAY_A.get()), "empty channels clear stale readings");
        helper.succeed();
    }

    @GameTest(template = "empty_small")
    public static void tankItemDisplaySelectsLocalAndSharedChambers(GameTestHelper helper) {
        //? if >=26.1 {
        var code = new TankCode(DyeColor.PURPLE, DyeColor.BLUE, DyeColor.LIME,
                java.util.Optional.of(java.util.UUID.randomUUID()), java.util.Optional.empty());
        //?} else {
        /*var code = uniqueCode(DyeColor.PURPLE, DyeColor.BLUE, DyeColor.LIME);
        *///?}
        var stack = new net.minecraft.world.item.ItemStack(DfmBlocks.DIMENSIONAL_DUAL_TANK.get());
        stack.set(DfmDataComponents.TANK_CODE.get(), code);
        stack.set(DfmDataComponents.TANK_LOCAL_A.get(), net.neoforged.neoforge.fluids.SimpleFluidContent.copyOf(lava(50000)));
        stack.set(DfmDataComponents.TANK_LOCAL_B.get(), net.neoforged.neoforge.fluids.SimpleFluidContent.copyOf(water(30000)));
        var channel = TankChannels.get(helper.getLevel().getServer()).channel(TankKind.DUAL, code);
        channel.set(0, water(90000));
        channel.set(1, lava(10000));
        dimensionalfluxminer.tank.TankItemDisplay.update(stack, helper.getLevel().getServer());
        helper.assertTrue(dimensionalfluxminer.tank.TankItemDisplay.fluid(stack, TankKind.DUAL, 0).getAmount() == 90000,
                "open mode displays the shared A chamber");
        stack.set(DfmDataComponents.TANK_CHANNEL_LOCKED.get(), true);
        dimensionalfluxminer.tank.TankItemDisplay.update(stack, helper.getLevel().getServer());
        helper.assertTrue(dimensionalfluxminer.tank.TankItemDisplay.fluid(stack, TankKind.DUAL, 0).is(Fluids.LAVA)
                && dimensionalfluxminer.tank.TankItemDisplay.fluid(stack, TankKind.DUAL, 0).getAmount() == 50000,
                "locked mode displays the local A chamber");
        helper.assertTrue(dimensionalfluxminer.tank.TankItemDisplay.fluid(stack, TankKind.DUAL, 1).getAmount() == 30000,
                "local B has its own fill height");
        stack.remove(DfmDataComponents.TANK_CHANNEL_LOCKED.get());
        dimensionalfluxminer.tank.TankItemDisplay.update(stack, helper.getLevel().getServer());
        helper.assertTrue(dimensionalfluxminer.tank.TankItemDisplay.fluid(stack, TankKind.DUAL, 1).getAmount() == 10000,
                "reconnection displays shared B without moving the local reserve");
        helper.assertTrue(stack.get(DfmDataComponents.TANK_LOCAL_A.get()).copy().getAmount() == 50000, "inactive local fluid is untouched");
        helper.succeed();
    }

    /** New single and dual tank items come with the channel locked; a saved tank without the key (older worlds) stays open. */
    @GameTest(template = "empty_small")
    public static void newTanksStartWithTheChannelLocked(GameTestHelper helper) {
        for (TankKind kind : new TankKind[] {TankKind.SINGLE, TankKind.DUAL}) {
            Block block = kind == TankKind.DUAL ? DfmBlocks.DIMENSIONAL_DUAL_TANK.get() : DfmBlocks.DIMENSIONAL_TANK.get();
            var fresh = new net.minecraft.world.item.ItemStack(block);
            helper.assertTrue(dimensionalfluxminer.tank.DimensionalTankItem.channelLocked(fresh), kind + " item starts locked");
            var be = place(helper, kind == TankKind.SINGLE ? FIRST : SECOND, kind, null);
            helper.assertTrue(!be.channelLocked(), kind + " block without the saved key stays open");
            be.applyComponentsFromItemStack(fresh);
            helper.assertTrue(be.channelLocked(), kind + " placed from a new item is locked");
            var carried = new net.minecraft.world.item.ItemStack(block);
            carried.set(DfmDataComponents.TANK_CODE.get(), TankCode.DEFAULT);
            helper.assertTrue(!dimensionalfluxminer.tank.DimensionalTankItem.channelLocked(carried), kind + " item with a code keeps the open mode");
        }
        helper.succeed();
    }

    @GameTest(template = "empty_small")
    public static void singleTankCanDisconnectAndPreserveBothStores(GameTestHelper helper) {
        localRoundTrip(helper, TankKind.SINGLE);
    }

    @GameTest(template = "empty_small")
    public static void dualTankCanDisconnectAndPreserveBothStores(GameTestHelper helper) {
        localRoundTrip(helper, TankKind.DUAL);
    }

    private static void localRoundTrip(GameTestHelper helper, TankKind kind) {
        //? if >=26.1 {
        TankCode code = new TankCode(DyeColor.RED, DyeColor.BLUE, DyeColor.GREEN,
                java.util.Optional.of(java.util.UUID.randomUUID()), java.util.Optional.empty());
        //?} else {
        /*TankCode code = uniqueCode(DyeColor.RED, DyeColor.BLUE, DyeColor.GREEN);
        *///?}
        var tank = place(helper, FIRST, kind, code);
        var peer = place(helper, SECOND, kind, null);
        peer.setCode(code);
        var retained = face(helper, tank, RelativeFace.RIGHT);
        //? if >=26.1 {
        retained.fill(water(3000), FluidTanks.FluidAction.EXECUTE);
        //?} else {
        /*retained.fill(water(3000), IFluidHandler.FluidAction.EXECUTE);
        *///?}
        if (kind == TankKind.DUAL) {
            //? if >=26.1 {
            tank.chamberHandler(1).fill(water(4000), FluidTanks.FluidAction.EXECUTE);
            //?} else {
            /*tank.chamberHandler(1).fill(water(4000), IFluidHandler.FluidAction.EXECUTE);
            *///?}
        }
        helper.assertTrue(!tank.channelLocked() && tank.setChannelLocked(true), "old and new tanks default to shared storage");
        helper.assertTrue(tank.chamber(0).isEmpty() && peer.chamber(0).getAmount() == 3000, "disconnecting does not copy or drain shared fluid");
        //? if >=26.1 {
        retained.fill(lava(2000), FluidTanks.FluidAction.EXECUTE);
        //?} else {
        /*retained.fill(lava(2000), IFluidHandler.FluidAction.EXECUTE);
        *///?}
        helper.assertTrue(tank.chamber(0).is(Fluids.LAVA) && peer.chamber(0).is(Fluids.WATER), "retained handlers follow the active storage");
        if (kind == TankKind.DUAL) {
            //? if >=26.1 {
            tank.chamberHandler(1).fill(lava(5000), FluidTanks.FluidAction.EXECUTE);
            //?} else {
            /*tank.chamberHandler(1).fill(lava(5000), IFluidHandler.FluidAction.EXECUTE);
            *///?}
        }
        helper.assertTrue(!tank.handleButton(null, DimensionalTankBlockEntity.BUTTON_CODE_BASE + DyeColor.BLACK.getId())
                && !tank.handleButton(null, DimensionalTankBlockEntity.BUTTON_PERSONAL), "server rejects channel changes while locked");
        tank.setCode(TankCode.DEFAULT);
        helper.assertTrue(tank.code().equals(code), "direct code changes are also rejected");
        //? if >=26.1 {
        var exposed = Transfer.exposeFluids(retained, tank.journal());
        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            exposed.extract(0, net.neoforged.neoforge.transfer.fluid.FluidResource.of(tank.chamber(0)), 700, transaction);
        }
        helper.assertTrue(tank.chamber(0).getAmount() == 2000, "cancelled local fluid transfers roll back");
        //?} else {
        /*helper.assertTrue(retained.drain(700, IFluidHandler.FluidAction.SIMULATE).getAmount() == 700
                && tank.chamber(0).getAmount() == 2000, "simulated local fluid transfers change nothing");
        *///?}
        var saved = tank.saveWithoutMetadata(helper.getLevel().registryAccess());
        //? if >=26.1 {
        TestSupport.load(tank, saved, helper.getLevel().registryAccess());
        //?} else {
        /*load(tank, saved, helper);
        *///?}
        helper.assertTrue(tank.channelLocked() && tank.chamber(0).getAmount() == 2000, "local mode and contents persist");
        tank.setChannelLocked(false);
        helper.assertTrue(tank.chamber(0).is(Fluids.WATER) && tank.chamber(0).getAmount() == 3000, "reconnecting restores the shared channel");
        saved = tank.saveWithoutMetadata(helper.getLevel().registryAccess());
        //? if >=26.1 {
        TestSupport.load(tank, saved, helper.getLevel().registryAccess());
        //?} else {
        /*load(tank, saved, helper);
        *///?}
        helper.getLevel().destroyBlock(tank.getBlockPos(), true);
        var drops = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(tank.getBlockPos()).inflate(1));
        var drop = drops.stream().map(net.minecraft.world.entity.item.ItemEntity::getItem)
                .filter(stack -> stack.getItem() instanceof dimensionalfluxminer.tank.DimensionalTankItem).findFirst().orElseThrow();
        helper.assertTrue(drop.get(DfmDataComponents.TANK_LOCAL_A.get()).copy().getAmount() == 2000,
                "loot keeps inactive local fluid while the shared store stays in the channel");
        helper.assertTrue(dimensionalfluxminer.tank.TankItemDisplay.fluid(drop, kind, 0).getAmount() == 3000,
                "the dropped item immediately carries the shared display reading");
        var replaced = place(helper, FIRST, kind, null);
        replaced.applyComponentsFromItemStack(drop);
        helper.assertTrue(!replaced.channelLocked() && replaced.chamber(0).getAmount() == 3000, "replacement preserves unlocked mode");
        replaced.setChannelLocked(true);
        helper.assertTrue(replaced.chamber(0).is(Fluids.LAVA) && replaced.chamber(0).getAmount() == 2000, "replacement retains its independent reserve");
        if (kind == TankKind.DUAL) {
            helper.assertTrue(replaced.chamber(1).is(Fluids.LAVA) && replaced.chamber(1).getAmount() == 5000
                    && peer.chamber(1).is(Fluids.WATER) && peer.chamber(1).getAmount() == 4000, "both chambers remain separate across replacement");
        }
        helper.succeed();
    }

    @GameTest(template = "empty_small")
    public static void lockedTankItemTransfersOnlyItsLocalFluid(GameTestHelper helper) {
        var stack = new net.minecraft.world.item.ItemStack(DfmBlocks.DIMENSIONAL_TANK.get());
        stack.set(DfmDataComponents.TANK_CHANNEL_LOCKED.get(), true);
        stack.set(DfmDataComponents.TANK_LOCAL_A.get(), net.neoforged.neoforge.fluids.SimpleFluidContent.copyOf(water(3000)));
        //? if >=26.1 {
        var handler = net.neoforged.neoforge.transfer.access.ItemAccess.forStack(stack).getCapability(Capabilities.Fluid.ITEM);
        var waterResource = net.neoforged.neoforge.transfer.fluid.FluidResource.of(water(1));
        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            helper.assertTrue(handler.extract(0, waterResource, 1000, transaction) == 1000, "local item can be drained");
        }
        helper.assertTrue(stack.get(DfmDataComponents.TANK_LOCAL_A.get()).copy().getAmount() == 3000, "cancelled item drain is restored");
        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            handler.extract(0, waterResource, 1000, transaction);
            transaction.commit();
        }
        helper.assertTrue(stack.get(DfmDataComponents.TANK_LOCAL_A.get()).copy().getAmount() == 2000, "committed drain updates the item component");
        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            helper.assertTrue(handler.insert(0, net.neoforged.neoforge.transfer.fluid.FluidResource.of(lava(1)), 1000, transaction) == 0,
                    "local item rejects mixed fluids");
            helper.assertTrue(handler.insert(0, waterResource, 200000, transaction) == 126000, "local item respects tank capacity");
            transaction.commit();
        }
        //?} else {
        /*var handler = itemFluids(stack);
        helper.assertTrue(handler.drain(water(1000), IFluidHandler.FluidAction.SIMULATE).getAmount() == 1000, "local item can be drained");
        helper.assertTrue(stack.get(DfmDataComponents.TANK_LOCAL_A.get()).copy().getAmount() == 3000, "a simulated item drain changes nothing");
        handler.drain(water(1000), IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(stack.get(DfmDataComponents.TANK_LOCAL_A.get()).copy().getAmount() == 2000, "an executed drain updates the item component");
        helper.assertTrue(handler.fill(lava(1000), IFluidHandler.FluidAction.EXECUTE) == 0, "local item rejects mixed fluids");
        helper.assertTrue(handler.fill(water(200000), IFluidHandler.FluidAction.EXECUTE) == 126000, "local item respects tank capacity");
        *///?}
        helper.succeed();
    //? if >=26.1 {
    }

    private static DimensionalTankBlockEntity place(GameTestHelper helper, BlockPos pos, TankKind kind, Direction facing, TankCode code) {
        Block block = kind == TankKind.DUAL ? DfmBlocks.DIMENSIONAL_DUAL_TANK.get() : DfmBlocks.DIMENSIONAL_TANK.get();
        helper.setBlock(pos, block.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing));
        DimensionalTankBlockEntity be = TestSupport.blockEntity(helper, pos);
        if (code != null) {
            be.setCode(code);
            // A test world kept from an earlier run may still hold liquid in this channel.
            TankChannels.Channel channel = be.channel();
            for (int i = 0; i < channel.chambers(); i++) {
                channel.set(i, FluidStack.EMPTY);
            }
        }
        return be;
    }

    private static DimensionalTankBlockEntity place(GameTestHelper helper, BlockPos pos, TankKind kind, TankCode code) {
        return place(helper, pos, kind, Direction.NORTH, code);
    }

    private static FluidTanks face(GameTestHelper helper, DimensionalTankBlockEntity be, RelativeFace face) {
        return Transfer.fluids(helper.getLevel(), be.getBlockPos(), face.toDirection(be.facing()));
    }

    private static FluidStack water(int amount) {
        return new FluidStack(Fluids.WATER, amount);
    }

    private static FluidStack lava(int amount) {
        return new FluidStack(Fluids.LAVA, amount);
    }

    private static TankCode code(DyeColor a, DyeColor b, DyeColor c) {
        return new TankCode(a, b, c);
    //?}
    }

    @GameTest(template = "empty_small", timeoutTicks = 1250)
    public static void containerSlotsWaitForRealTicksAndKeepIndependentProgress(GameTestHelper helper) {
        //? if >=26.1 {
        var tank = place(helper, FIRST, TankKind.SINGLE, new TankCode(DyeColor.RED, DyeColor.CYAN, DyeColor.BLUE,
                java.util.Optional.of(java.util.UUID.randomUUID()), java.util.Optional.empty()));
        //?} else {
        /*var tank = place(helper, FIRST, TankKind.SINGLE, uniqueCode(DyeColor.RED, DyeColor.CYAN, DyeColor.BLUE));
        *///?}
        int duration = dimensionalfluxminer.DfmConfig.tankContainerTicks();
        tank.channel().set(0, water(2000));
        tank.containers().setStackInSlot(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WATER_BUCKET));
        tank.containers().setStackInSlot(1, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BUCKET));
        tank.containers().tick();
        tank.containers().tick();
        if (duration > 1) {
            helper.assertTrue(tank.containers().progressSnapshot()[0] == 1 && tank.containers().progressSnapshot()[1] == 1,
                    "extra calls in the same world tick do not advance either timer");
            var saved = tank.containers().serializeNBT(helper.getLevel().registryAccess());
            var restored = new dimensionalfluxminer.tank.TankContainerSlots(tank);
            restored.load(helper.getLevel().registryAccess(), saved);
            helper.assertTrue(restored.progressSnapshot()[0] == 1 && restored.progressSnapshot()[1] == 1, "partial progress persists");
            restored.setStackInSlot(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WATER_BUCKET));
            helper.assertTrue(restored.progressSnapshot()[0] == 0 && restored.progressSnapshot()[1] == 1,
                    "replacing one input resets only its own timer");
        }
        if (duration > 2) {
            helper.runAfterDelay(duration - 2, () -> {
                helper.assertTrue(tank.containers().getStackInSlot(0).is(net.minecraft.world.item.Items.WATER_BUCKET)
                        && tank.containers().getStackInSlot(1).is(net.minecraft.world.item.Items.BUCKET)
                        && tank.chamber(0).getAmount() == 2000, "containers and fluid remain unchanged before the delay");
            });
        }
        helper.runAfterDelay(duration + 1, () -> {
            helper.assertTrue(tank.containers().getStackInSlot(0).is(net.minecraft.world.item.Items.BUCKET)
                    && tank.containers().getStackInSlot(1).is(net.minecraft.world.item.Items.WATER_BUCKET)
                    && tank.containers().finished(0) && tank.containers().finished(1)
                    && tank.chamber(0).getAmount() == 2000, "both independent slots complete after their delay");
            helper.succeed();
        });
    }

    @GameTest(template = "empty_small")
    public static void tankConvertsSpongesAndEveryConcreteColor(GameTestHelper helper) {
        //? if >=26.1 {
        var tank = place(helper, FIRST, TankKind.SINGLE, new TankCode(DyeColor.RED, DyeColor.CYAN, DyeColor.BLUE,
                java.util.Optional.of(java.util.UUID.randomUUID()), java.util.Optional.empty()));
        //?} else {
        /*var tank = place(helper, FIRST, TankKind.SINGLE, uniqueCode(DyeColor.RED, DyeColor.CYAN, DyeColor.BLUE));
        *///?}
        var slots = tank.containers();
        slots.setStackInSlot(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WET_SPONGE));
        slots.process();
        helper.assertTrue(slots.getStackInSlot(0).is(net.minecraft.world.item.Items.SPONGE) && tank.chamber(0).getAmount() == 1000,
                "wet sponge returns one bucket of water");
        slots.setStackInSlot(1, slots.extractItem(0, 1, false));
        slots.process();
        helper.assertTrue(slots.getStackInSlot(1).is(net.minecraft.world.item.Items.WET_SPONGE) && tank.chamber(0).isEmpty(),
                "dry sponge absorbs one bucket");
        for (DyeColor color : DyeColor.values()) {
            //? if >=26.1 {
            var input = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(
                    net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", color.getName() + "_concrete_powder"));
            var result = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(
                    net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", color.getName() + "_concrete"));
            //?} else {
            /*var input = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                    net.minecraft.resources.ResourceLocation.withDefaultNamespace(color.getName() + "_concrete_powder"));
            var result = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                    net.minecraft.resources.ResourceLocation.withDefaultNamespace(color.getName() + "_concrete"));
            *///?}
            tank.channel().set(0, water(999));
            slots.setStackInSlot(1, new net.minecraft.world.item.ItemStack(input));
            slots.process();
            helper.assertTrue(slots.getStackInSlot(1).is(input) && tank.chamber(0).getAmount() == 999, "insufficient water: " + color);
            tank.channel().set(0, water(1000));
            slots.process();
            helper.assertTrue(slots.getStackInSlot(1).is(result) && tank.chamber(0).isEmpty(), "concrete: " + color);
            helper.assertFalse(slots.isItemValid(0, slots.getStackInSlot(1)), "concrete is not reversible: " + color);
        }
        tank.channel().set(0, lava(1000));
        slots.setStackInSlot(1, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SPONGE));
        slots.process();
        helper.assertTrue(slots.getStackInSlot(1).is(net.minecraft.world.item.Items.SPONGE) && tank.chamber(0).is(Fluids.LAVA),
                "sponge refuses a different fluid");
        slots.setStackInSlot(1, net.minecraft.world.item.ItemStack.EMPTY);
        tank.channel().set(0, water(127999));
        slots.setStackInSlot(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WET_SPONGE));
        slots.process();
        helper.assertTrue(slots.getStackInSlot(0).is(net.minecraft.world.item.Items.WET_SPONGE) && tank.chamber(0).getAmount() == 127999,
                "wet sponge leaves no partial conversion when full");
        helper.succeed();
    }

    @GameTest(template = "empty_small")
    public static void optionalContainerRecipeProducesRegisteredItem(GameTestHelper helper) {
        if (!net.neoforged.fml.ModList.get().isLoaded("enderio")) {
            helper.succeed();
            return;
        }
        //? if >=26.1 {
        var tank = place(helper, FIRST, TankKind.SINGLE, new TankCode(DyeColor.RED, DyeColor.CYAN, DyeColor.BLUE,
                java.util.Optional.of(java.util.UUID.randomUUID()), java.util.Optional.empty()));
        var fluid = net.minecraft.core.registries.BuiltInRegistries.FLUID.getValue(
                net.minecraft.resources.Identifier.fromNamespaceAndPath("enderio", "fluid_nutrient_distillation_still"));
        var result = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(
                net.minecraft.resources.Identifier.fromNamespaceAndPath("enderio", "nutritious_stick"));
        //?} else {
        /*var tank = place(helper, FIRST, TankKind.SINGLE, uniqueCode(DyeColor.RED, DyeColor.CYAN, DyeColor.BLUE));
        var fluid = net.minecraft.core.registries.BuiltInRegistries.FLUID.get(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("enderio", "fluid_nutrient_distillation_still"));
        var result = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("enderio", "nutritious_stick"));
        *///?}
        helper.assertTrue(fluid != null && fluid != Fluids.EMPTY && result != net.minecraft.world.item.Items.AIR, "optional resources registered");
        tank.channel().set(0, new FluidStack(fluid, 1000));
        tank.containers().setStackInSlot(1, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK));
        tank.containers().process();
        helper.assertTrue(tank.containers().getStackInSlot(1).is(result) && tank.chamber(0).isEmpty(), "optional fluid converts a stick");
        helper.succeed();
    }

    @GameTest(template = "empty_small")
    public static void tankExperienceRecipesAcceptCommonFluids(GameTestHelper helper) {
        //? if >=26.1 {
        var tank = place(helper, FIRST, TankKind.SINGLE, new TankCode(DyeColor.RED, DyeColor.CYAN, DyeColor.BLUE,
                java.util.Optional.of(java.util.UUID.randomUUID()), java.util.Optional.empty()));
        //?} else {
        /*var tank = place(helper, FIRST, TankKind.SINGLE, uniqueCode(DyeColor.RED, DyeColor.CYAN, DyeColor.BLUE));
        *///?}
        var tag = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.FLUID,
                //? if >=26.1 {
                net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "experience"));
                //?} else {
                /*net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "experience"));
                *///?}
        int checked = 0;
        for (var fluid : net.minecraft.core.registries.BuiltInRegistries.FLUID.getTagOrEmpty(tag)) {
            tank.channel().set(0, new FluidStack(fluid, 500));
            tank.containers().setStackInSlot(0, net.minecraft.world.item.ItemStack.EMPTY);
            tank.containers().setStackInSlot(1, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE));
            tank.containers().process();
            helper.assertTrue(tank.containers().getStackInSlot(1).is(net.minecraft.world.item.Items.EXPERIENCE_BOTTLE)
                    && tank.chamber(0).getAmount() == 250 && tank.chamber(0).is(fluid.value()), "fills from tagged XP");
            tank.containers().setStackInSlot(0, tank.containers().extractItem(1, 1, false));
            tank.containers().process();
            helper.assertTrue(tank.containers().getStackInSlot(0).is(net.minecraft.world.item.Items.GLASS_BOTTLE)
                    && tank.chamber(0).getAmount() == 500 && tank.chamber(0).is(fluid.value()),
                    "emptying a bottle adds to the stored XP fluid through the common tag");
            tank.channel().set(0, FluidStack.EMPTY);
            tank.containers().setStackInSlot(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.EXPERIENCE_BOTTLE));
            tank.containers().process();
            helper.assertTrue(tank.containers().getStackInSlot(0).is(net.minecraft.world.item.Items.GLASS_BOTTLE)
                    && tank.chamber(0).getAmount() == 250
                    && tank.chamber(0).is(dimensionalfluxminer.registry.DfmFluids.LIQUID_EXPERIENCE.get()),
                    "empty tank always receives native liquid experience");
            checked++;
        }
        helper.assertTrue(checked > 0, "experience tag contains a fluid");
        helper.succeed();
    }

    @GameTest(template = "empty_small")
    public static void containerSlotsConvertBucketsAndExperienceWithoutLoss(GameTestHelper helper) {
        //? if >=26.1 {
        var tank = place(helper, FIRST, TankKind.SINGLE, new TankCode(DyeColor.RED, DyeColor.RED, DyeColor.RED,
                java.util.Optional.of(java.util.UUID.randomUUID()), java.util.Optional.empty()));
        //?} else {
        /*var tank = place(helper, FIRST, TankKind.SINGLE, uniqueCode(DyeColor.RED, DyeColor.RED, DyeColor.RED));
        *///?}
        var slots = tank.containers();
        var waterBucket = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WATER_BUCKET);
        helper.assertFalse(slots.isItemValid(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BUCKET)),
                "red input rejects the empty result");
        helper.assertFalse(slots.isItemValid(1, waterBucket), "blue input rejects a full bucket");
        helper.assertTrue(slots.insertItem(0, waterBucket, false).isEmpty(), "accept a filled bucket");
        helper.assertTrue(tank.configureItems(RelativeFace.BACK, SideRoute.CONTAINER_DRAIN_BOTH), "red bidirectional face");
        //? if >=26.1 {
        var face = Transfer.items(helper.getLevel(), tank.getBlockPos(), Direction.SOUTH);
        //?} else {
        /*var face = items(helper, tank.getBlockPos(), Direction.SOUTH);
        *///?}
        helper.assertTrue(face.extractItem(0, 1, true).isEmpty(), "automation cannot take unfinished input");
        slots.process();
        helper.assertTrue(slots.getStackInSlot(0).is(net.minecraft.world.item.Items.BUCKET)
                && tank.chamber(0).getAmount() == 1000, "empty bucket and store 1000 mB");
        helper.assertTrue(face.extractItem(0, 1, false).is(net.minecraft.world.item.Items.BUCKET), "same face extracts result");
        slots.setStackInSlot(1, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BUCKET));
        slots.process();
        helper.assertTrue(slots.getStackInSlot(1).is(net.minecraft.world.item.Items.WATER_BUCKET) && tank.chamber(0).isEmpty(),
                "blue slot fills from the tank");
        slots.setStackInSlot(1, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE));
        //? if >=26.1 {
        var xp = new FluidStack(dimensionalfluxminer.registry.DfmFluids.LIQUID_EXPERIENCE.get(), 249);
        tank.channel().set(0, xp);
        //?} else {
        /*tank.channel().set(0, new FluidStack(dimensionalfluxminer.registry.DfmFluids.LIQUID_EXPERIENCE.get(), 249));
        *///?}
        slots.process();
        helper.assertTrue(slots.getStackInSlot(1).is(net.minecraft.world.item.Items.GLASS_BOTTLE)
                && tank.chamber(0).getAmount() == 249, "insufficient XP is unchanged");
        tank.channel().set(0, new FluidStack(dimensionalfluxminer.registry.DfmFluids.LIQUID_EXPERIENCE.get(), 250));
        slots.process();
        helper.assertTrue(slots.getStackInSlot(1).is(net.minecraft.world.item.Items.EXPERIENCE_BOTTLE) && tank.chamber(0).isEmpty(),
                "250 mB fills one experience bottle: item=" + slots.getStackInSlot(1) + ", fluid=" + tank.chamber(0)
                        + ", finished=" + slots.finished(1) + ", recipes="
                        //? if >=26.1 {
                        + dimensionalfluxminer.recipe.Recipes.byType(helper.getLevel(), dimensionalfluxminer.registry.DfmRecipes.TANK_CONTAINER.get()).size());
                        //?} else {
                        /*+ helper.getLevel().getRecipeManager().getAllRecipesFor(dimensionalfluxminer.registry.DfmRecipes.TANK_CONTAINER.get()).size());
                        *///?}
        slots.setStackInSlot(0, slots.extractItem(1, 1, false));
        slots.process();
        helper.assertTrue(slots.getStackInSlot(0).is(net.minecraft.world.item.Items.GLASS_BOTTLE)
                && tank.chamber(0).getAmount() == 250, "reverse conversion returns exactly 250 mB");
        var saved = slots.serializeNBT(helper.getLevel().registryAccess());
        var reloaded = new dimensionalfluxminer.tank.TankContainerSlots(tank);
        reloaded.load(helper.getLevel().registryAccess(), saved);
        helper.assertTrue(reloaded.finished(0) && reloaded.getStackInSlot(0).is(net.minecraft.world.item.Items.GLASS_BOTTLE),
                "completion and result persist");
        tank.channel().set(0, water(127999));
        slots.setStackInSlot(0, waterBucket.copy());
        slots.process();
        helper.assertTrue(slots.getStackInSlot(0).is(net.minecraft.world.item.Items.WATER_BUCKET)
                && tank.chamber(0).getAmount() == 127999, "insufficient capacity leaves bucket and fluid intact");
        helper.succeed();
    }

    @GameTest(template = "empty_small")
    //? if >=26.1 {
    public static void containerFacesRollbackAndKeepSlotsLocal(GameTestHelper helper) {
        var shared = new TankCode(DyeColor.BLUE, DyeColor.RED, DyeColor.BLUE,
                java.util.Optional.of(java.util.UUID.randomUUID()), java.util.Optional.empty());
    //?} else {
    /*public static void containerFacesKeepOneContainerAndSlotsLocal(GameTestHelper helper) {
        var shared = uniqueCode(DyeColor.BLUE, DyeColor.RED, DyeColor.BLUE);
    *///?}
        var tank = place(helper, FIRST, TankKind.SINGLE, shared);
        var other = place(helper, SECOND, TankKind.SINGLE, shared);
        tank.configureItems(RelativeFace.BACK, SideRoute.CONTAINER_FILL_BOTH);
        //? if >=26.1 {
        var handler = helper.getLevel().getCapability(Capabilities.Item.BLOCK, tank.getBlockPos(), Direction.SOUTH);
        var bucket = net.neoforged.neoforge.transfer.item.ItemResource.of(net.minecraft.world.item.Items.BUCKET);
        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            helper.assertTrue(handler.insert(0, bucket, 16, transaction) == 1, "one container per slot");
        }
        helper.assertTrue(tank.containers().getStackInSlot(1).isEmpty(), "cancelled insertion is rolled back");
        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            handler.insert(0, bucket, 1, transaction);
            transaction.commit();
        }
        //?} else {
        /*var handler = items(helper, tank.getBlockPos(), Direction.SOUTH);
        var buckets = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BUCKET, 16);
        helper.assertTrue(handler.insertItem(0, buckets.copy(), true).getCount() == 15, "one container per slot");
        helper.assertTrue(tank.containers().getStackInSlot(1).isEmpty(), "a simulated insertion changes nothing");
        handler.insertItem(0, buckets.copyWithCount(1), false);
        *///?}
        tank.channel().set(0, water(1000));
        tank.containers().process();
        helper.assertTrue(other.containers().getStackInSlot(1).isEmpty() && other.chamber(0).isEmpty(),
                "items stay local while fluid is shared");
        helper.assertTrue(tank.configureItems(RelativeFace.BACK, SideRoute.CONTAINER_DRAIN_INPUT), "change route");
        //? if >=26.1 {
        helper.assertTrue(Transfer.items(helper.getLevel(), tank.getBlockPos(), Direction.SOUTH).extractItem(0, 1, false).isEmpty(),
        //?} else {
        /*helper.assertTrue(items(helper, tank.getBlockPos(), Direction.SOUTH).extractItem(0, 1, false).isEmpty(),
        *///?}
                "changed route no longer exposes the blue output");
        var dual = place(helper, new BlockPos(5, 1, 5), TankKind.DUAL, shared);
        //? if >=26.1 {
        helper.assertTrue(Transfer.items(helper.getLevel(), dual.getBlockPos(), Direction.SOUTH) == null, "dual has no item capability");
        //?} else {
        /*helper.assertTrue(items(helper, dual.getBlockPos(), Direction.SOUTH) == null, "dual has no item capability");
        *///?}
        var saved = tank.saveWithoutMetadata(helper.getLevel().registryAccess());
        //? if >=26.1 {
        TestSupport.load(tank, saved, helper.getLevel().registryAccess());
        //?} else {
        /*load(tank, saved, helper);
        *///?}
        helper.assertTrue(tank.containers().finished(1)
                && tank.containers().getStackInSlot(1).is(net.minecraft.world.item.Items.WATER_BUCKET), "block save keeps the result and completion");
        helper.destroyBlock(FIRST);
        int dropped = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(tank.getBlockPos()).inflate(2)).stream()
                .filter(item -> item.getItem().is(net.minecraft.world.item.Items.WATER_BUCKET))
                .mapToInt(item -> item.getItem().getCount()).sum();
        helper.assertTrue(dropped == 1, "breaking drops the local result exactly once");
        helper.succeed();
    }

    @GameTest(template = "empty_small")
    public static void containerAutomationUsesItsOwnToggles(GameTestHelper helper) {
        //? if >=26.1 {
        var tank = place(helper, FIRST, TankKind.SINGLE, new TankCode(DyeColor.RED, DyeColor.BLACK, DyeColor.BLUE,
                java.util.Optional.of(java.util.UUID.randomUUID()), java.util.Optional.empty()));
        //?} else {
        /*var tank = place(helper, FIRST, TankKind.SINGLE, uniqueCode(DyeColor.RED, DyeColor.BLACK, DyeColor.BLUE));
        *///?}
        helper.setBlock(FIRST.south(), net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState());
        //? if >=26.1 {
        net.minecraft.world.level.block.entity.ChestBlockEntity chest = TestSupport.blockEntity(helper, FIRST.south());
        //?} else {
        /*net.minecraft.world.level.block.entity.ChestBlockEntity chest = helper.getBlockEntity(FIRST.south());
        *///?}
        chest.setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WATER_BUCKET));
        tank.configureItems(RelativeFace.BACK, SideRoute.CONTAINER_DRAIN_BOTH);
        helper.assertTrue(tank.handleButton(null, AbstractMachineBlockEntity.BUTTON_AUTO_INPUT), "item input enabled");
        DimensionalTankBlockEntity.serverTick(helper.getLevel(), tank.getBlockPos(), tank.getBlockState(), tank);
        tank.containers().process();
        helper.assertTrue(chest.getItem(0).isEmpty() && tank.containers().finished(0), "pull and process one bucket");
        var data = new CompoundTag();
        tank.writeMenuData(data, helper.getLevel().registryAccess());
        //? if >=26.1 {
        helper.assertFalse(data.getBooleanOr("auto_input_fluid", false) || data.getBooleanOr("auto_eject_fluid", false),
        //?} else {
        /*helper.assertFalse(data.getBoolean("auto_input_fluid") || data.getBoolean("auto_eject_fluid"),
        *///?}
                "item automation leaves liquid automation off");
        tank.handleButton(null, AbstractMachineBlockEntity.BUTTON_AUTO_EJECT);
        DimensionalTankBlockEntity.serverTick(helper.getLevel(), tank.getBlockPos(), tank.getBlockState(), tank);
        helper.assertTrue(chest.getItem(0).is(net.minecraft.world.item.Items.BUCKET) && tank.containers().getStackInSlot(0).isEmpty(),
                "same face ejects its result without pulling it back");
        var saved = tank.saveWithoutMetadata(helper.getLevel().registryAccess());
        //? if >=26.1 {
        TestSupport.load(tank, saved, helper.getLevel().registryAccess());
        //?} else {
        /*load(tank, saved, helper);
        *///?}
        helper.assertTrue(tank.sides().get(RelativeFace.BACK, dimensionalfluxminer.machine.side.Transmission.ITEM, 0)
                == SideRoute.CONTAINER_DRAIN_BOTH, "item route persists");
        var buf = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            buf.writeBlockPos(helper.absolutePos(new BlockPos(6, 1, 6)));
            buf.writeEnum(TankKind.SINGLE);
            var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            var menu = new dimensionalfluxminer.tank.TankMenu(1, player.getInventory(), buf);
            helper.assertTrue(menu.kind() == TankKind.SINGLE && menu.slots.size() == 38,
                    "opening packet defines slots even before the client receives the block");
        } finally {
            buf.release();
        }
        helper.succeed();
    }

    //? if >=26.1 {
    /** Using a tank with an empty hand opens it instead of failing on the missing item. */
    @GameTest(template = "empty_small")
    public static void emptyHandUseIsSafe(GameTestHelper helper) {
        place(helper, FIRST, TankKind.SINGLE, null);
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        BlockPos pos = helper.absolutePos(FIRST);
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos), Direction.NORTH, pos, false);
        var result = helper.getLevel().getBlockState(pos).useItemOn(net.minecraft.world.item.ItemStack.EMPTY, helper.getLevel(), player,
                net.minecraft.world.InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(result == net.minecraft.world.InteractionResult.TRY_WITH_EMPTY_HAND, "falls back to the empty-hand use: " + result);
        helper.succeed();
    }
    //?}
    @GameTest(template = "empty_small")
    public static void newTankUsesWhiteCodeAndHasNoEnergy(GameTestHelper helper) {
        DimensionalTankBlockEntity be = place(helper, FIRST, TankKind.SINGLE, null);
        helper.assertTrue(be.code().equals(TankCode.DEFAULT), "white, white, white");
        for (Direction side : Direction.values()) {
            //? if >=26.1 {
            helper.assertTrue(Transfer.energy(helper.getLevel(), be.getBlockPos(), side) == null,
            //?} else {
            /*helper.assertTrue(helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, be.getBlockPos(), side) == null,
            *///?}
                    "no energy on " + side);
            //? if >=26.1 {
            helper.assertTrue(Transfer.items(helper.getLevel(), be.getBlockPos(), side) == null,
            //?} else {
            /*helper.assertTrue(helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, be.getBlockPos(), side) == null,
            *///?}
                    "no items on " + side);
        }
        helper.assertTrue(face(helper, be, RelativeFace.FRONT) == null, "the front (panel) has no pipe access");
        for (RelativeFace f : new RelativeFace[] {RelativeFace.BACK, RelativeFace.LEFT, RelativeFace.RIGHT, RelativeFace.UP, RelativeFace.DOWN}) {
            helper.assertTrue(face(helper, be, f) != null, "pipes connect without configuration: " + f);
        }
        //? if >=26.1 {
        FluidTanks view = Transfer.fluids(helper.getLevel(), be.getBlockPos(), null);
        //?} else {
        /*IFluidHandler view = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, be.getBlockPos(), null);
        *///?}
        helper.assertTrue(view != null && view.getTanks() == 1 && view.getTankCapacity(0) == 128_000, "one chamber of 128 000 mB");
        helper.succeed();
    }

    @GameTest(template = "empty_small")
    public static void sameCodeSharesOneReserve(GameTestHelper helper) {
        TankCode code = code(DyeColor.RED, DyeColor.BLUE, DyeColor.WHITE);
        DimensionalTankBlockEntity first = place(helper, FIRST, TankKind.SINGLE, code);
        DimensionalTankBlockEntity second = place(helper, SECOND, TankKind.SINGLE, code);
        //? if >=26.1 {
        FluidTanks in = face(helper, first, RelativeFace.BACK);
        FluidTanks out = face(helper, second, RelativeFace.UP);
        helper.assertTrue(in.fill(water(4 * BUCKET), FluidTanks.FluidAction.SIMULATE) == 4 * BUCKET, "simulated fill accepts");
        //?} else {
        /*IFluidHandler in = face(helper, first, RelativeFace.BACK);
        IFluidHandler out = face(helper, second, RelativeFace.UP);
        helper.assertTrue(in.fill(water(4 * BUCKET), IFluidHandler.FluidAction.SIMULATE) == 4 * BUCKET, "simulated fill accepts");
        *///?}
        helper.assertTrue(out.getFluidInTank(0).isEmpty(), "simulation changes nothing");
        //? if >=26.1 {
        helper.assertTrue(in.fill(water(4 * BUCKET), FluidTanks.FluidAction.EXECUTE) == 4 * BUCKET, "filled once");
        //?} else {
        /*helper.assertTrue(in.fill(water(4 * BUCKET), IFluidHandler.FluidAction.EXECUTE) == 4 * BUCKET, "filled once");
        *///?}
        helper.assertTrue(out.getFluidInTank(0).getAmount() == 4 * BUCKET, "the other tank sees the same water");
        //? if >=26.1 {
        helper.assertTrue(out.drain(BUCKET, FluidTanks.FluidAction.SIMULATE).getAmount() == BUCKET
        //?} else {
        /*helper.assertTrue(out.drain(BUCKET, IFluidHandler.FluidAction.SIMULATE).getAmount() == BUCKET
        *///?}
                && out.getFluidInTank(0).getAmount() == 4 * BUCKET, "simulated drain changes nothing");
        // Two takers never get more than the balance.
        //? if >=26.1 {
        helper.assertTrue(out.drain(3 * BUCKET, FluidTanks.FluidAction.EXECUTE).getAmount() == 3 * BUCKET, "first taker");
        helper.assertTrue(in.drain(5 * BUCKET, FluidTanks.FluidAction.EXECUTE).getAmount() == BUCKET, "second taker gets the rest");
        //?} else {
        /*helper.assertTrue(out.drain(3 * BUCKET, IFluidHandler.FluidAction.EXECUTE).getAmount() == 3 * BUCKET, "first taker");
        helper.assertTrue(in.drain(5 * BUCKET, IFluidHandler.FluidAction.EXECUTE).getAmount() == BUCKET, "second taker gets the rest");
        *///?}
        helper.assertTrue(first.chamber(0).isEmpty() && second.chamber(0).isEmpty(), "empty everywhere");
        helper.succeed();
    }

    /** Every endpoint of a channel sends its clients the change, whichever endpoint made it (block and gauge show it). */
    @GameTest(template = "empty_small")
    public static void changesReachTheClientsOfEveryEndpoint(GameTestHelper helper) {
        TankCode code = code(DyeColor.GREEN, DyeColor.WHITE, DyeColor.GREEN);
        DimensionalTankBlockEntity first = place(helper, FIRST, TankKind.SINGLE, code);
        DimensionalTankBlockEntity second = place(helper, SECOND, TankKind.SINGLE, code);
        helper.runAfterDelay(1, () -> {
            //? if >=26.1 {
            face(helper, first, RelativeFace.BACK).fill(water(BUCKET), FluidTanks.FluidAction.EXECUTE);
            //?} else {
            /*face(helper, first, RelativeFace.BACK).fill(water(BUCKET), IFluidHandler.FluidAction.EXECUTE);
            *///?}
            int version = first.channel().version();
            helper.runAfterDelay(4, () -> {
                helper.assertTrue(first.syncedVersion() == version, "the tank that was filled updated its clients");
                helper.assertTrue(second.syncedVersion() == version, "the other endpoint updated its clients too");
                var registries = helper.getLevel().registryAccess();
                //? if >=26.1 {
                var sent = DimensionalTankBlockEntity.readChambers(second.getUpdateTag(registries).getListOrEmpty("chambers"), 1, registries);
                //?} else {
                /*var sent = DimensionalTankBlockEntity.readChambers(second.getUpdateTag(registries).getList("chambers", 10), 1, registries);
                *///?}
                helper.assertTrue(sent[0].is(Fluids.WATER) && sent[0].getAmount() == BUCKET, "the update carries the liquid");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty_small")
    public static void colorOrderMatters(GameTestHelper helper) {
        DimensionalTankBlockEntity first = place(helper, FIRST, TankKind.SINGLE, code(DyeColor.LIME, DyeColor.PINK, DyeColor.GRAY));
        DimensionalTankBlockEntity second = place(helper, SECOND, TankKind.SINGLE, code(DyeColor.PINK, DyeColor.LIME, DyeColor.GRAY));
        //? if >=26.1 {
        face(helper, first, RelativeFace.BACK).fill(water(BUCKET), FluidTanks.FluidAction.EXECUTE);
        //?} else {
        /*face(helper, first, RelativeFace.BACK).fill(water(BUCKET), IFluidHandler.FluidAction.EXECUTE);
        *///?}
        helper.assertTrue(second.chamber(0).isEmpty(), "two colors swapped = another channel");
        helper.succeed();
    }

    @GameTest(template = "empty_small")
    public static void singleAndDualNeverShare(GameTestHelper helper) {
        TankCode code = code(DyeColor.CYAN, DyeColor.CYAN, DyeColor.BLACK);
        DimensionalTankBlockEntity single = place(helper, FIRST, TankKind.SINGLE, code);
        DimensionalTankBlockEntity dual = place(helper, SECOND, TankKind.DUAL, code);
        //? if >=26.1 {
        face(helper, single, RelativeFace.BACK).fill(water(BUCKET), FluidTanks.FluidAction.EXECUTE);
        face(helper, dual, RelativeFace.RIGHT).fill(lava(2 * BUCKET), FluidTanks.FluidAction.EXECUTE);
        //?} else {
        /*face(helper, single, RelativeFace.BACK).fill(water(BUCKET), IFluidHandler.FluidAction.EXECUTE);
        face(helper, dual, RelativeFace.RIGHT).fill(lava(2 * BUCKET), IFluidHandler.FluidAction.EXECUTE);
        *///?}
        helper.assertTrue(single.chamber(0).getAmount() == BUCKET && single.chamber(0).is(Fluids.WATER), "single keeps its water");
        helper.assertTrue(dual.chamber(0).is(Fluids.LAVA) && dual.chamber(1).isEmpty(), "dual keeps its own lava");
        helper.succeed();
    }

    @GameTest(template = "empty_small")
    public static void dualChambersStayApart(GameTestHelper helper) {
        TankCode code = code(DyeColor.ORANGE, DyeColor.MAGENTA, DyeColor.BROWN);
        DimensionalTankBlockEntity first = place(helper, FIRST, TankKind.DUAL, code);
        DimensionalTankBlockEntity second = place(helper, SECOND, TankKind.DUAL, Direction.EAST, code);
        helper.assertTrue(first.route(RelativeFace.RIGHT) == SideRoute.FLUID_A_INPUT_OUTPUT
                && first.route(RelativeFace.LEFT) == SideRoute.FLUID_B_INPUT_OUTPUT, "A on the right side, B on the left");
        //? if >=26.1 {
        FluidTanks a = face(helper, first, RelativeFace.RIGHT);
        FluidTanks b = face(helper, first, RelativeFace.LEFT);
        helper.assertTrue(a.fill(water(TankKind.CHAMBER_CAPACITY), FluidTanks.FluidAction.EXECUTE) == TankKind.CHAMBER_CAPACITY, "A full");
        helper.assertTrue(b.fill(lava(3 * BUCKET), FluidTanks.FluidAction.EXECUTE) == 3 * BUCKET, "lava in B");
        helper.assertTrue(a.fill(water(BUCKET), FluidTanks.FluidAction.EXECUTE) == 0, "a full A never spills into B");
        helper.assertTrue(a.fill(lava(BUCKET), FluidTanks.FluidAction.EXECUTE) == 0, "another liquid is refused");
        helper.assertTrue(b.fill(water(BUCKET), FluidTanks.FluidAction.EXECUTE) == 0, "water refused in the lava chamber");
        helper.assertTrue(b.drain(water(BUCKET), FluidTanks.FluidAction.EXECUTE).isEmpty(), "B face never drains A");
        //?} else {
        /*IFluidHandler a = face(helper, first, RelativeFace.RIGHT);
        IFluidHandler b = face(helper, first, RelativeFace.LEFT);
        helper.assertTrue(a.fill(water(TankKind.CHAMBER_CAPACITY), IFluidHandler.FluidAction.EXECUTE) == TankKind.CHAMBER_CAPACITY, "A full");
        helper.assertTrue(b.fill(lava(3 * BUCKET), IFluidHandler.FluidAction.EXECUTE) == 3 * BUCKET, "lava in B");
        helper.assertTrue(a.fill(water(BUCKET), IFluidHandler.FluidAction.EXECUTE) == 0, "a full A never spills into B");
        helper.assertTrue(a.fill(lava(BUCKET), IFluidHandler.FluidAction.EXECUTE) == 0, "another liquid is refused");
        helper.assertTrue(b.fill(water(BUCKET), IFluidHandler.FluidAction.EXECUTE) == 0, "water refused in the lava chamber");
        helper.assertTrue(b.drain(water(BUCKET), IFluidHandler.FluidAction.EXECUTE).isEmpty(), "B face never drains A");
        *///?}
        // The other dual (another facing, same code): A with A, B with B.
        helper.assertTrue(second.chamber(0).is(Fluids.WATER) && second.chamber(0).getAmount() == TankKind.CHAMBER_CAPACITY, "A shared");
        helper.assertTrue(second.chamber(1).is(Fluids.LAVA) && second.chamber(1).getAmount() == 3 * BUCKET, "B shared");
        //? if >=26.1 {
        face(helper, second, RelativeFace.RIGHT).drain(BUCKET, FluidTanks.FluidAction.EXECUTE);
        //?} else {
        /*face(helper, second, RelativeFace.RIGHT).drain(BUCKET, IFluidHandler.FluidAction.EXECUTE);
        *///?}
        helper.assertTrue(first.chamber(0).getAmount() == TankKind.CHAMBER_CAPACITY - BUCKET && first.chamber(1).getAmount() == 3 * BUCKET,
                "only A changed");
        helper.succeed();
    }

    /** A bucket on the right face (or the right half of the front) of a dual tank uses A, on the left face B. */
    @GameTest(template = "empty_small")
    public static void bucketSideChoosesTheChamber(GameTestHelper helper) {
        var block = (dimensionalfluxminer.tank.DimensionalTankBlock) DfmBlocks.DIMENSIONAL_DUAL_TANK.get();
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            for (RelativeFace face : new RelativeFace[] {RelativeFace.RIGHT, RelativeFace.LEFT}) {
                Direction side = face.toDirection(facing);
                net.minecraft.world.phys.Vec3 hit = new net.minecraft.world.phys.Vec3(0.5 + side.getStepX() * 0.5, 0.5, 0.5 + side.getStepZ() * 0.5);
                int expected = face == RelativeFace.RIGHT ? 0 : 1;
                helper.assertTrue(block.chamberAt(facing, hit) == expected, facing + " " + face + " -> " + (expected == 0 ? "A" : "B"));
            }
            // Front, a quarter to the player's right (towards the RIGHT face): A.
            Direction right = RelativeFace.RIGHT.toDirection(facing);
            net.minecraft.world.phys.Vec3 front = new net.minecraft.world.phys.Vec3(0.5 + facing.getStepX() * 0.5 + right.getStepX() * 0.25, 0.5,
                    0.5 + facing.getStepZ() * 0.5 + right.getStepZ() * 0.25);
            helper.assertTrue(block.chamberAt(facing, front) == 0, facing + " front right half -> A");
        }
        helper.succeed();
    }

    @GameTest(template = "empty_small")
    public static void changingTheCodeMovesNoLiquid(GameTestHelper helper) {
        TankCode before = code(DyeColor.YELLOW, DyeColor.GREEN, DyeColor.PURPLE);
        TankCode after = code(DyeColor.YELLOW, DyeColor.GREEN, DyeColor.LIGHT_BLUE);
        DimensionalTankBlockEntity be = place(helper, FIRST, TankKind.SINGLE, before);
        //? if >=26.1 {
        face(helper, be, RelativeFace.BACK).fill(water(5 * BUCKET), FluidTanks.FluidAction.EXECUTE);
        //?} else {
        /*face(helper, be, RelativeFace.BACK).fill(water(5 * BUCKET), IFluidHandler.FluidAction.EXECUTE);
        *///?}
        // Third position to light blue through the code window's button.
        helper.assertTrue(be.handleButton(null, DimensionalTankBlockEntity.BUTTON_CODE_BASE + 2 * 16 + DyeColor.LIGHT_BLUE.getId()), "button");
        helper.assertTrue(be.code().equals(after), "code changed");
        helper.assertTrue(be.chamber(0).isEmpty(), "the new channel is empty");
        TankChannels channels = TankChannels.get(helper.getLevel().getServer());
        helper.assertTrue(channels.channel(TankKind.SINGLE, before).get(0).getAmount() == 5 * BUCKET, "the old channel keeps its water");
        be.setCode(before);
        helper.assertTrue(be.chamber(0).getAmount() == 5 * BUCKET, "back on the old code: same water, nothing duplicated");
        helper.assertFalse(be.handleButton(null, DimensionalTankBlockEntity.BUTTON_CODE_BASE + 3 * 16), "no fourth position");
        helper.assertFalse(be.handleButton(null, DimensionalTankBlockEntity.BUTTON_CODE_BASE - 1), "outside the palette");
        helper.succeed();
    }

    @GameTest(template = "empty_small")
    public static void breakingKeepsLiquidAndCode(GameTestHelper helper) {
        TankCode code = code(DyeColor.BLACK, DyeColor.RED, DyeColor.LIME);
        DimensionalTankBlockEntity be = place(helper, FIRST, TankKind.SINGLE, code);
        //? if >=26.1 {
        face(helper, be, RelativeFace.BACK).fill(water(7 * BUCKET), FluidTanks.FluidAction.EXECUTE);
        //?} else {
        /*face(helper, be, RelativeFace.BACK).fill(water(7 * BUCKET), IFluidHandler.FluidAction.EXECUTE);
        *///?}
        helper.assertTrue(code.equals(be.collectComponents().get(DfmDataComponents.TANK_CODE.get())), "the item gets the code");
        CompoundTag saved = be.saveWithoutMetadata(helper.getLevel().registryAccess());
        helper.destroyBlock(FIRST);
        TankChannels channels = TankChannels.get(helper.getLevel().getServer());
        helper.assertTrue(channels.channel(TankKind.SINGLE, code).get(0).getAmount() == 7 * BUCKET, "the channel keeps the water");
        // A tank loaded from the save (or placed from the item) is back on the same channel.
        DimensionalTankBlockEntity again = place(helper, SECOND, TankKind.SINGLE, null);
        //? if >=26.1 {
        TestSupport.load(again, saved, helper.getLevel().registryAccess());
        //?} else {
        /*again.loadWithComponents(saved, helper.getLevel().registryAccess());
        *///?}
        helper.assertTrue(again.code().equals(code) && again.chamber(0).getAmount() == 7 * BUCKET, "reconnected");
        helper.succeed();
    }

    @GameTest(template = "empty_small")
    public static void liquidAutomationIsItsOwnToggle(GameTestHelper helper) {
        TankCode source = code(DyeColor.BLUE, DyeColor.BLUE, DyeColor.ORANGE);
        TankCode target = code(DyeColor.ORANGE, DyeColor.BLUE, DyeColor.BLUE);
        DimensionalTankBlockEntity from = place(helper, new BlockPos(2, 1, 1), TankKind.SINGLE, source);
        // Facing east, the north face (towards the first tank's back) is the RIGHT side: input/output by default.
        DimensionalTankBlockEntity to = place(helper, new BlockPos(2, 1, 2), TankKind.SINGLE, Direction.EAST, target);
        //? if >=26.1 {
        face(helper, from, RelativeFace.BACK).fill(water(10 * BUCKET), FluidTanks.FluidAction.EXECUTE);
        //?} else {
        /*face(helper, from, RelativeFace.BACK).fill(water(10 * BUCKET), IFluidHandler.FluidAction.EXECUTE);
        *///?}
        helper.assertTrue(from.handleButton(null, AbstractMachineBlockEntity.BUTTON_AUTO_FLUID_NEXT), "OFF -> Auto Input");
        helper.assertTrue(from.autoInputFluid() && !from.autoEjectFluid(), "only Auto Input");
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(to.chamber(0).isEmpty(), "Auto Input pulls nothing out of an empty neighbor");
            helper.assertTrue(from.handleButton(null, AbstractMachineBlockEntity.BUTTON_AUTO_FLUID_NEXT), "Auto Input -> Auto Eject");
            helper.assertTrue(!from.autoInputFluid() && from.autoEjectFluid(), "only Auto Eject");
            helper.runAfterDelay(3, () -> {
                helper.assertTrue(to.chamber(0).getAmount() == 10 * BUCKET && from.chamber(0).isEmpty(), "ejected without a per-tick limit");
                helper.succeed();
            });
        });
    }

    /** The two switches of the Liquids tab: each one turns only its own automation on and off. */
    @GameTest(template = "empty_small")
    public static void liquidInputAndEjectSwitchesAreIndependent(GameTestHelper helper) {
        DimensionalTankBlockEntity tank = place(helper, FIRST, TankKind.SINGLE, code(DyeColor.LIME, DyeColor.LIME, DyeColor.RED));
        helper.assertTrue(tank.handleButton(null, AbstractMachineBlockEntity.BUTTON_AUTO_INPUT_FLUID), "Auto Input switch");
        helper.assertTrue(tank.autoInputFluid() && !tank.autoEjectFluid(), "only Auto Input on");
        helper.assertTrue(tank.handleButton(null, AbstractMachineBlockEntity.BUTTON_AUTO_EJECT_FLUID), "Auto Eject switch");
        helper.assertTrue(tank.autoInputFluid() && tank.autoEjectFluid(), "both on");
        helper.assertTrue(tank.handleButton(null, AbstractMachineBlockEntity.BUTTON_AUTO_INPUT_FLUID), "Auto Input switch again");
        helper.assertTrue(!tank.autoInputFluid() && tank.autoEjectFluid(), "Auto Eject stays on");
        helper.succeed();
    }

    @GameTest(template = "empty_small")
    public static void personalCodeIsItsOwnChannel(GameTestHelper helper) {
        TankCode colors = code(DyeColor.PURPLE, DyeColor.PURPLE, DyeColor.CYAN);
        DimensionalTankBlockEntity open = place(helper, FIRST, TankKind.SINGLE, colors);
        DimensionalTankBlockEntity mine = place(helper, SECOND, TankKind.SINGLE, colors);
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        net.minecraft.world.item.ItemStack diamonds = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND, 2);
        helper.assertFalse(mine.makePersonal(player, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.EMERALD)),
                "only the personal item");
        helper.assertTrue(mine.makePersonal(player, diamonds) && diamonds.getCount() == 1, "one diamond used");
        helper.assertTrue(mine.code().personal() && mine.code().first() == DyeColor.PURPLE, "same colors, now personal");
        // A previous run may have left liquid in this player's channel.
        mine.channel().set(0, FluidStack.EMPTY);
        //? if >=26.1 {
        face(helper, open, RelativeFace.BACK).fill(water(BUCKET), FluidTanks.FluidAction.EXECUTE);
        //?} else {
        /*face(helper, open, RelativeFace.BACK).fill(water(BUCKET), IFluidHandler.FluidAction.EXECUTE);
        *///?}
        helper.assertTrue(mine.chamber(0).isEmpty(), "the public channel with the same colors is another one");
        //? if >=26.1 {
        face(helper, mine, RelativeFace.BACK).fill(lava(2 * BUCKET), FluidTanks.FluidAction.EXECUTE);
        //?} else {
        /*face(helper, mine, RelativeFace.BACK).fill(lava(2 * BUCKET), IFluidHandler.FluidAction.EXECUTE);
        *///?}
        helper.assertTrue(open.chamber(0).is(Fluids.WATER) && open.chamber(0).getAmount() == BUCKET, "public keeps its water");
        helper.assertTrue(mine.code().equals(mine.collectComponents().get(DfmDataComponents.TANK_CODE.get())), "the item keeps the owner");
        helper.assertTrue(mine.handleButton(null, DimensionalTankBlockEntity.BUTTON_CODE_BASE + DyeColor.RED.getId()) && mine.code().personal(),
                "changing a color keeps it personal");
        mine.setCode(mine.code().with(0, DyeColor.PURPLE));
        helper.assertTrue(mine.makePublic(player) && !mine.code().personal(), "public again");
        helper.assertTrue(player.getInventory().countItem(net.minecraft.world.item.Items.DIAMOND) == 1, "diamond given back");
        helper.assertTrue(mine.chamber(0).is(Fluids.WATER), "back on the public channel");
        helper.succeed();
    }

    /** A full dual tank ejecting each chamber into a single tank at its side empties completely. */
    @GameTest(template = "empty_small")
    public static void dualEjectsEverythingToBothSides(GameTestHelper helper) {
        DimensionalTankBlockEntity dual = place(helper, new BlockPos(2, 1, 2), TankKind.DUAL, code(DyeColor.GRAY, DyeColor.BROWN, DyeColor.GRAY));
        // Facing north: the right side (A) is west, the left side (B) east. The singles face north too: their east and
        // west faces are input/output.
        DimensionalTankBlockEntity west = place(helper, new BlockPos(1, 1, 2), TankKind.SINGLE, code(DyeColor.GRAY, DyeColor.GRAY, DyeColor.BROWN));
        DimensionalTankBlockEntity east = place(helper, new BlockPos(3, 1, 2), TankKind.SINGLE, code(DyeColor.BROWN, DyeColor.GRAY, DyeColor.GRAY));
        //? if >=26.1 {
        face(helper, dual, RelativeFace.RIGHT).fill(water(TankKind.CHAMBER_CAPACITY), FluidTanks.FluidAction.EXECUTE);
        face(helper, dual, RelativeFace.LEFT).fill(lava(TankKind.CHAMBER_CAPACITY), FluidTanks.FluidAction.EXECUTE);
        //?} else {
        /*face(helper, dual, RelativeFace.RIGHT).fill(water(TankKind.CHAMBER_CAPACITY), IFluidHandler.FluidAction.EXECUTE);
        face(helper, dual, RelativeFace.LEFT).fill(lava(TankKind.CHAMBER_CAPACITY), IFluidHandler.FluidAction.EXECUTE);
        *///?}
        dual.setAutoEjectFluid(true);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(dual.chamber(0).isEmpty() && dual.chamber(1).isEmpty(),
                    "dual emptied: A " + dual.chamber(0).getAmount() + " mB, B " + dual.chamber(1).getAmount() + " mB");
            helper.assertTrue(west.chamber(0).is(Fluids.WATER) && west.chamber(0).getAmount() == TankKind.CHAMBER_CAPACITY, "A went west");
            helper.assertTrue(east.chamber(0).is(Fluids.LAVA) && east.chamber(0).getAmount() == TankKind.CHAMBER_CAPACITY, "B went east");
            // An ejecting face takes nothing back from outside (a pipe network would bounce the last mB back).
            //? if >=26.1 {
            helper.assertTrue(face(helper, dual, RelativeFace.RIGHT).fill(water(BUCKET), FluidTanks.FluidAction.EXECUTE) == 0,
            //?} else {
            /*helper.assertTrue(face(helper, dual, RelativeFace.RIGHT).fill(water(BUCKET), IFluidHandler.FluidAction.EXECUTE) == 0,
            *///?}
                    "ejecting face refuses input");
            dual.setAutoEjectFluid(false);
            //? if >=26.1 {
            helper.assertTrue(face(helper, dual, RelativeFace.RIGHT).fill(water(BUCKET), FluidTanks.FluidAction.EXECUTE) == BUCKET,
            //?} else {
            /*helper.assertTrue(face(helper, dual, RelativeFace.RIGHT).fill(water(BUCKET), IFluidHandler.FluidAction.EXECUTE) == BUCKET,
            *///?}
                    "Auto Eject off: the face takes input again");
            helper.succeed();
        });
    }

    /** Tanks, machines and Miner parts hold water, so under water it surrounds them instead of stopping at their edge. */
    @GameTest(template = "empty_small")
    public static void modBlocksHoldWater(GameTestHelper helper) {
        Block[] blocks = {DfmBlocks.DIMENSIONAL_TANK.get(), DfmBlocks.DIMENSIONAL_DUAL_TANK.get(), DfmBlocks.CREATIVE_DIMENSIONAL_TANK.get(),
                DfmBlocks.MECHANICAL_CRUSHER.get(),
                DfmBlocks.ELECTRIC_FURNACE.get(), DfmBlocks.MINER_TERMINAL.get(), DfmBlocks.STEEL_CHASSIS.get(), DfmBlocks.DIMENSIONAL_PORT.get(),
                DfmBlocks.REINFORCED_STRUCTURAL_GLASS.get(), DfmBlocks.VENTILATION_UNIT.get(), DfmBlocks.QUANTUM_CORE.get()};
        for (int i = 0; i < blocks.length; i++) {
            BlockPos pos = new BlockPos(i % 4, 1, i / 4);
            helper.assertFalse(blocks[i].defaultBlockState().getValue(BlockStateProperties.WATERLOGGED), blocks[i] + " starts dry");
            helper.setBlock(pos, blocks[i].defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true));
            helper.assertTrue(helper.getLevel().getFluidState(helper.absolutePos(pos)).is(Fluids.WATER), blocks[i] + " holds water");
        }
        helper.succeed();
    }

    /** No hostile mob spawns on top of a machine, tank or Miner part (loose or formed). */
    @GameTest(template = "empty_small")
    public static void noMobSpawnsOnModBlocks(GameTestHelper helper) {
        Block[] blocks = {DfmBlocks.DIMENSIONAL_TANK.get(), DfmBlocks.DIMENSIONAL_DUAL_TANK.get(), DfmBlocks.CREATIVE_DIMENSIONAL_TANK.get(),
                DfmBlocks.MECHANICAL_CRUSHER.get(),
                DfmBlocks.ELECTRIC_FURNACE.get(), DfmBlocks.MINER_TERMINAL.get(), DfmBlocks.STEEL_CHASSIS.get(), DfmBlocks.DIMENSIONAL_PORT.get(),
                DfmBlocks.REINFORCED_STRUCTURAL_GLASS.get(), DfmBlocks.VENTILATION_UNIT.get(), DfmBlocks.QUANTUM_CORE.get()};
        for (int i = 0; i < blocks.length; i++) {
            BlockPos pos = new BlockPos(i % 4, 1, i / 4);
            helper.setBlock(pos, blocks[i].defaultBlockState());
            var state = helper.getBlockState(pos);
            //? if <26.3 {
            for (var type : java.util.List.of(net.minecraft.world.entity.EntityType.ZOMBIE, net.minecraft.world.entity.EntityType.CREEPER,
                    net.minecraft.world.entity.EntityType.SPIDER)) {
            //?} else {
            /*for (var type : java.util.List.of(net.minecraft.world.entity.EntityTypes.ZOMBIE, net.minecraft.world.entity.EntityTypes.CREEPER,
                    net.minecraft.world.entity.EntityTypes.SPIDER)) {
            *///?}
                helper.assertFalse(state.isValidSpawn(helper.getLevel(), helper.absolutePos(pos), type), type + " spawns on " + blocks[i]);
            }
            if (state.hasProperty(dimensionalfluxminer.content.DfmStateProperties.FORMED)) {
                var formed = state.setValue(dimensionalfluxminer.content.DfmStateProperties.FORMED, true);
                //? if <26.3 {
                helper.assertFalse(formed.isValidSpawn(helper.getLevel(), helper.absolutePos(pos), net.minecraft.world.entity.EntityType.ZOMBIE),
                //?} else {
                /*helper.assertFalse(formed.isValidSpawn(helper.getLevel(), helper.absolutePos(pos), net.minecraft.world.entity.EntityTypes.ZOMBIE),
                *///?}
                        "zombie spawns on formed " + blocks[i]);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty_small")
    public static void outputOnlyFaceRefusesInput(GameTestHelper helper) {
        DimensionalTankBlockEntity be = place(helper, FIRST, TankKind.SINGLE, code(DyeColor.WHITE, DyeColor.BLACK, DyeColor.WHITE));
        helper.assertTrue(be.configureSide(RelativeFace.BACK, SideRoute.FLUID_OUTPUT), "output route");
        helper.assertFalse(be.configureSide(RelativeFace.BACK, SideRoute.FLUID_A_INPUT), "dual routes are not offered on the single tank");
        helper.assertFalse(be.configureSide(RelativeFace.FRONT, SideRoute.FLUID_INPUT), "the front stays locked");
        //? if >=26.1 {
        helper.assertTrue(face(helper, be, RelativeFace.BACK).fill(water(BUCKET), FluidTanks.FluidAction.EXECUTE) == 0, "no input there");
        //?} else {
        /*helper.assertTrue(face(helper, be, RelativeFace.BACK).fill(water(BUCKET), IFluidHandler.FluidAction.EXECUTE) == 0, "no input there");
        *///?}
        helper.assertTrue(be.configureSide(RelativeFace.BACK, SideRoute.NONE), "none");
        helper.assertTrue(face(helper, be, RelativeFace.BACK) == null, "no route, no handler");
        helper.succeed();
    }

    // ---------------------------------------------------------------- creative tank

    private static DimensionalTankBlockEntity placeCreative(GameTestHelper helper, BlockPos pos, Direction facing) {
        helper.setBlock(pos, DfmBlocks.CREATIVE_DIMENSIONAL_TANK.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing));
        //? if >=26.1 {
        return TestSupport.blockEntity(helper, pos);
        //?} else {
        /*return helper.getBlockEntity(pos);
        *///?}
    }

    /** A liquid poured in by a player becomes endless: draining gives what is asked and never lowers it. */
    @GameTest(template = "empty_small")
    public static void creativeTankGivesEndlessLiquid(GameTestHelper helper) {
        DimensionalTankBlockEntity be = placeCreative(helper, FIRST, Direction.NORTH);
        helper.assertTrue(be.kind() == TankKind.CREATIVE && be.chamber(0).isEmpty(), "starts empty");
        //? if >=26.1 {
        helper.assertTrue(be.chamberHandler(0).fill(water(BUCKET), FluidTanks.FluidAction.SIMULATE) == BUCKET && be.chamber(0).isEmpty(),
        //?} else {
        /*helper.assertTrue(be.chamberHandler(0).fill(water(BUCKET), IFluidHandler.FluidAction.SIMULATE) == BUCKET && be.chamber(0).isEmpty(),
        *///?}
                "a simulated fill picks nothing");
        //? if >=26.1 {
        helper.assertTrue(be.chamberHandler(0).fill(water(BUCKET), FluidTanks.FluidAction.EXECUTE) == BUCKET, "a bucket picks the liquid");
        //?} else {
        /*helper.assertTrue(be.chamberHandler(0).fill(water(BUCKET), IFluidHandler.FluidAction.EXECUTE) == BUCKET, "a bucket picks the liquid");
        *///?}
        helper.assertTrue(be.chamber(0).is(Fluids.WATER) && be.chamber(0).getAmount() == Integer.MAX_VALUE, "water, always full");
        //? if >=26.1 {
        FluidTanks back = face(helper, be, RelativeFace.BACK);
        //?} else {
        /*IFluidHandler back = face(helper, be, RelativeFace.BACK);
        *///?}
        for (int i = 0; i < 3; i++) {
            //? if >=26.1 {
            helper.assertTrue(back.drain(64 * BUCKET, FluidTanks.FluidAction.EXECUTE).getAmount() == 64 * BUCKET, "drained " + i);
        }
        helper.assertTrue(back.drain(lava(BUCKET), FluidTanks.FluidAction.EXECUTE).isEmpty(), "only its own liquid comes out");
            //?} else {
            /*helper.assertTrue(back.drain(64 * BUCKET, IFluidHandler.FluidAction.EXECUTE).getAmount() == 64 * BUCKET, "drained " + i);
        }
        helper.assertTrue(back.drain(lava(BUCKET), IFluidHandler.FluidAction.EXECUTE).isEmpty(), "only its own liquid comes out");
            *///?}
        helper.assertTrue(be.chamber(0).getAmount() == Integer.MAX_VALUE, "never runs out");
        //? if >=26.1 {
        helper.assertTrue(be.chamberHandler(0).fill(lava(BUCKET), FluidTanks.FluidAction.EXECUTE) == 0
                && back.fill(water(BUCKET), FluidTanks.FluidAction.EXECUTE) == 0, "a full creative tank takes nothing in");
        FluidTanks view = Transfer.fluids(helper.getLevel(), be.getBlockPos(), null);
        //?} else {
        /*helper.assertTrue(be.chamberHandler(0).fill(lava(BUCKET), IFluidHandler.FluidAction.EXECUTE) == 0
                && back.fill(water(BUCKET), IFluidHandler.FluidAction.EXECUTE) == 0, "a full creative tank takes nothing in");
        IFluidHandler view = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, be.getBlockPos(), null);
        *///?}
        helper.assertTrue(view != null && view.getTanks() == 1 && view.getTankCapacity(0) == Integer.MAX_VALUE, "one endless chamber");
        helper.succeed();
    }

    /** Empty, the creative tank swallows what pipes and automation send, and keeps nothing. */
    @GameTest(template = "empty_small")
    public static void emptyCreativeTankVoidsPipes(GameTestHelper helper) {
        DimensionalTankBlockEntity be = placeCreative(helper, FIRST, Direction.NORTH);
        //? if >=26.1 {
        FluidTanks back = face(helper, be, RelativeFace.BACK);
        helper.assertTrue(back.fill(lava(50 * BUCKET), FluidTanks.FluidAction.EXECUTE) == 50 * BUCKET, "accepted");
        //?} else {
        /*IFluidHandler back = face(helper, be, RelativeFace.BACK);
        helper.assertTrue(back.fill(lava(50 * BUCKET), IFluidHandler.FluidAction.EXECUTE) == 50 * BUCKET, "accepted");
        *///?}
        helper.assertTrue(be.chamber(0).isEmpty(), "and gone");
        //? if >=26.1 {
        helper.assertTrue(back.drain(BUCKET, FluidTanks.FluidAction.EXECUTE).isEmpty(), "nothing to drain");
        //?} else {
        /*helper.assertTrue(back.drain(BUCKET, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "nothing to drain");
        *///?}
        helper.succeed();
    }

    /** No code, no personal channel: the buttons are refused and nothing is used up; the clear button forgets the liquid. */
    @GameTest(template = "empty_small")
    public static void creativeTankHasNoChannels(GameTestHelper helper) {
        DimensionalTankBlockEntity be = placeCreative(helper, FIRST, Direction.NORTH);
        helper.assertTrue(be.channel() == null, "no channel");
        helper.assertFalse(be.handleButton(null, DimensionalTankBlockEntity.BUTTON_CODE_BASE + DyeColor.RED.getId()), "no code");
        be.setCode(code(DyeColor.RED, DyeColor.RED, DyeColor.RED));
        helper.assertTrue(be.code().equals(TankCode.DEFAULT), "the code never changes");
        net.minecraft.world.entity.player.Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        net.minecraft.world.item.ItemStack diamond = new net.minecraft.world.item.ItemStack(dimensionalfluxminer.DfmConfig.tankPersonalItem());
        helper.assertFalse(be.makePersonal(player, diamond), "no personal code");
        helper.assertTrue(diamond.getCount() == 1, "the personal item is not used");
        helper.assertFalse(be.handleButton(null, DimensionalTankBlockEntity.BUTTON_CLEAR), "nothing to clear");
        //? if >=26.1 {
        be.chamberHandler(0).fill(water(BUCKET), FluidTanks.FluidAction.EXECUTE);
        //?} else {
        /*be.chamberHandler(0).fill(water(BUCKET), IFluidHandler.FluidAction.EXECUTE);
        *///?}
        helper.assertTrue(be.handleButton(null, DimensionalTankBlockEntity.BUTTON_CLEAR) && be.chamber(0).isEmpty(), "cleared");
        helper.assertFalse(DfmBlocks.DIMENSIONAL_TANK.get().defaultBlockState().is(DfmBlocks.CREATIVE_DIMENSIONAL_TANK.get()), "a block of its own");
        helper.succeed();
    }

    /** The item and the save keep the liquid; placing the item gives the tank that liquid. */
    @GameTest(template = "empty_small")
    public static void creativeTankKeepsItsLiquid(GameTestHelper helper) {
        DimensionalTankBlockEntity be = placeCreative(helper, FIRST, Direction.NORTH);
        //? if >=26.1 {
        be.chamberHandler(0).fill(lava(BUCKET), FluidTanks.FluidAction.EXECUTE);
        //?} else {
        /*be.chamberHandler(0).fill(lava(BUCKET), IFluidHandler.FluidAction.EXECUTE);
        *///?}
        var stored = be.collectComponents().get(DfmDataComponents.CREATIVE_TANK_FLUID.get());
        helper.assertTrue(stored != null && stored.copy().is(Fluids.LAVA), "the item gets the lava");
        helper.assertTrue(be.collectComponents().get(DfmDataComponents.TANK_CODE.get()) == null, "and no code");
        CompoundTag saved = be.saveWithoutMetadata(helper.getLevel().registryAccess());
        DimensionalTankBlockEntity again = placeCreative(helper, SECOND, Direction.NORTH);
        //? if >=26.1 {
        TestSupport.load(again, saved, helper.getLevel().registryAccess());
        //?} else {
        /*again.loadWithComponents(saved, helper.getLevel().registryAccess());
        *///?}
        helper.assertTrue(again.chamber(0).is(Fluids.LAVA) && again.chamber(0).getAmount() == Integer.MAX_VALUE, "loaded");
        helper.assertTrue(again.lightLevel() == 15, "lava lights the area");
        helper.succeed();
    }

    /** One filled creative tank per source liquid of the game, each a different stack. */
    @GameTest(template = "empty_small")
    public static void creativeTanksForEveryLiquid(GameTestHelper helper) {
        var stacks = dimensionalfluxminer.tank.CreativeTanks.filledForEveryFluid(helper.getLevel().registryAccess());
        java.util.Set<net.minecraft.world.level.material.Fluid> fluids = new java.util.HashSet<>();
        for (var stack : stacks) {
            var content = stack.get(DfmDataComponents.CREATIVE_TANK_FLUID.get());
            helper.assertTrue(content != null && !content.isEmpty(), "every stack holds a liquid");
            helper.assertTrue(fluids.add(content.copy().getFluid()), "once per liquid");
        }
        helper.assertTrue(fluids.contains(Fluids.WATER) && fluids.contains(Fluids.LAVA), "water and lava");
        helper.assertFalse(fluids.contains(Fluids.FLOWING_WATER) || fluids.contains(Fluids.EMPTY), "only sources");
        helper.succeed();
    }

    /** Auto Eject fills the neighbor without a per-tick limit and the creative tank stays full. */
    @GameTest(template = "empty_small")
    public static void creativeTankEjectsWithoutEnd(GameTestHelper helper) {
        DimensionalTankBlockEntity creative = placeCreative(helper, new BlockPos(2, 1, 1), Direction.NORTH);
        DimensionalTankBlockEntity to = place(helper, new BlockPos(2, 1, 2), TankKind.SINGLE, Direction.EAST,
                code(DyeColor.MAGENTA, DyeColor.BROWN, DyeColor.MAGENTA));
        //? if >=26.1 {
        creative.chamberHandler(0).fill(water(BUCKET), FluidTanks.FluidAction.EXECUTE);
        //?} else {
        /*creative.chamberHandler(0).fill(water(BUCKET), IFluidHandler.FluidAction.EXECUTE);
        *///?}
        helper.assertTrue(creative.handleButton(null, AbstractMachineBlockEntity.BUTTON_AUTO_EJECT_FLUID), "Auto Eject on");
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(to.chamber(0).is(Fluids.WATER) && to.chamber(0).getAmount() == TankKind.CHAMBER_CAPACITY, "the neighbor is full");
            helper.assertTrue(creative.chamber(0).getAmount() == Integer.MAX_VALUE, "still endless");
            helper.succeed();
        });
    }

    // ---------------------------------------------------------------- bucket mode

    //? if >=26.1 {
    /** The item's fluid capability, reached through the stack itself (null without one). */
    private static FluidTanks itemFluids(net.minecraft.world.item.ItemStack stack) {
        return Transfer.fluids(net.neoforged.neoforge.transfer.access.ItemAccess.forStack(stack).getCapability(Capabilities.Fluid.ITEM));
    }

    //?}
    private static net.minecraft.world.item.ItemStack bucketMode(net.minecraft.world.item.ItemStack stack) {
        stack.set(DfmDataComponents.TANK_BUCKET_MODE.get(), true);
        return stack;
    }

    /** Single and creative tank items are fluid containers in any mode (they fill tanks); the dual tank never. */
    @GameTest(template = "empty_small")
    public static void tankItemIsAContainerInAnyMode(GameTestHelper helper) {
        var single = new net.minecraft.world.item.ItemStack(dimensionalfluxminer.registry.DfmItems.DIMENSIONAL_TANK.get());
        //? if >=26.1 {
        helper.assertTrue(itemFluids(single) != null, "a container with bucket mode off");
        helper.assertTrue(itemFluids(bucketMode(single)) != null, "and on");
        //?} else {
        /*helper.assertTrue(single.getCapability(Capabilities.FluidHandler.ITEM) != null, "a container with bucket mode off");
        helper.assertTrue(bucketMode(single).getCapability(Capabilities.FluidHandler.ITEM) != null, "and on");
        *///?}
        var dual = bucketMode(new net.minecraft.world.item.ItemStack(dimensionalfluxminer.registry.DfmItems.DIMENSIONAL_DUAL_TANK.get()));
        //? if >=26.1 {
        helper.assertTrue(itemFluids(dual) == null, "never the dual tank");
        //?} else {
        /*helper.assertTrue(dual.getCapability(Capabilities.FluidHandler.ITEM) == null, "never the dual tank");
        *///?}
        var creative = bucketMode(new net.minecraft.world.item.ItemStack(dimensionalfluxminer.registry.DfmItems.CREATIVE_DIMENSIONAL_TANK.get()));
        //? if >=26.1 {
        helper.assertTrue(itemFluids(creative) != null, "the creative tank too");
        //?} else {
        /*helper.assertTrue(creative.getCapability(Capabilities.FluidHandler.ITEM) != null, "the creative tank too");
        *///?}
        helper.succeed();
    }

    /** A single tank item in bucket mode is its channel: what it takes or gives is the channel's liquid. */
    @GameTest(template = "empty_small")
    public static void tankItemUsesItsChannel(GameTestHelper helper) {
        TankCode code = code(DyeColor.CYAN, DyeColor.GRAY, DyeColor.CYAN);
        DimensionalTankBlockEntity be = place(helper, FIRST, TankKind.SINGLE, code);
        var stack = bucketMode(new net.minecraft.world.item.ItemStack(dimensionalfluxminer.registry.DfmItems.DIMENSIONAL_TANK.get()));
        stack.set(DfmDataComponents.TANK_CODE.get(), code);
        //? if >=26.1 {
        FluidTanks item = itemFluids(stack);
        helper.assertTrue(item.fill(water(5 * BUCKET), FluidTanks.FluidAction.EXECUTE) == 5 * BUCKET, "filled");
        //?} else {
        /*IFluidHandler item = stack.getCapability(Capabilities.FluidHandler.ITEM);
        helper.assertTrue(item.fill(water(5 * BUCKET), IFluidHandler.FluidAction.EXECUTE) == 5 * BUCKET, "filled");
        *///?}
        helper.assertTrue(be.chamber(0).getAmount() == 5 * BUCKET, "the tank of the same code sees it");
        // Pouring one bucket on the ground takes it out of the channel.
        BlockPos ground = helper.absolutePos(new BlockPos(3, 1, 1));
        //? if >=26.1 {
        var handler = net.neoforged.neoforge.transfer.access.ItemAccess.forStack(stack).getCapability(Capabilities.Fluid.ITEM);
        helper.assertTrue(!net.neoforged.neoforge.transfer.fluid.FluidUtil.tryPlaceFluid(handler, null, helper.getLevel(), ground, true, null).isEmpty(),
                "poured");
        //?} else {
        /*helper.assertTrue(net.neoforged.neoforge.fluids.FluidUtil.tryPlaceFluid(null, helper.getLevel(), net.minecraft.world.InteractionHand.MAIN_HAND,
                ground, item, water(BUCKET)), "poured");
        *///?}
        helper.assertTrue(helper.getLevel().getFluidState(ground).is(Fluids.WATER), "water on the ground");
        helper.assertTrue(be.chamber(0).getAmount() == 4 * BUCKET, "one bucket less");
        helper.succeed();
    }

    /** A creative tank item never runs out and fills another tank completely in one go. */
    @GameTest(template = "empty_small")
    public static void creativeTankItemFillsATank(GameTestHelper helper) {
        DimensionalTankBlockEntity be = place(helper, FIRST, TankKind.SINGLE, code(DyeColor.PINK, DyeColor.GRAY, DyeColor.PINK));
        var empty = bucketMode(new net.minecraft.world.item.ItemStack(dimensionalfluxminer.registry.DfmItems.CREATIVE_DIMENSIONAL_TANK.get()));
        //? if >=26.1 {
        FluidTanks picker = itemFluids(empty);
        helper.assertTrue(picker.fill(lava(BUCKET), FluidTanks.FluidAction.EXECUTE) == BUCKET, "an empty creative item picks its liquid");
        //?} else {
        /*IFluidHandler picker = empty.getCapability(Capabilities.FluidHandler.ITEM);
        helper.assertTrue(picker.fill(lava(BUCKET), IFluidHandler.FluidAction.EXECUTE) == BUCKET, "an empty creative item picks its liquid");
        *///?}
        helper.assertTrue(dimensionalfluxminer.tank.TankItemFluidHandler.creativeFluid(empty).is(Fluids.LAVA), "kept on the item");
        var stack = bucketMode(dimensionalfluxminer.tank.CreativeTanks.filled(Fluids.WATER));
        //? if >=26.1 {
        FluidTanks item = itemFluids(stack);
        var moved = FluidTanks.move(face(helper, be, RelativeFace.BACK), item, Integer.MAX_VALUE);
        //?} else {
        /*IFluidHandler item = stack.getCapability(Capabilities.FluidHandler.ITEM);
        var moved = net.neoforged.neoforge.fluids.FluidUtil.tryFluidTransfer(face(helper, be, RelativeFace.BACK), item, Integer.MAX_VALUE, true);
        *///?}
        helper.assertTrue(moved.getAmount() == TankKind.CHAMBER_CAPACITY && be.chamber(0).getAmount() == TankKind.CHAMBER_CAPACITY, "full at once");
        helper.assertTrue(item.getFluidInTank(0).getAmount() == Integer.MAX_VALUE, "still endless");
        helper.succeed();
    }

    /** Empty deletes a chamber's liquid from the channel, for every tank with the code; the other chamber stays. */
    @GameTest(template = "empty_small")
    public static void emptyButtonClearsTheChannel(GameTestHelper helper) {
        TankCode code = code(DyeColor.LIGHT_BLUE, DyeColor.YELLOW, DyeColor.LIGHT_BLUE);
        DimensionalTankBlockEntity first = place(helper, FIRST, TankKind.DUAL, code);
        DimensionalTankBlockEntity second = place(helper, SECOND, TankKind.DUAL, code);
        //? if >=26.1 {
        first.chamberHandler(0).fill(water(3 * BUCKET), FluidTanks.FluidAction.EXECUTE);
        first.chamberHandler(1).fill(lava(2 * BUCKET), FluidTanks.FluidAction.EXECUTE);
        //?} else {
        /*first.chamberHandler(0).fill(water(3 * BUCKET), IFluidHandler.FluidAction.EXECUTE);
        first.chamberHandler(1).fill(lava(2 * BUCKET), IFluidHandler.FluidAction.EXECUTE);
        *///?}
        helper.assertTrue(second.handleButton(null, DimensionalTankBlockEntity.BUTTON_CLEAR + 1), "B emptied");
        helper.assertTrue(first.chamber(1).isEmpty() && first.chamber(0).getAmount() == 3 * BUCKET, "only B, on every tank of the code");
        helper.assertFalse(first.handleButton(null, DimensionalTankBlockEntity.BUTTON_CLEAR + 1), "nothing left in B");
        helper.assertFalse(first.handleButton(null, DimensionalTankBlockEntity.BUTTON_CLEAR + 2), "no third chamber");
        helper.succeed();
    }
}
