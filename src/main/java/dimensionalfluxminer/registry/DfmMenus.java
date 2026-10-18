package dimensionalfluxminer.registry;

import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.crusher.CrusherMenu;
import dimensionalfluxminer.furnace.ElectricFurnaceMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class DfmMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, DimensionalFluxMiner.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<CrusherMenu>> CRUSHER = MENUS.register("mechanical_crusher",
            () -> IMenuTypeExtension.create(CrusherMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<ElectricFurnaceMenu>> ELECTRIC_FURNACE = MENUS.register("electric_furnace",
            () -> IMenuTypeExtension.create(ElectricFurnaceMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<dimensionalfluxminer.miner.MinerMenu>> MINER = MENUS.register("dimensional_flux_miner",
            () -> IMenuTypeExtension.create(dimensionalfluxminer.miner.MinerMenu::new));

    private DfmMenus() {
    }
}
