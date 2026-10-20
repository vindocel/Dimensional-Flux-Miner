package dimensionalfluxminer.gametest;

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
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Dimensional Tanks. Channels belong to the whole save and the tests run together, so every test uses a code of its
 * own (only the first one reads the default white code).
 */
@GameTestHolder(DimensionalFluxMiner.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TankGameTests {
    private static final BlockPos FIRST = new BlockPos(1, 1, 1);
    private static final BlockPos SECOND = new BlockPos(3, 1, 3);
    private static final int BUCKET = 1000;

    private TankGameTests() {
    }

    private static DimensionalTankBlockEntity place(GameTestHelper helper, BlockPos pos, TankKind kind, Direction facing, TankCode code) {
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

    @GameTest(template = "empty_small")
    public static void newTankUsesWhiteCodeAndHasNoEnergy(GameTestHelper helper) {
        DimensionalTankBlockEntity be = place(helper, FIRST, TankKind.SINGLE, null);
        helper.assertTrue(be.code().equals(TankCode.DEFAULT), "white, white, white");
        for (Direction side : Direction.values()) {
            helper.assertTrue(helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, be.getBlockPos(), side) == null,
                    "no energy on " + side);
            helper.assertTrue(helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, be.getBlockPos(), side) == null,
                    "no items on " + side);
        }
        helper.assertTrue(face(helper, be, RelativeFace.FRONT) == null, "the front (panel) has no pipe access");
        for (RelativeFace f : new RelativeFace[] {RelativeFace.BACK, RelativeFace.LEFT, RelativeFace.RIGHT, RelativeFace.UP, RelativeFace.DOWN}) {
            helper.assertTrue(face(helper, be, f) != null, "pipes connect without configuration: " + f);
        }
        IFluidHandler view = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, be.getBlockPos(), null);
        helper.assertTrue(view != null && view.getTanks() == 1 && view.getTankCapacity(0) == 128_000, "one chamber of 128 000 mB");
        helper.succeed();
    }

    @GameTest(template = "empty_small")
    public static void sameCodeSharesOneReserve(GameTestHelper helper) {
        TankCode code = code(DyeColor.RED, DyeColor.BLUE, DyeColor.WHITE);
        DimensionalTankBlockEntity first = place(helper, FIRST, TankKind.SINGLE, code);
        DimensionalTankBlockEntity second = place(helper, SECOND, TankKind.SINGLE, code);
        IFluidHandler in = face(helper, first, RelativeFace.BACK);
        IFluidHandler out = face(helper, second, RelativeFace.UP);
        helper.assertTrue(in.fill(water(4 * BUCKET), IFluidHandler.FluidAction.SIMULATE) == 4 * BUCKET, "simulated fill accepts");
        helper.assertTrue(out.getFluidInTank(0).isEmpty(), "simulation changes nothing");
        helper.assertTrue(in.fill(water(4 * BUCKET), IFluidHandler.FluidAction.EXECUTE) == 4 * BUCKET, "filled once");
        helper.assertTrue(out.getFluidInTank(0).getAmount() == 4 * BUCKET, "the other tank sees the same water");
        helper.assertTrue(out.drain(BUCKET, IFluidHandler.FluidAction.SIMULATE).getAmount() == BUCKET
                && out.getFluidInTank(0).getAmount() == 4 * BUCKET, "simulated drain changes nothing");
        // Two takers never get more than the balance.
        helper.assertTrue(out.drain(3 * BUCKET, IFluidHandler.FluidAction.EXECUTE).getAmount() == 3 * BUCKET, "first taker");
        helper.assertTrue(in.drain(5 * BUCKET, IFluidHandler.FluidAction.EXECUTE).getAmount() == BUCKET, "second taker gets the rest");
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
            face(helper, first, RelativeFace.BACK).fill(water(BUCKET), IFluidHandler.FluidAction.EXECUTE);
            int version = first.channel().version();
            helper.runAfterDelay(4, () -> {
                helper.assertTrue(first.syncedVersion() == version, "the tank that was filled updated its clients");
                helper.assertTrue(second.syncedVersion() == version, "the other endpoint updated its clients too");
                var registries = helper.getLevel().registryAccess();
                var sent = DimensionalTankBlockEntity.readChambers(second.getUpdateTag(registries).getList("chambers", 10), 1, registries);
                helper.assertTrue(sent[0].is(Fluids.WATER) && sent[0].getAmount() == BUCKET, "the update carries the liquid");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty_small")
    public static void colorOrderMatters(GameTestHelper helper) {
        DimensionalTankBlockEntity first = place(helper, FIRST, TankKind.SINGLE, code(DyeColor.LIME, DyeColor.PINK, DyeColor.GRAY));
        DimensionalTankBlockEntity second = place(helper, SECOND, TankKind.SINGLE, code(DyeColor.PINK, DyeColor.LIME, DyeColor.GRAY));
        face(helper, first, RelativeFace.BACK).fill(water(BUCKET), IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(second.chamber(0).isEmpty(), "two colors swapped = another channel");
        helper.succeed();
    }

    @GameTest(template = "empty_small")
    public static void singleAndDualNeverShare(GameTestHelper helper) {
        TankCode code = code(DyeColor.CYAN, DyeColor.CYAN, DyeColor.BLACK);
        DimensionalTankBlockEntity single = place(helper, FIRST, TankKind.SINGLE, code);
        DimensionalTankBlockEntity dual = place(helper, SECOND, TankKind.DUAL, code);
        face(helper, single, RelativeFace.BACK).fill(water(BUCKET), IFluidHandler.FluidAction.EXECUTE);
        face(helper, dual, RelativeFace.RIGHT).fill(lava(2 * BUCKET), IFluidHandler.FluidAction.EXECUTE);
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
        IFluidHandler a = face(helper, first, RelativeFace.RIGHT);
        IFluidHandler b = face(helper, first, RelativeFace.LEFT);
        helper.assertTrue(a.fill(water(TankKind.CHAMBER_CAPACITY), IFluidHandler.FluidAction.EXECUTE) == TankKind.CHAMBER_CAPACITY, "A full");
        helper.assertTrue(b.fill(lava(3 * BUCKET), IFluidHandler.FluidAction.EXECUTE) == 3 * BUCKET, "lava in B");
        helper.assertTrue(a.fill(water(BUCKET), IFluidHandler.FluidAction.EXECUTE) == 0, "a full A never spills into B");
        helper.assertTrue(a.fill(lava(BUCKET), IFluidHandler.FluidAction.EXECUTE) == 0, "another liquid is refused");
        helper.assertTrue(b.fill(water(BUCKET), IFluidHandler.FluidAction.EXECUTE) == 0, "water refused in the lava chamber");
        helper.assertTrue(b.drain(water(BUCKET), IFluidHandler.FluidAction.EXECUTE).isEmpty(), "B face never drains A");
        // The other dual (another facing, same code): A with A, B with B.
        helper.assertTrue(second.chamber(0).is(Fluids.WATER) && second.chamber(0).getAmount() == TankKind.CHAMBER_CAPACITY, "A shared");
        helper.assertTrue(second.chamber(1).is(Fluids.LAVA) && second.chamber(1).getAmount() == 3 * BUCKET, "B shared");
        face(helper, second, RelativeFace.RIGHT).drain(BUCKET, IFluidHandler.FluidAction.EXECUTE);
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
        face(helper, be, RelativeFace.BACK).fill(water(5 * BUCKET), IFluidHandler.FluidAction.EXECUTE);
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
        face(helper, be, RelativeFace.BACK).fill(water(7 * BUCKET), IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(code.equals(be.collectComponents().get(DfmDataComponents.TANK_CODE.get())), "the item gets the code");
        CompoundTag saved = be.saveWithoutMetadata(helper.getLevel().registryAccess());
        helper.destroyBlock(FIRST);
        TankChannels channels = TankChannels.get(helper.getLevel().getServer());
        helper.assertTrue(channels.channel(TankKind.SINGLE, code).get(0).getAmount() == 7 * BUCKET, "the channel keeps the water");
        // A tank loaded from the save (or placed from the item) is back on the same channel.
        DimensionalTankBlockEntity again = place(helper, SECOND, TankKind.SINGLE, null);
        again.loadWithComponents(saved, helper.getLevel().registryAccess());
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
        face(helper, from, RelativeFace.BACK).fill(water(10 * BUCKET), IFluidHandler.FluidAction.EXECUTE);
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
        face(helper, open, RelativeFace.BACK).fill(water(BUCKET), IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(mine.chamber(0).isEmpty(), "the public channel with the same colors is another one");
        face(helper, mine, RelativeFace.BACK).fill(lava(2 * BUCKET), IFluidHandler.FluidAction.EXECUTE);
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
        face(helper, dual, RelativeFace.RIGHT).fill(water(TankKind.CHAMBER_CAPACITY), IFluidHandler.FluidAction.EXECUTE);
        face(helper, dual, RelativeFace.LEFT).fill(lava(TankKind.CHAMBER_CAPACITY), IFluidHandler.FluidAction.EXECUTE);
        dual.setAutoEjectFluid(true);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(dual.chamber(0).isEmpty() && dual.chamber(1).isEmpty(),
                    "dual emptied: A " + dual.chamber(0).getAmount() + " mB, B " + dual.chamber(1).getAmount() + " mB");
            helper.assertTrue(west.chamber(0).is(Fluids.WATER) && west.chamber(0).getAmount() == TankKind.CHAMBER_CAPACITY, "A went west");
            helper.assertTrue(east.chamber(0).is(Fluids.LAVA) && east.chamber(0).getAmount() == TankKind.CHAMBER_CAPACITY, "B went east");
            // An ejecting face takes nothing back from outside (a pipe network would bounce the last mB back).
            helper.assertTrue(face(helper, dual, RelativeFace.RIGHT).fill(water(BUCKET), IFluidHandler.FluidAction.EXECUTE) == 0,
                    "ejecting face refuses input");
            dual.setAutoEjectFluid(false);
            helper.assertTrue(face(helper, dual, RelativeFace.RIGHT).fill(water(BUCKET), IFluidHandler.FluidAction.EXECUTE) == BUCKET,
                    "Auto Eject off: the face takes input again");
            helper.succeed();
        });
    }

    @GameTest(template = "empty_small")
    public static void outputOnlyFaceRefusesInput(GameTestHelper helper) {
        DimensionalTankBlockEntity be = place(helper, FIRST, TankKind.SINGLE, code(DyeColor.WHITE, DyeColor.BLACK, DyeColor.WHITE));
        helper.assertTrue(be.configureSide(RelativeFace.BACK, SideRoute.FLUID_OUTPUT), "output route");
        helper.assertFalse(be.configureSide(RelativeFace.BACK, SideRoute.FLUID_A_INPUT), "dual routes are not offered on the single tank");
        helper.assertFalse(be.configureSide(RelativeFace.FRONT, SideRoute.FLUID_INPUT), "the front stays locked");
        helper.assertTrue(face(helper, be, RelativeFace.BACK).fill(water(BUCKET), IFluidHandler.FluidAction.EXECUTE) == 0, "no input there");
        helper.assertTrue(be.configureSide(RelativeFace.BACK, SideRoute.NONE), "none");
        helper.assertTrue(face(helper, be, RelativeFace.BACK) == null, "no route, no handler");
        helper.succeed();
    }
}
