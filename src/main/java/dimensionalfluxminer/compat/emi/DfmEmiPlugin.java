package dimensionalfluxminer.compat.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiStackInteraction;
import dimensionalfluxminer.client.screen.DfmScreen;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.client.screen.CrusherLayout;
import dimensionalfluxminer.client.screen.ElectricFurnaceLayout;
//? if <26.1 {
/*import dimensionalfluxminer.client.screen.FluidGauge;
import dimensionalfluxminer.client.screen.TankRecipeLayout;
*///?}
import dimensionalfluxminer.compat.RecipeViewerData;
import dimensionalfluxminer.crusher.CrusherProcessing;
import dimensionalfluxminer.recipe.AlloyingRecipe;
import dimensionalfluxminer.registry.DfmBlocks;
import java.util.List;
import net.minecraft.client.Minecraft;
//? if >=26.1 {
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * EMI integration: Crushing, Electric Furnace furnace mode and Alloying. The Electric Furnace recipes use the machine's own
 * processing element (inputs, pipes, heat chamber, outputs).
 */
@EmiEntrypoint
public class DfmEmiPlugin implements EmiPlugin {
    static final EmiStack CRUSHER = EmiStack.of(DfmBlocks.MECHANICAL_CRUSHER.get());
    static final EmiStack ELECTRIC_FURNACE = EmiStack.of(DfmBlocks.ELECTRIC_FURNACE.get());
    static final EmiRecipeCategory CRUSHING = new EmiRecipeCategory(DimensionalFluxMiner.id("crushing"), CRUSHER);
    static final EmiRecipeCategory SMELTING = new EmiRecipeCategory(DimensionalFluxMiner.id("smelting"), ELECTRIC_FURNACE);
    static final EmiRecipeCategory ALLOYING = new EmiRecipeCategory(DimensionalFluxMiner.id("alloying"), ELECTRIC_FURNACE);
//?} else {
/*import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/^*
 * EMI integration: Crushing, Electric Furnace furnace mode, Alloying and the container conversions of the Dimensional
 * Tank. The Electric Furnace recipes use the machine's own processing element (inputs, pipes, heat chamber, outputs); the
 * tank pages use the narrow liquid gauge of the other recipe viewers.
 ^/
@EmiEntrypoint
public class DfmEmiPlugin implements EmiPlugin {
    static final EmiStack CRUSHER = EmiStack.of(DfmBlocks.MECHANICAL_CRUSHER.get());
    static final EmiStack ELECTRIC_FURNACE = EmiStack.of(DfmBlocks.ELECTRIC_FURNACE.get());
    static final EmiRecipeCategory CRUSHING = new EmiRecipeCategory(DimensionalFluxMiner.id("crushing"), CRUSHER);
    static final EmiRecipeCategory SMELTING = new EmiRecipeCategory(DimensionalFluxMiner.id("smelting"), ELECTRIC_FURNACE);
    static final EmiRecipeCategory ALLOYING = new EmiRecipeCategory(DimensionalFluxMiner.id("alloying"), ELECTRIC_FURNACE);
    static final EmiStack TANK = EmiStack.of(DfmBlocks.DIMENSIONAL_TANK.get());
    static final EmiRecipeCategory TANK_CONTAINER = new EmiRecipeCategory(DimensionalFluxMiner.id("tank_container"), TANK);
*///?}
    /** The element plus a 1 px margin (the alloy element is drawn 1 px around the slot area). */
    private static final int ELEMENT_W = ElectricFurnaceLayout.WIDTH + 2;
    private static final int ELEMENT_H = ElectricFurnaceLayout.HEIGHT + 2;
    /** Crushing preview: one operation every 4 s. */
    private static final long PREVIEW_MS = 4000;

