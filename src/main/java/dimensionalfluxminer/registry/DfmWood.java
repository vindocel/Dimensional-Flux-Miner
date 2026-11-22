package dimensionalfluxminer.registry;

import dimensionalfluxminer.DimensionalFluxMiner;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.HangingSignItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
//? if >=26.1 {
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.boat.ChestBoat;
//?}

/**
 * Corrupted wood: the wood family of the trees Dimensional Death corrupts (logs, wood, the stripped forms, leaves and
 * vines) and everything crafted from it. Every block copies the behaviour of the vanilla pale oak block (oak on 1.21.1,
 * which has no pale oak): hardness, sounds, flammability, fuel, recipes and tags; only the textures are new. There is
 * no sapling (a corrupted tree is a normal tree that got corrupted) and the shelf only exists where vanilla has shelves
 * (26.1.2 and 26.3).
 */
public final class DfmWood {
    /** Wood type and block set id: signs, boats and screens find their textures under {@code corrupted}. */
    public static final String TYPE_ID = DimensionalFluxMiner.MOD_ID + ":corrupted";
    public static final BlockSetType BLOCK_SET = BlockSetType.register(new BlockSetType(TYPE_ID));
    public static final WoodType WOOD_TYPE = WoodType.register(new WoodType(TYPE_ID, BLOCK_SET));

    /** Map colours: purple planks and stripped wood, dark purple bark. */
    private static final MapColor WOOD_COLOR = MapColor.COLOR_PURPLE;
    private static final MapColor BARK_COLOR = MapColor.TERRACOTTA_PURPLE;

    // The vanilla blocks whose behaviour each corrupted block copies.
    //? if >=26.1 {
    private static final Block LOG_SOURCE = Blocks.PALE_OAK_LOG;
    private static final Block WOOD_SOURCE = Blocks.PALE_OAK_WOOD;
    private static final Block STRIPPED_LOG_SOURCE = Blocks.STRIPPED_PALE_OAK_LOG;
    private static final Block STRIPPED_WOOD_SOURCE = Blocks.STRIPPED_PALE_OAK_WOOD;
    private static final Block PLANKS_SOURCE = Blocks.PALE_OAK_PLANKS;
    private static final Block STAIRS_SOURCE = Blocks.PALE_OAK_STAIRS;
    private static final Block SLAB_SOURCE = Blocks.PALE_OAK_SLAB;
    private static final Block FENCE_SOURCE = Blocks.PALE_OAK_FENCE;
    private static final Block FENCE_GATE_SOURCE = Blocks.PALE_OAK_FENCE_GATE;
    private static final Block DOOR_SOURCE = Blocks.PALE_OAK_DOOR;
    private static final Block TRAPDOOR_SOURCE = Blocks.PALE_OAK_TRAPDOOR;
    private static final Block PRESSURE_PLATE_SOURCE = Blocks.PALE_OAK_PRESSURE_PLATE;
    private static final Block BUTTON_SOURCE = Blocks.PALE_OAK_BUTTON;
    private static final Block SIGN_SOURCE = Blocks.PALE_OAK_SIGN;
    private static final Block WALL_SIGN_SOURCE = Blocks.PALE_OAK_WALL_SIGN;
    private static final Block HANGING_SIGN_SOURCE = Blocks.PALE_OAK_HANGING_SIGN;
    private static final Block WALL_HANGING_SIGN_SOURCE = Blocks.PALE_OAK_WALL_HANGING_SIGN;
    private static final Block LEAVES_SOURCE = Blocks.PALE_OAK_LEAVES;
    //?} else {
    /*private static final Block LOG_SOURCE = Blocks.OAK_LOG;
    private static final Block WOOD_SOURCE = Blocks.OAK_WOOD;
    private static final Block STRIPPED_LOG_SOURCE = Blocks.STRIPPED_OAK_LOG;
    private static final Block STRIPPED_WOOD_SOURCE = Blocks.STRIPPED_OAK_WOOD;
    private static final Block PLANKS_SOURCE = Blocks.OAK_PLANKS;
    private static final Block STAIRS_SOURCE = Blocks.OAK_STAIRS;
    private static final Block SLAB_SOURCE = Blocks.OAK_SLAB;
    private static final Block FENCE_SOURCE = Blocks.OAK_FENCE;
    private static final Block FENCE_GATE_SOURCE = Blocks.OAK_FENCE_GATE;
    private static final Block DOOR_SOURCE = Blocks.OAK_DOOR;
    private static final Block TRAPDOOR_SOURCE = Blocks.OAK_TRAPDOOR;
    private static final Block PRESSURE_PLATE_SOURCE = Blocks.OAK_PRESSURE_PLATE;
    private static final Block BUTTON_SOURCE = Blocks.OAK_BUTTON;
    private static final Block SIGN_SOURCE = Blocks.OAK_SIGN;
    private static final Block WALL_SIGN_SOURCE = Blocks.OAK_WALL_SIGN;
    private static final Block HANGING_SIGN_SOURCE = Blocks.OAK_HANGING_SIGN;
    private static final Block WALL_HANGING_SIGN_SOURCE = Blocks.OAK_WALL_HANGING_SIGN;
    private static final Block LEAVES_SOURCE = Blocks.OAK_LEAVES;
    *///?}

