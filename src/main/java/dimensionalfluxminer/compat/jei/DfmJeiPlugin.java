package dimensionalfluxminer.compat.jei;

import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.client.screen.CrusherLayout;
import dimensionalfluxminer.client.screen.ElectricFurnaceLayout;
import dimensionalfluxminer.client.screen.FluidGauge;
import dimensionalfluxminer.client.screen.TankRecipeLayout;
import dimensionalfluxminer.compat.RecipeViewerData;
import dimensionalfluxminer.compat.RecipeViewers;
import dimensionalfluxminer.crusher.CrusherProcessing;
import dimensionalfluxminer.recipe.AlloyingRecipe;
import dimensionalfluxminer.registry.DfmBlocks;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import dimensionalfluxminer.client.screen.DfmScreen;
import java.util.Optional;
import mezz.jei.api.gui.builder.IClickableIngredientFactory;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.runtime.IClickableIngredient;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}
import net.minecraft.network.chat.Component;
//? if >=26.1 {
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.resources.ResourceLocation;
*///?}
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * JEI integration: Crushing, Electric Furnace furnace mode and Alloying (the Electric Furnace recipes use the machine's
 * own processing element). Skipped when EMI or REI is installed, which have their own plugins.
 */
@JeiPlugin
public class DfmJeiPlugin implements IModPlugin {
    static final RecipeType<CrusherProcessing.Display> CRUSHING = RecipeType.create(DimensionalFluxMiner.MOD_ID, "crushing",
            CrusherProcessing.Display.class);
    static final RecipeType<RecipeViewerData.FurnaceDisplay> SMELTING = RecipeType.create(DimensionalFluxMiner.MOD_ID, "smelting",
            RecipeViewerData.FurnaceDisplay.class);
    @SuppressWarnings({"unchecked", "rawtypes"})
    static final RecipeType<RecipeHolder<AlloyingRecipe>> ALLOYING = RecipeType.create(DimensionalFluxMiner.MOD_ID, "alloying",
            (Class) RecipeHolder.class);
    static final RecipeType<dimensionalfluxminer.recipe.TankContainerRecipe> TANK_CONTAINER = RecipeType.create(
            DimensionalFluxMiner.MOD_ID, "tank_container", dimensionalfluxminer.recipe.TankContainerRecipe.class);
    /** The element plus a 1 px margin (the alloy element is drawn 1 px around the slot area). */
    private static final int ELEMENT_W = ElectricFurnaceLayout.WIDTH + 2;
    private static final int ELEMENT_H = ElectricFurnaceLayout.HEIGHT + 2;

    //? if <26.3 {
    private static boolean enabled() {
        return !RecipeViewers.emi() && !RecipeViewers.rei();
    //?} else {
    /*/^* EMI and REI have no 26.x build: JEI always lists the recipes on this line. ^/
    private static boolean enabled() {
        return true;
    *///?}
    }

    @Override
    //? if >=26.1 {
    public Identifier getPluginUid() {
    //?} else {
    /*public ResourceLocation getPluginUid() {
    *///?}
        return DimensionalFluxMiner.id("jei_plugin");
    }

    /** Creative tanks holding different liquids are different entries. */
    @Override
    public void registerItemSubtypes(mezz.jei.api.registration.ISubtypeRegistration registration) {
        //? if >=26.1 {
        registration.registerFromDataComponentTypes(dimensionalfluxminer.registry.DfmItems.CREATIVE_DIMENSIONAL_TANK.get(),
                dimensionalfluxminer.registry.DfmDataComponents.CREATIVE_TANK_FLUID.get());
        //?} else {
        /*registration.registerSubtypeInterpreter(dimensionalfluxminer.registry.DfmItems.CREATIVE_DIMENSIONAL_TANK.get(),
                new mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter<net.minecraft.world.item.ItemStack>() {
                    @Override
                    public Object getSubtypeData(net.minecraft.world.item.ItemStack stack,
                            mezz.jei.api.ingredients.subtypes.UidContext context) {
                        return stack.get(dimensionalfluxminer.registry.DfmDataComponents.CREATIVE_TANK_FLUID.get());
                    }

                    @Override
                    public String getLegacyStringSubtypeInfo(net.minecraft.world.item.ItemStack stack,
                            mezz.jei.api.ingredients.subtypes.UidContext context) {
                        var content = stack.get(dimensionalfluxminer.registry.DfmDataComponents.CREATIVE_TANK_FLUID.get());
                        return content == null || content.isEmpty() ? ""
                                : net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(content.getFluid()).toString();
                    }
                });
        *///?}
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        JeiBridge.setRuntime(runtime);
    }

