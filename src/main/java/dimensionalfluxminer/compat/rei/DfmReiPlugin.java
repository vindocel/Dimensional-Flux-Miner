package dimensionalfluxminer.compat.rei;

import dev.architectury.event.CompoundEventResult;
import dimensionalfluxminer.client.screen.CrusherLayout;
import dimensionalfluxminer.client.screen.DfmScreen;
import dimensionalfluxminer.client.screen.ElectricFurnaceLayout;
import dimensionalfluxminer.client.screen.GuiTheme;
import dimensionalfluxminer.compat.RecipeViewerData;
import dimensionalfluxminer.compat.RecipeViewers;
import dimensionalfluxminer.crusher.CrusherProcessing;
import dimensionalfluxminer.recipe.AlloyingRecipe;
import dimensionalfluxminer.registry.DfmBlocks;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.config.ConfigObject;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.screen.ExclusionZones;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.forge.REIPluginClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * REI integration: the same Crushing, Electric Furnace furnace mode and Alloying pages as the JEI and EMI plugins, drawn
 * with the machines' own elements. R / U work on the machine items of the screens, and the side tabs and open windows
 * (also while dragged) are kept free of REI's lists. The pages follow REI's own light or dark theme. Skipped when EMI is installed, which has its own plugin.
 */
@REIPluginClient
public final class DfmReiPlugin implements REIClientPlugin {
    static final CategoryIdentifier<Page> CRUSHING = CategoryIdentifier.of(RecipeViewers.CRUSHING);
    static final CategoryIdentifier<Page> SMELTING = CategoryIdentifier.of(RecipeViewers.SMELTING);
    static final CategoryIdentifier<Page> ALLOYING = CategoryIdentifier.of(RecipeViewers.ALLOYING);
    /** The element plus a 1 px margin (the alloy element is drawn 1 px around the slot area). */
    private static final int ELEMENT_W = ElectricFurnaceLayout.WIDTH + 2;
    private static final int ELEMENT_H = ElectricFurnaceLayout.HEIGHT + 2;
    /** Space between REI's page frame and the element. */
    private static final int PADDING = 4;
    /** Crushing preview: one operation every 4 s. */
    private static final long PREVIEW_MS = 4000;

    /** Creative tanks holding different liquids are different entries. */
    @Override
    public void registerItemComparators(me.shedaniel.rei.api.common.entry.comparison.ItemComparatorRegistry registry) {
        registry.registerComponents(dimensionalfluxminer.registry.DfmItems.CREATIVE_DIMENSIONAL_TANK.get());
    }

    /** One creative tank per liquid, right after the empty one (and only once, whatever REI took from the creative tab). */
    @Override
    public void registerEntries(me.shedaniel.rei.api.client.registry.entry.EntryRegistry registry) {
        Level level = Minecraft.getInstance().level;
        if (RecipeViewers.emi() || level == null) {
            return;
        }
        net.minecraft.world.item.Item tank = dimensionalfluxminer.registry.DfmItems.CREATIVE_DIMENSIONAL_TANK.get();
        registry.removeEntryIf(entry -> entry.getValue() instanceof ItemStack stack && stack.is(tank)
                && stack.has(dimensionalfluxminer.registry.DfmDataComponents.CREATIVE_TANK_FLUID.get()));
        List<EntryStack<ItemStack>> filled = new ArrayList<>();
        for (ItemStack stack : dimensionalfluxminer.tank.CreativeTanks.filledForEveryFluid(level.registryAccess())) {
            filled.add(EntryStacks.of(stack));
        }
        registry.addEntriesAfter(EntryStacks.of(tank), filled);
    }

