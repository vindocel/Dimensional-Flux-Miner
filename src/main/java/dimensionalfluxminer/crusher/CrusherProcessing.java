package dimensionalfluxminer.crusher;

import dimensionalfluxminer.recipe.CanonicalItems;
import dimensionalfluxminer.recipe.CrushingRecipe;
import dimensionalfluxminer.recipe.OreCrushingRecipe;
import dimensionalfluxminer.registry.DfmItems;
import dimensionalfluxminer.registry.DfmRecipes;
import dimensionalfluxminer.registry.DfmTags;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

/**
 * Resolves what the Crusher makes from one input item. Priority:
 * <ol>
 *     <li>explicit {@code dimensionalfluxminer:crushing} recipe;</li>
 *     <li>explicit {@code dimensionalfluxminer:ore_crushing} recipe;</li>
 *     <li>ore family through {@code c:ores/<material>} -> {@code c:raw_materials/<material>} -> {@code c:gems/<material>};</li>
 *     <li>Resource Profile {@code crusher_output} override;</li>
 *     <li>material dust: an item in {@code c:ingots/<m>}, {@code c:raw_materials/<m>} or {@code c:gems/<m>} gives one
 *     item of {@code c:dusts/<m>} (vanilla first, then this mod's dust, then any other mod's).</li>
 * </ol>
 * No Raw item is ever invented: families without Raw use their canonical mineral product.
 */
public final class CrusherProcessing {
    /** Tag folders whose members are crushed into the dust of the same material. */
    private static final List<String> DUST_SOURCES = List.of("ingots/", "raw_materials/", "gems/");

    /** Ore family -> canonical product supplied by Resource Profile datapack overrides. */
    private static final Map<Item, Item> PROFILE_OVERRIDES = new ConcurrentHashMap<>();

    private CrusherProcessing() {
    }

    public static void setProfileOverrides(Map<Item, Item> overrides) {
        PROFILE_OVERRIDES.clear();
        PROFILE_OVERRIDES.putAll(overrides);
    }

    /**
     * @param result output of one operation (count = items produced per input item)
     * @param ore    true when the result follows the ore-processing yield (2 / 3 with upgrade)
     */
    public record Resolution(ItemStack result, boolean ore) {
    }

    /**
     * Same as {@link #resolve(Level, ItemStack, boolean)}, but the explicit recipe {@code chosen} (when not null and
     * still accepting the input) replaces the default one.
     */
    public static Optional<Resolution> resolve(Level level, ItemStack input, boolean efficiencyUpgrade, Identifier chosen) {
        if (chosen != null) {
            for (Option option : options(level, input, efficiencyUpgrade)) {
                if (option.id().equals(chosen)) {
                    return Optional.of(option.resolution());
                }
            }
        }
        return resolve(level, input, efficiencyUpgrade);
    }

    /** One explicit recipe that accepts an input and what it makes. */
    public record Option(Identifier id, Resolution resolution) {
    }

    /**
     * Every explicit recipe ({@code crushing}, then {@code ore_crushing}) that accepts the input, the default one first;
     * recipes making the same result are listed once. Empty when the default result does not come from a recipe (tags,
     * Resource Profile, dusts): there is nothing to choose from then.
     */
    public static List<Option> options(Level level, ItemStack input, boolean efficiencyUpgrade) {
        if (input.isEmpty() || input.is(DfmItems.EFFICIENCY_UPGRADE.get())) {
            return List.of();
        }
        SingleRecipeInput recipeInput = new SingleRecipeInput(input.copyWithCount(1));
        List<Option> options = new ArrayList<>();
        Optional<RecipeHolder<CrushingRecipe>> crushing = dimensionalfluxminer.recipe.Recipes.first(level, DfmRecipes.CRUSHING.get(), recipeInput);
        Optional<RecipeHolder<OreCrushingRecipe>> ore = dimensionalfluxminer.recipe.Recipes.first(level, DfmRecipes.ORE_CRUSHING.get(), recipeInput);
        if (crushing.isEmpty() && (ore.isEmpty() || ore.get().value().product().isEmpty())) {
            return List.of();
        }
        crushing.ifPresent(h -> addOption(options, h.id().identifier(), new Resolution(h.value().result().copy(), false)));
        for (RecipeHolder<CrushingRecipe> h : dimensionalfluxminer.recipe.Recipes.matching(level, DfmRecipes.CRUSHING.get(), recipeInput)) {
            addOption(options, h.id().identifier(), new Resolution(h.value().result().copy(), false));
        }
        ore.ifPresent(h -> addOre(options, h, efficiencyUpgrade));
        for (RecipeHolder<OreCrushingRecipe> h : dimensionalfluxminer.recipe.Recipes.matching(level, DfmRecipes.ORE_CRUSHING.get(), recipeInput)) {
            addOre(options, h, efficiencyUpgrade);
        }
        return options;
    }

    private static void addOre(List<Option> options, RecipeHolder<OreCrushingRecipe> holder, boolean efficiencyUpgrade) {
        ItemStack product = holder.value().product();
        if (!product.isEmpty()) {
            addOption(options, holder.id().identifier(), new Resolution(product.copyWithCount(holder.value().yield(efficiencyUpgrade)), true));
        }
    }

    private static void addOption(List<Option> options, Identifier id, Resolution resolution) {
        for (Option known : options) {
            if (known.id().equals(id) || ItemStack.matches(known.resolution().result(), resolution.result())) {
                return;
            }
        }
        options.add(new Option(id, resolution));
    }

