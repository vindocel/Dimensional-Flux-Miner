package dimensionalfluxminer.tank;

/** Dimensional Tank variants: one chamber or two independent chambers (A = index 0, B = index 1). */
public enum TankKind {
    SINGLE("single", 1),
    DUAL("dual", 2);

    /** Capacity of each chamber, in mB. */
    public static final int CHAMBER_CAPACITY = 128_000;

    private final String id;
    private final int chambers;

    TankKind(String id, int chambers) {
        this.id = id;
        this.chambers = chambers;
    }

    public String id() {
        return id;
    }

    public int chambers() {
        return chambers;
    }

    public static TankKind byId(String id) {
        for (TankKind kind : values()) {
            if (kind.id.equals(id)) {
                return kind;
            }
        }
        return null;
    }
}
