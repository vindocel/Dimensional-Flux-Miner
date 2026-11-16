package dimensionalfluxminer.client.screen;

import dimensionalfluxminer.machine.RedstoneMode;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/** Shared texts of the single-block machine screens. */
final class MachineText {
    private MachineText() {
    }

    static Component onOff(boolean on) {
        return Component.translatable(on ? "gui.dimensionalfluxminer.on" : "gui.dimensionalfluxminer.off");
    }

    static Component status(CompoundTag d) {
        //? if >=26.1 {
        return Component.translatable("gui.dimensionalfluxminer.status." + d.getStringOr("status", ""));
        //?} else {
        /*return Component.translatable("gui.dimensionalfluxminer.status." + d.getString("status"));
        *///?}
    }

    /** Status line for the inner screens. */
    static Component statusLine(CompoundTag d) {
        //? if >=26.1 {
        int color = switch (d.getStringOr("status", "")) {
        //?} else {
        /*int color = switch (d.getString("status")) {
        *///?}
            case "running" -> DfmScreen.screenText();
            case "output_blocked", "redstone_halt", "no_energy" -> DfmScreen.SCREEN_ERROR;
            default -> DfmScreen.SCREEN_WARN;
        };
        return status(d).copy().withColor(color & 0xFFFFFF);
    }

    /** "Redstone Detection: NORMAL" line of Crusher and Electric Furnace. */
    static Component redstone(CompoundTag d) {
        return Component.translatable("gui.dimensionalfluxminer.redstone_control",
                //? if >=26.1 {
                Component.translatable(RedstoneMode.byName(d.getStringOr("redstone_mode", "")).translationKey()));
                //?} else {
                /*Component.translatable(RedstoneMode.byName(d.getString("redstone_mode")).translationKey()));
                *///?}
    }

    static Component energyLine(CompoundTag d) {
        //? if >=26.1 {
        return Component.translatable("gui.dimensionalfluxminer.energy", DfmScreen.fe(d.getLongOr("fe", 0L)), DfmScreen.fe(Math.max(1, d.getLongOr("fe_capacity", 0L))));
        //?} else {
        /*return Component.translatable("gui.dimensionalfluxminer.energy", DfmScreen.fe(d.getLong("fe")), DfmScreen.fe(Math.max(1, d.getLong("fe_capacity"))));
        *///?}
    }

    static List<Component> energyTooltip(CompoundTag d) {
        return List.of(
                energyLine(d),
                //? if >=26.1 {
                Component.translatable("gui.dimensionalfluxminer.power_limit", DfmScreen.fe(d.getLongOr("power", 0L)) + "/t"));
                //?} else {
                /*Component.translatable("gui.dimensionalfluxminer.power_limit", DfmScreen.fe(d.getLong("power")) + "/t"));
                *///?}
    }
}
