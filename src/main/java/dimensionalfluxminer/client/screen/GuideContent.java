package dimensionalfluxminer.client.screen;

import dimensionalfluxminer.DfmConfig;
import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.compat.RecipeViewers;
import dimensionalfluxminer.crusher.DimensionalCrusherBlockEntity;
import dimensionalfluxminer.machine.math.MinerProductionCurve;
import dimensionalfluxminer.machine.math.RateScale;
import dimensionalfluxminer.miner.MatterValues;
import dimensionalfluxminer.registry.DfmDataMaps;
import dimensionalfluxminer.registry.DfmFluids;
import dimensionalfluxminer.registry.DfmItems;
import dimensionalfluxminer.registry.DfmTags;
import net.minecraft.core.registries.BuiltInRegistries;
import dimensionalfluxminer.smelter.AlloySmelterBlockEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;

/**
 * Chapters of the Dimensional Flux Guide. All text lives in the language files under
 * {@code guide.dimensionalfluxminer.<chapter>.*}; the Rate tables are computed with the machines' own formulas and the
 * current server configuration.
 */
final class GuideContent {
    /** Cooking time of a standard furnace recipe, used for the Alloy Smelter table. */
    private static final int STANDARD_RECIPE_TICKS = 200;

    record Chapter(String id, Supplier<ItemStack> icon, List<GuideElement> elements) {
    }

    private GuideContent() {
    }

    static List<Chapter> chapters() {
        return List.of(
                start(), materials(), crusher(), smelter(),
                assembly(), miner(), dimensions(), rates(), blight());
    }

    // ------------------------------------------------------------------ chapters

    private static Chapter start() {
        Builder b = new Builder("start");
        b.heading("welcome").text("welcome");
        b.heading("energy").text("energy");
        b.heading("path").text("path");
        b.heading("interface").text("interface").text("slots");
        b.tip("viewer");
        b.heading("guide").text("guide");
        b.crafting("guide_book");
        return b.chapter(DfmItems.GUIDE_BOOK);
    }

    private static Chapter materials() {
        Builder b = new Builder("materials");
        b.heading("steel").text("steel");
        b.items(DfmItems.DUSTS.get("iron"), DfmItems.DUSTS.get("coal"), DfmItems.GRAIN_CALCITE, DfmItems.INGOT_STEEL, DfmItems.SLAG);
        b.text("slag").crafting("slag_aggregate");
        b.heading("eye").text("eye").crafting("ender_eye_reinforced");
        b.heading("parts").text("parts");
        for (String part : List.of("reinforced_structural_glass", "steel_chassis", "ventilation_unit", "dimensional_port",
                "quantum_core", "miner_terminal")) {
            b.crafting(part);
        }
        b.heading("cores").crafting("basic_dimensional_core").crafting("stabilized_dimensional_core").crafting("efficiency_upgrade");
        b.heading("dusts").text("dusts");
        return b.chapter(DfmItems.INGOT_STEEL);
    }

    private static Chapter crusher() {
        Builder b = new Builder("crusher");
        b.item(DfmItems.DIMENSIONAL_CRUSHER, "about").crafting("dimensional_crusher");
        b.heading("ores").text("ores");
        b.heading("dusts").text("dusts");
        b.heading("slots").text("slots");
        b.heading("sides").text("sides");
        b.tip("speed");
        b.viewer("viewer", RecipeViewers.CRUSHING);
        return b.chapter(DfmItems.DIMENSIONAL_CRUSHER);
    }

    private static Chapter smelter() {
        Builder b = new Builder("smelter");
        b.item(DfmItems.ALLOY_SMELTER, "about").crafting("alloy_smelter");
        b.heading("modes").text("modes");
        b.heading("bulk").text("bulk");
        b.heading("split").text("split");
        b.heading("outputs").text("outputs");
        b.heading("xp").text("xp");
        b.viewer("viewer", RecipeViewers.ALLOYING, RecipeViewers.SMELTING);
        b.tip("multiply");
        return b.chapter(DfmItems.ALLOY_SMELTER);
    }

    private static Chapter assembly() {
        Builder b = new Builder("assembly");
        // The 3D view fills the chapter's first (left) page and the parts list the right one, so hovering a part
        // highlights it in the view on the same spread.
        b.elements.addAll(GuideMultiblock.elements());
        b.text("intro");
        b.heading("layers").text("layers");
        b.heading("front").text("front");
        b.heading("build").text("build");
        b.warning("power");
        return b.chapter(DfmItems.MINER_TERMINAL);
    }

    private static Chapter miner() {
        Builder b = new Builder("miner");
        b.heading("how").text("how");
        b.heading("cores").item(DfmItems.BASIC_DIMENSIONAL_CORE, "basic").item(DfmItems.STABILIZED_DIMENSIONAL_CORE, "stabilized");
        b.heading("matter").text("matter").add(matterTable()).text("matter_ores").text("matter_upgrade");
        b.heading("terminal").text("terminal");
        b.heading("buttons").text("buttons");
        b.heading("redstone").text("redstone");
        b.heading("output").text("output");
        return b.chapter(DfmItems.BASIC_DIMENSIONAL_CORE);
    }

