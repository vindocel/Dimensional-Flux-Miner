package dimensionalfluxminer.compat.polymorph;

import com.illusivesoulworks.polymorph.api.client.PolymorphWidgets;
import com.illusivesoulworks.polymorph.api.client.base.AbstractRecipesWidget;
import com.illusivesoulworks.polymorph.api.client.base.PersistentRecipesWidget;
import dimensionalfluxminer.client.screen.CrusherScreen;
import dimensionalfluxminer.client.screen.ElectricFurnaceScreen;
import dimensionalfluxminer.client.screen.GuiTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;

/** Client side of the Polymorph integration: the recipe button on the Crusher and Electric Furnace screens. */
public final class PolymorphClientCompat {
    private PolymorphClientCompat() {
    }

    public static void register() {
        PolymorphWidgets.getInstance().registerWidget(screen -> {
            if (screen instanceof CrusherScreen) {
                return new MachineRecipesWidget(screen, CrusherScreen.RECIPE_BUTTON_X, CrusherScreen.RECIPE_BUTTON_Y);
            }
            if (screen instanceof ElectricFurnaceScreen) {
                return new MachineRecipesWidget(screen, ElectricFurnaceScreen.RECIPE_BUTTON_X, ElectricFurnaceScreen.RECIPE_BUTTON_Y);
            }
            return null;
        });
    }

    /**
     * The machine items are drawn by the screen, not held in slots: the widget is placed by a slot that only marks
     * where the button goes (the button sits {@link AbstractRecipesWidget#BUTTON_Y_OFFSET} above it).
     */
    private static final class MachineRecipesWidget extends PersistentRecipesWidget {
        private final Slot anchor;

        MachineRecipesWidget(AbstractContainerScreen<?> screen, int buttonX, int buttonY) {
            super(screen);
            anchor = new Slot(new SimpleContainer(1), 0, buttonX - AbstractRecipesWidget.BUTTON_X_OFFSET,
                    buttonY - AbstractRecipesWidget.BUTTON_Y_OFFSET);
        }

        @Override
        public Slot getOutputSlot() {
            return anchor;
        }

        /** The button and its list take the light or dark look of the machine screen. */
        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            GuiTheme.drawOnMachineScreen(containerScreen, () -> super.render(graphics, mouseX, mouseY, partialTick));
        }
    }
}
