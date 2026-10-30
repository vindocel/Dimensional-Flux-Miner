package dimensionalfluxminer.registry;

import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.crusher.MechanicalCrusherBlockEntity;
import dimensionalfluxminer.miner.MinerControllerBlockEntity;
import dimensionalfluxminer.miner.MinerLinkBlockEntity;
import dimensionalfluxminer.furnace.ElectricFurnaceBlockEntity;
import dimensionalfluxminer.machine.transfer.Transfer;
import dimensionalfluxminer.tank.DimensionalTankBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class DfmBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, DimensionalFluxMiner.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MechanicalCrusherBlockEntity>> MECHANICAL_CRUSHER =
            BLOCK_ENTITIES.register("mechanical_crusher", () -> new BlockEntityType<>(MechanicalCrusherBlockEntity::new, DfmBlocks.MECHANICAL_CRUSHER.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectricFurnaceBlockEntity>> ELECTRIC_FURNACE =
            BLOCK_ENTITIES.register("electric_furnace", () -> new BlockEntityType<>(ElectricFurnaceBlockEntity::new, DfmBlocks.ELECTRIC_FURNACE.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MinerControllerBlockEntity>> MINER_CONTROLLER =
            BLOCK_ENTITIES.register("miner_terminal", () -> new BlockEntityType<>(MinerControllerBlockEntity::new, DfmBlocks.MINER_TERMINAL.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MinerLinkBlockEntity>> MINER_LINK =
            BLOCK_ENTITIES.register("miner_link", () -> new BlockEntityType<>(MinerLinkBlockEntity::new,
                    DfmBlocks.STEEL_CHASSIS.get(), DfmBlocks.DIMENSIONAL_PORT.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DimensionalTankBlockEntity>> DIMENSIONAL_TANK =
            BLOCK_ENTITIES.register("dimensional_tank", () -> new BlockEntityType<>(DimensionalTankBlockEntity::new,
                    DfmBlocks.DIMENSIONAL_TANK.get(), DfmBlocks.DIMENSIONAL_DUAL_TANK.get(), DfmBlocks.CREATIVE_DIMENSIONAL_TANK.get()));

    /** The mod's slot, energy and tank views, as NeoForge transfer handlers (see {@code machine.transfer.Transfer}). */
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, DIMENSIONAL_TANK.get(), (be, side) -> Transfer.exposeFluids(be.fluidFor(side), be.journal()));
        // Single and creative tank items are fluid containers (used on a tank they fill it); the dual one is not.
        event.registerItem(Capabilities.Fluid.ITEM, (stack, access) -> dimensionalfluxminer.tank.DimensionalTankItem.fluidHandler(access),
                DfmItems.DIMENSIONAL_TANK.get(), DfmItems.CREATIVE_DIMENSIONAL_TANK.get());
        // The terminal has no port of its own: only its null side (read-only views for information tools) answers.
        event.registerBlockEntity(Capabilities.Item.BLOCK, MINER_CONTROLLER.get(), (be, side) -> Transfer.exposeItems(be.itemPort(be.getBlockPos(), side), be.journal()));
        event.registerBlockEntity(Capabilities.Energy.BLOCK, MINER_CONTROLLER.get(), (be, side) -> Transfer.exposeEnergy(be.energyPort(be.getBlockPos(), side), be.journal()));
        event.registerBlockEntity(Capabilities.Item.BLOCK, MINER_LINK.get(), (be, side) -> be.exposedItems(side));
        event.registerBlockEntity(Capabilities.Energy.BLOCK, MINER_LINK.get(), (be, side) -> be.exposedEnergy(side));
        event.registerBlockEntity(Capabilities.Item.BLOCK, MECHANICAL_CRUSHER.get(), (be, side) -> Transfer.exposeItems(be.itemHandlerFor(side), be.journal()));
        event.registerBlockEntity(Capabilities.Energy.BLOCK, MECHANICAL_CRUSHER.get(), (be, side) -> Transfer.exposeEnergy(be.energyFor(side), be.journal()));
        event.registerBlockEntity(Capabilities.Item.BLOCK, ELECTRIC_FURNACE.get(), (be, side) -> Transfer.exposeItems(be.itemHandlerFor(side), be.journal()));
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ELECTRIC_FURNACE.get(), (be, side) -> Transfer.exposeEnergy(be.energyFor(side), be.journal()));
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ELECTRIC_FURNACE.get(), (be, side) -> Transfer.exposeFluids(be.experienceFor(side), be.journal()));
    }

    private DfmBlockEntities() {
    }
}
