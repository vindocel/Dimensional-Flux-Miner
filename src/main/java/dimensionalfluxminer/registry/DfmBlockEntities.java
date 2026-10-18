package dimensionalfluxminer.registry;

import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.crusher.MechanicalCrusherBlockEntity;
import dimensionalfluxminer.miner.MinerControllerBlockEntity;
import dimensionalfluxminer.miner.MinerLinkBlockEntity;
import dimensionalfluxminer.smelter.AlloySmelterBlockEntity;
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
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AlloySmelterBlockEntity>> ALLOY_SMELTER =
            BLOCK_ENTITIES.register("alloy_smelter", () -> BlockEntityType.Builder.of(AlloySmelterBlockEntity::new, DfmBlocks.ALLOY_SMELTER.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MinerControllerBlockEntity>> MINER_CONTROLLER =
            BLOCK_ENTITIES.register("miner_terminal", () -> BlockEntityType.Builder.of(MinerControllerBlockEntity::new, DfmBlocks.MINER_TERMINAL.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MinerLinkBlockEntity>> MINER_LINK =
            BLOCK_ENTITIES.register("miner_link", () -> BlockEntityType.Builder.of(MinerLinkBlockEntity::new,
                    DfmBlocks.STEEL_CHASSIS.get(), DfmBlocks.DIMENSIONAL_PORT.get()).build(null));

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        // The terminal has no port of its own: only its null side (read-only views for information tools) answers.
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, MINER_CONTROLLER.get(), (be, side) -> be.itemPort(be.getBlockPos(), side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, MINER_CONTROLLER.get(), (be, side) -> be.energyPort(be.getBlockPos(), side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, MINER_LINK.get(), MinerLinkBlockEntity::itemHandler);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, MINER_LINK.get(), MinerLinkBlockEntity::energyStorage);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, MECHANICAL_CRUSHER.get(), (be, side) -> be.itemHandlerFor(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, MECHANICAL_CRUSHER.get(), (be, side) -> be.energyFor(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ALLOY_SMELTER.get(), (be, side) -> be.itemHandlerFor(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ALLOY_SMELTER.get(), (be, side) -> be.energyFor(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ALLOY_SMELTER.get(), (be, side) -> be.experienceFor(side));
    }

    private DfmBlockEntities() {
    }
}