    private static Chapter dimensions() {
        Builder b = new Builder("dimensions");
        b.text("intro");
        b.heading("overworld").items(Items.STONE, Items.DEEPSLATE, Items.DIRT, Items.DIAMOND_ORE).text("overworld");
        b.heading("nether").items(Items.NETHERRACK, Items.GLOWSTONE, Items.SOUL_SAND, Items.NETHER_BRICKS, Items.NETHER_QUARTZ_ORE)
                .text("nether");
        b.heading("end").items(Items.END_STONE, Items.PURPUR_BLOCK).text("end");
        b.item(Items.DRAGON_EGG, "egg");
        b.tip("discover");
        b.heading("ores").text("ores");
        b.item(Items.OBSIDIAN, "obsidian");
        b.heading("modded").text("modded");
        b.tip("table");
        return b.chapter(Items.CRYING_OBSIDIAN);
    }

    private static Chapter rates() {
        Builder b = new Builder("rates");
        b.text("intro").text("buffer");
        b.heading("miner").add(minerTable()).text("miner_note");
        b.heading("crusher").add(crusherTable()).text("crusher_note");
        b.heading("smelter").add(smelterTable()).text("smelter_note");
        b.heading("upgrade").item(DfmItems.EFFICIENCY_UPGRADE, "upgrade_about").text("upgrade");
        return b.chapter(DfmItems.EFFICIENCY_UPGRADE);
    }

    private static Chapter blight() {
        Builder b = new Builder("blight");
        b.text("intro");
        b.itemsAndFluids(List.of(DfmItems.CORRUPTED_GRASS, DfmItems.CORRUPTED_DIRT, DfmItems.CORRUPTED_STONE, DfmItems.CORRUPTED_SEDIMENT,
                DfmItems.CORRUPTED_GRASS_PLANT), List.<Supplier<? extends Fluid>>of(DfmFluids.CORRUPTED_WATER));
        b.heading("terrain").text("terrain");
        b.heading("water").text("water");
        b.heading("creatures").text("creatures");
        b.tip("avoid");
        return b.chapter(DfmItems.CORRUPTED_GRASS);
    }

    // ------------------------------------------------------------------ Rate tables

    private static GuideElement minerTable() {
        return new GuideElements.Table(headers("level", "fe", "ores", "dm"), new float[] {0.6F, 1.2F, 1.2F, 1.0F}, () -> {
            double[] fe = DfmConfig.range(DfmConfig.MINER_MIN_FE, DfmConfig.MINER_MAX_FE);
            double[] rate = DfmConfig.range(DfmConfig.MINER_MIN_RATE, DfmConfig.MINER_MAX_RATE);
            MinerProductionCurve curve = new MinerProductionCurve(fe[0], fe[1], rate[0], rate[1]);
            List<List<String>> rows = new ArrayList<>();
            for (int level = RateScale.MIN_LEVEL; level <= RateScale.MAX_LEVEL; level++) {
                double perSecond = curve.levelRate(level) * 20;
                rows.add(List.of(Integer.toString(level), DfmScreen.compact(Math.round(curve.levelPower(level))),
                        number(perSecond), number(perSecond * 2)));
            }
            return rows;
        });
    }

    private static GuideElement crusherTable() {
        return new GuideElements.Table(headers("level", "fe", "fe_upgrade", "operations"), new float[] {0.6F, 1.1F, 1.3F, 1.2F}, () -> {
            double[] fe = DfmConfig.range(DfmConfig.CRUSHER_MIN_FE, DfmConfig.CRUSHER_MAX_FE);
            List<List<String>> rows = new ArrayList<>();
            for (int level = RateScale.MIN_LEVEL; level <= RateScale.MAX_LEVEL; level++) {
                double perSecond = RateScale.value(level, DimensionalCrusherBlockEntity.MIN_OPERATIONS_PER_TICK,
                        DimensionalCrusherBlockEntity.MAX_OPERATIONS_PER_TICK) * 20;
                rows.add(List.of(Integer.toString(level), DfmScreen.compact(RateScale.powerLimit(level, fe[0], fe[1], 1.0)),
                        DfmScreen.compact(RateScale.powerLimit(level, fe[0], fe[1], 0.5)), number(perSecond)));
            }
            return rows;
        });
    }

    private static GuideElement smelterTable() {
        return new GuideElements.Table(headers("level", "fe", "fe_upgrade", "lane"), new float[] {0.6F, 1.0F, 1.2F, 1.1F}, () -> {
            double[] fe = DfmConfig.range(DfmConfig.SMELTER_MIN_FE, DfmConfig.SMELTER_MAX_FE);
            List<List<String>> rows = new ArrayList<>();
            for (int level = RateScale.MIN_LEVEL; level <= RateScale.MAX_LEVEL; level++) {
                double lane = RateScale.smelterOperationsPerTick(level, STANDARD_RECIPE_TICKS,
                        AlloySmelterBlockEntity.RATE_TEN_ITEMS_PER_TICK) * 20;
                rows.add(List.of(Integer.toString(level), DfmScreen.compact(RateScale.powerLimit(level, fe[0], fe[1], 1.0)),
                        DfmScreen.compact(RateScale.powerLimit(level, fe[0], fe[1], 0.5)), number(lane)));
            }
            return rows;
        });
    }

