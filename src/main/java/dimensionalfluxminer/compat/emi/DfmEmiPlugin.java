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
import dimensionalfluxminer.client.screen.SmelterLayout;
import dimensionalfluxminer.compat.RecipeViewerData;
import dimensionalfluxminer.crusher.CrusherProcessing;
import dimensionalfluxminer.recipe.AlloyingRecipe;
import dimensionalfluxminer.registry.DfmBlocks;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * EMI integration: Crushing, Alloy Smelter furnace mode and Alloying. The Alloy Smelter recipes use the machine's own
 * processing element (inputs, pipes, heat chamber, outputs).
 */
@EmiEntrypoint
public class DfmEmiPlugin implements EmiPlugin {
    static final EmiStack CRUSHER = EmiStack.of(DfmBlocks.DIMENSIONAL_CRUSHER.get());
    static final EmiStack SMELTER = EmiStack.of(DfmBlocks.ALLOY_SMELTER.get());
    static final EmiRecipeCategory CRUSHING = new EmiRecipeCategory(DimensionalFluxMiner.id("crushing"), CRUSHER);
    static final EmiRecipeCategory SMELTING = new EmiRecipeCategory(DimensionalFluxMiner.id("smelting"), SMELTER);
    static final EmiRecipeCategory ALLOYING = new EmiRecipeCategory(DimensionalFluxMiner.id("alloying"), SMELTER);
    /** The element plus a 1 px margin (the alloy element is drawn 1 px around the slot area). */
    private static final int ELEMENT_W = SmelterLayout.WIDTH + 2;
    private static final int ELEMENT_H = SmelterLayout.HEIGHT + 2;
    /** Crushing preview: one operation every 4 s. */
    private static final long PREVIEW_MS = 4000;

    @Override
    public void register(EmiRegistry registry) {
        // Machine items are drawn by the screen, not in vanilla slots: R / U work on them through this provider. It is
        // generic because EMI matches class providers on the exact screen class (each machine has its own subclass).
        // Not clickable: mouse clicks stay with the screen, which takes and places the items.
        registry.addGenericStackProvider((screen, x, y) -> screen instanceof DfmScreen<?> machine
                ? machine.machineItemAt(x, y).map(item -> new EmiStackInteraction(EmiStack.of(item.stack()), null, false))
                        .orElse(EmiStackInteraction.EMPTY)
                : EmiStackInteraction.EMPTY);
        registry.addCategory(CRUSHING);
        registry.addCategory(SMELTING);
        registry.addCategory(ALLOYING);
        registry.addWorkstation(CRUSHING, CRUSHER);
        registry.addWorkstation(SMELTING, SMELTER);
        registry.addWorkstation(ALLOYING, SMELTER);

        List<CrusherProcessing.Display> crushing = RecipeViewerData.crushing();
        for (int i = 0; i < crushing.size(); i++) {
            registry.addRecipe(new Crushing(DimensionalFluxMiner.id("/crushing/" + i), crushing.get(i)));
        }
        if (Minecraft.getInstance().level != null) {
            for (RecipeViewerData.FurnaceDisplay display : RecipeViewerData.smelterFurnace(registry.getRecipeManager(),
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

        Crushing(ResourceLocation id, CrusherProcessing.Display display) {
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
            super(SMELTING, ResourceLocation.fromNamespaceAndPath(DimensionalFluxMiner.MOD_ID,
                    "/smelting/" + display.id().getNamespace() + "/" + display.id().getPath()), ELEMENT_W, ELEMENT_H);
            this.display = display;
            inputs.add(EmiIngredient.of(display.input(), display.inputCount()));
            outputs.add(EmiStack.of(display.output()));
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            widgets.addDrawable(0, 0, ELEMENT_W, ELEMENT_H, (g, mouseX, mouseY, delta) -> {
                SmelterLayout.drawElement(g, 1, 1, SmelterLayout.Variant.FURNACE);
                SmelterLayout.previewHeat(g, 1, 1, SmelterLayout.Variant.FURNACE, display.cookTicks() * 50L);
            });
            widgets.addSlot(inputs.getFirst(), 1 + SmelterLayout.INPUT_X[0], 1 + SmelterLayout.INPUT_Y).drawBack(false);
            var output = widgets.addSlot(outputs.getFirst(), 1 + SmelterLayout.Variant.FURNACE.outputX[0], 1 + SmelterLayout.OUTPUT_Y)
                    .drawBack(false).recipeContext(this);
            if (display.bulk()) {
                output.appendTooltip(RecipeViewerData.bulkLine());
            }
        }
    }

    /** Alloy mode: the three inputs feed one chamber; main output and byproduct below. */
    private static final class Alloying extends BasicEmiRecipe {
        private final long cycleMs;

        Alloying(ResourceLocation id, AlloyingRecipe recipe) {
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
                SmelterLayout.drawElement(g, 1, 1, SmelterLayout.Variant.ALLOY);
                SmelterLayout.previewHeat(g, 1, 1, SmelterLayout.Variant.ALLOY, cycleMs);
            });
            for (int i = 0; i < 3; i++) {
                EmiIngredient input = i < inputs.size() ? inputs.get(i) : EmiStack.EMPTY;
                widgets.addSlot(input, 1 + SmelterLayout.INPUT_X[i], 1 + SmelterLayout.INPUT_Y).drawBack(false);
            }
            for (int i = 0; i < outputs.size(); i++) {
                // The byproduct stack carries its chance; EMI shows it on the slot.
                widgets.addSlot(outputs.get(i), 1 + SmelterLayout.Variant.ALLOY.outputX[i], 1 + SmelterLayout.OUTPUT_Y)
                        .drawBack(false).recipeContext(this);
            }
        }
    }
}