    // Logs and wood
    public static final DeferredBlock<RotatedPillarBlock> LOG = block("corrupted_log", RotatedPillarBlock::new,
            () -> copy(LOG_SOURCE).mapColor(DfmWood::logColor));
    public static final DeferredBlock<RotatedPillarBlock> WOOD = block("corrupted_wood", RotatedPillarBlock::new,
            () -> copy(WOOD_SOURCE).mapColor(BARK_COLOR));
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_LOG = block("stripped_corrupted_log", RotatedPillarBlock::new,
            () -> copy(STRIPPED_LOG_SOURCE).mapColor(WOOD_COLOR));
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_WOOD = block("stripped_corrupted_wood", RotatedPillarBlock::new,
            () -> copy(STRIPPED_WOOD_SOURCE).mapColor(WOOD_COLOR));

    // Planks and what is crafted from them
    public static final DeferredBlock<Block> PLANKS = block("corrupted_planks", Block::new, () -> copy(PLANKS_SOURCE).mapColor(WOOD_COLOR));
    public static final DeferredBlock<StairBlock> STAIRS = block("corrupted_stairs",
            props -> new StairBlock(PLANKS.get().defaultBlockState(), props), () -> copy(STAIRS_SOURCE).mapColor(WOOD_COLOR));
    public static final DeferredBlock<SlabBlock> SLAB = block("corrupted_slab", SlabBlock::new, () -> copy(SLAB_SOURCE).mapColor(WOOD_COLOR));
    public static final DeferredBlock<FenceBlock> FENCE = block("corrupted_fence", FenceBlock::new, () -> copy(FENCE_SOURCE).mapColor(WOOD_COLOR));
    public static final DeferredBlock<FenceGateBlock> FENCE_GATE = block("corrupted_fence_gate",
            props -> new FenceGateBlock(WOOD_TYPE, props), () -> copy(FENCE_GATE_SOURCE).mapColor(WOOD_COLOR));
    public static final DeferredBlock<DoorBlock> DOOR = block("corrupted_door",
            props -> new DoorBlock(BLOCK_SET, props), () -> copy(DOOR_SOURCE).mapColor(WOOD_COLOR));
    public static final DeferredBlock<TrapDoorBlock> TRAPDOOR = block("corrupted_trapdoor",
            props -> new TrapDoorBlock(BLOCK_SET, props), () -> copy(TRAPDOOR_SOURCE).mapColor(WOOD_COLOR));
    public static final DeferredBlock<PressurePlateBlock> PRESSURE_PLATE = block("corrupted_pressure_plate",
            props -> new PressurePlateBlock(BLOCK_SET, props), () -> copy(PRESSURE_PLATE_SOURCE).mapColor(WOOD_COLOR));
    public static final DeferredBlock<ButtonBlock> BUTTON = block("corrupted_button",
            props -> new ButtonBlock(BLOCK_SET, 30, props), () -> copy(BUTTON_SOURCE));
    // The wall variants drop the standing sign through their own loot tables (wall_sign / wall_hanging_sign).
    public static final DeferredBlock<StandingSignBlock> SIGN = block("corrupted_sign",
            props -> new StandingSignBlock(WOOD_TYPE, props), () -> copy(SIGN_SOURCE).mapColor(WOOD_COLOR));
    public static final DeferredBlock<WallSignBlock> WALL_SIGN = block("corrupted_wall_sign",
            props -> new WallSignBlock(WOOD_TYPE, props), () -> BlockBehaviour.Properties.ofLegacyCopy(WALL_SIGN_SOURCE).mapColor(WOOD_COLOR));
    public static final DeferredBlock<CeilingHangingSignBlock> HANGING_SIGN = block("corrupted_hanging_sign",
            props -> new CeilingHangingSignBlock(WOOD_TYPE, props), () -> copy(HANGING_SIGN_SOURCE).mapColor(WOOD_COLOR));
    public static final DeferredBlock<WallHangingSignBlock> WALL_HANGING_SIGN = block("corrupted_wall_hanging_sign",
            props -> new WallHangingSignBlock(WOOD_TYPE, props),
            () -> BlockBehaviour.Properties.ofLegacyCopy(WALL_HANGING_SIGN_SOURCE).mapColor(WOOD_COLOR));
    //? if >=26.1 {
    public static final DeferredBlock<net.minecraft.world.level.block.ShelfBlock> SHELF = block("corrupted_shelf",
            net.minecraft.world.level.block.ShelfBlock::new, () -> copy(Blocks.PALE_OAK_SHELF).mapColor(WOOD_COLOR));
    //?}

