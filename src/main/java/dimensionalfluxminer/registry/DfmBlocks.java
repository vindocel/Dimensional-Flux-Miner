package dimensionalfluxminer.registry;

import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.content.CorruptedGrassPlantBlock;
import dimensionalfluxminer.content.CorruptedTallGrassBlock;
import dimensionalfluxminer.crusher.MechanicalCrusherBlock;
import dimensionalfluxminer.miner.block.MinerChassisBlock;
import dimensionalfluxminer.miner.block.MinerPartBlock;
import dimensionalfluxminer.miner.block.MinerPortBlock;
import dimensionalfluxminer.miner.block.MinerTerminalBlock;
import dimensionalfluxminer.furnace.ElectricFurnaceBlock;
import dimensionalfluxminer.content.CorruptedWaterPlants;
import dimensionalfluxminer.tank.DimensionalTankBlock;
import dimensionalfluxminer.tank.TankKind;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class DfmBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(DimensionalFluxMiner.MOD_ID);

    // Dimensional Flux Miner assembly parts
    //? if >=26.1 {
    public static final DeferredBlock<MinerChassisBlock> STEEL_CHASSIS = BLOCKS.registerBlock("steel_chassis", MinerChassisBlock::new, DfmBlocks::assemblyPart);
    public static final DeferredBlock<MinerPartBlock> VENTILATION_UNIT = BLOCKS.registerBlock("ventilation_unit", MinerPartBlock::new, DfmBlocks::assemblyPart);
    //?} else {
    /*public static final DeferredBlock<MinerChassisBlock> STEEL_CHASSIS = BLOCKS.registerBlock("steel_chassis", MinerChassisBlock::new, assemblyPart());
    public static final DeferredBlock<MinerPartBlock> VENTILATION_UNIT = BLOCKS.registerBlock("ventilation_unit", MinerPartBlock::new, assemblyPart());
    *///?}
    public static final DeferredBlock<MinerPartBlock> REINFORCED_STRUCTURAL_GLASS = BLOCKS.registerBlock("reinforced_structural_glass", MinerPartBlock::new,
            //? if >=26.1 {
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(3.0F, 1200.0F).sound(SoundType.GLASS)
            //?} else {
            /*BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(3.0F, 1200.0F).sound(SoundType.GLASS)
            *///?}
                    .requiresCorrectToolForDrops().noOcclusion().isValidSpawn(Blocks::never).isRedstoneConductor((s, l, p) -> false)
                    //? if =26.1.2 {
                    .isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false).pushReaction(PushReaction.BLOCK));
    public static final DeferredBlock<MinerPortBlock> DIMENSIONAL_PORT = BLOCKS.registerBlock("dimensional_port", MinerPortBlock::new, DfmBlocks::assemblyPart);
    public static final DeferredBlock<MinerPartBlock> QUANTUM_CORE = BLOCKS.registerBlock("quantum_core", MinerPartBlock::new, DfmBlocks::assemblyPart);
    public static final DeferredBlock<MinerTerminalBlock> MINER_TERMINAL = BLOCKS.registerBlock("miner_terminal", MinerTerminalBlock::new, DfmBlocks::assemblyPart);
                    //?} else if >=26.3 {
                    /*.isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p, a) -> false).pushReaction(PushReaction.IMMOVEABLE));
    public static final DeferredBlock<MinerPortBlock> DIMENSIONAL_PORT = BLOCKS.registerBlock("dimensional_port", MinerPortBlock::new, DfmBlocks::assemblyPart);
    public static final DeferredBlock<MinerPartBlock> QUANTUM_CORE = BLOCKS.registerBlock("quantum_core", MinerPartBlock::new, DfmBlocks::assemblyPart);
    public static final DeferredBlock<MinerTerminalBlock> MINER_TERMINAL = BLOCKS.registerBlock("miner_terminal", MinerTerminalBlock::new, DfmBlocks::assemblyPart);
                    *///?} else {
                    /*.isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false).pushReaction(PushReaction.BLOCK));
    public static final DeferredBlock<MinerPortBlock> DIMENSIONAL_PORT = BLOCKS.registerBlock("dimensional_port", MinerPortBlock::new, assemblyPart());
    public static final DeferredBlock<MinerPartBlock> QUANTUM_CORE = BLOCKS.registerBlock("quantum_core", MinerPartBlock::new, assemblyPart());
    public static final DeferredBlock<MinerTerminalBlock> MINER_TERMINAL = BLOCKS.registerBlock("miner_terminal", MinerTerminalBlock::new, assemblyPart());
                    *///?}

    // Single-block machines
    public static final DeferredBlock<MechanicalCrusherBlock> MECHANICAL_CRUSHER = BLOCKS.registerBlock("mechanical_crusher", MechanicalCrusherBlock::new,
            //? if >=26.1 {
            () -> machineCasing().noOcclusion());
            //?} else {
            /*machineCasing().noOcclusion());
            *///?}
    public static final DeferredBlock<ElectricFurnaceBlock> ELECTRIC_FURNACE = BLOCKS.registerBlock("electric_furnace", ElectricFurnaceBlock::new,
            //? if >=26.1 {
            () -> machineCasing().noOcclusion().lightLevel(ElectricFurnaceBlock::lightFromState));
            //?} else {
            /*machineCasing().noOcclusion().lightLevel(ElectricFurnaceBlock::lightFromState));
            *///?}

    // Dimensional Tanks
    public static final DeferredBlock<DimensionalTankBlock> DIMENSIONAL_TANK = BLOCKS.registerBlock("dimensional_tank",
            //? if >=26.1 {
            props -> new DimensionalTankBlock(TankKind.SINGLE, props), () -> machineCasing().noOcclusion());
            //?} else {
            /*props -> new DimensionalTankBlock(TankKind.SINGLE, props), machineCasing().noOcclusion());
            *///?}
    public static final DeferredBlock<DimensionalTankBlock> DIMENSIONAL_DUAL_TANK = BLOCKS.registerBlock("dimensional_dual_tank",
            //? if >=26.1 {
            props -> new DimensionalTankBlock(TankKind.DUAL, props), () -> machineCasing().noOcclusion());
            //?} else {
            /*props -> new DimensionalTankBlock(TankKind.DUAL, props), machineCasing().noOcclusion());
            *///?}
    /** Creative only (no recipe): one chamber of endless liquid, no channels. */
    public static final DeferredBlock<DimensionalTankBlock> CREATIVE_DIMENSIONAL_TANK = BLOCKS.registerBlock("creative_dimensional_tank",
            //? if >=26.1 {
            props -> new DimensionalTankBlock(TankKind.CREATIVE, props), () -> machineCasing().noOcclusion());
            //?} else {
            /*props -> new DimensionalTankBlock(TankKind.CREATIVE, props), machineCasing().noOcclusion());
            *///?}

    // Materials
    public static final DeferredBlock<Block> SLAG_AGGREGATE = BLOCKS.registerSimpleBlock("slag_aggregate",
            //? if >=26.1 {
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).instrument(NoteBlockInstrument.BASEDRUM)
            //?} else {
            /*BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).instrument(NoteBlockInstrument.BASEDRUM)
            *///?}
                    .requiresCorrectToolForDrops().strength(2.0F, 6.0F).sound(SoundType.STONE));

    // Dimensional Blight
    public static final DeferredBlock<Block> CORRUPTED_DIRT = BLOCKS.registerSimpleBlock("corrupted_dirt",
            //? if >=26.1 {
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(0.5F).sound(SoundType.GRAVEL));
            //?} else {
            /*BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(0.5F).sound(SoundType.GRAVEL));
            *///?}
    public static final DeferredBlock<Block> CORRUPTED_GRASS = BLOCKS.registerSimpleBlock("corrupted_grass",
            //? if >=26.1 {
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(0.6F).sound(SoundType.GRASS));
            //?} else {
            /*BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(0.6F).sound(SoundType.GRASS));
            *///?}
    public static final DeferredBlock<Block> CORRUPTED_STONE = BLOCKS.registerSimpleBlock("corrupted_stone",
            //? if >=26.1 {
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).instrument(NoteBlockInstrument.BASEDRUM)
            //?} else {
            /*BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).instrument(NoteBlockInstrument.BASEDRUM)
            *///?}
                    .requiresCorrectToolForDrops().strength(1.5F, 6.0F));
    /** Replaces every terracotta (plain, colored and glazed). */
    public static final DeferredBlock<Block> CORRUPTED_TERRACOTTA = BLOCKS.registerSimpleBlock("corrupted_terracotta",
            //? if >=26.1 {
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_PURPLE).instrument(NoteBlockInstrument.BASEDRUM)
            //?} else {
            /*BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_PURPLE).instrument(NoteBlockInstrument.BASEDRUM)
            *///?}
                    .requiresCorrectToolForDrops().strength(1.25F, 4.2F));
    /** Replaces sands and gravels; falls like them and smelts into glass. */
    public static final DeferredBlock<ColoredFallingBlock> CORRUPTED_SEDIMENT = BLOCKS.registerBlock("corrupted_sediment",
            props -> new ColoredFallingBlock(new ColorRGBA(0x4A3550), props),
            //? if >=26.1 {
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).instrument(NoteBlockInstrument.SNARE)
            //?} else {
            /*BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).instrument(NoteBlockInstrument.SNARE)
            *///?}
                    .strength(0.5F).sound(SoundType.SAND));
    /** Kelp and seagrass holding Corrupted Water: same look, behaviour and drops as the vanilla plants. */
    public static final DeferredBlock<CorruptedWaterPlants.Kelp> CORRUPTED_KELP = BLOCKS.registerBlock("corrupted_kelp",
            //? if >=26.1 {
            CorruptedWaterPlants.Kelp::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.KELP).overrideLootTable(Blocks.KELP.getLootTable()));
            //?} else {
            /*CorruptedWaterPlants.Kelp::new, BlockBehaviour.Properties.ofFullCopy(Blocks.KELP).dropsLike(Blocks.KELP));
            *///?}
    public static final DeferredBlock<CorruptedWaterPlants.KelpPlant> CORRUPTED_KELP_PLANT = BLOCKS.registerBlock("corrupted_kelp_plant",
            //? if >=26.1 {
            CorruptedWaterPlants.KelpPlant::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.KELP_PLANT).overrideLootTable(Blocks.KELP_PLANT.getLootTable()));
            //?} else {
            /*CorruptedWaterPlants.KelpPlant::new, BlockBehaviour.Properties.ofFullCopy(Blocks.KELP_PLANT).dropsLike(Blocks.KELP_PLANT));
            *///?}
    public static final DeferredBlock<CorruptedWaterPlants.Seagrass> CORRUPTED_SEAGRASS = BLOCKS.registerBlock("corrupted_seagrass",
            //? if >=26.1 {
            CorruptedWaterPlants.Seagrass::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.SEAGRASS).overrideLootTable(Blocks.SEAGRASS.getLootTable()));
            //?} else {
            /*CorruptedWaterPlants.Seagrass::new, BlockBehaviour.Properties.ofFullCopy(Blocks.SEAGRASS).dropsLike(Blocks.SEAGRASS));
            *///?}
    public static final DeferredBlock<CorruptedWaterPlants.TallSeagrass> CORRUPTED_TALL_SEAGRASS = BLOCKS.registerBlock("corrupted_tall_seagrass",
            //? if >=26.1 {
            CorruptedWaterPlants.TallSeagrass::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.TALL_SEAGRASS).overrideLootTable(Blocks.TALL_SEAGRASS.getLootTable()));
            //?} else {
            /*CorruptedWaterPlants.TallSeagrass::new, BlockBehaviour.Properties.ofFullCopy(Blocks.TALL_SEAGRASS).dropsLike(Blocks.TALL_SEAGRASS));
            *///?}
    public static final DeferredBlock<CorruptedGrassPlantBlock> CORRUPTED_GRASS_PLANT = BLOCKS.registerBlock("corrupted_grass_plant", CorruptedGrassPlantBlock::new,
            //? if =26.1.2 {
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).replaceable().noCollision().instabreak()
                    .sound(SoundType.GRASS).offsetType(BlockBehaviour.OffsetType.XYZ).ignitedByLava().pushReaction(PushReaction.DESTROY));
            //?} else if >=26.3 {
            /*() -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).replaceable().noCollision().instabreak()
                    .sound(SoundType.GRASS).offsetType(BlockBehaviour.OffsetType.XYZ).ignitedByLava().pushReaction(PushReaction.POPPED));
            *///?} else {
            /*BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).replaceable().noCollission().instabreak()
                    .sound(SoundType.GRASS).offsetType(BlockBehaviour.OffsetType.XYZ).ignitedByLava().pushReaction(PushReaction.DESTROY));
            *///?}
    /** Same shape and behaviour as vanilla tall grass, standing on corrupted ground. */
    public static final DeferredBlock<CorruptedTallGrassBlock> CORRUPTED_TALL_GRASS = BLOCKS.registerBlock("corrupted_tall_grass", CorruptedTallGrassBlock::new,
            //? if =26.1.2 {
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).replaceable().noCollision().instabreak()
                    .sound(SoundType.GRASS).offsetType(BlockBehaviour.OffsetType.XZ).ignitedByLava().pushReaction(PushReaction.DESTROY));
            //?} else if >=26.3 {
            /*() -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).replaceable().noCollision().instabreak()
                    .sound(SoundType.GRASS).offsetType(BlockBehaviour.OffsetType.XZ).ignitedByLava().pushReaction(PushReaction.POPPED));
            *///?} else {
            /*BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).replaceable().noCollission().instabreak()
                    .sound(SoundType.GRASS).offsetType(BlockBehaviour.OffsetType.XZ).ignitedByLava().pushReaction(PushReaction.DESTROY));
            *///?}
    public static final DeferredBlock<LiquidBlock> CORRUPTED_WATER = BLOCKS.registerBlock("corrupted_water",
            //? if >=26.1 {
            props -> new LiquidBlock(DfmFluids.CORRUPTED_WATER.get(), props), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).mapColor(MapColor.COLOR_PURPLE));
            //?} else {
            /*props -> new LiquidBlock(DfmFluids.CORRUPTED_WATER.get(), props), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).mapColor(MapColor.COLOR_PURPLE));
            *///?}

    /** Assembly parts cannot be moved by pistons, so a formed machine can only change by being broken. */
    private static BlockBehaviour.Properties assemblyPart() {
        //? if <26.3 {
        return machineCasing().pushReaction(PushReaction.BLOCK);
        //?} else {
        /*return machineCasing().pushReaction(PushReaction.IMMOVEABLE);
        *///?}
    }

    /** Machines, tanks and Miner parts: metal blocks no mob ever spawns on. */
    private static BlockBehaviour.Properties machineCasing() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                .requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.METAL).isValidSpawn(Blocks::never);
    }

    private DfmBlocks() {
    }
}
