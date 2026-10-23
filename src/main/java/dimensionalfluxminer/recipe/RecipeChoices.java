package dimensionalfluxminer.recipe;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Recipe the player chose for each input item of one machine, when more than one recipe accepts the same item. The
 * choice is made through an optional recipe-selection mod; without it nothing is applied and every machine keeps its
 * default recipe. Saved with the machine (NBT {@code recipe_choices}), so taking that mod out and back keeps them.
 */
public final class RecipeChoices {
    /** True while a recipe-selection integration is installed; set once at startup. */
    private static volatile boolean enabled;

    private final Map<Item, Identifier> chosen = new LinkedHashMap<>();
    /** Bumped on every change; machines compare it to drop the recipes they remembered. */
    private int version;

    public static void enable() {
        enabled = true;
    }

    public static boolean enabled() {
        return enabled;
    }

    /** Recipe chosen for the item of {@code input}, or null to use the default one. */
    public Identifier chosen(ItemStack input) {
        return enabled && !input.isEmpty() ? chosen.get(input.getItem()) : null;
    }

    public void choose(ItemStack input, Identifier recipe) {
        if (!input.isEmpty() && !recipe.equals(chosen.put(input.getItem(), recipe))) {
            version++;
        }
    }

    public int version() {
        return version;
    }

    /** One recipe that accepts an input, with what one operation makes. */
    public record Option(Identifier id, ItemStack result) {
    }

    /** The input whose recipe can be chosen and every recipe that accepts it, the default one first. */
    public record Offer(ItemStack input, List<Option> options) {
        /** The chosen option when it still accepts the input, otherwise the default one. */
        public Identifier selected(RecipeChoices choices) {
            Identifier id = choices.chosen(input);
            return id != null && options.stream().anyMatch(o -> o.id().equals(id)) ? id : options.getFirst().id();
        }
    }

    /** A machine whose recipe can be chosen per input item. */
    public interface Host {
        RecipeChoices recipeChoices();

        /** What can be chosen right now; empty when the machine holds nothing with a recipe. */
        Optional<Offer> recipeOffer();

        /** Chooses {@code recipe} for the input of the current offer; false when it is not one of its options. */
        default boolean chooseRecipe(Identifier recipe) {
            Optional<Offer> offer = recipeOffer();
            if (offer.isEmpty() || offer.get().options().stream().noneMatch(o -> o.id().equals(recipe))) {
                return false;
            }
            recipeChoices().choose(offer.get().input(), recipe);
            recipeChoicesChanged();
            return true;
        }

        void recipeChoicesChanged();
    }

    /** Writes {@code recipe_choices} when something was chosen. */
    public void save(CompoundTag tag) {
        if (chosen.isEmpty()) {
            return;
        }
        ListTag list = new ListTag();
        chosen.forEach((item, recipe) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("item", BuiltInRegistries.ITEM.getKey(item).toString());
            entry.putString("recipe", recipe.toString());
            list.add(entry);
        });
        tag.put("recipe_choices", list);
    }

    /** Reads {@code recipe_choices}; machines saved without it simply have no choice. */
    public void load(CompoundTag tag) {
        chosen.clear();
        ListTag list = tag.getListOrEmpty("recipe_choices");
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompoundOrEmpty(i);
            Identifier item = Identifier.tryParse(entry.getStringOr("item", ""));
            Identifier recipe = Identifier.tryParse(entry.getStringOr("recipe", ""));
            Item resolved = item == null ? Items.AIR : BuiltInRegistries.ITEM.getValue(item);
            if (resolved != Items.AIR && recipe != null) {
                chosen.put(resolved, recipe);
            }
        }
        version++;
    }
}
