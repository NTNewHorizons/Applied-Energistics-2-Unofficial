package appeng.util.nbt;

public enum NBTComparison {

    EQUALS("==", true),
    NOT_EQUALS("!=", true),
    GREATER_THAN(">", true),
    GREATER_THAN_OR_EQUAL(">=", true),
    LESS_THAN("<", true),
    LESS_THAN_OR_EQUAL("<=", true),
    EXISTS("Exists", false),
    NOT_EXISTS("Not Exists", false);

    private final String displayName;
    private final boolean requiresValue;

    NBTComparison(final String displayName, final boolean requiresValue) {
        this.displayName = displayName;
        this.requiresValue = requiresValue;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public boolean requiresValue() {
        return this.requiresValue;
    }

    public NBTComparison rotate(final boolean backwards) {
        final NBTComparison[] values = values();
        final int offset = backwards ? values.length - 1 : 1;
        return values[(this.ordinal() + offset) % values.length];
    }
}
