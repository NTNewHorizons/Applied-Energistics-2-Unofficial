package appeng.util.nbt;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.common.util.Constants.NBT;

public final class NBTFilterConfig {

    public static final String NBT_KEY = "NBTFilter";
    public static final int MAX_FILTERS = 25;
    private static final String MODE_KEY = "Mode";
    private static final String FILTERS_KEY = "Filters";

    private NBTFilterMode mode = NBTFilterMode.ALL;
    private final List<NBTFilterEntry> filters = new ArrayList<>();
    private CompiledNBTFilterEntry[] compiledFilters = new CompiledNBTFilterEntry[0];
    private boolean hasInvalidFilters;

    public NBTFilterMode getMode() {
        return this.mode;
    }

    public void setMode(final NBTFilterMode mode) {
        this.mode = mode == null ? NBTFilterMode.ALL : mode;
        this.rebuildCompiledFilters();
    }

    public List<NBTFilterEntry> getFilters() {
        return Collections.unmodifiableList(this.filters);
    }

    public NBTFilterConfig copy() {
        final NBTFilterConfig copy = new NBTFilterConfig();
        copy.mode = this.mode;
        for (final NBTFilterEntry entry : this.filters) {
            copy.filters.add(new NBTFilterEntry(entry.getPath(), entry.getComparison(), entry.getValue()));
        }
        copy.rebuildCompiledFilters();
        return copy;
    }

    public void replaceWith(final NBTFilterConfig other) {
        this.filters.clear();
        this.mode = other == null ? NBTFilterMode.ALL : other.mode;
        if (other != null) {
            for (final NBTFilterEntry entry : other.filters) {
                this.filters.add(new NBTFilterEntry(entry.getPath(), entry.getComparison(), entry.getValue()));
            }
        }
        this.rebuildCompiledFilters();
    }

    public void addFilter(final NBTFilterEntry entry) {
        if (this.filters.size() >= MAX_FILTERS) return;
        this.filters.add(entry == null ? new NBTFilterEntry() : entry);
        this.rebuildCompiledFilters();
    }

    public void updateFilter(final int index, final String path, final NBTComparison comparison, final String value) {
        if (index < 0 || index >= this.filters.size()) return;
        final NBTFilterEntry entry = this.filters.get(index);
        entry.setPath(path);
        entry.setComparison(comparison);
        entry.setValue(value);
        this.rebuildCompiledFilters();
    }

    public void removeFilter(final int index) {
        if (index < 0 || index >= this.filters.size()) return;
        this.filters.remove(index);
        this.rebuildCompiledFilters();
    }

    public void clear() {
        this.filters.clear();
        this.rebuildCompiledFilters();
    }

    public void replaceWithNBT(final NBTTagCompound root, final int maxFilters) {
        this.filters.clear();
        this.mode = NBTFilterMode.ALL;
        if (root != null && maxFilters > 0)
            this.addFiltersFromCompound(root, root, "", Math.min(maxFilters, MAX_FILTERS));
        this.rebuildCompiledFilters();
    }

    private void addFiltersFromCompound(final NBTTagCompound root, final NBTTagCompound compound,
            final String parentPath, final int maxFilters) {
        final List<String> keys = new ArrayList<>(compound.func_150296_c());
        Collections.sort(keys);
        for (final String key : keys) {
            if (this.filters.size() >= maxFilters) return;
            final String path = NBTPath.appendCompoundKey(parentPath, key);
            this.addFiltersFromTag(root, compound.getTag(key), path, maxFilters);
        }
    }

