package dimensionalfluxminer.client.screen;

import net.minecraft.client.renderer.RenderPipelines;
import com.mojang.blaze3d.systems.RenderSystem;
import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.compat.RecipeViewers;
import dimensionalfluxminer.menu.MachineMenu;
import dimensionalfluxminer.menu.WindowSlot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.platform.InputConstants;

/**
 * Base machine screen: textured panel, typed slots, side tabs (26 px holders with an 18 px button inside) and
 * floating windows that can be dragged by their top bar, closed with their close button or ESC and remember
 * their last position. Everything is drawn in immediate mode; clickable areas are registered as hotspots while
 * drawing and resolved against the topmost layer under the mouse.
 */
public abstract class DfmScreen<M extends MachineMenu<?>> extends AbstractContainerScreen<M> {
    public static final int TITLE = 0xFF404040;
    public static final int HEADING = 0xFF202020;
    public static final int SUBHEADING = 0xFF787878;
    public static final int SCREEN_TEXT = 0xFF52D9DB;
    public static final int SCREEN_WARN = 0xFFFFD04A;
    public static final int SCREEN_ERROR = 0xFFFF5A5A;
    public static final int BUTTON_TEXT = 0xFFFFFFFF;
    public static final int BUTTON_TEXT_INACTIVE = 0xFFA0A0A0;
    public static final int TAB_DEFAULT = 0xFFFFFFFF;
    public static final int TAB_REDSTONE = 0xFFC9071F;

    protected static final int TAB_SIZE = 26;
    protected static final int TAB_INNER = 18;
    /** Button of a left tab inside its ear (a right tab mirrors it). */
    private static final int TAB_BUTTON_LEFT = 5;
    private static final int TAB_BUTTON_TOP = 4;
    /** Material tabs of the side configuration windows (Items, Energy, Fluids...): a larger button in a wider ear. */
    static final int MATERIAL_TAB_INNER = 22;
    static final int MATERIAL_TAB_SIZE = MATERIAL_TAB_INNER + TAB_SIZE - TAB_INNER;
    /** Rows of the material tabs: the button plus a 4 px gap. */
    static final int MATERIAL_TAB_PITCH = MATERIAL_TAB_INNER + 4;
    protected static final int WINDOW_BAR = 18;
    /** Depth of the first window and between stacked windows (items and counts inside need ~350). */
    private static final int WINDOW_Z = 400;
    private static final int WINDOW_Z_STEP = 400;

    static final Identifier BUTTON_DISABLED = sprite("button/disabled");
    static final Identifier BUTTON_NORMAL = sprite("button/normal");
    static final Identifier BUTTON_HIGHLIGHTED = sprite("button/highlighted");
    /** Small buttons of a window's top bar (close, Auto Input, Auto Eject). */
    static final int SMALL_BUTTON = 10;
    /** Window close button, the size of the other top-bar buttons, with the close icon centered on it. */
    private static final int CLOSE_SIZE = SMALL_BUTTON;
    /** Window title area starts after the close button. */
    private static final int TITLE_START = 18;
    /** Energy bar frame (14 x 72); the shimmering fill sheet (42 frames of 8 x 66, one every 100 ms) is the icon color's. */
    static final Identifier ENERGY_EMPTY = DimensionalFluxMiner.id("textures/gui/energy/energy_empty.png");

    /** Text color of the terminal screens, following the icon color (light blue, or light purple with purple icons). */
    public static int screenText() {
        return GuiTheme.icons().screenText();
    }

    /** Energy fill of the icon color of the screen being drawn. */
    static Identifier energySheet() {
        return GuiTheme.icons().energySheet();
    }
    static final int ENERGY_FRAME_HEIGHT = 72;
    static final int ENERGY_FILL_HEIGHT = 66;
    static final int ENERGY_FRAMES = 42;
    static final long ENERGY_FRAME_MS = 100;
    static final Identifier CHECK = sprite("slot/overlay_check");

    static final GuiIcon ICON_SIDE_CONFIG = GuiIcon.SIDE_CONFIG;
    static final GuiIcon ICON_UPGRADE = GuiIcon.UPGRADE;
    static final GuiIcon ICON_REDSTONE_OFF = GuiIcon.REDSTONE_OFF;
    static final GuiIcon ICON_REDSTONE_ON = GuiIcon.REDSTONE_ON;
    static final GuiIcon ICON_ENERGY = GuiIcon.ENERGY;
    static final GuiIcon ICON_ITEMS = GuiIcon.ITEMS;
    static final GuiIcon ICON_FLUIDS = GuiIcon.FLUIDS;
    static final GuiIcon ICON_SORTING = GuiIcon.SORTING;
    static final GuiIcon ICON_STATS = GuiIcon.STATS;
    static final GuiIcon ICON_SMELTING = GuiIcon.SMELTING;
    static final GuiIcon ICON_SMELTING_ALLOY = GuiIcon.SMELTING_ALLOY;
    static final GuiIcon ICON_SMELTING_ALLOY_FURNACE = GuiIcon.SMELTING_ALLOY_FURNACE;
    static final GuiIcon ICON_SETTINGS = GuiIcon.SETTINGS;
    /** Strip color of each rate level, from level 1 (bottom) to level 10 (top). */
    private static final int[] RATE_COLORS = {
            0xFF65DEF5, 0xFF24ADC7, 0xFF0D798E,
            0xFF65F565, 0xFF24C724, 0xFF0D8E0D,
            0xFFF5657D, 0xFFC7243F, 0xFF8E0D22,
            0xFF6F0D8E};
    private static final int RATE_STRIP_WIDTH = 8;
    private static final long RATE_BLINK_MS = 500;
    static final GuiIcon ICON_REDSTONE_LOW = GuiIcon.REDSTONE_LOW;
    static final Identifier WARNING_SIGN = sprite("tab/warning_sign");