    @Override
    public void registerCategories(CategoryRegistry registry) {
        if (RecipeViewers.emi()) {
            return;
        }
        registry.add(new Category(CRUSHING, "crushing", DfmBlocks.MECHANICAL_CRUSHER.get().asItem().getDefaultInstance(),
                CrusherLayout.WIDTH, CrusherLayout.HEIGHT));
        registry.add(new Category(SMELTING, "smelting", DfmBlocks.ELECTRIC_FURNACE.get().asItem().getDefaultInstance(),
                ELEMENT_W, ELEMENT_H));
        registry.add(new Category(ALLOYING, "alloying", DfmBlocks.ELECTRIC_FURNACE.get().asItem().getDefaultInstance(),
                ELEMENT_W, ELEMENT_H));
        registry.addWorkstations(CRUSHING, EntryStacks.of(DfmBlocks.MECHANICAL_CRUSHER.get()));
        registry.addWorkstations(SMELTING, EntryStacks.of(DfmBlocks.ELECTRIC_FURNACE.get()));
        registry.addWorkstations(ALLOYING, EntryStacks.of(DfmBlocks.ELECTRIC_FURNACE.get()));
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        Level level = Minecraft.getInstance().level;
        if (RecipeViewers.emi() || level == null) {
            return;
        }
        for (CrusherProcessing.Display display : RecipeViewerData.crushing()) {
            EntryIngredient output = EntryIngredients.of(display.output());
            if (display.upgradedOutput().getCount() != display.output().getCount()) {
                output = withLine(output, RecipeViewerData.upgradeLine(display));
            }
            registry.add(new Page(CRUSHING, List.of(EntryIngredients.ofItemStacks(display.inputs())), List.of(output), null, 0));
        }
        for (RecipeViewerData.FurnaceDisplay display : RecipeViewerData.electricFurnaceSmelting(level.getRecipeManager(),
                level.registryAccess())) {
            List<ItemStack> inputs = List.of(display.input().getItems()).stream().map(s -> s.copyWithCount(display.inputCount())).toList();
            EntryIngredient output = EntryIngredients.of(display.output());
            if (display.bulk()) {
                output = withLine(output, RecipeViewerData.bulkLine());
            }
            registry.add(new Page(SMELTING, List.of(EntryIngredients.ofItemStacks(inputs)), List.of(output), display.id(),
                    display.cookTicks() * 50L));
        }
        for (RecipeHolder<AlloyingRecipe> holder : RecipeViewerData.alloying(level.getRecipeManager())) {
            AlloyingRecipe recipe = holder.value();
            List<EntryIngredient> inputs = new ArrayList<>();
            for (SizedIngredient sized : recipe.ingredients()) {
                inputs.add(EntryIngredients.ofItemStacks(List.of(sized.getItems())));
            }
            List<EntryIngredient> outputs = new ArrayList<>();
            outputs.add(EntryIngredients.of(recipe.result()));
            recipe.byproduct().ifPresent(b -> outputs.add(withLine(EntryIngredients.of(b.stack()), RecipeViewerData.chanceLine(b.chance()))));
            registry.add(new Page(ALLOYING, inputs, outputs, holder.id(), recipe.processingTicks() * 50L));
        }
    }

    /** Machine items are drawn by the screen, not in vanilla slots: R / U work on them through this provider. */
    @Override
    public void registerScreens(ScreenRegistry registry) {
        registry.registerFocusedStack((screen, mouse) -> {
            if (screen instanceof DfmScreen<?> machine) {
                Optional<EntryStack<?>> stack = machine.machineItemAt(mouse.x, mouse.y).map(item -> EntryStacks.of(item.stack()));
                if (stack.isPresent()) {
                    return CompoundEventResult.interruptTrue(stack.get());
                }
            }
            return CompoundEventResult.pass();
        });
    }

    @Override
    public void registerExclusionZones(ExclusionZones zones) {
        zones.register(Screen.class, screen -> screen instanceof DfmScreen<?> machine
                ? machine.outsideAreas().stream().map(area -> new Rectangle(area.getX(), area.getY(), area.getWidth(), area.getHeight())).toList()
                : List.of());
    }

    /** The same entries with one more tooltip line. */
    private static EntryIngredient withLine(EntryIngredient ingredient, Component line) {
        List<EntryStack<?>> stacks = new ArrayList<>();
        for (EntryStack<?> stack : ingredient) {
            stacks.add(stack.copy().tooltip(line));
        }
        return EntryIngredient.of(stacks);
    }

