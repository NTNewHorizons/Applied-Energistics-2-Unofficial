package appeng.util.prioitylist;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import appeng.api.config.FuzzyMode;
import appeng.api.storage.data.IAEItemStack;
import appeng.util.nbt.NBTFilterConfig;
import appeng.util.nbt.NBTItemFilterMatcher;

/** Combines NBT rules with an optional item partition; intentionally has no item result cache. */
public final class NBTFilteredList implements IPartitionList<IAEItemStack> {

    private final NBTFilterConfig config;
    private final List<IAEItemStack> items = new ArrayList<>();
    @Nullable
    private final FuzzyMode fuzzyMode;

    public NBTFilteredList(final NBTFilterConfig config) {
        this(config, null, null);
    }

    public NBTFilteredList(final NBTFilterConfig config, @Nullable final Iterable<IAEItemStack> items,
            @Nullable final FuzzyMode fuzzyMode) {
        this.config = config.copy();
        if (items != null) {
            for (final IAEItemStack item : items) {
                if (item != null) this.items.add(item.copy());
            }
        }
        this.fuzzyMode = fuzzyMode;
    }

    @Override
    public boolean isListed(final IAEItemStack input) {
        if (input == null || !this.config.matches(input.getItemStack())) return false;
        if (this.items.isEmpty()) return true;
        for (final IAEItemStack item : this.items) {
            if (NBTItemFilterMatcher.matchesItem(item, input, this.fuzzyMode)) return true;
        }
        return false;
    }

    @Override
    public boolean isEmpty() {
        return this.config.getFilters().isEmpty() && this.items.isEmpty();
    }

    @Override
    public Iterable<IAEItemStack> getItems() {
        return null;
    }
}
