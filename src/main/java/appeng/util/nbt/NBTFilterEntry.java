package appeng.util.nbt;

import net.minecraft.nbt.NBTTagCompound;

public final class NBTFilterEntry {

    private static final String PATH_KEY = "Path";
    private static final String COMPARISON_KEY = "Comparison";
    private static final String VALUE_KEY = "Value";

    private String path;
    private NBTComparison comparison;
    private String value;

    public NBTFilterEntry() {
        this("", NBTComparison.EQUALS, "");
    }

    public NBTFilterEntry(final String path, final NBTComparison comparison, final String value) {
        this.path = path == null ? "" : path;
        this.comparison = comparison == null ? NBTComparison.EQUALS : comparison;
        this.value = value == null ? "" : value;
    }

    public String getPath() {
        return this.path;
    }

    public void setPath(final String path) {
        this.path = path == null ? "" : path;
    }

    public NBTComparison getComparison() {
        return this.comparison;
    }

    public void setComparison(final NBTComparison comparison) {
        this.comparison = comparison == null ? NBTComparison.EQUALS : comparison;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(final String value) {
        this.value = value == null ? "" : value;
    }

    public boolean isCompletelyBlank() {
        return this.path.trim().isEmpty() && this.value.trim().isEmpty();
    }

    public NBTTagCompound writeToNBT() {
        final NBTTagCompound tag = new NBTTagCompound();
        tag.setString(PATH_KEY, this.path);
        tag.setString(COMPARISON_KEY, this.comparison.name());
        tag.setString(VALUE_KEY, this.value);
        return tag;
    }

    public static NBTFilterEntry readFromNBT(final NBTTagCompound tag) {
        NBTComparison comparison = NBTComparison.EQUALS;
        try {
            comparison = NBTComparison.valueOf(tag.getString(COMPARISON_KEY));
        } catch (final IllegalArgumentException ignored) {}
        return new NBTFilterEntry(tag.getString(PATH_KEY), comparison, tag.getString(VALUE_KEY));
    }
}
