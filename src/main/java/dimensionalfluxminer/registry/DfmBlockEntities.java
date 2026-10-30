package dimensionalfluxminer.registry;

import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.crusher.MechanicalCrusherBlockEntity;
import dimensionalfluxminer.miner.MinerControllerBlockEntity;
import dimensionalfluxminer.miner.MinerLinkBlockEntity;
import dimensionalfluxminer.furnace.ElectricFurnaceBlockEntity;
import dimensionalfluxminer.tank.DimensionalTankBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class DfmBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, DimensionalFluxMiner.MOD_ID);

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MechanicalCrusherBlockEntity>> MECHANICAL_CRUSHER =
            BLOCK_ENTITIES.register("mechanical_crusher", () -> BlockEntityType.Builder.of(MechanicalCrusherBlockEntity::new, DfmBlocks.MECHANICAL_CRUSHER.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectricFurnaceBlockEntity>> ELECTRIC_FURNACE =
            BLOCK_ENTITIES.register("electric_furnace", () -> BlockEntityType.Builder.of(ElectricFurnaceBlockEntity::new, DfmBlocks.ELECTRIC_FURNACE.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MinerControllerBlockEntity>> MINER_CONTROLLER =
            BLOCK_ENTITIES.register("miner_terminal", () -> BlockEntityType.Builder.of(MinerControllerBlockEntity::new, DfmBlocks.MINER_TERMINAL.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MinerLinkBlockEntity>> MINER_LINK =
            BLOCK_ENTITIES.register("miner_link", () -> BlockEntityType.Builder.of(MinerLinkBlockEntity::new,
                    DfmBlocks.STEEL_CHASSIS.get(), DfmBlocks.DIMENSIONAL_PORT.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DimensionalTankBlockEntity>> DIMENSIONAL_TANK =
            BLOCK_ENTITIES.register("dimensional_tank", () -> BlockEntityType.Builder.of(DimensionalTankBlockEntity::new,
                    DfmBlocks.DIMENSIONAL_TANK.get(), DfmBlocks.DIMENSIONAL_DUAL_TANK.get(),
                    DfmBlocks.CREATIVE_DIMENSIONAL_TANK.get()).build(null));

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, DIMENSIONAL_TANK.get(), DimensionalTankBlockEntity::fluidFor);
        // Single and creative tank items are fluid containers (used on a tank they fill it); the dual one is not.
        event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> dimensionalfluxminer.tank.DimensionalTankItem.fluidHandler(stack),
                DfmItems.DIMENSIONAL_TANK.get(), DfmItems.CREATIVE_DIMENSIONAL_TANK.get());
        // The terminal has no port of its own: only its null side (read-only views for information tools) answers.
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, MINER_CONTROLLER.get(), (be, side) -> be.itemPort(be.getBlockPos(), side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, MINER_CONTROLLER.get(), (be, side) -> be.energyPort(be.getBlockPos(), side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, MINER_LINK.get(), MinerLinkBlockEntity::itemHandler);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, MINER_LINK.get(), MinerLinkBlockEntity::energyStorage);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, MECHANICAL_CRUSHER.get(), (be, side) -> be.itemHandlerFor(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, MECHANICAL_CRUSHER.get(), (be, side) -> be.energyFor(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ELECTRIC_FURNACE.get(), (be, side) -> be.itemHandlerFor(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ELECTRIC_FURNACE.get(), (be, side) -> be.energyFor(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ELECTRIC_FURNACE.get(), (be, side) -> be.experienceFor(side));
    }

    private DfmBlockEntities() {
    }
}