    /**
     * Matter values of the non-ore substrates, read from the {@code matter_value} data map (synced to clients), so a
     * datapack that changes them changes the guide too.
     */
    private static GuideElement matterTable() {
        return new GuideElements.Table(headers("item", "dimension", "dm_value", "dm_upgrade"), new float[] {1.7F, 1.3F, 0.6F, 1.0F}, () -> {
            record Row(String name, int dimensionOrder, String dimension, int units) {
            }
            List<Row> list = new ArrayList<>();
            for (var entry : BuiltInRegistries.ITEM.getDataMap(DfmDataMaps.MATTER_VALUE).entrySet()) {
                ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(entry.getKey()));
                if (stack.isEmpty() || stack.is(DfmTags.Items.common("ores"))) {
                    continue;
                }
                DfmDataMaps.MatterValue value = entry.getValue();
                String dimensionId = value.dimensions().isEmpty() ? "any" : value.dimensions().getFirst().location().getPath();
                list.add(new Row(stack.getHoverName().getString(), dimensionOrder(dimensionId),
                        Component.translatable("guide.dimensionalfluxminer.dimension." + dimensionId).getString(), value.units()));
            }
            list.sort(java.util.Comparator.comparingInt(Row::dimensionOrder).thenComparing(Row::units, java.util.Comparator.reverseOrder())
                    .thenComparing(Row::name));
            List<List<String>> rows = new ArrayList<>();
            for (Row row : list) {
                rows.add(List.of(row.name(), row.dimension(), number(row.units() / (double) MatterValues.UNITS_PER_DM),
                        number(MatterValues.matterInputUnits(row.units(), true) / (double) MatterValues.UNITS_PER_DM)));
            }
            return rows;
        });
    }

    private static int dimensionOrder(String dimension) {
        return switch (dimension) {
            case "overworld" -> 0;
            case "the_nether" -> 1;
            case "the_end" -> 2;
            default -> 3;
        };
    }

    private static List<Component> headers(String... keys) {
        List<Component> list = new ArrayList<>();
        for (String key : keys) {
            list.add(Component.translatable("guide.dimensionalfluxminer.table." + key));
        }
        return list;
    }

    /** Three significant digits at most, no trailing zeros. */
    static String number(double value) {
        String text;
        if (value >= 100) {
            text = String.format(Locale.ROOT, "%.0f", value);
        } else if (value >= 10) {
            text = String.format(Locale.ROOT, "%.1f", value);
        } else if (value >= 1) {
            text = String.format(Locale.ROOT, "%.2f", value);
        } else {
            text = String.format(Locale.ROOT, "%.3f", value);
        }
        if (text.contains(".")) {
            text = text.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return text;
    }

    // ------------------------------------------------------------------ builder

    /** Collects the elements of one chapter; text keys are relative to {@code guide.dimensionalfluxminer.<chapter>}. */
    private static final class Builder {
        private final String id;
        private final List<GuideElement> elements = new ArrayList<>();

        Builder(String id) {
            this.id = id;
        }

        private Component key(String kind, String name) {
            return Component.translatable("guide.dimensionalfluxminer." + id + "." + kind + name);
        }

        Builder heading(String name) {
            elements.add(new GuideElements.Heading(key("h.", name)));
            return this;
        }

        Builder text(String name) {
            elements.add(new GuideElements.Text(key("", name)));
            return this;
        }

        Builder tip(String name) {
            elements.add(new GuideElements.Note(Component.translatable("guide.dimensionalfluxminer.tip"), key("", name), GuideElements.TIP));
            return this;
        }

        Builder warning(String name) {
            elements.add(new GuideElements.Note(Component.translatable("guide.dimensionalfluxminer.warning"), key("", name),
                    GuideElements.WARNING));
            return this;
        }

        Builder item(ItemLike item, String name) {
            elements.add(new GuideElements.ItemNote(() -> new ItemStack(item), key("", name)));
            return this;
        }

        Builder items(ItemLike... items) {
            return itemsAndFluids(List.of(items), List.of());
        }

        Builder itemsAndFluids(List<? extends ItemLike> items, List<Supplier<? extends Fluid>> fluids) {
            elements.add(new GuideElements.ItemRow(() -> {
                List<ItemStack> list = new ArrayList<>();
                for (ItemLike item : items) {
                    list.add(new ItemStack(item));
                }
                return list;
            }, fluids));
            return this;
        }

        Builder crafting(String recipe) {
            elements.add(new GuideElements.Crafting(DimensionalFluxMiner.id(recipe)));
            return this;
        }

        Builder viewer(String name, net.minecraft.resources.ResourceLocation... categories) {
            elements.add(new GuideElements.ViewerButton(key("", name), categories));
            return this;
        }

        Builder add(GuideElement element) {
            elements.add(element);
            return this;
        }

        Chapter chapter(ItemLike icon) {
            return new Chapter(id, () -> new ItemStack(icon), elements);
        }
    }
}
