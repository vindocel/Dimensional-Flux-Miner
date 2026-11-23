package dimensionalfluxminer.client.config;

import dimensionalfluxminer.DfmConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import java.util.Map;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Adapters for the mod's own settings and typed draft validation. */
final class ConfigSettings {
    private ConfigSettings() {}
    static Map<String, ModConfigSpec.ConfigValue<?>> clientOptions() { return Map.of(); }
    static Map<String, ModConfigSpec.ConfigValue<?>> serverOptions() { return DfmConfig.keys(); }
    static boolean isLoaded(boolean client) { return client || DfmConfig.SPEC.isLoaded(); }
    static Map<String, Object> fileValues(boolean client) { return client ? Map.of() : DfmConfig.fileValues(); }
    static Map<String, Object> effectiveServerValues() { return DfmConfig.effectiveValues(); }
    static Map<String, Object> appearanceDefaults() {
        Map<String, Object> defaults = new java.util.LinkedHashMap<>();
        defaults.put("appearance.dark", false);
        defaults.put("appearance.shared", true);
        defaults.put("appearance.icons", "blue");
        defaults.put("appearance.color", "EF8B32");
        return java.util.Collections.unmodifiableMap(defaults);
    }
    static Object checkedValue(String key, Object raw, boolean client) {
        if (!key.startsWith("appearance.")) return DfmConfig.checkedValue(key, raw);
        Object fallback = appearanceDefaults().get(key);
        if (fallback instanceof Boolean && raw instanceof Boolean) return raw;
        if (key.equals("appearance.icons") && java.util.List.of("blue", "purple", "custom").contains(raw)) return raw;
        if (key.equals("appearance.color") && raw instanceof String text && text.matches("#?[0-9a-fA-F]{6}"))
            return text.replace("#", "").toUpperCase(java.util.Locale.ROOT);
        throw new IllegalArgumentException("Invalid appearance preference");
    }
    static void saveValues(boolean client, Map<String, ?> changes) {
        if (client && !changes.isEmpty()) throw new IllegalArgumentException("No client TOML settings");
        if (!client) DfmConfig.saveValues(changes);
    }
    static void screen(Minecraft minecraft, Screen screen) {
        //? if >=26.3 {
        /*minecraft.gui.setScreen(screen);
        *///?} else {
        minecraft.setScreen(screen);
        //?}
    }
}