    /** Last position of each window (relative to the panel), kept while the game runs. */
    private static final Map<String, int[]> LAST_POSITIONS = new HashMap<>();

    private final List<Window> windows = new ArrayList<>();
    private final ThemeWindow themeWindow = new ThemeWindow(this);
    private final List<Hotspot> mainHotspots = new ArrayList<>();
    /** Machine items drawn this frame: they are not vanilla slots, so recipe viewers ask {@link #machineItemAt}. */
    private final List<ItemArea> itemAreas = new ArrayList<>();
    private Window drawing;
    private Window dragged;
    private double dragOffsetX;
    private double dragOffsetY;

    protected DfmScreen(M menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    protected DfmScreen(M menu, Inventory inventory, Component title, int width, int height) {
        super(menu, inventory, title, width, height);
    }

    static Identifier sprite(String path) {
        return DimensionalFluxMiner.id(path);
    }

    protected CompoundTag data() {
        return menu.data();
    }

    int left() {
        return leftPos;
    }

    int top() {
        return topPos;
    }

    int panelWidth() {
        return imageWidth;
    }

    int panelHeight() {
        return imageHeight;
    }

    /** Position of the machine this screen belongs to. */
    net.minecraft.core.BlockPos machinePos() {
        return menu.pos();
    }

    /** Settings tab (left column, row {@code row} from the top of the panel): opens the look of the screens. */
    void themeTab(GuiGraphicsExtractor g, int x, int y, int row, int mouseX, int mouseY) {
        windowTab(g, x - TAB_SIZE, y + row, true, TAB_DEFAULT, ICON_SETTINGS, themeWindow, mouseX, mouseY,
                () -> List.of(Component.translatable("gui.dimensionalfluxminer.theme")));
    }

    @Override
    protected void init() {
        super.init();
        titleLabelY = 6;
        inventoryLabelX = 8;
        inventoryLabelY = imageHeight - 94;
        for (Window w : windows) {
            clamp(w);
        }
    }

    // ------------------------------------------------------------------ main panel

    /** Draws the machine content (absolute coordinates). Panel, labels and player slots are already drawn. */
    protected abstract void renderMain(GuiGraphicsExtractor g, int x, int y, int mouseX, int mouseY);

    /** Top of the player inventory slots (menu coordinates). */
    protected abstract int inventoryTop();

    @Override
    public final void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        // The look applies to this screen only (set here, cleared once the screen is drawn): recipe viewers and
        // tooltips drawn after it stay as they are.
        GuiTheme.setDark(GuiThemes.dark(menu.pos()));
        GuiTheme.setIcons(GuiThemes.icons(menu.pos()));
        pendingAreas.clear();
        mainHotspots.clear();
        itemAreas.clear();
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int x = leftPos;
        int y = topPos;
        renderPanel(g, x, y);
        fitted(g, title, x, y + titleLabelY, imageWidth, GuiTheme.title(), 1.0F, true);
        g.text(font, playerInventoryTitle, x + inventoryLabelX, y + inventoryLabelY, GuiTheme.title(), false);
        renderMain(g, x, y, mouseX, mouseY);
        drawTabs(g);
    }

    /** Panel with its metal frame and player inventory slot frames; screens with their own artwork override it. */
    protected void renderPanel(GuiGraphicsExtractor g, int x, int y) {
        MetalFrame.panel(g, x, y, imageWidth, imageHeight);
        int top = inventoryTop();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                slot(g, SlotKind.NORMAL, x + 7 + col * 18, y + top - 1 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            slot(g, SlotKind.NORMAL, x + 7 + col * 18, y + top + 57);
        }
    }