    @Override
    public void onRuntimeUnavailable() {
        JeiBridge.setRuntime(null);
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        if (!enabled()) {
            return;
        }
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new CrushingCategory(gui), new FurnaceCategory(gui), new AlloyingCategory(gui), new TankContainerCategory(gui));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (!enabled() || level == null) {
            return;
        }
        //? if >=26.1 {
        registration.addRecipes(TANK_CONTAINER, dimensionalfluxminer.recipe.Recipes.byType(level,
        //?} else {
        /*registration.addRecipes(TANK_CONTAINER, level.getRecipeManager().getAllRecipesFor(
        *///?}
                dimensionalfluxminer.registry.DfmRecipes.TANK_CONTAINER.get()).stream().map(RecipeHolder::value).toList());
        registration.addRecipes(CRUSHING, RecipeViewerData.crushing());
        //? if >=26.1 {
        registration.addRecipes(SMELTING, RecipeViewerData.electricFurnaceSmelting(dimensionalfluxminer.recipe.Recipes.map(level), level.registryAccess()));
        registration.addRecipes(ALLOYING, RecipeViewerData.alloying(dimensionalfluxminer.recipe.Recipes.map(level)));
        //?} else {
        /*registration.addRecipes(SMELTING, RecipeViewerData.electricFurnaceSmelting(level.getRecipeManager(), level.registryAccess()));
        registration.addRecipes(ALLOYING, RecipeViewerData.alloying(level.getRecipeManager()));
        *///?}
    }

    /**
     * Machine items are drawn by the screen, not in vanilla slots: R / U work on them through this handler. The side tabs
     * and the open windows (also while dragged) are kept free of JEI's lists.
     */
    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        if (!enabled()) {
            return;
        }
        registration.addGenericGuiContainerHandler(DfmScreen.class, new IGuiContainerHandler<DfmScreen<?>>() {
            @Override
            public Optional<? extends IClickableIngredient<?>> getClickableIngredientUnderMouse(IClickableIngredientFactory factory,
                                                                                              DfmScreen<?> screen, double mouseX, double mouseY) {
                return screen.machineItemAt(mouseX, mouseY)
                        .flatMap(item -> factory.createBuilder(item.stack()).buildWithArea(item.x(), item.y(), 16, 16));
            }

            @Override
            public List<net.minecraft.client.renderer.Rect2i> getGuiExtraAreas(DfmScreen<?> screen) {
                return screen.outsideAreas();
            }
        });
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        if (!enabled()) {
            return;
        }
        registration.addRecipeCatalyst(DfmBlocks.DIMENSIONAL_TANK.get(), TANK_CONTAINER);
        registration.addRecipeCatalyst(DfmBlocks.MECHANICAL_CRUSHER.get(), CRUSHING);
        registration.addRecipeCatalyst(DfmBlocks.ELECTRIC_FURNACE.get(), SMELTING, ALLOYING);
    }

    private static final class TankContainerCategory implements IRecipeCategory<dimensionalfluxminer.recipe.TankContainerRecipe> {
        private final IDrawable icon;

        private TankContainerCategory(IGuiHelper gui) {
            icon = gui.createDrawableItemStack(DfmBlocks.DIMENSIONAL_TANK.get().asItem().getDefaultInstance());
        }

        @Override
        public RecipeType<dimensionalfluxminer.recipe.TankContainerRecipe> getRecipeType() {
            return TANK_CONTAINER;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("emi.category.dimensionalfluxminer.tank_container");
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public int getWidth() {
            return TankRecipeLayout.WIDTH;
        }

        @Override
        public int getHeight() {
            return TankRecipeLayout.HEIGHT;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, dimensionalfluxminer.recipe.TankContainerRecipe recipe, IFocusGroup focuses) {
            builder.addInputSlot(TankRecipeLayout.INPUT_ITEM_X, TankRecipeLayout.ITEM_Y).setStandardSlotBackground().addIngredients(recipe.ingredient());
            builder.addOutputSlot(TankRecipeLayout.OUTPUT_ITEM_X, TankRecipeLayout.ITEM_Y).setStandardSlotBackground().addItemStack(recipe.result());
            var fluidSlot = builder.addSlot(recipe.filling() ? mezz.jei.api.recipe.RecipeIngredientRole.INPUT : mezz.jei.api.recipe.RecipeIngredientRole.OUTPUT,
                    TankRecipeLayout.fluidX(recipe.filling()), TankRecipeLayout.FLUID_Y)
                    .setCustomRenderer(mezz.jei.api.neoforge.NeoForgeTypes.FLUID_STACK, new TankFluidRenderer());
            for (var fluid : recipe.fluids()) {
                fluidSlot.addFluidStack(fluid.getFluid(), fluid.getAmount(), fluid.getComponentsPatch());
            }
        }

        @Override
        //? if >=26.1 {
        public void draw(dimensionalfluxminer.recipe.TankContainerRecipe recipe, IRecipeSlotsView slots, GuiGraphicsExtractor g,
        //?} else {
        /*public void draw(dimensionalfluxminer.recipe.TankContainerRecipe recipe, IRecipeSlotsView slots, net.minecraft.client.gui.GuiGraphics g,
        *///?}
                         double mouseX, double mouseY) {
            TankRecipeLayout.drawEmptySideAndArrow(g, 0, 0, recipe.filling());
        }
    }

    private static final class TankFluidRenderer implements mezz.jei.api.ingredients.IIngredientRenderer<net.neoforged.neoforge.fluids.FluidStack> {
        @Override
        //? if >=26.1 {
        public void render(GuiGraphicsExtractor g, net.neoforged.neoforge.fluids.FluidStack fluid) {
        //?} else {
        /*public void render(net.minecraft.client.gui.GuiGraphics g, net.neoforged.neoforge.fluids.FluidStack fluid) {
        *///?}
            FluidGauge.drawNarrow(g, 0, 0, fluid, fluid.getAmount());
        }

        @Override
        public List<Component> getTooltip(net.neoforged.neoforge.fluids.FluidStack fluid, net.minecraft.world.item.TooltipFlag flag) {
            return List.of(fluid.getHoverName(), Component.literal(fluid.getAmount() + " mB"));
        }

        @Override
        public int getWidth() {
            return FluidGauge.NARROW_WIDTH;
        }

        @Override
        public int getHeight() {
            return FluidGauge.HEIGHT;
        }
    }

    private static final class CrushingCategory implements IRecipeCategory<CrusherProcessing.Display> {
        private final IDrawable icon;

        CrushingCategory(IGuiHelper gui) {
            this.icon = gui.createDrawableItemLike(DfmBlocks.MECHANICAL_CRUSHER.get());
        }

        @Override
        public RecipeType<CrusherProcessing.Display> getRecipeType() {
            return CRUSHING;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("emi.category.dimensionalfluxminer.crushing");
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public int getWidth() {
            return CrusherLayout.WIDTH;
        }

        @Override
        public int getHeight() {
            return CrusherLayout.HEIGHT;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, CrusherProcessing.Display recipe, IFocusGroup focuses) {
            builder.addInputSlot(CrusherLayout.INPUT_X + 1, CrusherLayout.INPUT_Y + 1).addItemStacks(recipe.inputs());
            var output = builder.addOutputSlot(CrusherLayout.OUTPUT_X + 1, CrusherLayout.OUTPUT_Y + 1).addItemStack(recipe.output());
            if (recipe.upgradedOutput().getCount() != recipe.output().getCount()) {
                output.addRichTooltipCallback((view, tooltip) -> tooltip.add(RecipeViewerData.upgradeLine(recipe)));
            }
        }

        @Override
        //? if >=26.1 {
        public void draw(CrusherProcessing.Display recipe, IRecipeSlotsView view, GuiGraphicsExtractor g, double mouseX, double mouseY) {
        //?} else {
        /*public void draw(CrusherProcessing.Display recipe, IRecipeSlotsView view, GuiGraphics g, double mouseX, double mouseY) {
        *///?}
            CrusherLayout.drawMechanism(g, 0, 0, CrusherLayout.WIDTH);
            CrusherLayout.drawPreview(g, 0, 0, 4000);
        }
    }

    /** Furnace mode: one lane of the furnace element. */
    private static final class FurnaceCategory implements IRecipeCategory<RecipeViewerData.FurnaceDisplay> {
        private final IDrawable icon;

        FurnaceCategory(IGuiHelper gui) {
            this.icon = gui.createDrawableItemLike(DfmBlocks.ELECTRIC_FURNACE.get());
        }

        @Override
        public RecipeType<RecipeViewerData.FurnaceDisplay> getRecipeType() {
            return SMELTING;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("emi.category.dimensionalfluxminer.smelting");
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public int getWidth() {
            return ELEMENT_W;
        }

        @Override
        public int getHeight() {
            return ELEMENT_H;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, RecipeViewerData.FurnaceDisplay recipe, IFocusGroup focuses) {
            builder.addInputSlot(2 + ElectricFurnaceLayout.INPUT_X[0], 2 + ElectricFurnaceLayout.INPUT_Y)
                    //? if >=26.1 {
                    .addItemStacks(List.of(dimensionalfluxminer.recipe.Recipes.stacks(recipe.input())).stream().map(s -> s.copyWithCount(recipe.inputCount())).toList());
                    //?} else {
                    /*.addItemStacks(List.of(recipe.input().getItems()).stream().map(s -> s.copyWithCount(recipe.inputCount())).toList());
                    *///?}
            var output = builder.addOutputSlot(2 + ElectricFurnaceLayout.Variant.FURNACE.outputX[0], 2 + ElectricFurnaceLayout.OUTPUT_Y)
                    .addItemStack(recipe.output());
            if (recipe.bulk()) {
                output.addRichTooltipCallback((view, tooltip) -> tooltip.add(RecipeViewerData.bulkLine()));
            }
        }

        @Override
        //? if >=26.1 {
        public void draw(RecipeViewerData.FurnaceDisplay recipe, IRecipeSlotsView view, GuiGraphicsExtractor g, double mouseX, double mouseY) {
        //?} else {
        /*public void draw(RecipeViewerData.FurnaceDisplay recipe, IRecipeSlotsView view, GuiGraphics g, double mouseX, double mouseY) {
        *///?}
            ElectricFurnaceLayout.drawElement(g, 1, 1, ElectricFurnaceLayout.Variant.FURNACE);
            ElectricFurnaceLayout.previewHeat(g, 1, 1, ElectricFurnaceLayout.Variant.FURNACE, recipe.cookTicks() * 50L);
        }

        @Override
        //? if >=26.1 {
        public Identifier getRegistryName(RecipeViewerData.FurnaceDisplay recipe) {
        //?} else {
        /*public ResourceLocation getRegistryName(RecipeViewerData.FurnaceDisplay recipe) {
        *///?}
            return recipe.id();
        }
    }

    /** Alloy mode: the three inputs feed one chamber; main output and byproduct below. */
    private static final class AlloyingCategory implements IRecipeCategory<RecipeHolder<AlloyingRecipe>> {
        private final IDrawable icon;

        AlloyingCategory(IGuiHelper gui) {
            this.icon = gui.createDrawableItemLike(DfmBlocks.ELECTRIC_FURNACE.get());
        }

        @Override
        public RecipeType<RecipeHolder<AlloyingRecipe>> getRecipeType() {
            return ALLOYING;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("emi.category.dimensionalfluxminer.alloying");
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public int getWidth() {
            return ELEMENT_W;
        }

        @Override
        public int getHeight() {
            return ELEMENT_H;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<AlloyingRecipe> holder, IFocusGroup focuses) {
            AlloyingRecipe recipe = holder.value();
            List<SizedIngredient> ingredients = recipe.ingredients();
            for (int i = 0; i < ingredients.size() && i < 3; i++) {
                builder.addInputSlot(2 + ElectricFurnaceLayout.INPUT_X[i], 2 + ElectricFurnaceLayout.INPUT_Y)
                        //? if >=26.1 {
                        .addItemStacks(List.of(dimensionalfluxminer.recipe.Recipes.stacks(ingredients.get(i).ingredient())));
                        //?} else {
                        /*.addItemStacks(List.of(ingredients.get(i).getItems()));
                        *///?}
            }
            builder.addOutputSlot(2 + ElectricFurnaceLayout.Variant.ALLOY.outputX[0], 2 + ElectricFurnaceLayout.OUTPUT_Y).addItemStack(recipe.result());
            recipe.byproduct().ifPresent(b -> builder.addOutputSlot(2 + ElectricFurnaceLayout.Variant.ALLOY.outputX[1], 2 + ElectricFurnaceLayout.OUTPUT_Y)
                    .addItemStack(b.stack())
                    .addRichTooltipCallback((view, tooltip) -> tooltip.add(RecipeViewerData.chanceLine(b.chance()))));
        }

        @Override
        //? if >=26.1 {
        public void draw(RecipeHolder<AlloyingRecipe> recipe, IRecipeSlotsView view, GuiGraphicsExtractor g, double mouseX, double mouseY) {
        //?} else {
        /*public void draw(RecipeHolder<AlloyingRecipe> recipe, IRecipeSlotsView view, GuiGraphics g, double mouseX, double mouseY) {
        *///?}
            ElectricFurnaceLayout.drawElement(g, 1, 1, ElectricFurnaceLayout.Variant.ALLOY);
            ElectricFurnaceLayout.previewHeat(g, 1, 1, ElectricFurnaceLayout.Variant.ALLOY, recipe.value().processingTicks() * 50L);
        }

        @Override
        //? if >=26.1 {
        public Identifier getRegistryName(RecipeHolder<AlloyingRecipe> recipe) {
            return recipe.id().identifier();
        //?} else {
        /*public ResourceLocation getRegistryName(RecipeHolder<AlloyingRecipe> recipe) {
            return recipe.id();
        *///?}
        }
    }
}
