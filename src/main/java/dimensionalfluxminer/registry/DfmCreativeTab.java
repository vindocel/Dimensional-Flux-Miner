package dimensionalfluxminer.registry;

import dimensionalfluxminer.DimensionalFluxMiner;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class DfmCreativeTab {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, DimensionalFluxMiner.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.dimensionalfluxminer"))
            .icon(DfmCreativeTab::icon)
            .displayItems((params, output) -> {
                output.accept(DfmItems.GUIDE_BOOK.get());
                output.accept(DfmItems.STEEL_CHASSIS.get());
                output.accept(DfmItems.VENTILATION_UNIT.get());
                output.accept(DfmItems.REINFORCED_STRUCTURAL_GLASS.get());
                output.accept(DfmItems.DIMENSIONAL_PORT.get());
                output.accept(DfmItems.QUANTUM_CORE.get());
                output.accept(DfmItems.MINER_TERMINAL.get());
                output.accept(DfmItems.BASIC_DIMENSIONAL_CORE.get());
                output.accept(DfmItems.STABILIZED_DIMENSIONAL_CORE.get());
                output.accept(DfmItems.EFFICIENCY_UPGRADE.get());
                output.accept(DfmItems.DIMENSIONAL_CRUSHER.get());
                output.accept(DfmItems.ALLOY_SMELTER.get());
                output.accept(DfmItems.ENDER_EYE_REINFORCED.get());
                output.accept(DfmItems.INGOT_STEEL.get());
                output.accept(DfmItems.GRAIN_CALCITE.get());
                output.accept(DfmItems.SLAG.get());
                output.accept(DfmItems.SLAG_AGGREGATE.get());
                for (String material : DfmItems.DUSTS_WITH_BUILTIN_RECIPE) {
                    output.accept(DfmItems.DUSTS.get(material).get());
                }
                output.accept(DfmItems.CORRUPTED_DIRT.get());
                output.accept(DfmItems.CORRUPTED_GRASS.get());
                output.accept(DfmItems.CORRUPTED_STONE.get());
                output.accept(DfmItems.CORRUPTED_TERRACOTTA.get());
                output.accept(DfmItems.CORRUPTED_SEDIMENT.get());
                output.accept(DfmItems.CORRUPTED_GRASS_PLANT.get());
                output.accept(DfmItems.CORRUPTED_WATER_BUCKET.get());
            })
            .build());

    /**
     * Tab icon: the Alloy Smelter shown processing (lit). Custom model data 1 selects the active model in the item
     * model's overrides; the item itself keeps its normal model everywhere else.
     */
    private static ItemStack icon() {
        ItemStack stack = new ItemStack(DfmItems.ALLOY_SMELTER.get());
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(1));
        return stack;
    }

    private DfmCreativeTab() {
    }
}
