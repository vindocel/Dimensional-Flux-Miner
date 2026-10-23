package dimensionalfluxminer.client.screen;

import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.compat.RecipeViewers;
import dimensionalfluxminer.crusher.MechanicalCrusherBlockEntity;
import dimensionalfluxminer.machine.math.MinerProductionCurve;
import dimensionalfluxminer.machine.math.RateScale;
import dimensionalfluxminer.miner.MatterValues;
import dimensionalfluxminer.registry.DfmDataMaps;
import dimensionalfluxminer.registry.DfmFluids;
import dimensionalfluxminer.registry.DfmItems;
import dimensionalfluxminer.registry.DfmTags;
import net.minecraft.core.registries.BuiltInRegistries;
import dimensionalfluxminer.furnace.ElectricFurnaceBlockEntity;
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
    /** Cooking time of a standard furnace recipe, used for the Electric Furnace table. */
    private static final int STANDARD_RECIPE_TICKS = 200;

    record Chapter(String id, Supplier<ItemStack> icon, List<GuideElement> elements) {
    }

    private GuideContent() {
    }

    static List<Chapter> chapters() {
        return List.of(
                start(), materials(), crusher(), furnace(),
                assembly(), miner(), automation(), tanks(), dimensions(), rates(), blight(), trouble());
    }

    // ------------------------------------------------------------------ chapters

    private static Chapter start() {
        Builder b = new Builder("start");
        b.heading("welcome").text("welcome");
        b.heading("energy").text("energy");
        b.heading("path").text("path");
        b.heading("interface").text("interface").text("slots");
        b.tip("viewer");
        b.heading("breaking").text("breaking");
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
        b.item(DfmItems.MECHANICAL_CRUSHER, "about").crafting("mechanical_crusher");
        b.heading("ores").text("ores");
        b.heading("dusts").text("dusts");
        b.heading("slots").text("slots");
        b.heading("sides").text("sides");
        b.tip("speed");
        b.viewer("viewer", RecipeViewers.CRUSHING);
        return b.chapter(DfmItems.MECHANICAL_CRUSHER);
    }

    private static Chapter furnace() {
        Builder b = new Builder("electric_furnace");
        b.item(DfmItems.ELECTRIC_FURNACE, "about").crafting("electric_furnace");
        b.heading("modes").text("modes");
        b.heading("bulk").text("bulk");
        b.heading("split").text("split");
        b.heading("outputs").text("outputs");
        b.heading("lanes").text("lanes");
        b.heading("xp").text("xp").text("xp_pipes");
        b.viewer("viewer", RecipeViewers.ALLOYING, RecipeViewers.SMELTING);
        b.tip("multiply");
        b.tip("external");
        return b.chapter(DfmItems.ELECTRIC_FURNACE);
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
        b.text("compare");
        b.tip("loaded");
        b.heading("matter").text("matter").add(matterTable()).text("matter_ores").text("matter_upgrade");
        b.heading("terminal").text("terminal");
        b.heading("buttons").text("buttons");
        b.heading("filter").text("filter");
        b.heading("output").text("output");
        return b.chapter(DfmItems.BASIC_DIMENSIONAL_CORE);
    }

    private static Chapter dimensions() {
        Builder b = new Builder("dimensions");
        b.text("intro");
        // Each slot cycles through a block and its variants, so the reader sees a family, not a single item.
        b.heading("overworld").cycling(dimensionalfluxminer.miner.MatterExamples.OVERWORLD).text("overworld");
        b.heading("nether").cycling(dimensionalfluxminer.miner.MatterExamples.NETHER).text("nether");
        b.heading("end").cycling(dimensionalfluxminer.miner.MatterExamples.END).text("end");
        b.item(Items.DRAGON_EGG, "egg");
        b.heading("barren").text("barren");
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
        b.heading("electric_furnace").add(electricFurnaceTable()).text("electric_furnace_note");
        b.heading("upgrade").item(DfmItems.EFFICIENCY_UPGRADE, "upgrade_about").text("upgrade");
        return b.chapter(DfmItems.EFFICIENCY_UPGRADE);
    }

    private static Chapter blight() {
        Builder b = new Builder("blight");
        b.text("intro");
        b.itemsAndFluids(List.of(DfmItems.CORRUPTED_GRASS, DfmItems.CORRUPTED_DIRT, DfmItems.CORRUPTED_STONE, DfmItems.CORRUPTED_TERRACOTTA, DfmItems.CORRUPTED_SEDIMENT,
                DfmItems.CORRUPTED_GRASS_PLANT), List.<Supplier<? extends Fluid>>of(DfmFluids.CORRUPTED_WATER));
        b.heading("terrain").text("terrain");
        b.heading("water").text("water");
        b.heading("creatures").text("creatures");
        b.heading("protection").text("protection");
        b.tip("avoid");
        return b.chapter(DfmItems.CORRUPTED_GRASS);
    }

    private static Chapter automation() {
        Builder b = new Builder("automation");
        b.text("intro");
        b.heading("sides").text("sides").text("colors");
        b.heading("auto").text("auto");
        b.heading("redstone").text("redstone");
        b.heading("cables").text("cables");
        b.heading("network").compat("network", "network", "lanes");
        b.heading("recipes").compat("recipes", "recipes");
        b.heading("accel").text("accel");
        return b.chapter(DfmItems.DIMENSIONAL_PORT);
    }

    private static Chapter tanks() {
        Builder b = new Builder("tanks");
        b.item(DfmItems.DIMENSIONAL_TANK, "about").crafting("dimensional_tank");
        b.heading("dual").item(DfmItems.DIMENSIONAL_DUAL_TANK, "dual").crafting("dimensional_dual_tank");
        b.heading("code").text("code").text("code_change");
        b.heading("personal").text("personal");
        b.heading("buckets").text("buckets");
        b.heading("pipes").text("pipes").text("pipes_dual");
        b.heading("display").text("display");
        b.tip("break");
        return b.chapter(DfmItems.DIMENSIONAL_TANK);
    }

    private static Chapter trouble() {
        Builder b = new Builder("trouble");
        b.text("intro");
        b.heading("miner").text("miner");
        b.heading("machines").text("machines");
        b.tip("energy");
        return b.chapter(Items.REDSTONE_TORCH);
    }

    // ------------------------------------------------------------------ Rate tables

    private static GuideElement minerTable() {
        return new GuideElements.Table(headers("level", "fe", "ores", "dm"), new float[] {0.6F, 1.2F, 1.2F, 1.0F}, () -> {
            MinerProductionCurve curve = dimensionalfluxminer.miner.MinerControllerBlockEntity.curve();
            List<List<String>> rows = new ArrayList<>();
            for (int level = RateScale.MIN_LEVEL; level <= RateScale.MAX_LEVEL; level++) {
                double perSecond = curve.levelRate(level) * 20;
                rows.add(List.of(Integer.toString(level), DfmScreen.compact(RateScale.powerLimit(curve.levelPower(level))),
                        number(perSecond), number(perSecond * 2)));
            }
            return rows;
        });
    }

    private static GuideElement crusherTable() {
        return new GuideElements.Table(headers("level", "fe", "fe_upgrade", "operations"), new float[] {0.6F, 1.1F, 1.3F, 1.2F}, () -> {
            List<List<String>> rows = new ArrayList<>();
            for (int level = RateScale.MIN_LEVEL; level <= RateScale.MAX_LEVEL; level++) {
                double ops = MechanicalCrusherBlockEntity.operationsPerTick(level);
                rows.add(List.of(Integer.toString(level),
                        DfmScreen.compact(RateScale.powerLimit(RateScale.power(ops, MechanicalCrusherBlockEntity.energyPerOperation(level, false)))),
                        DfmScreen.compact(RateScale.powerLimit(RateScale.power(ops, MechanicalCrusherBlockEntity.energyPerOperation(level, true)))),
                        number(ops * 20)));
            }
            return rows;
        });
    }

    private static GuideElement electricFurnaceTable() {
        return new GuideElements.Table(headers("level", "fe", "fe_upgrade", "lane"), new float[] {0.6F, 1.0F, 1.2F, 1.1F}, () -> {
            List<List<String>> rows = new ArrayList<>();
            for (int level = RateScale.MIN_LEVEL; level <= RateScale.MAX_LEVEL; level++) {
                double lane = dimensionalfluxminer.furnace.ElectricFurnaceEnergy.laneOperationsPerTick(level, STANDARD_RECIPE_TICKS) * 20;
                // Furnace profile with the three lanes busy, the larger of the two profiles.
                rows.add(List.of(Integer.toString(level),
                        DfmScreen.compact(RateScale.powerLimit(dimensionalfluxminer.furnace.ElectricFurnaceEnergy.furnaceStandardPower(level, false))),
                        DfmScreen.compact(RateScale.powerLimit(dimensionalfluxminer.furnace.ElectricFurnaceEnergy.furnaceStandardPower(level, true))), number(lane)));
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
                ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.getValue(entry.getKey()));
                if (stack.isEmpty() || stack.is(DfmTags.Items.common("ores"))) {
                    continue;
                }
                // The 16 dyed terracottas share the plain terracotta's row.
                if (stack.is(net.minecraft.tags.ItemTags.TERRACOTTA) && !stack.is(Items.TERRACOTTA)) {
                    continue;
                }
                DfmDataMaps.MatterValue value = entry.getValue();
                String dimensionId = value.dimensions().isEmpty() ? "any" : value.dimensions().getFirst().identifier().getPath();
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
            GuideReferences.startChapter();
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

        /**
         * Compatibility box: the summary {@code <name>_summary} and a button that shows the {@code details} paragraphs
         * under it while open.
         */
        Builder compat(String name, String... details) {
            String box = id + "." + name;
            elements.add(new GuideElements.Expandable(box, Component.translatable("guide.dimensionalfluxminer.compat"),
                    key("", name + "_summary")));
            if (GuideScreen.expanded(box)) {
                for (String detail : details) {
                    text(detail);
                }
            }
            return this;
        }

        Builder item(ItemLike item, String name) {
            elements.add(new GuideElements.ItemNote(() -> new ItemStack(item), key("", name)));
            return this;
        }

        Builder items(ItemLike... items) {
            return itemsAndFluids(List.of(items), List.of());
        }

        /** A row of slots, each cycling once a second through its items (a block and its variants, the ores...). */
        Builder cycling(List<? extends List<? extends ItemLike>> slots) {
            elements.add(new GuideElements.ItemRow(() -> {
                long tick = net.minecraft.util.Util.getMillis() / 1000;
                List<ItemStack> list = new ArrayList<>();
                for (List<? extends ItemLike> slot : slots) {
                    list.add(new ItemStack(slot.get((int) (tick % slot.size()))));
                }
                return list;
            }, List.of()));
            return this;
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

        Builder viewer(String name, net.minecraft.resources.Identifier... categories) {
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
