package dimensionalfluxminer.content;

import net.minecraft.util.StringRepresentable;

/** Visual/operational state shared by the single-block machines. */
public enum MachineState implements StringRepresentable {
    OFF("off"),
    IDLE("idle"),
    ACTIVE("active");

    private final String name;

    MachineState(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
