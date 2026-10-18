package dimensionalfluxminer.compat.jade;

import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.crusher.MechanicalCrusherBlock;
import dimensionalfluxminer.machine.AbstractMachineBlockEntity;
import dimensionalfluxminer.miner.MinerControllerBlockEntity;
import dimensionalfluxminer.miner.MinerLinkBlockEntity;
import dimensionalfluxminer.miner.block.MinerPartBlock;
import dimensionalfluxminer.furnace.ElectricFurnaceBlock;
import dimensionalfluxminer.furnace.ElectricFurnaceBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.view.HideThingsExtensionProvider;

/**
 * Jade integration. Items come from the machines' read-only {@code null}-side capabilities; this plugin replaces
 * Jade's energy line with the machines' own energy bar and adds the Electric Furnace's stored experience as a
 * Liquid Experience fluid bar under it.
 */
@WailaPlugin
public class DfmJadePlugin implements IWailaPlugin {
    static final ResourceLocation UID = DimensionalFluxMiner.id("machine_bars");
    static final String ENERGY = "energy";
    static final String CAPACITY = "capacity";
    static final String XP = "stored_xp";

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(MachineBarsData.INSTANCE, AbstractMachineBlockEntity.class);
        registration.registerBlockDataProvider(MachineBarsData.INSTANCE, MinerControllerBlockEntity.class);
        registration.registerBlockDataProvider(MachineBarsData.INSTANCE, MinerLinkBlockEntity.class);
        // The machine energy bar replaces Jade's own energy line.
        registration.registerEnergyStorage(HideThingsExtensionProvider.instance(), AbstractMachineBlockEntity.class);
        registration.registerEnergyStorage(HideThingsExtensionProvider.instance(), MinerControllerBlockEntity.class);
        registration.registerEnergyStorage(HideThingsExtensionProvider.instance(), MinerLinkBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(MachineBarsComponent.INSTANCE, MechanicalCrusherBlock.class);
        registration.registerBlockComponent(MachineBarsComponent.INSTANCE, ElectricFurnaceBlock.class);
        registration.registerBlockComponent(MachineBarsComponent.INSTANCE, MinerPartBlock.class);
    }

    enum MachineBarsData implements IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            BlockEntity be = accessor.getBlockEntity();
            if (be instanceof MinerLinkBlockEntity link) {
                be = link.controller();
            }
            if (be instanceof AbstractMachineBlockEntity machine) {
                data.putLong(ENERGY, machine.energyBuffer().stored());
                data.putLong(CAPACITY, machine.energyBuffer().capacity());
            } else if (be instanceof MinerControllerBlockEntity miner && miner.formed()) {
                data.putLong(ENERGY, miner.energyBuffer().stored());
                data.putLong(CAPACITY, miner.energyBuffer().capacity());
            }
            if (be instanceof ElectricFurnaceBlockEntity furnace) {
                data.putDouble(XP, furnace.experience().xp());
            }
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }
}
