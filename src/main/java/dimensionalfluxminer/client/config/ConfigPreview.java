package dimensionalfluxminer.client.config;

import dimensionalfluxminer.DimensionalFluxMiner;
import net.minecraft.client.Minecraft;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Example picture under an option's help text: the guide table or the machine screen the option changes. Pictures are
 * captures taken at GUI scale 2 and cropped 1:1, so each PNG holds twice the GUI size and is never resampled on disk.
 * Guide tables have one picture per language ({@code <name>_<language>.png}, falling back to en_us).
 */
final class ConfigPreview {
    private record Picture(String file, int width, int height, boolean localized) {}

    private static final int FRAME = 0xFF2E5A62, MIN_WIDTH = 48;
    private static final Picture MACHINE_LIGHT = screen("machine_light"), MACHINE_DARK = screen("machine_dark"),
            ICONS_BLUE = screen("icons_blue"), ICONS_PURPLE = screen("icons_purple"), ICONS_CUSTOM = screen("icons_custom");
    private static final Map<String, Picture> BY_OPTION = new HashMap<>();
    /** Resolved texture path per picture and language; {@code null} when no file exists. */
    private static final Map<String, String> RESOLVED = new HashMap<>();

    static {
        BY_OPTION.put("appearance.shared", screen("shared_appearance"));
        BY_OPTION.put("appearance.color", ICONS_CUSTOM);
        BY_OPTION.put("corruption.enabled", screen("dimensional_death"));
        table("rates_miner", "miner.energyPerAttempt", "miner.minimumProductionRate", "miner.maximumProductionRate",
                "miner.speedByLevel", "miner.powerByLevel");
        table("rates_crusher", "crusher.energyPerOperation", "crusher.speedByLevel", "crusher.powerByLevel");
        table("rates_furnace", "electric_furnace.furnaceEnergyPerThreeItems", "electric_furnace.speedByLevel",
                "electric_furnace.furnacePowerByLevel");
        table("rates_alloy", "electric_furnace.alloyEnergyPerOperation", "electric_furnace.alloyPowerByLevel");
    }

    private ConfigPreview() {}

    private static Picture screen(String file) {
        return new Picture(file, 208, 117, false);
    }

    private static void table(String file, String... options) {
        Picture picture = new Picture(file, 170, 146, true);
        for (String option : List.of(options)) BY_OPTION.put(option, picture);
    }

    /** The picture for an option; the appearance pictures follow the value being edited. */
    private static Picture picture(String option, Object value) {
        if (option.equals("appearance.dark")) return Boolean.TRUE.equals(value) ? MACHINE_DARK : MACHINE_LIGHT;
        if (option.equals("appearance.icons")) return switch (String.valueOf(value)) {
            case "purple" -> ICONS_PURPLE;
            case "custom" -> ICONS_CUSTOM;
            default -> ICONS_BLUE;
        };
        return BY_OPTION.get(option);
    }

    /**
     * Draws the option's picture, framed and centered, scaled down to fit {@code maxWidth} x {@code maxHeight}.
     * Nothing is drawn when the option has no picture or the space is too small.
     */
    //? if >=26.1 {
    static void draw(GuiGraphicsExtractor g, String option, Object value, int x, int y, int maxWidth, int maxHeight) {
    //?} else {
    /*static void draw(GuiGraphics g, String option, Object value, int x, int y, int maxWidth, int maxHeight) {
    *///?}
        Picture picture = picture(option, value);
        if (picture == null) return;
        float scale = Math.min(1F, Math.min((maxWidth - 2) / (float) picture.width, (maxHeight - 2) / (float) picture.height));
        int w = Math.round(picture.width * scale), h = Math.round(picture.height * scale);
        if (w < MIN_WIDTH) return;
        String path = path(picture);
        if (path == null) return;
        int left = x + (maxWidth - w) / 2, top = y + 1;
        ConfigStyle.fill(g, left - 1, top - 1, left + w + 1, top + h + 1, FRAME);
        //? if >=26.1 {
        g.blit(DimensionalFluxMiner.id(path), left, top, left + w, top + h, 0, 1, 0, 1);
        //?} else {
        /*int textureWidth = picture.width * 2, textureHeight = picture.height * 2;
        g.blit(DimensionalFluxMiner.id(path), left, top, w, h, 0, 0, textureWidth, textureHeight, textureWidth, textureHeight);
        *///?}
    }

    /** Forgets the resolved files, so a resource pack change is picked up the next time the screen opens. */
    static void reset() {
        RESOLVED.clear();
    }

    private static String path(Picture picture) {
        String language = picture.localized ? Minecraft.getInstance().getLanguageManager().getSelected() : "";
        String key = picture.file + "|" + language;
        if (!RESOLVED.containsKey(key)) {
            String own = existing(picture.localized ? picture.file + "_" + language : picture.file);
            RESOLVED.put(key, own != null || !picture.localized ? own : existing(picture.file + "_en_us"));
        }
        return RESOLVED.get(key);
    }

    private static String existing(String file) {
        String path = "textures/gui/config/" + file + ".png";
        return Minecraft.getInstance().getResourceManager().getResource(DimensionalFluxMiner.id(path)).isPresent() ? path : null;
    }
}