    @Override
    public void register(EmiRegistry registry) {
        // Creative tanks holding different liquids are different entries.
        registry.setDefaultComparison(EmiStack.of(dimensionalfluxminer.registry.DfmItems.CREATIVE_DIMENSIONAL_TANK.get()),
                dev.emi.emi.api.stack.Comparison.compareComponents());
        // Machine items are drawn by the screen, not in vanilla slots: R / U work on them through this provider. It is
        // generic because EMI matches class providers on the exact screen class (each machine has its own subclass).
        // Not clickable: mouse clicks stay with the screen, which takes and places the items.
        registry.addGenericStackProvider((screen, x, y) -> screen instanceof DfmScreen<?> machine
                ? machine.machineItemAt(x, y).map(item -> new EmiStackInteraction(EmiStack.of(item.stack()), null, false))
                        .orElse(EmiStackInteraction.EMPTY)
                : EmiStackInteraction.EMPTY);
        // The side tabs and the open windows (also while dragged) are kept free of EMI's lists.
        registry.addGenericExclusionArea((screen, consumer) -> {
            if (screen instanceof DfmScreen<?> machine) {
                for (var area : machine.outsideAreas()) {
                    consumer.accept(new dev.emi.emi.api.widget.Bounds(area.getX(), area.getY(), area.getWidth(), area.getHeight()));
                }
            }
        });
        registry.addCategory(CRUSHING);
        registry.addCategory(SMELTING);
        registry.addCategory(ALLOYING);
        registry.addWorkstation(CRUSHING, CRUSHER);
        registry.addWorkstation(SMELTING, ELECTRIC_FURNACE);
        registry.addWorkstation(ALLOYING, ELECTRIC_FURNACE);
        //? if <26.1 {
        /*registry.addCategory(TANK_CONTAINER);
        registry.addWorkstation(TANK_CONTAINER, TANK);
        for (var holder : registry.getRecipeManager().getAllRecipesFor(dimensionalfluxminer.registry.DfmRecipes.TANK_CONTAINER.get())) {
            registry.addRecipe(new TankContainer(holder.id(), holder.value()));
        }
        *///?}

        List<CrusherProcessing.Display> crushing = RecipeViewerData.crushing();
        for (int i = 0; i < crushing.size(); i++) {
            registry.addRecipe(new Crushing(DimensionalFluxMiner.id("/crushing/" + i), crushing.get(i)));
        }
        if (Minecraft.getInstance().level != null) {
            for (RecipeViewerData.FurnaceDisplay display : RecipeViewerData.electricFurnaceSmelting(registry.getRecipeManager(),
                    Minecraft.getInstance().level.registryAccess())) {
                registry.addRecipe(new Furnace(display));
            }
        }
        for (RecipeHolder<AlloyingRecipe> holder : RecipeViewerData.alloying(registry.getRecipeManager())) {
            registry.addRecipe(new Alloying(holder.id(), holder.value()));
        }
    }

    private static final class Crushing extends BasicEmiRecipe {
        private final CrusherProcessing.Display display;

        //? if >=26.1 {
        Crushing(Identifier id, CrusherProcessing.Display display) {
        //?} else {
        /*Crushing(ResourceLocation id, CrusherProcessing.Display display) {
        *///?}
            super(CRUSHING, id, CrusherLayout.WIDTH, CrusherLayout.HEIGHT);
            this.display = display;
            inputs.add(EmiIngredient.of(display.inputs().stream().map(EmiStack::of).toList()));
            outputs.add(EmiStack.of(display.output()));
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            widgets.addDrawable(0, 0, CrusherLayout.WIDTH, CrusherLayout.HEIGHT, (g, mouseX, mouseY, delta) -> {
                CrusherLayout.drawMechanism(g, 0, 0, CrusherLayout.WIDTH);
                CrusherLayout.drawPreview(g, 0, 0, PREVIEW_MS);
            });
            widgets.addSlot(inputs.getFirst(), CrusherLayout.INPUT_X, CrusherLayout.INPUT_Y).drawBack(false);
            var output = widgets.addSlot(outputs.getFirst(), CrusherLayout.OUTPUT_X, CrusherLayout.OUTPUT_Y)
                    .drawBack(false).recipeContext(this);
            if (display.upgradedOutput().getCount() != display.output().getCount()) {
                output.appendTooltip(RecipeViewerData.upgradeLine(display));
            }
        }
    }

    /** Furnace mode: one lane of the furnace element. */
    private static final class Furnace extends BasicEmiRecipe {
        private final RecipeViewerData.FurnaceDisplay display;

        Furnace(RecipeViewerData.FurnaceDisplay display) {
            //? if >=26.1 {
            super(SMELTING, Identifier.fromNamespaceAndPath(DimensionalFluxMiner.MOD_ID,
            //?} else {
            /*super(SMELTING, ResourceLocation.fromNamespaceAndPath(DimensionalFluxMiner.MOD_ID,
            *///?}
                    "/smelting/" + display.id().getNamespace() + "/" + display.id().getPath()), ELEMENT_W, ELEMENT_H);
            this.display = display;
            inputs.add(EmiIngredient.of(display.input(), display.inputCount()));
            outputs.add(EmiStack.of(display.output()));
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            widgets.addDrawable(0, 0, ELEMENT_W, ELEMENT_H, (g, mouseX, mouseY, delta) -> {
                ElectricFurnaceLayout.drawElement(g, 1, 1, ElectricFurnaceLayout.Variant.FURNACE);
                ElectricFurnaceLayout.previewHeat(g, 1, 1, ElectricFurnaceLayout.Variant.FURNACE, display.cookTicks() * 50L);
            });
            widgets.addSlot(inputs.getFirst(), 1 + ElectricFurnaceLayout.INPUT_X[0], 1 + ElectricFurnaceLayout.INPUT_Y).drawBack(false);
            var output = widgets.addSlot(outputs.getFirst(), 1 + ElectricFurnaceLayout.Variant.FURNACE.outputX[0], 1 + ElectricFurnaceLayout.OUTPUT_Y)
                    .drawBack(false).recipeContext(this);
            if (display.bulk()) {
                output.appendTooltip(RecipeViewerData.bulkLine());
            }
        }
    }