    private void addFiltersFromTag(final NBTTagCompound root, final NBTBase tag, final String path,
            final int maxFilters) {
        if (tag == null || this.filters.size() >= maxFilters) return;

        if (tag instanceof NBTTagCompound compound) {
            if (compound.hasNoTags()) {
                this.addGeneratedFilter(root, tag, path, NBTComparison.EXISTS, "");
            } else {
                this.addFiltersFromCompound(root, compound, path, maxFilters);
            }
            return;
        }

        if (tag instanceof NBTTagList list) {
            if (list.tagCount() == 0) {
                this.addGeneratedFilter(root, tag, path, NBTComparison.EXISTS, "");
            } else {
                for (int i = 0; i < list.tagCount() && this.filters.size() < maxFilters; i++) {
                    this.addFiltersFromTag(
                            root,
                            (NBTBase) list.tagList.get(i),
                            NBTPath.appendListIndex(path, i),
                            maxFilters);
                }
            }
            return;
        }

        final String value;
        if (tag instanceof NBTTagString string) {
            value = '"' + escapeQuotedString(string.func_150285_a_()) + '"';
        } else if (tag instanceof NBTBase.NBTPrimitive primitive) {
            if (tag.getId() >= 1 && tag.getId() <= 4) {
                value = Long.toString(primitive.func_150291_c());
            } else {
                final double numericValue = primitive.func_150286_g();
                if (!Double.isFinite(numericValue)) {
                    this.addGeneratedFilter(root, tag, path, NBTComparison.EXISTS, "");
                    return;
                }
                value = BigDecimal.valueOf(numericValue).stripTrailingZeros().toPlainString();
            }
        } else {
            this.addGeneratedFilter(root, tag, path, NBTComparison.EXISTS, "");
            return;
        }
        this.addGeneratedFilter(root, tag, path, NBTComparison.EQUALS, value);
    }

    private static String escapeQuotedString(final String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void addGeneratedFilter(final NBTTagCompound root, final NBTBase expected, final String path,
            final NBTComparison comparison, final String value) {
        final NBTPath compiledPath = NBTPath.compile(path);
        if (compiledPath != null && compiledPath.resolve(root) == expected) {
            this.filters.add(new NBTFilterEntry(path, comparison, value));
        }
    }

    public void rebuildCompiledFilters() {
        final List<CompiledNBTFilterEntry> compiled = new ArrayList<>(this.filters.size());
        boolean hasInvalidFilters = false;
        for (final NBTFilterEntry entry : this.filters) {
            final CompiledNBTFilterEntry compiledEntry = CompiledNBTFilterEntry.compile(entry);
            if (compiledEntry != null) {
                compiled.add(compiledEntry);
            } else {
                hasInvalidFilters = true;
            }
        }
        this.compiledFilters = compiled.toArray(new CompiledNBTFilterEntry[0]);
        this.hasInvalidFilters = hasInvalidFilters;
    }

    public boolean matches(final ItemStack stack) {
        return stack != null && this.matches(stack.getTagCompound());
    }

    public boolean matches(final NBTTagCompound root) {
        if (this.filters.isEmpty()) return false;

        if (this.mode == NBTFilterMode.ALL) {
            if (this.hasInvalidFilters) return false;
            for (final CompiledNBTFilterEntry filter : this.compiledFilters) {
                if (!filter.matches(root)) return false;
            }
            return true;
        }

        for (final CompiledNBTFilterEntry filter : this.compiledFilters) {
            if (filter.matches(root)) return true;
        }
        return false;
    }

    public void readFromNBT(final NBTTagCompound parent) {
        this.filters.clear();
        final NBTTagCompound tag = parent.hasKey(NBT_KEY, NBT.TAG_COMPOUND) ? parent.getCompoundTag(NBT_KEY) : parent;
        try {
            this.mode = NBTFilterMode.valueOf(tag.getString(MODE_KEY));
        } catch (final IllegalArgumentException ignored) {
            this.mode = NBTFilterMode.ALL;
        }

        final NBTTagList list = tag.getTagList(FILTERS_KEY, NBT.TAG_COMPOUND);
        for (int i = 0; i < Math.min(list.tagCount(), MAX_FILTERS); i++) {
            this.filters.add(NBTFilterEntry.readFromNBT(list.getCompoundTagAt(i)));
        }
        this.rebuildCompiledFilters();
    }

    public void writeToNBT(final NBTTagCompound parent) {
        final NBTTagCompound tag = new NBTTagCompound();
        tag.setString(MODE_KEY, this.mode.name());
        final NBTTagList list = new NBTTagList();
        for (final NBTFilterEntry entry : this.filters) list.appendTag(entry.writeToNBT());
        tag.setTag(FILTERS_KEY, list);
        parent.setTag(NBT_KEY, tag);
    }
}
