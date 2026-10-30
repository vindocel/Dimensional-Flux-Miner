package dimensionalfluxminer.compat.polymorph;

import com.illusivesoulworks.polymorph.api.PolymorphApi;
import dimensionalfluxminer.crusher.MechanicalCrusherBlockEntity;
import dimensionalfluxminer.furnace.ElectricFurnaceBlockEntity;
import dimensionalfluxminer.menu.MachineMenu;
import dimensionalfluxminer.recipe.RecipeChoices;

/**
 * Optional Polymorph integration: when an item in the Mechanical Crusher or the Electric Furnace has more than one
 * recipe, the player picks the result on the machine's screen. Only called when Polymorph is installed.
 */
public final class PolymorphCompat {
    public static final String MOD_ID = "polymorph";

    private PolymorphCompat() {
    }

    public static void register() {
        RecipeChoices.enable();
        PolymorphApi api = PolymorphApi.getInstance();
        api.registerBlockEntity(MechanicalCrusherBlockEntity.class, MachineRecipeData::create);
        api.registerBlockEntity(ElectricFurnaceBlockEntity.class, MachineRecipeData::create);
        api.registerMenu(menu -> menu instanceof MachineMenu<?> machine && machine.blockEntity() instanceof RecipeChoices.Host
                ? machine.blockEntity() : null);
    }
}