    // Leaves and vine of corrupted trees. The leaves drop sticks, never a sapling.
    public static final DeferredBlock<LeavesBlock> LEAVES = block("corrupted_leaves",
            //? if =26.1.2 {
            props -> new net.minecraft.world.level.block.UntintedParticleLeavesBlock(0.02F,
                    net.minecraft.core.particles.ParticleTypes.PALE_OAK_LEAVES, props) {
                // Purple falling leaves instead of the white pale oak ones (the constructor particle stays unused).
                @Override
                protected void spawnFallingLeavesParticle(net.minecraft.world.level.Level level,
                        net.minecraft.core.BlockPos pos, net.minecraft.util.RandomSource random) {
                    net.minecraft.util.ParticleUtils.spawnParticleBelow(level, pos, random, DfmParticles.CORRUPTED_LEAVES.get());
                }
            },
            //?} else if >=26.3 {
            /*props -> new net.minecraft.world.level.block.UntintedParticleLeavesBlock(0.02F,
                    net.minecraft.core.particles.ParticleTypes.PALE_OAK_LEAVES,
                    net.minecraft.world.level.block.sounds.AmbientLeavesBlockSoundPlayer.noAmbientSound(), props) {
                // Purple falling leaves instead of the white pale oak ones (the constructor particle stays unused).
                @Override
                protected void spawnFallingLeavesParticle(net.minecraft.world.level.Level level,
                        net.minecraft.core.BlockPos pos, net.minecraft.util.RandomSource random) {
                    net.minecraft.util.ParticleUtils.spawnParticleBelow(level, pos, random, DfmParticles.CORRUPTED_LEAVES.get());
                }
            },
            *///?} else {
            /*LeavesBlock::new,
            *///?}
            () -> copy(LEAVES_SOURCE).mapColor(MapColor.COLOR_PURPLE));
    /** Same as the vine (climbable, spreads, needs shears), purple and untinted. */
    public static final DeferredBlock<VineBlock> VINE = block("corrupted_vine", VineBlock::new,
            () -> copy(Blocks.VINE).mapColor(MapColor.COLOR_PURPLE));

