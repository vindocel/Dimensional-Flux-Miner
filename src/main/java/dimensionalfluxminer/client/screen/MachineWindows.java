package dimensionalfluxminer.client.screen;

import dimensionalfluxminer.menu.WindowSlot;
import dimensionalfluxminer.registry.DfmItems;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Reusable windows: the upgrade window (holds the real upgrade slot) and read-only information screens. */
final class MachineWindows {
    private MachineWindows() {
    }

    /** Upgrade window: information screen on the left, the upgrade slot on the right. */
    static final class UpgradeWindow extends DfmScreen.Window {
        private final WindowSlot slot;

        UpgradeWindow(DfmScreen<?> screen, String key, WindowSlot slot) {
            super(screen, key, 156);
            this.slot = slot;
            slot.place(false, 0, 0);
        }

        @Override
        public Component title() {
            return Component.translatable("gui.dimensionalfluxminer.upgrades");
        }

        @Override
        public int height() {
            return 60;
        }

        @Override
        protected void render(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
            boolean installed = slot.getItem().is(DfmItems.EFFICIENCY_UPGRADE.get());
            screen.innerScreen(g, x + 6, y + 18, 120, 36);
            screen.screenLines(g, List.of(
                    Component.translatable("item.dimensionalfluxminer.efficiency_upgrade"),
                    Component.translatable(installed ? "gui.dimensionalfluxminer.upgrade.installed" : "gui.dimensionalfluxminer.upgrade.missing"),
                    Component.translatable("gui.dimensionalfluxminer.upgrade.count", installed ? 1 : 0, 1)),
                    x + 10, y + 22, 112, 10);
            screen.windowSlot(g, slot, DfmScreen.SlotKind.NORMAL, x + 131, y + 27, new ItemStack(DfmItems.EFFICIENCY_UPGRADE.get()), mouseX, mouseY);
        }

        @Override
        protected void onClose() {
            slot.place(false, 0, 0);
        }
    }

    /** Read-only window: lines of screen text. */
    static final class InfoWindow extends DfmScreen.Window {
        private final Component title;
        private final Supplier<List<Component>> lines;

        InfoWindow(DfmScreen<?> screen, String key, Component title, Supplier<List<Component>> lines) {
            super(screen, key, 176);
            this.title = title;
            this.lines = lines;
        }

        @Override
        public Component title() {
            return title;
        }

        @Override
        public int height() {
            return 18 + lines.get().size() * 10 + 14;
        }

        @Override
        protected void render(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
            List<Component> list = lines.get();
            screen.innerScreen(g, x + 6, y + 18, width - 12, list.size() * 10 + 8);
            screen.screenLines(g, list, x + 10, y + 23, width - 20, 10);
        }
    }
}