    public static Optional<Resolution> resolve(Level level, ItemStack input, boolean efficiencyUpgrade) {
        if (input.isEmpty() || input.is(DfmItems.EFFICIENCY_UPGRADE.get())) {
            return Optional.empty();
        }
        SingleRecipeInput recipeInput = new SingleRecipeInput(input.copyWithCount(1));
        Optional<CrushingRecipe> crushing = dimensionalfluxminer.recipe.Recipes.first(level, DfmRecipes.CRUSHING.get(), recipeInput).map(h -> h.value());
        if (crushing.isPresent()) {
            return Optional.of(new Resolution(crushing.get().result().copy(), false));
        }
        Optional<OreCrushingRecipe> ore = dimensionalfluxminer.recipe.Recipes.first(level, DfmRecipes.ORE_CRUSHING.get(), recipeInput).map(h -> h.value());
        if (ore.isPresent()) {
            ItemStack product = ore.get().product();
            if (!product.isEmpty()) {
                return Optional.of(new Resolution(product.copyWithCount(ore.get().yield(efficiencyUpgrade)), true));
            }
        }
        Optional<Item> family = oreFamilyProduct(input);
        if (family.isEmpty()) {
            Item override = PROFILE_OVERRIDES.get(input.getItem());
            family = Optional.ofNullable(override);
        }
        if (family.isPresent()) {
            return Optional.of(new Resolution(new ItemStack(family.get(), efficiencyUpgrade ? 3 : 2), true));
        }
        return materialDust(input).map(dust -> new Resolution(new ItemStack(dust), false));
    }

    /** {@code c:ores/<material>} membership resolved to the Raw item, else the gem, of the same material. */
    static Optional<Item> oreFamilyProduct(ItemStack input) {
        return input.typeHolder().tags()
                .filter(tag -> tag.location().getNamespace().equals("c") && tag.location().getPath().startsWith("ores/"))
                .map(tag -> tag.location().getPath().substring("ores/".length()))
                .map(material -> {
                    Optional<Item> raw = CanonicalItems.first(DfmTags.Items.common("raw_materials/" + material));
                    return raw.isPresent() ? raw : CanonicalItems.first(DfmTags.Items.common("gems/" + material));
                })
                .flatMap(Optional::stream)
                .findFirst();
    }

    /** Ingot, raw material or gem of a material that has a dust: one dust per item. */
    static Optional<Item> materialDust(ItemStack input) {
        if (input.typeHolder().tags().anyMatch(tag -> tag.location().getNamespace().equals("c") && tag.location().getPath().startsWith("dusts"))) {
            return Optional.empty();
        }
        return input.typeHolder().tags()
                .filter(tag -> tag.location().getNamespace().equals("c"))
                .map(tag -> tag.location().getPath())
                .flatMap(path -> DUST_SOURCES.stream().filter(path::startsWith).map(prefix -> path.substring(prefix.length())))
                .filter(material -> !material.isEmpty() && !material.contains("/"))
                .map(material -> CanonicalItems.first(DfmTags.Items.common("dusts/" + material)))
                .flatMap(Optional::stream)
                .filter(dust -> dust != input.getItem())
                .findFirst();
    }

    /** One recipe-viewer entry: every input giving the same output (and the same output with the upgrade). */
    public record Display(List<ItemStack> inputs, ItemStack output, ItemStack upgradedOutput) {
    }

    /** Everything the Crusher can process right now, grouped by result, for recipe viewers. */
    public static List<Display> displays(Level level) {
        Set<Item> candidates = new LinkedHashSet<>();
        for (RecipeHolder<CrushingRecipe> holder : dimensionalfluxminer.recipe.Recipes.byType(level, DfmRecipes.CRUSHING.get())) {
            for (ItemStack option : dimensionalfluxminer.recipe.Recipes.stacks(holder.value().ingredient())) {
                candidates.add(option.getItem());
            }
        }
        for (RecipeHolder<OreCrushingRecipe> holder : dimensionalfluxminer.recipe.Recipes.byType(level, DfmRecipes.ORE_CRUSHING.get())) {
            for (ItemStack option : dimensionalfluxminer.recipe.Recipes.stacks(holder.value().ingredient())) {
                candidates.add(option.getItem());
            }
        }
        BuiltInRegistries.ITEM.getTags().forEach(pair -> {
            var id = pair.key().location();
            if (id.getNamespace().equals("c") && (id.getPath().startsWith("ores/") || DUST_SOURCES.stream().anyMatch(id.getPath()::startsWith))) {
                pair.stream().map(Holder::value).forEach(candidates::add);
            }
        });
        Map<String, Display> grouped = new LinkedHashMap<>();
        for (Item item : candidates) {
            ItemStack input = new ItemStack(item);
            Optional<Resolution> normal = resolve(level, input, false);
            if (normal.isEmpty()) {
                continue;
            }
            ItemStack output = normal.get().result();
            ItemStack upgraded = resolve(level, input, true).map(Resolution::result).orElse(output);
            String key = BuiltInRegistries.ITEM.getKey(output.getItem()) + "|" + output.getCount() + "|" + upgraded.getCount();
            Display display = grouped.computeIfAbsent(key, k -> new Display(new ArrayList<>(), output, upgraded));
            display.inputs().add(input);
        }
        return List.copyOf(grouped.values());
    }
}
