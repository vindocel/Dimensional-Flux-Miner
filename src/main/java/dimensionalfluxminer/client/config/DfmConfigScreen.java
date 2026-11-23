package dimensionalfluxminer.client.config;

import dimensionalfluxminer.DfmConfig;
import dimensionalfluxminer.DimensionalFluxMiner;

import dimensionalfluxminer.client.screen.GuiThemes;


//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Full-screen, draft-based editor. Remote server rules and scripted options are read-only. */
public final class DfmConfigScreen extends Screen {
    public static void register(net.neoforged.fml.ModContainer container) {
        container.registerExtensionPoint(net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,
                (net.neoforged.neoforge.client.gui.IConfigScreenFactory) (mod, parent) -> new DfmConfigScreen(parent));
    }
    private record Option(String key, int tab, boolean client, boolean appearance, Object fallback) {}
    private record Row(Option option, int y, AbstractWidget control, Button reset) {}
    private final Screen parent;
    private final List<Option> options = new ArrayList<>();
    private final Map<String, Object> opened = new LinkedHashMap<>(), draft = new LinkedHashMap<>();
    private final Map<String, String> errors = new LinkedHashMap<>();
    private final List<Row> rows = new ArrayList<>();
    private int tab, scroll, panelX, panelWidth, listWidth, contentTop, visibleRows, controlWidth, barRight;
    private String inspected;
    private boolean rebuild, saving;
    private Component saveError = Component.empty();
    private Button done;

    public DfmConfigScreen(Screen parent) {
        super(text("title"));
        this.parent = parent;
        ConfigSettings.clientOptions().forEach((key, value) -> options.add(new Option(key, 0, true, false, value.getDefault())));
        ConfigSettings.appearanceDefaults().forEach((key, fallback) -> options.add(new Option(key, 0, true, true, fallback)));
        ConfigSettings.serverOptions().forEach((key, value) -> options.add(new Option(key,
                key.startsWith("miner.") ? 1 : key.startsWith("crusher.") || key.startsWith("electric_furnace.") ? 2 : 3,
                false, false, value.getDefault())));
        opened.putAll(ConfigSettings.fileValues(true));
        opened.putAll(ConfigSettings.effectiveServerValues());
        opened.putAll(GuiThemes.globalPreferences());
        draft.putAll(opened);
        ConfigPreview.reset();
    }

    static Component text(String key, Object... args) {
        return Component.translatable("config.dimensionalfluxminer." + key, args);
    }

    private Component name(Option option) { return text("option." + option.key); }
    private boolean scripted(Option option) { return !option.client && DfmConfig.scriptedKeys().contains(option.key); }
    private boolean editable(Option option) {
        return !saving && !scripted(option) && (option.appearance || option.client && ConfigSettings.isLoaded(true)
                || !option.client && minecraft.getSingleplayerServer() != null && ConfigSettings.isLoaded(false));
    }

