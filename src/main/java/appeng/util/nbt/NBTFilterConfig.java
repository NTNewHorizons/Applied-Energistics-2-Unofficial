package appeng.util.nbt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants.NBT;

public final class NBTFilterConfig {

    public static final String NBT_KEY = "NBTFilter";
    private static final String MODE_KEY = "Mode";
    private static final String FILTERS_KEY = "Filters";

    private NBTFilterMode mode = NBTFilterMode.ALL;
    private final List<NBTFilterEntry> filters = new ArrayList<>();
    private CompiledNBTFilterEntry[] compiledFilters = new CompiledNBTFilterEntry[0];

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

    public void rebuildCompiledFilters() {
        final List<CompiledNBTFilterEntry> compiled = new ArrayList<>(this.filters.size());
        for (final NBTFilterEntry entry : this.filters) {
            final CompiledNBTFilterEntry compiledEntry = CompiledNBTFilterEntry.compile(entry);
            if (compiledEntry != null) compiled.add(compiledEntry);
        }
        this.compiledFilters = compiled.toArray(new CompiledNBTFilterEntry[0]);
    }

    public boolean matches(final ItemStack stack) {
        return stack != null && this.matches(stack.getTagCompound());
    }

    public boolean matches(final NBTTagCompound root) {
        if (this.mode == NBTFilterMode.ALL) {
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
        for (int i = 0; i < list.tagCount(); i++) {
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
