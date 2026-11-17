package dimensionalfluxminer.registry;

import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.crusher.MechanicalCrusherBlockEntity;
import dimensionalfluxminer.miner.MinerControllerBlockEntity;
import dimensionalfluxminer.miner.MinerLinkBlockEntity;
import dimensionalfluxminer.furnace.ElectricFurnaceBlockEntity;
//? if >=26.1 {
import dimensionalfluxminer.machine.transfer.Transfer;
//?}
import dimensionalfluxminer.tank.DimensionalTankBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class DfmBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, DimensionalFluxMiner.MOD_ID);

    //? if <26.1 {
    /*@SuppressWarnings("DataFlowIssue")
    *///?}
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MechanicalCrusherBlockEntity>> MECHANICAL_CRUSHER =
            //? if >=26.1 {
            BLOCK_ENTITIES.register("mechanical_crusher", () -> new BlockEntityType<>(MechanicalCrusherBlockEntity::new, DfmBlocks.MECHANICAL_CRUSHER.get()));

            //?} else {
            /*BLOCK_ENTITIES.register("mechanical_crusher", () -> BlockEntityType.Builder.of(MechanicalCrusherBlockEntity::new, DfmBlocks.MECHANICAL_CRUSHER.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
            *///?}
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectricFurnaceBlockEntity>> ELECTRIC_FURNACE =
            //? if >=26.1 {
            BLOCK_ENTITIES.register("electric_furnace", () -> new BlockEntityType<>(ElectricFurnaceBlockEntity::new, DfmBlocks.ELECTRIC_FURNACE.get()));

            //?} else {
            /*BLOCK_ENTITIES.register("electric_furnace", () -> BlockEntityType.Builder.of(ElectricFurnaceBlockEntity::new, DfmBlocks.ELECTRIC_FURNACE.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
            *///?}
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MinerControllerBlockEntity>> MINER_CONTROLLER =
            //? if >=26.1 {
            BLOCK_ENTITIES.register("miner_terminal", () -> new BlockEntityType<>(MinerControllerBlockEntity::new, DfmBlocks.MINER_TERMINAL.get()));

            //?} else {
            /*BLOCK_ENTITIES.register("miner_terminal", () -> BlockEntityType.Builder.of(MinerControllerBlockEntity::new, DfmBlocks.MINER_TERMINAL.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
            *///?}
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MinerLinkBlockEntity>> MINER_LINK =
            //? if >=26.1 {
            BLOCK_ENTITIES.register("miner_link", () -> new BlockEntityType<>(MinerLinkBlockEntity::new,
                    DfmBlocks.STEEL_CHASSIS.get(), DfmBlocks.DIMENSIONAL_PORT.get()));

            //?} else {
            /*BLOCK_ENTITIES.register("miner_link", () -> BlockEntityType.Builder.of(MinerLinkBlockEntity::new,
                    DfmBlocks.STEEL_CHASSIS.get(), DfmBlocks.DIMENSIONAL_PORT.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
            *///?}
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DimensionalTankBlockEntity>> DIMENSIONAL_TANK =
            //? if >=26.1 {
            BLOCK_ENTITIES.register("dimensional_tank", () -> new BlockEntityType<>(DimensionalTankBlockEntity::new,
                    DfmBlocks.DIMENSIONAL_TANK.get(), DfmBlocks.DIMENSIONAL_DUAL_TANK.get(), DfmBlocks.CREATIVE_DIMENSIONAL_TANK.get()));

    /** The mod's slot, energy and tank views, as NeoForge transfer handlers (see {@code machine.transfer.Transfer}). */
            //?} else {
            /*BLOCK_ENTITIES.register("dimensional_tank", () -> BlockEntityType.Builder.of(DimensionalTankBlockEntity::new,
                    DfmBlocks.DIMENSIONAL_TANK.get(), DfmBlocks.DIMENSIONAL_DUAL_TANK.get(),
                    DfmBlocks.CREATIVE_DIMENSIONAL_TANK.get()).build(null));

            *///?}
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        //? if >=26.1 {
        event.registerBlockEntity(Capabilities.Item.BLOCK, DIMENSIONAL_TANK.get(), (be, side) -> Transfer.exposeItems(be.itemFor(side), be.journal()));
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, DIMENSIONAL_TANK.get(), (be, side) -> Transfer.exposeFluids(be.fluidFor(side), be.journal()));
        //?} else {
        /*event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DIMENSIONAL_TANK.get(), DimensionalTankBlockEntity::itemFor);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, DIMENSIONAL_TANK.get(), DimensionalTankBlockEntity::fluidFor);
        *///?}
        // Single and creative tank items are fluid containers (used on a tank they fill it); the dual one is not.
        //? if >=26.1 {
        event.registerItem(Capabilities.Fluid.ITEM, (stack, access) -> dimensionalfluxminer.tank.DimensionalTankItem.fluidHandler(access),
        //?} else {
        /*event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> dimensionalfluxminer.tank.DimensionalTankItem.fluidHandler(stack),
        *///?}
                DfmItems.DIMENSIONAL_TANK.get(), DfmItems.CREATIVE_DIMENSIONAL_TANK.get());
        // The terminal has no port of its own. Its unsided item view is available to information tools.
        //? if >=26.1 {
        event.registerBlockEntity(Capabilities.Item.BLOCK, MINER_CONTROLLER.get(), (be, side) -> Transfer.exposeItems(be.itemPort(be.getBlockPos(), side), be.journal()));
        event.registerBlockEntity(Capabilities.Energy.BLOCK, MINER_CONTROLLER.get(), (be, side) -> Transfer.exposeEnergy(be.energyPort(be.getBlockPos(), side), be.journal()));
        event.registerBlockEntity(Capabilities.Item.BLOCK, MINER_LINK.get(), (be, side) -> be.exposedItems(side));
        event.registerBlockEntity(Capabilities.Energy.BLOCK, MINER_LINK.get(), (be, side) -> be.exposedEnergy(side));
        event.registerBlockEntity(Capabilities.Item.BLOCK, MECHANICAL_CRUSHER.get(), (be, side) -> Transfer.exposeItems(be.itemHandlerFor(side), be.journal()));
        event.registerBlockEntity(Capabilities.Energy.BLOCK, MECHANICAL_CRUSHER.get(), (be, side) -> Transfer.exposeEnergy(be.energyFor(side), be.journal()));
        event.registerBlockEntity(Capabilities.Item.BLOCK, ELECTRIC_FURNACE.get(), (be, side) -> Transfer.exposeItems(be.itemHandlerFor(side), be.journal()));
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ELECTRIC_FURNACE.get(), (be, side) -> Transfer.exposeEnergy(be.energyFor(side), be.journal()));
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ELECTRIC_FURNACE.get(), (be, side) -> Transfer.exposeFluids(be.experienceFor(side), be.journal()));
        //?} else {
        /*event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, MINER_CONTROLLER.get(), (be, side) -> be.itemPort(be.getBlockPos(), side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, MINER_CONTROLLER.get(), (be, side) -> be.energyPort(be.getBlockPos(), side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, MINER_LINK.get(), MinerLinkBlockEntity::itemHandler);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, MINER_LINK.get(), MinerLinkBlockEntity::energyStorage);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, MECHANICAL_CRUSHER.get(), (be, side) -> be.itemHandlerFor(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, MECHANICAL_CRUSHER.get(), (be, side) -> be.energyFor(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ELECTRIC_FURNACE.get(), (be, side) -> be.itemHandlerFor(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ELECTRIC_FURNACE.get(), (be, side) -> be.energyFor(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ELECTRIC_FURNACE.get(), (be, side) -> be.experienceFor(side));
        *///?}
    }

    private DfmBlockEntities() {
    }
}