    @Override
    protected void init() {
        rows.clear();
        panelWidth = Math.clamp((width - 18) * 36 / 100, 110, 224);
        panelX = width - 6 - panelWidth;
        listWidth = panelX - 12;
        contentTop = 30;
        visibleRows = Math.max(1, (height - contentTop - 6) / ConfigStyle.ROW);
        barRight = 6 + listWidth - ConfigStyle.RESET - 12;
        controlWidth = Math.clamp((barRight - 6) * 42 / 100, 52, 110);
        String[] tabs = {"client", "miner", "machines", "world"};
        int tabWidth = (width - 12 - 6) / 4;
        for (int i = 0; i < 4; i++) {
            int selected = i;
            int x = 6 + i * (tabWidth + 2), w = i == 3 ? width - 6 - x : tabWidth;
            addRenderableWidget(ConfigStyle.button(text("tab." + tabs[i]), x, 6, w, 20,
                    button -> { tab = selected; scroll = 0; inspected = null; rebuild = true; }, () -> tab == selected));
        }
        List<Option> current = options.stream().filter(option -> option.tab == tab).toList();
        scroll = Math.clamp(scroll, 0, Math.max(0, current.size() - visibleRows));
        for (int i = scroll; i < Math.min(current.size(), scroll + visibleRows); i++) {
            Option option = current.get(i);
            int y = contentTop + (i - scroll) * ConfigStyle.ROW;
            AbstractWidget control;
            if (option.key.equals("appearance.icons") || option.key.equals("corruption.claimFailurePolicy")) {
                List<String> choices = option.appearance ? List.of("blue", "purple", "custom") : List.of("deny", "ignore");
                control = ConfigStyle.button(text("choice." + draft.get(option.key)), barRight - controlWidth - 6, y + 1,
                        controlWidth, 20, button -> {
                            String next = choices.get((choices.indexOf(draft.get(option.key)) + 1) % choices.size());
                            draft.put(option.key, next); rebuild = true;
                        });
            } else if (option.fallback instanceof Boolean) {
                control = new ConfigToggle(barRight - 34, y + 4, name(option), () -> Boolean.TRUE.equals(draft.get(option.key)),
                        () -> draft.put(option.key, !Boolean.TRUE.equals(draft.get(option.key))));
            } else if (option.fallback instanceof List<?>) {
                control = ConfigStyle.button(text("edit_list", ((List<?>) draft.get(option.key)).size()), barRight - controlWidth - 6,
                        y + 1, controlWidth, 20, button -> ConfigSettings.screen(minecraft, new ConfigListScreen(this, name(option),
                                option.key, (List<?>) draft.get(option.key), values -> {
                                    draft.put(option.key, values); rebuild = true;
                                })));
            } else {
                EditBox field = new EditBox(font, barRight - controlWidth - 6, y + 2, controlWidth, 18, name(option));
                field.setMaxLength(option.fallback instanceof String
                        ? Math.max(256, String.valueOf(draft.get(option.key)).length()) : 32);
                field.setValue(String.valueOf(draft.get(option.key)));
                field.setResponder(raw -> {
                    draft.put(option.key, raw);
                    try { ConfigSettings.checkedValue(option.key, raw, option.client); errors.remove(option.key); }
                    catch (IllegalArgumentException error) { errors.put(option.key, error.getMessage()); }
                });
                control = field;
            }
            control.active = editable(option);
            if (scripted(option)) control.setTooltip(Tooltip.create(text("scripted")));
            else if (!editable(option) && !option.client) control.setTooltip(Tooltip.create(text("server_read_only")));
            addRenderableWidget(control);
            Button reset = addRenderableWidget(ConfigStyle.button(Component.literal("↺"), barRight + 4, y + 1, 20, 20,
                    button -> { draft.put(option.key, option.fallback); errors.remove(option.key); rebuild = true; }));
            reset.setTooltip(Tooltip.create(text("reset_option")));
            rows.add(new Row(option, y, control, reset));
        }
        int footerX = panelX + 8, footerWidth = panelWidth - 16, half = (footerWidth - 4) / 2;
        Button defaults = addRenderableWidget(ConfigStyle.button(text("defaults"), footerX, height - 52, half, 20,
                button -> restoreCategory(false)));
        defaults.setTooltip(Tooltip.create(text("category_actions")));
        Button undo = addRenderableWidget(ConfigStyle.button(text("undo"), footerX + half + 4, height - 52,
                footerWidth - half - 4, 20, button -> restoreCategory(true)));
        undo.setTooltip(Tooltip.create(text("category_actions")));
        done = addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> saveAndClose())
                .bounds(footerX, height - 28, footerWidth, 20).build());
        updateControls();
    }

    private boolean changed(Option option) {
        try { return !Objects.equals(checked(option), opened.get(option.key)); }
        catch (IllegalArgumentException error) { return true; }
    }

    private Object checked(Option option) {
        return ConfigSettings.checkedValue(option.key, draft.get(option.key), option.client);
    }

    private void restoreCategory(boolean undo) {
        for (Option option : options) if (option.tab == tab && editable(option)) {
            draft.put(option.key, undo ? opened.get(option.key) : option.fallback);
            errors.remove(option.key);
        }
        saveError = Component.empty();
        rebuild = true;
    }

    private void updateControls() {
        for (Row row : rows) {
            row.control.active = editable(row.option);
            row.reset.active = editable(row.option) && !Objects.equals(draft.get(row.option.key), row.option.fallback);
        }
        if (done != null) done.active = !saving && options.stream().noneMatch(option -> editable(option) && errors.containsKey(option.key));
    }

    @Override
    //? if >=26.1 {
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
    //?} else {
    /*public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    *///?}
        if (rebuild) { rebuild = false; rebuildWidgets(); }
        updateControls();
        for (Row row : rows) if (mouseX >= 6 && mouseX < panelX - 6 && mouseY >= row.y && mouseY < row.y + 24
                || row.control.isFocused() || row.reset.isFocused()) inspected = row.option.key;
        //? if >=26.1 {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        //?} else {
        /*super.render(g, mouseX, mouseY, partialTick);
        *///?}
    }

    @Override
    //? if >=26.1 {
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
    //?} else {
    /*public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    *///?}
        //? if >=26.1 {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        //?} else {
        /*super.renderBackground(g, mouseX, mouseY, partialTick);
        *///?}
        for (Row row : rows) {
            boolean focused = Objects.equals(inspected, row.option.key);
            ConfigStyle.fill(g, 6, row.y, barRight, row.y + 22, focused ? 0x99000000 : 0x66000000);
            if (focused && editable(row.option)) ConfigStyle.fill(g, 6, row.y, 8, row.y + 22, ConfigStyle.ACCENT);
            if (errors.containsKey(row.option.key)) ConfigStyle.fill(g, row.control.getX() - 1, row.y + 1,
                    row.control.getX() + row.control.getWidth() + 1, row.y + 22, 0xFF8B3333);
            ConfigPaint.scrollingText(g, font, name(row.option).getString(), 14, row.y + 7,
                    Math.max(1, row.control.getX() - 20), editable(row.option) ? ConfigStyle.TEXT : ConfigStyle.MUTED);
        }
        ConfigStyle.fill(g, panelX, contentTop, panelX + panelWidth, height - 6, 0xB4101014);
        ConfigStyle.fill(g, panelX, contentTop, panelX + panelWidth, contentTop + 1, ConfigStyle.ACCENT);
        Option selected = options.stream().filter(option -> option.key.equals(inspected) && option.tab == tab).findFirst().orElse(null);
        int y = contentTop + 8;
        Component heading = selected == null ? title : name(selected);
        Component description = selected == null ? text("intro") : text("description." + selected.key);
        y = paragraph(g, heading, y, ConfigStyle.ACCENT);
        y = paragraph(g, description, y + 4, ConfigStyle.TEXT);
        if (selected != null) {
            y = paragraph(g, text("default_value", String.valueOf(selected.fallback)), y + 6, ConfigStyle.MUTED);
            if (!selected.appearance && DfmConfig.keys().get(selected.key).getSpec().getRange() != null)
                y = paragraph(g, text("range", DfmConfig.keys().get(selected.key).getSpec().getRange().toString()), y + 4, ConfigStyle.MUTED);
            if (scripted(selected)) y = paragraph(g, text("scripted"), y + 4, ConfigStyle.ACCENT);
            else if (!editable(selected) && !selected.client) y = paragraph(g, text("server_read_only"), y + 4, ConfigStyle.MUTED);
            if (errors.containsKey(selected.key)) y = paragraph(g, text("invalid_value"), y + 4, 0xFFFF7777);
            ConfigPreview.draw(g, selected.key, draft.get(selected.key), panelX + 8, y + 8, panelWidth - 16, height - 60 - (y + 8));
        }
        if (!saveError.getString().isEmpty()) paragraph(g, saveError, contentTop + 8, 0xFFFF7777);
        List<Option> current = options.stream().filter(option -> option.tab == tab).toList();
        if (current.size() > visibleRows) {
            int track = visibleRows * 24, thumb = Math.max(12, track * visibleRows / current.size());
            int top = contentTop + (track - thumb) * scroll / Math.max(1, current.size() - visibleRows);
            ConfigStyle.fill(g, panelX - 9, top, panelX - 7, top + thumb, 0xFFBFBFBF);
        }
    }

    //? if >=26.1 {
    private int paragraph(GuiGraphicsExtractor g, Component value, int y, int color) {
    //?} else {
    /*private int paragraph(GuiGraphics g, Component value, int y, int color) {
    *///?}
        for (var line : font.split(value, panelWidth - 16)) {
            if (y + 9 > height - 60) break;
            ConfigPaint.text(g, font, line, panelX + 8, y, color, false);
            y += 10;
        }
        return y;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (x < panelX && y >= contentTop && vertical != 0) {
            scroll -= (int) Math.signum(vertical); rebuild = true; return true;
        }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }

    private void saveAndClose() {
        Map<String, Object> client = new LinkedHashMap<>(), server = new LinkedHashMap<>();
        try {
            for (Option option : options) if (editable(option) && !option.appearance && changed(option))
                (option.client ? client : server).put(option.key, checked(option));
            if (!client.isEmpty()) ConfigSettings.saveValues(true, client);
            if (options.stream().anyMatch(option -> option.appearance && changed(option))) {
                Map<String, Object> appearance = new LinkedHashMap<>();
                for (Option option : options) if (option.appearance) appearance.put(option.key, checked(option));
                GuiThemes.setGlobalPreferences(appearance);
            }
            if (server.isEmpty()) { onClose(); return; }
            var localServer = minecraft.getSingleplayerServer();
            if (localServer == null) throw new IllegalStateException("Server settings are read-only");
            saving = true;
            localServer.execute(() -> {
                try {
                    ConfigSettings.saveValues(false, server);
                    minecraft.execute(() -> { saving = false; onClose(); });
                } catch (RuntimeException error) {
                    DimensionalFluxMiner.LOGGER.error("Could not save server configuration", error);
                    minecraft.execute(() -> { saving = false; saveError = text("save_failed"); });
                }
            });
        } catch (RuntimeException error) {
            DimensionalFluxMiner.LOGGER.error("Could not save configuration", error);
            saveError = text("save_failed");
        }
    }

    @Override
    public void onClose() { if (!saving) ConfigSettings.screen(minecraft, parent); }

    @Override public boolean shouldCloseOnEsc() { return !saving; }
}