    /** One page: its inputs, outputs, recipe id (null for crushing) and the length of its heat animation. */
    record Page(CategoryIdentifier<Page> category, List<EntryIngredient> inputs, List<EntryIngredient> outputs,
                ResourceLocation location, long cycleMs) implements Display {
        @Override
        public List<EntryIngredient> getInputEntries() {
            return inputs;
        }

        @Override
        public List<EntryIngredient> getOutputEntries() {
            return outputs;
        }

        @Override
        public CategoryIdentifier<?> getCategoryIdentifier() {
            return category;
        }

        @Override
        public Optional<ResourceLocation> getDisplayLocation() {
            return Optional.ofNullable(location);
        }
    }

    private record Category(CategoryIdentifier<Page> id, String name, ItemStack icon, int width, int height)
            implements DisplayCategory<Page> {
        @Override
        public CategoryIdentifier<Page> getCategoryIdentifier() {
            return id;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("emi.category.dimensionalfluxminer." + name);
        }

        @Override
        public Renderer getIcon() {
            return EntryStacks.of(icon);
        }

        @Override
        public int getDisplayWidth(Page page) {
            return width + 2 * PADDING;
        }

        @Override
        public int getDisplayHeight() {
            return height + 2 * PADDING;
        }

        @Override
        public List<Widget> setupDisplay(Page page, Rectangle bounds) {
            List<Widget> widgets = new ArrayList<>();
            int x = bounds.x + PADDING;
            int y = bounds.y + PADDING;
            widgets.add(Widgets.createRecipeBase(bounds));
            // The element (slot frames, energy and heat parts included) follows REI's light or dark theme, read on
            // every draw so switching it in REI applies at once.
            widgets.add(Widgets.createDrawableWidget((GuiGraphics g, int mouseX, int mouseY, float delta) ->
                    GuiTheme.withDark(ConfigObject.getInstance().isUsingDarkTheme(), () -> {
                        if (id == CRUSHING) {
                            CrusherLayout.drawMechanism(g, x, y, CrusherLayout.WIDTH);
                            CrusherLayout.drawPreview(g, x, y, PREVIEW_MS);
                        } else {
                            ElectricFurnaceLayout.Variant variant = id == SMELTING ? ElectricFurnaceLayout.Variant.FURNACE
                                    : ElectricFurnaceLayout.Variant.ALLOY;
                            ElectricFurnaceLayout.drawElement(g, x + 1, y + 1, variant);
                            ElectricFurnaceLayout.previewHeat(g, x + 1, y + 1, variant, page.cycleMs());
                        }
                    })));
            if (id == CRUSHING) {
                slot(widgets, page.inputs().getFirst(), x + CrusherLayout.INPUT_X, y + CrusherLayout.INPUT_Y, false);
                slot(widgets, page.outputs().getFirst(), x + CrusherLayout.OUTPUT_X, y + CrusherLayout.OUTPUT_Y, true);
                return widgets;
            }
            ElectricFurnaceLayout.Variant variant = id == SMELTING ? ElectricFurnaceLayout.Variant.FURNACE : ElectricFurnaceLayout.Variant.ALLOY;
            int inputs = id == SMELTING ? 1 : 3;
            for (int i = 0; i < inputs; i++) {
                EntryIngredient input = i < page.inputs().size() ? page.inputs().get(i) : EntryIngredient.empty();
                slot(widgets, input, x + 1 + ElectricFurnaceLayout.INPUT_X[i], y + 1 + ElectricFurnaceLayout.INPUT_Y, false);
            }
            for (int i = 0; i < page.outputs().size(); i++) {
                slot(widgets, page.outputs().get(i), x + 1 + variant.outputX[i], y + 1 + ElectricFurnaceLayout.OUTPUT_Y, true);
            }
            return widgets;
        }
    }

    /** Slot whose 18 x 18 frame is at (x, y): the element already draws the frame, the item sits 1 px inside it. */
    private static void slot(List<Widget> widgets, EntryIngredient ingredient, int x, int y, boolean output) {
        var slot = Widgets.createSlot(new Rectangle(x, y, 18, 18)).entries(ingredient).disableBackground();
        widgets.add(output ? slot.markOutput() : slot.markInput());
    }
}