    //? if <26.1 {
    /*/^* A container converted in a single tank slot: the liquid it takes or gives in the narrow gauge, the item in the middle. ^/
    private static final class TankContainer extends BasicEmiRecipe {
        private final boolean filling;
        private final EmiIngredient fluid;

        TankContainer(ResourceLocation id, dimensionalfluxminer.recipe.TankContainerRecipe recipe) {
            super(TANK_CONTAINER, id, TankRecipeLayout.WIDTH, TankRecipeLayout.HEIGHT);
            this.filling = recipe.filling();
            this.fluid = EmiIngredient.of(recipe.fluids().stream()
                    .map(stack -> EmiStack.of(stack.getFluid(), stack.getComponentsPatch(), stack.getAmount())).toList());
            inputs.add(EmiIngredient.of(recipe.ingredient()));
            outputs.add(EmiStack.of(recipe.result()));
            if (filling) {
                inputs.add(fluid);
            } else {
                fluid.getEmiStacks().forEach(outputs::add);
            }
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            widgets.addDrawable(0, 0, TankRecipeLayout.WIDTH, TankRecipeLayout.HEIGHT,
                    (g, mouseX, mouseY, delta) -> TankRecipeLayout.drawEmptySideAndArrow(g, 0, 0, filling));
            widgets.addSlot(inputs.getFirst(), TankRecipeLayout.INPUT_ITEM_X - 1, TankRecipeLayout.ITEM_Y - 1);
            widgets.addSlot(outputs.getFirst(), TankRecipeLayout.OUTPUT_ITEM_X - 1, TankRecipeLayout.ITEM_Y - 1).recipeContext(this);
            FluidSlot slot = widgets.add(new FluidSlot(fluid, TankRecipeLayout.fluidX(filling), TankRecipeLayout.FLUID_Y));
            if (!filling) {
                slot.recipeContext(this);
            }
        }
    }

    /^* Fluid slot shown as the narrow liquid gauge (18 x 64), cycling through the liquids of the recipe's tag. ^/
    private static final class FluidSlot extends dev.emi.emi.api.widget.SlotWidget {
        FluidSlot(EmiIngredient stack, int x, int y) {
            super(stack, x, y);
            drawBack(false);
        }

        @Override
        public dev.emi.emi.api.widget.Bounds getBounds() {
            return new dev.emi.emi.api.widget.Bounds(x, y, FluidGauge.NARROW_WIDTH, FluidGauge.HEIGHT);
        }

        @Override
        public void drawStack(net.minecraft.client.gui.GuiGraphics g, int mouseX, int mouseY, float delta) {
            List<EmiStack> stacks = getStack().getEmiStacks();
            if (stacks.isEmpty()) {
                return;
            }
            EmiStack shown = stacks.get((int) (System.currentTimeMillis() / 1000 % stacks.size()));
            if (shown.getKey() instanceof net.minecraft.world.level.material.Fluid fluid) {
                var stack = new net.neoforged.neoforge.fluids.FluidStack(fluid.builtInRegistryHolder(), (int) shown.getAmount(),
                        shown.getComponentChanges());
                FluidGauge.drawNarrow(g, x, y, stack, stack.getAmount());
            }
        }
    }

    *///?}
    /** Alloy mode: the three inputs feed one chamber; main output and byproduct below. */
    private static final class Alloying extends BasicEmiRecipe {
        private final long cycleMs;

        //? if >=26.1 {
        Alloying(Identifier id, AlloyingRecipe recipe) {
        //?} else {
        /*Alloying(ResourceLocation id, AlloyingRecipe recipe) {
        *///?}
            super(ALLOYING, id, ELEMENT_W, ELEMENT_H);
            this.cycleMs = recipe.processingTicks() * 50L;
            for (SizedIngredient sized : recipe.ingredients()) {
                inputs.add(EmiIngredient.of(sized.ingredient(), sized.count()));
            }
            outputs.add(EmiStack.of(recipe.result()));
            recipe.byproduct().ifPresent(b -> outputs.add(EmiStack.of(b.stack()).setChance(b.chance())));
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            widgets.addDrawable(0, 0, ELEMENT_W, ELEMENT_H, (g, mouseX, mouseY, delta) -> {
                ElectricFurnaceLayout.drawElement(g, 1, 1, ElectricFurnaceLayout.Variant.ALLOY);
                ElectricFurnaceLayout.previewHeat(g, 1, 1, ElectricFurnaceLayout.Variant.ALLOY, cycleMs);
            });
            for (int i = 0; i < 3; i++) {
                EmiIngredient input = i < inputs.size() ? inputs.get(i) : EmiStack.EMPTY;
                widgets.addSlot(input, 1 + ElectricFurnaceLayout.INPUT_X[i], 1 + ElectricFurnaceLayout.INPUT_Y).drawBack(false);
            }
            for (int i = 0; i < outputs.size(); i++) {
                // The byproduct stack carries its chance; EMI shows it on the slot.
                widgets.addSlot(outputs.get(i), 1 + ElectricFurnaceLayout.Variant.ALLOY.outputX[i], 1 + ElectricFurnaceLayout.OUTPUT_Y)
                        .drawBack(false).recipeContext(this);
            }
        }
    }
}
