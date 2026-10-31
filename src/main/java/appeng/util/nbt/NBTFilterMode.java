package appeng.util.nbt;

public enum NBTFilterMode {

    ALL,
    ANY;

    public NBTFilterMode rotate(final boolean backwards) {
        final NBTFilterMode[] values = values();
        final int offset = backwards ? values.length - 1 : 1;
        return values[(this.ordinal() + offset) % values.length];
    }
}