    // Items
    public static final DeferredItem<BlockItem> LOG_ITEM = blockItem(LOG, Fuel.WOOD_BLOCKS);
    public static final DeferredItem<BlockItem> WOOD_ITEM = blockItem(WOOD, Fuel.WOOD_BLOCKS);
    public static final DeferredItem<BlockItem> STRIPPED_LOG_ITEM = blockItem(STRIPPED_LOG, Fuel.WOOD_BLOCKS);
    public static final DeferredItem<BlockItem> STRIPPED_WOOD_ITEM = blockItem(STRIPPED_WOOD, Fuel.WOOD_BLOCKS);
    public static final DeferredItem<BlockItem> PLANKS_ITEM = blockItem(PLANKS, Fuel.WOOD_BLOCKS);
    public static final DeferredItem<BlockItem> STAIRS_ITEM = blockItem(STAIRS, Fuel.WOOD_BLOCKS);
    public static final DeferredItem<BlockItem> SLAB_ITEM = blockItem(SLAB, Fuel.WOOD_SLABS);
    public static final DeferredItem<BlockItem> FENCE_ITEM = blockItem(FENCE, Fuel.WOOD_BLOCKS);
    public static final DeferredItem<BlockItem> FENCE_GATE_ITEM = blockItem(FENCE_GATE, Fuel.WOOD_BLOCKS);
    public static final DeferredItem<DoubleHighBlockItem> DOOR_ITEM = item("corrupted_door",
            props -> new DoubleHighBlockItem(DOOR.get(), props), () -> blockProperties(Fuel.WOOD_ITEMS_LARGE));
    public static final DeferredItem<BlockItem> TRAPDOOR_ITEM = blockItem(TRAPDOOR, Fuel.WOOD_BLOCKS);
    public static final DeferredItem<BlockItem> PRESSURE_PLATE_ITEM = blockItem(PRESSURE_PLATE, Fuel.WOOD_BLOCKS);
    public static final DeferredItem<BlockItem> BUTTON_ITEM = blockItem(BUTTON, Fuel.WOOD_ITEMS_EXTRA_SMALL);
    //? if =26.1.2 {
    public static final DeferredItem<net.minecraft.world.item.SignItem> SIGN_ITEM = item("corrupted_sign",
            props -> new net.minecraft.world.item.SignItem(SIGN.get(), WALL_SIGN.get(), props), () -> blockProperties(Fuel.NONE).stacksTo(16));
    public static final DeferredItem<HangingSignItem> HANGING_SIGN_ITEM = item("corrupted_hanging_sign",
            props -> new HangingSignItem(HANGING_SIGN.get(), WALL_HANGING_SIGN.get(), props), () -> blockProperties(Fuel.NONE).stacksTo(16));
    //?} else if >=26.3 {
    /*public static final DeferredItem<net.minecraft.world.item.StandingAndWallBlockItem> SIGN_ITEM = item("corrupted_sign",
            props -> new net.minecraft.world.item.StandingAndWallBlockItem(SIGN.get(), WALL_SIGN.get(), net.minecraft.core.Direction.DOWN, props),
            () -> blockProperties(Fuel.WOOD_ITEMS_LARGE).stacksTo(16).signText());
    public static final DeferredItem<HangingSignItem> HANGING_SIGN_ITEM = item("corrupted_hanging_sign",
            props -> new HangingSignItem(HANGING_SIGN.get(), WALL_HANGING_SIGN.get(), props),
            () -> blockProperties(Fuel.HANGING_SIGNS).stacksTo(16).signText());
    *///?} else {
    /*public static final DeferredItem<net.minecraft.world.item.SignItem> SIGN_ITEM = item("corrupted_sign",
            props -> new net.minecraft.world.item.SignItem(props, SIGN.get(), WALL_SIGN.get()), () -> new Item.Properties().stacksTo(16));
    public static final DeferredItem<HangingSignItem> HANGING_SIGN_ITEM = item("corrupted_hanging_sign",
            props -> new HangingSignItem(HANGING_SIGN.get(), WALL_HANGING_SIGN.get(), props), () -> new Item.Properties().stacksTo(16));
    *///?}
    //? if >=26.1 {
    public static final DeferredItem<BlockItem> SHELF_ITEM = item("corrupted_shelf", props -> new BlockItem(SHELF.get(), props),
            () -> blockProperties(Fuel.WOOD_BLOCKS).component(net.minecraft.core.component.DataComponents.CONTAINER,
                    net.minecraft.world.item.component.ItemContainerContents.EMPTY));
    //?}
    public static final DeferredItem<BlockItem> LEAVES_ITEM = blockItem(LEAVES, Fuel.COMPOST_LEAVES);
    public static final DeferredItem<BlockItem> VINE_ITEM = blockItem(VINE, Fuel.COMPOST_VINE);