    /** Title and inventory label are drawn with the panel. */
    @Override
    protected final void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
    }

    /** Windows go above the panel and its slot items; the carried item and tooltips are drawn above them. */
    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractContents(graphics, mouseX, mouseY, partialTick);
        renderWindows(graphics, mouseX, mouseY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        try {
            super.extractRenderState(graphics, mouseX, mouseY, partialTick);
            outsideAreas = List.copyOf(pendingAreas);
        } finally {
            GuiTheme.setDark(false);
            GuiTheme.setIcons(IconColor.WHITE);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!menu.getCarried().isEmpty()) {
            return;
        }
        Window top = windowAt(mouseX, mouseY);
        List<Hotspot> layer = top == null ? mainHotspots : top.hotspots;
        for (Hotspot hotspot : layer) {
            if (hotspot.contains(mouseX, mouseY) && hotspot.tooltip != null) {
                List<Component> lines = hotspot.tooltip.get();
                if (!lines.isEmpty()) {
                    graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
                    return;
                }
                break;
            }
        }
        super.extractTooltip(graphics, mouseX, mouseY);
    }

    // ------------------------------------------------------------------ windows

    /** A floating window. Coordinates passed to {@link #render} are absolute. */
    public abstract static class Window {
        protected final DfmScreen<?> screen;
        private final String key;
        protected final int width;
        private int relX;
        private int relY;
        /** Hotspots and slot positions of the last complete frame; the next frame builds the pending lists. */
        private List<Hotspot> hotspots = new ArrayList<>();
        private List<int[]> slotRects = new ArrayList<>();
        private List<Hotspot> pendingHotspots = new ArrayList<>();
        private List<int[]> pendingSlots = new ArrayList<>();

        protected Window(DfmScreen<?> screen, String key, int width) {
            this.screen = screen;
            this.key = key;
            this.width = width;
        }

        public abstract Component title();

        public abstract int height();

        protected int defaultX() {
            return (screen.panelWidth() - width) / 2;
        }

        protected int defaultY() {
            return 15;
        }

        /** Extra room kept free at the right of the title (buttons in the top bar). */
        protected int titlePadEnd() {
            return 0;
        }

        protected abstract void render(GuiGraphicsExtractor g, int x, int y, int mouseX, int mouseY);

        protected void onClose() {
        }

        /** A key pressed while this is the top window (Escape excepted); true consumes it. */
        protected boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            return false;
        }

        /** A character typed while this is the top window; true consumes it. */
        protected boolean charTyped(char c, int modifiers) {
            return false;
        }

        /** Any mouse press on the screen, before it is handled; {@code onThis} = the press lands on this window. */
        protected void mousePressed(double mouseX, double mouseY, boolean onThis) {
        }

        int x() {
            return screen.left() + relX;
        }

        int y() {
            return screen.top() + relY;
        }

        /** Inside the window body or on one of its elements drawn outside it (side tabs). */
        boolean contains(double mx, double my) {
            if (mx >= x() && my >= y() && mx < x() + width && my < y() + height()) {
                return true;
            }
            for (Hotspot hotspot : hotspots) {
                if (hotspot.contains(mx, my)) {
                    return true;
                }
            }
            return false;
        }

        boolean hasSlot(int slotX, int slotY) {
            for (int[] r : slotRects) {
                if (r[0] == slotX && r[1] == slotY) {
                    return true;
                }
            }
            return false;
        }
    }

    protected boolean isWindowOpen(Window w) {
        return windows.contains(w);
    }

    protected void openWindow(Window w) {
        if (windows.remove(w)) {
            windows.add(w);
            return;
        }
        int[] last = LAST_POSITIONS.get(w.key);
        w.relX = last != null ? last[0] : w.defaultX();
        w.relY = last != null ? last[1] : w.defaultY();
        windows.add(w);
        clamp(w);
    }

    protected void closeWindow(Window w) {
        if (windows.remove(w)) {
            LAST_POSITIONS.put(w.key, new int[] {w.relX, w.relY});
            if (dragged == w) {
                dragged = null;
            }
            w.onClose();
        }
    }

    private void clamp(Window w) {
        int ax = Mth.clamp(leftPos + w.relX, 0, Math.max(0, width - w.width));
        int ay = Mth.clamp(topPos + w.relY, 0, Math.max(0, height - w.height()));
        w.relX = ax - leftPos;
        w.relY = ay - topPos;
    }

    private Window windowAt(double mx, double my) {
        for (int i = windows.size() - 1; i >= 0; i--) {
            if (windows.get(i).contains(mx, my)) {
                return windows.get(i);
            }
        }
        return null;
    }

    private void renderWindows(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        for (int i = 0; i < windows.size(); i++) {
            Window w = windows.get(i);
            w.pendingHotspots = new ArrayList<>();
            w.pendingSlots = new ArrayList<>();
            int x = w.x();
            int y = w.y();
            int h = w.height();
            // Above the slot items and their counts of the panel and of the windows below.
            g.nextStratum();
            drawing = w;
            MetalFrame.panel(g, x, y, w.width, h);
            pendingAreas.add(new net.minecraft.client.renderer.Rect2i(x, y, w.width, h));
            closeButton(g, x + 6, y + 6, mouseX, mouseY, w);
            // The title uses the bar between the close button and the window's own top-right button, if any.
            int titleEnd = x + w.width - Math.max(6, w.titlePadEnd());
            scrollingText(g, w.title(), x + TITLE_START, y + 5, titleEnd - (x + TITLE_START), GuiTheme.title());
            w.render(g, x, y, mouseX, mouseY);
            drawTabs(g);
            w.hotspots = w.pendingHotspots;
            w.slotRects = w.pendingSlots;
            drawing = null;
        }
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        Window top = windowAt(mouseX, mouseY);
        for (Window w : List.copyOf(windows)) {
            w.mousePressed(mouseX, mouseY, w == top);
        }
        if (top != null) {
            windows.remove(top);
            windows.add(top);
            if (click(top.hotspots, mouseX, mouseY, button, event.hasShiftDown())) {
                return true;
            }
            if (hoveredSlot != null && top.hasSlot(hoveredSlot.x, hoveredSlot.y)) {
                return super.mouseClicked(event, doubleClick);
            }
            if (button == InputConstants.MOUSE_BUTTON_LEFT && mouseY < top.y() + WINDOW_BAR) {
                dragged = top;
                dragOffsetX = mouseX - top.x();
                dragOffsetY = mouseY - top.y();
            }
            return true;
        }
        if (click(mainHotspots, mouseX, mouseY, button, event.hasShiftDown())) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (dragged != null) {
            dragged.relX = (int) Math.round(event.x() - dragOffsetX) - leftPos;
            dragged.relY = (int) Math.round(event.y() - dragOffsetY) - topPos;
            clamp(dragged);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragged != null && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            dragged = null;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_ESCAPE && !windows.isEmpty()) {
            closeWindow(windows.getLast());
            return true;
        }
        if (!windows.isEmpty() && windows.getLast().keyPressed(event.key(), event.keycode(), event.modifiers())) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (!windows.isEmpty() && Character.isBmpCodePoint(event.codepoint())
                && windows.getLast().charTyped((char) event.codepoint(), 0)) {
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    protected boolean isHovering(int x, int y, int w, int h, double mouseX, double mouseY) {
        Window top = windowAt(mouseX, mouseY);
        if (top != null && !top.hasSlot(x, y)) {
            return false;
        }
        return super.isHovering(x, y, w, h, mouseX, mouseY);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop) {
        if (windowAt(mouseX, mouseY) != null) {
            return false;
        }
        for (Hotspot hotspot : mainHotspots) {
            if (hotspot.contains(mouseX, mouseY)) {
                return false;
            }
        }
        return super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop);
    }

    @Override
    public void removed() {
        for (Window w : List.copyOf(windows)) {
            closeWindow(w);
        }
        super.removed();
    }

    private boolean click(List<Hotspot> list, double mouseX, double mouseY, int button, boolean shift) {
        for (Hotspot hotspot : List.copyOf(list)) {
            if (hotspot.contains(mouseX, mouseY)) {
                boolean middle = button == InputConstants.MOUSE_BUTTON_MIDDLE;
                IntSupplier action = button == InputConstants.MOUSE_BUTTON_RIGHT ? hotspot.rightId
                        : button == InputConstants.MOUSE_BUTTON_LEFT ? hotspot.leftId : middle ? hotspot.middleId : null;
                if (action == null) {
                    return true;
                }
                if (hotspot.sound) {
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                }
                int id = action.getAsInt();
                if (id >= 0) {
                    if (!middle && shift && hotspot.shiftOffset > 0) {
                        id += hotspot.shiftOffset;
                    }
                    send(id);
                }
                return true;
            }
        }
        return false;
    }

    protected void send(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    /** True when the mouse is over the given area and no window above the current layer covers it. */
    protected boolean hovered(int x, int y, int w, int h, int mouseX, int mouseY) {
        return mouseX >= x && mouseY >= y && mouseX < x + w && mouseY < y + h && windowAt(mouseX, mouseY) == drawing;
    }

    protected Hotspot hotspot(int x, int y, int w, int h, IntSupplier left, IntSupplier right, Supplier<List<Component>> tooltip, int shiftOffset) {
        return hotspot(x, y, w, h, left, right, tooltip, shiftOffset, null);
    }

    /** A machine slot: {@code middle} is the creative pick (a full stack of the item to the cursor), as on vanilla slots. */
    protected Hotspot hotspot(int x, int y, int w, int h, IntSupplier left, IntSupplier right, Supplier<List<Component>> tooltip, int shiftOffset,
                              IntSupplier middle) {
        Hotspot spot = new Hotspot(x, y, w, h, left, right, tooltip, shiftOffset, false, middle);
        (drawing != null ? drawing.pendingHotspots : mainHotspots).add(spot);
        return spot;
    }

    /** Middle click id of a machine slot: only in creative, with an empty cursor, like the vanilla clone click. */
    protected IntSupplier creativePick(int id) {
        return () -> minecraft != null && minecraft.player != null && minecraft.player.hasInfiniteMaterials()
                && menu.getCarried().isEmpty() ? id : -1;
    }

    private void buttonHotspot(int x, int y, int w, int h, IntSupplier left, IntSupplier right, Supplier<List<Component>> tooltip) {
        Hotspot spot = new Hotspot(x, y, w, h, left, right, tooltip, 0, true, null);
        (drawing != null ? drawing.pendingHotspots : mainHotspots).add(spot);
    }

    // ------------------------------------------------------------------ elements (absolute coordinates)

    enum SlotKind {
        NORMAL("slot/normal"),
        OUTPUT("slot/output"),
        EXTRA("slot/extra"),
        OUTPUT_WARNING("slot/output_warning");

        final Identifier sprite;

        SlotKind(String path) {
            this.sprite = sprite(path);
        }
    }

    /** 18 x 18 slot frame; the item area starts at (x + 1, y + 1). */
    protected void slot(GuiGraphicsExtractor g, SlotKind kind, int x, int y) {
        GuiTheme.chrome(() -> g.blitSprite(RenderPipelines.GUI_TEXTURED, kind.sprite, x, y, 18, 18));
    }

    /** Draws a real container slot inside the window being drawn and keeps the menu slot under it. */
    protected void windowSlot(GuiGraphicsExtractor g, WindowSlot slot, SlotKind kind, int x, int y, ItemStack ghost, int mouseX, int mouseY) {
        int rx = x + 1 - leftPos;
        int ry = y + 1 - topPos;
        slot.place(true, rx, ry);
        if (drawing != null) {
            drawing.pendingSlots.add(new int[] {rx, ry});
        }
        slot(g, kind, x, y);
        ItemStack stack = slot.getItem();
        if (!stack.isEmpty()) {
            g.item(stack, x + 1, y + 1);
            g.itemDecorations(font, stack, x + 1, y + 1);
        } else if (!ghost.isEmpty()) {
            ghost(g, ghost, x + 1, y + 1);
        }
        if (hovered(x + 1, y + 1, 16, 16, mouseX, mouseY)) {
            g.fill(x + 1, y + 1, x + 17, y + 17, 0x80FFFFFF);
        }
    }

    /** Faded item hinting what an empty slot accepts. */
    protected void ghost(GuiGraphicsExtractor g, ItemStack stack, int x, int y) {
        g.fakeItem(stack, x, y);
        g.fill(x, y, x + 16, y + 16, GuiTheme.gray(0xA08B8B8B));
    }

    /** Item icon with an aggregate count (logical queue display). */
    protected void queueEntry(GuiGraphicsExtractor g, ItemStack prototype, long count, int x, int y) {
        if (prototype.isEmpty() || count <= 0) {
            return;
        }
        itemAreas.add(new ItemArea(new HoveredItem(prototype, x, y), drawing));
        g.item(prototype, x, y);
        g.itemDecorations(font, prototype, x, y, compact(count));
    }

    /** Read-out window (status, contents, automation): a glass viewport, see {@link GlassViewport}. */
    protected void innerScreen(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        GlassViewport.draw(g, x, y, w, h);
    }

    /** Lines of screen text (scale 0.8), one every {@code pitch} pixels starting at {@code y}. */
    protected void screenLines(GuiGraphicsExtractor g, List<Component> lines, int x, int y, int maxWidth, int pitch) {
        for (int i = 0; i < lines.size(); i++) {
            fitted(g, lines.get(i), x, y + i * pitch, maxWidth, screenText(), 0.8F, false);
        }
    }

    /**
     * Text scaled by {@code scale} (and further down if needed to fit {@code maxWidth}); centered in the width
     * when {@code center} is set.
     */
    protected void fitted(GuiGraphicsExtractor g, Component text, int x, int y, int maxWidth, int color, float scale, boolean center) {
        int width = font.width(text);
        float s = width * scale > maxWidth && width > 0 ? maxWidth / (float) width : scale;
        float drawn = width * s;
        float dx = center ? x + (maxWidth - drawn) / 2.0F : x;
        float dy = y + (font.lineHeight * (scale - s)) / 2.0F;
        g.pose().pushMatrix();
        g.pose().translate(dx, dy);
        g.pose().scale(s, s);
        g.text(font, text, 0, 0, color, false);
        g.pose().popMatrix();
    }

    protected void text(GuiGraphicsExtractor g, Component text, int x, int y, int color) {
        g.text(font, text, x, y, color, false);
    }

    /**
     * Text centered in {@code width}; when it does not fit, it slides from one side to the other (and back) inside
     * that width, so all of it can be read without shrinking or cutting it.
     */
    protected void scrollingText(GuiGraphicsExtractor g, Component text, int x, int y, int width, int color) {
        int textWidth = font.width(text);
        if (textWidth <= width) {
            g.text(font, text, x + (width - textWidth) / 2, y, color, false);
            return;
        }
        int overflow = textWidth - width;
        double seconds = Util.getMillis() / 1000.0;
        double period = Math.max(overflow * 0.5, 3.0);
        double phase = Math.sin(Math.PI / 2 * Math.cos(2 * Math.PI * seconds / period)) / 2 + 0.5;
        int offset = (int) Math.round(Mth.lerp(phase, 0, overflow));
        g.enableScissor(x, y - 1, x + width, y + font.lineHeight + 1);
        g.text(font, text, x - offset, y, color, false);
        g.disableScissor();
    }

    /** Close button of a window with the close icon centered on it. */
    private void closeButton(GuiGraphicsExtractor g, int x, int y, int mouseX, int mouseY, Window window) {
        boolean hover = hovered(x, y, CLOSE_SIZE, CLOSE_SIZE, mouseX, mouseY);
        buttonHotspot(x, y, CLOSE_SIZE, CLOSE_SIZE, () -> {
            closeWindow(window);
            return -1;
        }, null, null);
        ButtonArt.draw(g, x, y, CLOSE_SIZE, CLOSE_SIZE, true, hover, false);
        GuiIcon.CLOSE.drawCentered(g, x, y, CLOSE_SIZE, CLOSE_SIZE);
    }

    /**
     * Energy bar shared by every machine: 14 px wide frame of {@code height} px (at most 72) with a shimmering fill
     * (8 px, 3 px inside the frame) that grows from the bottom. Shorter bars keep the frame's top border and its
     * bottom-aligned part so the frame marks and the fill stay aligned.
     */
    protected void energyBar(GuiGraphicsExtractor g, int x, int y, int height, double fraction) {
        int h = Math.min(ENERGY_FRAME_HEIGHT, height);
        GuiTheme.chrome(() -> {
            g.blit(RenderPipelines.GUI_TEXTURED, ENERGY_EMPTY, x, y, 0, 0, 14, 3, 14, ENERGY_FRAME_HEIGHT);
            g.blit(RenderPipelines.GUI_TEXTURED, ENERGY_EMPTY, x, y + 3, 0, ENERGY_FRAME_HEIGHT - (h - 3), 14, h - 3, 14, ENERGY_FRAME_HEIGHT);
        });
        int area = h - 6;
        int filled = (int) Math.round(area * Mth.clamp(fraction, 0, 1));
        if (filled > 0) {
            int frame = (int) ((Util.getMillis() / ENERGY_FRAME_MS) % ENERGY_FRAMES);
            g.blit(RenderPipelines.GUI_TEXTURED, energySheet(), x + 3, y + 3 + area - filled, 0, frame * ENERGY_FILL_HEIGHT + ENERGY_FILL_HEIGHT - filled,
                    8, filled, 8, ENERGY_FILL_HEIGHT * ENERGY_FRAMES);
        }
    }


    /** Text button. Inactive buttons are drawn dark and ignore clicks. */
    protected void button(GuiGraphicsExtractor g, int x, int y, int w, int h, Component label, boolean active, int mouseX, int mouseY,
                          IntSupplier left, IntSupplier right, Supplier<List<Component>> tooltip) {
        toggleButton(g, x, y, w, h, label, active, false, mouseX, mouseY, left, right, tooltip);
    }

    /** Text button; {@code lit} keeps the highlighted background (toggle ON). */
    protected void toggleButton(GuiGraphicsExtractor g, int x, int y, int w, int h, Component label, boolean active, boolean lit, int mouseX,
                                int mouseY, IntSupplier left, IntSupplier right, Supplier<List<Component>> tooltip) {
        boolean hover = hovered(x, y, w, h, mouseX, mouseY);
        buttonHotspot(x, y, w, h, active ? left : null, active ? right : null, tooltip);
        ButtonArt.draw(g, x, y, w, h, active, hover, lit);
        int textWidth = font.width(label);
        float s = textWidth > w - 4 ? (w - 4) / (float) textWidth : 1.0F;
        g.pose().pushMatrix();
        g.pose().translate(x + (w - textWidth * s) / 2.0F, y + (h - 8 * s) / 2.0F);
        g.pose().scale(s, s);
        g.text(font, label, 0, 0, active ? BUTTON_TEXT : BUTTON_TEXT_INACTIVE, true);
        g.pose().popMatrix();
    }

    /**
     * Square button with an icon of {@code iconW} x {@code iconH} drawn at its own pixel size, centered. A
     * {@code pressed} button keeps the switched-on background and stays clickable: it is a push-on / push-off switch.
     */
    protected void iconButton(GuiGraphicsExtractor g, int x, int y, int size, GuiIcon icon, boolean pressed,
                              int mouseX, int mouseY, IntSupplier left, Supplier<List<Component>> tooltip) {
        boolean hover = hovered(x, y, size, size, mouseX, mouseY);
        buttonHotspot(x, y, size, size, left, null, tooltip);
        ButtonArt.draw(g, x, y, size, size, true, hover, pressed);
        icon.drawCentered(g, x, y, size, size);
    }

    /**
     * The machine energy bar laid on its side: {@code length} px long (frame middle and shimmering fill repeated, so the
     * pixels keep their size), 14 px tall, filling from the left.
     */
    protected void energyBarHorizontal(GuiGraphicsExtractor g, int x, int y, int length, double fraction) {
        int border = 3;
        int area = length - 2 * border;
        int filled = (int) Math.round(area * Mth.clamp(fraction, 0, 1));
        g.pose().pushMatrix();
        // The vertical bar turned a quarter: its bottom, where the fill starts, ends up on the left.
        g.pose().translate(x + length, y);
        g.pose().rotate((float) Math.toRadians(90));
        GuiTheme.chrome(() -> {
            g.blit(RenderPipelines.GUI_TEXTURED, ENERGY_EMPTY, 0, 0, 0, 0, 14, border, 14, ENERGY_FRAME_HEIGHT);
            for (int pos = border; pos < length - border; pos += ENERGY_FILL_HEIGHT) {
                int piece = Math.min(ENERGY_FILL_HEIGHT, length - border - pos);
                g.blit(RenderPipelines.GUI_TEXTURED, ENERGY_EMPTY, 0, pos, 0, border, 14, piece, 14, ENERGY_FRAME_HEIGHT);
            }
            g.blit(RenderPipelines.GUI_TEXTURED, ENERGY_EMPTY, 0, length - border, 0, ENERGY_FRAME_HEIGHT - border, 14, border, 14, ENERGY_FRAME_HEIGHT);
        });
        int frame = (int) ((Util.getMillis() / ENERGY_FRAME_MS) % ENERGY_FRAMES);
        for (int drawn = 0; drawn < filled; drawn += ENERGY_FILL_HEIGHT) {
            int piece = Math.min(ENERGY_FILL_HEIGHT, filled - drawn);
            g.blit(RenderPipelines.GUI_TEXTURED, energySheet(), border, border + area - drawn - piece, 0, frame * ENERGY_FILL_HEIGHT + ENERGY_FILL_HEIGHT - piece,
                    8, piece, 8, ENERGY_FILL_HEIGHT * ENERGY_FRAMES);
        }
        g.pose().popMatrix();
    }

    /** Square button with an icon covering it. {@code lit} keeps the highlighted background (toggle ON). */
    protected void imageButton(GuiGraphicsExtractor g, int x, int y, int size, GuiIcon icon, boolean active, boolean lit,
                               int mouseX, int mouseY, IntSupplier left, IntSupplier right, Supplier<List<Component>> tooltip) {
        boolean hover = hovered(x, y, size, size, mouseX, mouseY);
        buttonHotspot(x, y, size, size, active ? left : null, active ? right : null, tooltip);
        ButtonArt.draw(g, x, y, size, size, active, hover, lit);
        icon.drawCentered(g, x, y, size, size);
    }

    /** Square button whose light border is marked with {@code color} (null keeps the plain button). */
    protected void colorButton(GuiGraphicsExtractor g, int x, int y, int size, Integer color, boolean active, int mouseX, int mouseY,
                               IntSupplier left, IntSupplier right, Supplier<List<Component>> tooltip) {
        boolean hover = hovered(x, y, size, size, mouseX, mouseY);
        buttonHotspot(x, y, size, size, active ? left : null, active ? right : null, tooltip);
        if (color != null) {
            ButtonArt.drawMarked(g, x, y, size, size, active, hover, buttonTint(color));
        } else {
            ButtonArt.draw(g, x, y, size, size, active, hover, false);
        }
    }

    /** Button tint: the color with saturation -0.1 and value +0.1. */
    private static int buttonTint(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255.0F;
        float gr = ((rgb >> 8) & 0xFF) / 255.0F;
        float b = (rgb & 0xFF) / 255.0F;
        float max = Math.max(r, Math.max(gr, b));
        float min = Math.min(r, Math.min(gr, b));
        float delta = max - min;
        float hue;
        if (delta == 0) {
            hue = 0;
        } else if (max == r) {
            hue = ((gr - b) / delta) % 6;
        } else if (max == gr) {
            hue = (b - r) / delta + 2;
        } else {
            hue = (r - gr) / delta + 4;
        }
        hue /= 6.0F;
        if (hue < 0) {
            hue += 1;
        }
        float sat = max == 0 ? 0 : delta / max;
        return Mth.hsvToRgb(hue, Math.max(0, sat - 0.1F), Math.min(1, max + 0.1F));
    }

    /**
     * Side tab: a 26 x 26 metal ear of the panel frame (tinted with {@code color}) on the left or right edge with
     * an 18 x 18 button inside. A disabled tab (its window already open) is drawn dark and ignores clicks.
     */
    protected void tab(GuiGraphicsExtractor g, int x, int y, boolean leftSide, int color, GuiIcon icon, boolean enabled,
                       int mouseX, int mouseY, IntSupplier left, IntSupplier right, Supplier<List<Component>> tooltip) {
        iconTab(g, x, y, leftSide, color, icon, TAB_INNER, enabled, false, mouseX, mouseY, left, right, tooltip);
    }

    /**
     * Tab with an icon of {@code iconSize} centered on the button. {@code lit} keeps the highlighted background
     * (toggle ON).
     */
    protected void iconTab(GuiGraphicsExtractor g, int x, int y, boolean leftSide, int color, GuiIcon icon, int iconSize, boolean enabled,
                           boolean lit, int mouseX, int mouseY, IntSupplier left, IntSupplier right, Supplier<List<Component>> tooltip) {
        iconTab(g, x, y, leftSide, color, icon, iconSize, enabled, lit, false, mouseX, mouseY, left, right, tooltip);
    }

    /** Same as above; {@code warning} draws the holder with hazard stripes (the tab is related to a problem). */
    protected void iconTab(GuiGraphicsExtractor g, int x, int y, boolean leftSide, int color, GuiIcon icon, int iconSize, boolean enabled,
                           boolean lit, boolean warning, int mouseX, int mouseY, IntSupplier left, IntSupplier right,
                           Supplier<List<Component>> tooltip) {
        int[] inner = tabFrame(g, x, y, TAB_INNER, leftSide, color, enabled, lit, warning, mouseX, mouseY, left, right, tooltip);
        int offset = (TAB_INNER - iconSize) / 2;
        tabContents.add(() -> icon.drawCentered(g, inner[0] + offset, inner[1] + offset, iconSize, iconSize));
    }

    /**
     * Material tab of a side configuration window ({@link #MATERIAL_TAB_SIZE} wide, button of
     * {@link #MATERIAL_TAB_INNER}); its icon keeps its own pixel size, centered, and no color ring (the icon tells the
     * material). A disabled tab is the open one.
     */
    void materialTab(GuiGraphicsExtractor g, int x, int y, boolean leftSide, GuiIcon icon, boolean enabled, int mouseX, int mouseY,
                     IntSupplier left, Supplier<List<Component>> tooltip) {
        int[] inner = tabFrame(g, x, y, MATERIAL_TAB_INNER, leftSide, TAB_DEFAULT, enabled, false, false, mouseX, mouseY, left, null, tooltip);
        tabContents.add(() -> icon.drawCentered(g, inner[0], inner[1], MATERIAL_TAB_INNER, MATERIAL_TAB_INNER));
    }

    /** Problems tab (26 x 26, left side): lists what keeps the machine from running. */
    protected void warningTab(GuiGraphicsExtractor g, int x, int y, List<Component> problems) {
        ears.add(new Ear(x, y, TAB_SIZE, TAB_SIZE, true));
        // Square glass where the other tabs have their button, with the yellow warning sign (no hazard ring: that one
        // marks the Redstone tab).
        int bx = x + TAB_BUTTON_LEFT;
        int by = y + TAB_BUTTON_TOP;
        tabContents.add(() -> {
            GlassViewport.button(g, bx, by, TAB_INNER);
            g.blitSprite(RenderPipelines.GUI_TEXTURED, WARNING_SIGN, bx + (TAB_INNER - 10) / 2, by + (TAB_INNER - 10) / 2, 10, 10);
        });
        hotspot(x, y, TAB_SIZE, TAB_SIZE, null, null, () -> {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("gui.dimensionalfluxminer.problems").withStyle(net.minecraft.ChatFormatting.YELLOW));
            for (Component problem : problems) {
                lines.add(Component.literal(" - ").append(problem));
            }
            return lines;
        }, 0);
    }

    /**
     * Holder and button background of a tab; returns the button's top-left corner. The holder is only noted here and
     * the button drawn later ({@link #drawTabs}), so tabs stacked next to each other share one holder.
     */
    private int[] tabFrame(GuiGraphicsExtractor g, int x, int y, int button, boolean leftSide, int color, boolean enabled, boolean lit,
                           boolean warning, int mouseX, int mouseY, IntSupplier left, IntSupplier right, Supplier<List<Component>> tooltip) {
        int size = button + TAB_SIZE - TAB_INNER;
        ears.add(new Ear(x, y, size, size, leftSide));
        // Centered on the metal between the outer bevel and the panel's inner line.
        int bx = x + (leftSide ? TAB_BUTTON_LEFT : size - TAB_BUTTON_LEFT - button);
        int by = y + TAB_BUTTON_TOP;
        boolean hover = hovered(bx, by, button, button, mouseX, mouseY);
        buttonHotspot(bx, by, button, button, enabled ? left : null, enabled ? right : null, tooltip);
        tabContents.add(() -> {
            // The tab's color (or the hazard dashes of a related problem) lights a ring around the button.
            if (warning || color != TAB_DEFAULT) {
                MetalFrame.ring(g, bx, by, button, color, warning);
            }
            ButtonArt.draw(g, bx, by, button, button, enabled, hover, lit);
        });
        return new int[] {bx, by};
    }

    /** Areas drawn outside the panel in the last frame: tab holders and open windows (screen coordinates). */
    private List<net.minecraft.client.renderer.Rect2i> outsideAreas = List.of();
    private final List<net.minecraft.client.renderer.Rect2i> pendingAreas = new ArrayList<>();

    /**
     * What this screen draws outside its panel (side tabs and the open windows with their tabs), so the recipe viewers
     * (JEI, EMI, REI) move their lists out of the way, also while a window is dragged.
     */
    public List<net.minecraft.client.renderer.Rect2i> outsideAreas() {
        return outsideAreas;
    }

    /** A tab holder noted while drawing: {@code width} wide and {@code height} tall, on the left or right edge. */
    private record Ear(int x, int y, int width, int height, boolean left) {
    }

    private final List<Ear> ears = new ArrayList<>();
    /** What the tabs draw on their holders (buttons, icons), in order, once the holders are down. */
    private final List<Runnable> tabContents = new ArrayList<>();

    /**
     * Draws the tab holders noted since the last call, then their buttons. Holders on the same edge that touch (the
     * usual 2 px between stacked tabs) are joined into one block holding all their buttons.
     */
    private void drawTabs(GuiGraphicsExtractor g) {
        List<Ear> sorted = new ArrayList<>(ears);
        sorted.sort(java.util.Comparator.comparing(Ear::left).thenComparingInt(Ear::x).thenComparingInt(Ear::y));
        Ear block = null;
        for (Ear ear : sorted) {
            if (block != null && block.left() == ear.left() && block.x() == ear.x() && block.width() == ear.width()
                    && ear.y() <= block.y() + block.height() + 2) {
                block = new Ear(block.x(), block.y(), block.width(), Math.max(block.height(), ear.y() + ear.height() - block.y()), block.left());
                continue;
            }
            if (block != null) {
                drawEar(g, block);
            }
            block = ear;
        }
        if (block != null) {
            drawEar(g, block);
        }
        for (Runnable content : tabContents) {
            content.run();
        }
        ears.clear();
        tabContents.clear();
    }

    private void drawEar(GuiGraphicsExtractor g, Ear block) {
        MetalFrame.ear(g, block.x(), block.y(), block.width(), block.height(), block.left());
        pendingAreas.add(new net.minecraft.client.renderer.Rect2i(block.x(), block.y(), block.width(), block.height()));
    }

    /** Tab that opens {@code window}; disabled while the window is open. */
    protected void windowTab(GuiGraphicsExtractor g, int x, int y, boolean leftSide, int color, GuiIcon icon, Window window,
                             int mouseX, int mouseY, Supplier<List<Component>> tooltip) {
        tab(g, x, y, leftSide, color, icon, !isWindowOpen(window), mouseX, mouseY, () -> {
            openWindow(window);
            return -1;
        }, null, tooltip);
    }

    /** Vanilla-style highlight of a machine slot (frame at x, y) under the mouse, drawn above its item. */
    protected void slotHover(GuiGraphicsExtractor g, int x, int y, int mouseX, int mouseY) {
        if (hovered(x + 1, y + 1, 16, 16, mouseX, mouseY)) {
            g.fill(x + 1, y + 1, x + 17, y + 17, 0x80FFFFFF);
        }
    }

    /**
     * Clickable progress area: opens the given recipe categories in the installed recipe viewer (EMI or JEI). Does
     * nothing when no viewer is installed.
     */
    protected void recipeArea(int x, int y, int w, int h, Supplier<Identifier[]> categories, Supplier<List<Component>> tooltip) {
        if (!RecipeViewers.available()) {
            hotspot(x, y, w, h, null, null, tooltip, 0);
            return;
        }
        hotspot(x, y, w, h, () -> {
            RecipeViewers.show(categories.get());
            return -1;
        }, null, () -> {
            List<Component> lines = new ArrayList<>(tooltip.get());
            lines.add(Component.translatable("gui.dimensionalfluxminer.show_recipes").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
            return lines;
        }, 0);
    }

    /**
     * Rate selector tab: one colored strip per level stacked from the bottom, the strip of the current level blinking.
     * Left click next level (10 wraps to 1), right click previous.
     */
    protected void rateTab(GuiGraphicsExtractor g, int x, int y, boolean leftSide, int rate, int nextId, int previousId, int mouseX, int mouseY,
                           Supplier<List<Component>> details) {
        int[] inner = tabFrame(g, x, y, TAB_INNER, leftSide, TAB_DEFAULT, true, false, false, mouseX, mouseY, () -> nextId, () -> previousId, () -> {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("gui.dimensionalfluxminer.rate.tooltip", rate));
            lines.addAll(details.get());
            lines.add(Component.translatable("gui.dimensionalfluxminer.rate.cycle").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
            return lines;
        });
        int left = inner[0] + (TAB_INNER - RATE_STRIP_WIDTH) / 2;
        int bottom = inner[1] + (TAB_INNER + RATE_COLORS.length) / 2;
        boolean blinkOn = (Util.getMillis() / RATE_BLINK_MS) % 2 == 0;
        int shown = Mth.clamp(rate, 0, RATE_COLORS.length);
        tabContents.add(() -> {
            for (int level = 1; level <= shown; level++) {
                if (level < shown || blinkOn) {
                    g.fill(left, bottom - level, left + RATE_STRIP_WIDTH, bottom - level + 1, RATE_COLORS[level - 1]);
                }
            }
        });
    }

    // ------------------------------------------------------------------ formatting

    public static String compact(long value) {
        if (value < 1000) {
            return Long.toString(value);
        }
        if (value < 1_000_000) {
            return trim(value / 1000.0) + "k";
        }
        if (value < 1_000_000_000L) {
            return trim(value / 1_000_000.0) + "M";
        }
        return trim(value / 1_000_000_000.0) + "G";
    }

    private static String trim(double v) {
        return v >= 100 ? String.format(Locale.ROOT, "%.0f", v) : String.format(Locale.ROOT, "%.1f", v);
    }

    public static String fe(long value) {
        return compact(value) + " FE";
    }

    public static String number(double value) {
        if (value >= 100) {
            return String.format(Locale.ROOT, "%.0f", value);
        }
        if (value >= 1) {
            return String.format(Locale.ROOT, "%.2f", value);
        }
        return String.format(Locale.ROOT, "%.4f", value);
    }

    /** A machine item shown on screen (16 x 16 at x, y). */
    public record HoveredItem(ItemStack stack, int x, int y) {
    }

    private record ItemArea(HoveredItem item, Window owner) {
    }

    /**
     * The machine item under the mouse, unless a window covers it: lets EMI / JEI show its recipes and uses (R / U)
     * as they do on vanilla slots.
     */
    public java.util.Optional<HoveredItem> machineItemAt(double mouseX, double mouseY) {
        Window top = windowAt(mouseX, mouseY);
        for (int i = itemAreas.size() - 1; i >= 0; i--) {
            ItemArea area = itemAreas.get(i);
            HoveredItem item = area.item();
            if (area.owner() == top && mouseX >= item.x() && mouseY >= item.y() && mouseX < item.x() + 16 && mouseY < item.y() + 16) {
                return java.util.Optional.of(item);
            }
        }
        return java.util.Optional.empty();
    }

    public record Hotspot(int x, int y, int w, int h, IntSupplier leftId, IntSupplier rightId,
                          Supplier<List<Component>> tooltip, int shiftOffset, boolean sound, IntSupplier middleId) {
        public boolean contains(double mx, double my) {
            return mx >= x && my >= y && mx < x + w && my < y + h;
        }
    }
}