    // Boats: own entity types on 26.x (as vanilla since 1.21.2), a Boat.Type on 1.21.1.
    //? if >=26.1 {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, DimensionalFluxMiner.MOD_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<Boat>> BOAT_ENTITY = ENTITIES.register("corrupted_boat",
            id -> EntityType.Builder.<Boat>of((type, level) -> new Boat(type, level, DfmWood::boatItem), MobCategory.MISC)
                    .noLootTable().sized(1.375F, 0.5625F).eyeHeight(0.5625F).clientTrackingRange(10)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, id)));
    public static final DeferredHolder<EntityType<?>, EntityType<ChestBoat>> CHEST_BOAT_ENTITY = ENTITIES.register("corrupted_chest_boat",
            id -> EntityType.Builder.<ChestBoat>of((type, level) -> new ChestBoat(type, level, DfmWood::chestBoatItem), MobCategory.MISC)
                    .noLootTable().sized(1.375F, 0.5625F).eyeHeight(0.5625F).clientTrackingRange(10)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, id)));
    public static final DeferredItem<BoatItem> BOAT_ITEM = item("corrupted_boat",
            props -> new BoatItem(BOAT_ENTITY.get(), props), () -> fuel(new Item.Properties(), Fuel.BOATS).stacksTo(1));
    public static final DeferredItem<BoatItem> CHEST_BOAT_ITEM = item("corrupted_chest_boat",
            props -> new BoatItem(CHEST_BOAT_ENTITY.get(), props), () -> fuel(new Item.Properties(), Fuel.BOATS).stacksTo(1));
    //?} else {
    /*public static final DeferredItem<BoatItem> BOAT_ITEM = item("corrupted_boat",
            props -> new BoatItem(false, CorruptedBoatType.get(), props), () -> new Item.Properties().stacksTo(1));
    public static final DeferredItem<BoatItem> CHEST_BOAT_ITEM = item("corrupted_chest_boat",
            props -> new BoatItem(true, CorruptedBoatType.get(), props), () -> new Item.Properties().stacksTo(1));
    *///?}

    /** Creative tab order, like the vanilla building blocks tab. */
    public static List<DeferredItem<?>> creativeItems() {
        //? if >=26.1 {
        return List.of(LOG_ITEM, WOOD_ITEM, STRIPPED_LOG_ITEM, STRIPPED_WOOD_ITEM, PLANKS_ITEM, STAIRS_ITEM, SLAB_ITEM, FENCE_ITEM,
                FENCE_GATE_ITEM, DOOR_ITEM, TRAPDOOR_ITEM, PRESSURE_PLATE_ITEM, BUTTON_ITEM, SIGN_ITEM, HANGING_SIGN_ITEM, SHELF_ITEM,
                BOAT_ITEM, CHEST_BOAT_ITEM, LEAVES_ITEM, VINE_ITEM);
        //?} else {
        /*return List.of(LOG_ITEM, WOOD_ITEM, STRIPPED_LOG_ITEM, STRIPPED_WOOD_ITEM, PLANKS_ITEM, STAIRS_ITEM, SLAB_ITEM, FENCE_ITEM,
                FENCE_GATE_ITEM, DOOR_ITEM, TRAPDOOR_ITEM, PRESSURE_PLATE_ITEM, BUTTON_ITEM, SIGN_ITEM, HANGING_SIGN_ITEM,
                BOAT_ITEM, CHEST_BOAT_ITEM, LEAVES_ITEM, VINE_ITEM);
        *///?}
    }

    /** Loads the class (and so registers everything above) and hooks the events; called from the mod constructor. */
    public static void register(net.neoforged.bus.api.IEventBus modBus) {
        //? if >=26.1 {
        ENTITIES.register(modBus);
        //?}
        modBus.addListener(DfmWood::onAddBlockEntityBlocks);
        modBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) -> event.enqueueWork(DfmWood::commonSetup));
        //? if <26.3 {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(DfmWood::onToolModification);
        //?}
    }

    // Signs, hanging signs and shelves use the vanilla block entities, which must accept the corrupted blocks.
    private static void onAddBlockEntityBlocks(BlockEntityTypeAddBlocksEvent event) {
        //? if >=26.3 {
        /*event.modify(net.minecraft.world.level.block.entity.BlockEntityTypes.SIGN, SIGN.get(), WALL_SIGN.get());
        event.modify(net.minecraft.world.level.block.entity.BlockEntityTypes.HANGING_SIGN, HANGING_SIGN.get(), WALL_HANGING_SIGN.get());
        event.modify(net.minecraft.world.level.block.entity.BlockEntityTypes.SHELF, SHELF.get());
        *///?} else if >=26.1 {
        event.modify(BlockEntityType.SIGN, SIGN.get(), WALL_SIGN.get());
        event.modify(BlockEntityType.HANGING_SIGN, HANGING_SIGN.get(), WALL_HANGING_SIGN.get());
        event.modify(BlockEntityType.SHELF, SHELF.get());
        //?} else {
        /*event.modify(BlockEntityType.SIGN, SIGN.get(), WALL_SIGN.get());
        event.modify(BlockEntityType.HANGING_SIGN, HANGING_SIGN.get(), WALL_HANGING_SIGN.get());
        *///?}
    }

    // Same fire odds as the vanilla wood (FireBlock.bootStrap), and dispensers place the boats.
    private static void commonSetup() {
        FireBlock fire = (FireBlock) Blocks.FIRE;
        for (Block block : List.of(PLANKS.get(), SLAB.get(), FENCE_GATE.get(), FENCE.get(), STAIRS.get())) {
            fire.setFlammable(block, 5, 20);
        }
        for (Block block : List.of(LOG.get(), WOOD.get(), STRIPPED_LOG.get(), STRIPPED_WOOD.get())) {
            fire.setFlammable(block, 5, 5);
        }
        fire.setFlammable(LEAVES.get(), 30, 60);
        fire.setFlammable(VINE.get(), 15, 100);
        //? if >=26.1 {
        fire.setFlammable(SHELF.get(), 30, 20);
        net.minecraft.world.level.block.DispenserBlock.registerBehavior(BOAT_ITEM.get(),
                new net.minecraft.core.dispenser.BoatDispenseItemBehavior(BOAT_ENTITY.get()));
        net.minecraft.world.level.block.DispenserBlock.registerBehavior(CHEST_BOAT_ITEM.get(),
                new net.minecraft.core.dispenser.BoatDispenseItemBehavior(CHEST_BOAT_ENTITY.get()));
        //?} else {
        /*net.minecraft.world.level.block.DispenserBlock.registerBehavior(BOAT_ITEM.get(),
                new net.minecraft.core.dispenser.BoatDispenseItemBehavior(CorruptedBoatType.get()));
        net.minecraft.world.level.block.DispenserBlock.registerBehavior(CHEST_BOAT_ITEM.get(),
                new net.minecraft.core.dispenser.BoatDispenseItemBehavior(CorruptedBoatType.get(), true));
        *///?}
    }

    // An axe strips the corrupted log and wood like the vanilla ones (axis kept). On 26.3 the axe uses the
    // neoforge:transformables data map instead (data/neoforge/data_maps/block/transformables.json).
    //? if <26.3 {
    private static void onToolModification(BlockEvent.BlockToolModificationEvent event) {
        if (event.getItemAbility() != ItemAbilities.AXE_STRIP) {
            return;
        }
        BlockState state = event.getState();
        Block stripped = state.is(LOG.get()) ? STRIPPED_LOG.get() : state.is(WOOD.get()) ? STRIPPED_WOOD.get() : null;
        if (stripped != null) {
            event.setFinalState(stripped.defaultBlockState().setValue(RotatedPillarBlock.AXIS, state.getValue(RotatedPillarBlock.AXIS)));
        }
    }
    //?}

    private static MapColor logColor(BlockState state) {
        return state.getValue(RotatedPillarBlock.AXIS) == net.minecraft.core.Direction.Axis.Y ? WOOD_COLOR : BARK_COLOR;
    }

    /**
     * Full copy of a vanilla block's properties. On 26.x the copied loot table and name follow the new block's id; on
     * 1.21.1 no copied block sets them.
     */
    private static BlockBehaviour.Properties copy(Block source) {
        return BlockBehaviour.Properties.ofFullCopy(source);
    }

    //? if >=26.1 {
    private static Item boatItem() {
        return BOAT_ITEM.get();
    }

    private static Item chestBoatItem() {
        return CHEST_BOAT_ITEM.get();
    }
    //?}

    private static <B extends Block> DeferredBlock<B> block(String name, Function<BlockBehaviour.Properties, B> factory,
                                                           Supplier<BlockBehaviour.Properties> properties) {
        //? if >=26.1 {
        return DfmBlocks.BLOCKS.registerBlock(name, factory, properties);
        //?} else {
        /*return DfmBlocks.BLOCKS.registerBlock(name, factory, properties.get());
        *///?}
    }

    private static <I extends Item> DeferredItem<I> item(String name, Function<Item.Properties, I> factory, Supplier<Item.Properties> properties) {
        //? if >=26.1 {
        return DfmItems.ITEMS.registerItem(name, factory, properties);
        //?} else {
        /*return DfmItems.ITEMS.registerItem(name, factory, properties.get());
        *///?}
    }

    private static DeferredItem<BlockItem> blockItem(DeferredBlock<? extends Block> block, Fuel fuel) {
        return item(block.getId().getPath(), props -> new BlockItem(block.get(), props), () -> blockProperties(fuel));
    }

    private static Item.Properties blockProperties(Fuel fuel) {
        //? if >=26.1 {
        return fuel(new Item.Properties().useBlockDescriptionPrefix(), fuel);
        //?} else {
        /*return new Item.Properties();
        *///?}
    }

    /**
     * Fuel and compost values on 26.3, where vanilla sets them on the item. On 26.1.2 and 1.21.1 they come from the
     * item tags (logs, planks, wooden_slabs...) and the compostables data map.
     */
    private enum Fuel {
        NONE, WOOD_BLOCKS, WOOD_SLABS, WOOD_ITEMS_EXTRA_SMALL, WOOD_ITEMS_LARGE, HANGING_SIGNS, BOATS, COMPOST_LEAVES, COMPOST_VINE
    }

    private static Item.Properties fuel(Item.Properties properties, Fuel fuel) {
        //? if >=26.3 {
        /*return switch (fuel) {
            case NONE -> properties;
            case WOOD_BLOCKS -> properties.cookingFuel(net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.COOKING_TIME_WOOD_BLOCKS);
            case WOOD_SLABS -> properties.cookingFuel(net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.COOKING_TIME_WOOD_SLABS);
            case WOOD_ITEMS_EXTRA_SMALL -> properties.cookingFuel(net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.COOKING_TIME_WOOD_ITEMS_EXTRA_SMALL);
            case WOOD_ITEMS_LARGE -> properties.cookingFuel(net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.COOKING_TIME_WOOD_ITEMS_LARGE);
            case HANGING_SIGNS -> properties.cookingFuel(net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.COOKING_TIME_HANGING_SIGNS);
            case BOATS -> properties.cookingFuel(net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.COOKING_TIME_BOATS);
            case COMPOST_LEAVES -> properties.compostable(net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.COMPOSTABLE_LOW);
            case COMPOST_VINE -> properties.compostable(net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.COMPOSTABLE_LOW_MEDIUM);
        };
        *///?} else {
        return properties;
        //?}
    }

    //? if <26.1 {
    /*// The corrupted Boat.Type, added through META-INF/enumextensions.json (named "dimensionalfluxminer:corrupted", so
    // the boat textures are textures/entity/boat/corrupted.png and textures/entity/chest_boat/corrupted.png). Kept in its
    // own class so the enum extension can read the proxy without loading DfmWood.
    public static final class CorruptedBoatType {
        public static final net.neoforged.fml.common.asm.enumextension.EnumProxy<net.minecraft.world.entity.vehicle.Boat.Type> PROXY =
                new net.neoforged.fml.common.asm.enumextension.EnumProxy<>(net.minecraft.world.entity.vehicle.Boat.Type.class,
                        (Supplier<Block>) () -> DfmWood.PLANKS.get(), TYPE_ID,
                        (Supplier<Item>) () -> DfmWood.BOAT_ITEM.get(), (Supplier<Item>) () -> DfmWood.CHEST_BOAT_ITEM.get(),
                        (Supplier<Item>) () -> net.minecraft.world.item.Items.STICK, false);

        public static net.minecraft.world.entity.vehicle.Boat.Type get() {
            return PROXY.getValue();
        }

        private CorruptedBoatType() {
        }
    }

    *///?}
    private DfmWood() {
    }
}
