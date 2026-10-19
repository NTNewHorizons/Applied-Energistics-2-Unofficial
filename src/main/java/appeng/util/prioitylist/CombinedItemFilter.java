package appeng.util.prioitylist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import appeng.api.config.FuzzyMode;
import appeng.api.storage.data.IAEItemStack;
import appeng.util.Platform;
import appeng.util.nbt.NBTFilterConfig;
import appeng.util.nbt.NBTItemFilterMatcher;

/** Combines configured items, an ore-dictionary expression and NBT rules using AND semantics. */
public final class CombinedItemFilter implements IPartitionList<IAEItemStack> {

    private final List<IAEItemStack> items = new ArrayList<>();
    @Nullable
    private final FuzzyMode fuzzyMode;
    private final boolean hasOreFilter;
    @Nullable
    private final Predicate<IAEItemStack> oreFilter;
    @Nullable
    private final NBTFilterConfig nbtFilter;

    public CombinedItemFilter(@Nullable final Iterable<IAEItemStack> items, @Nullable final FuzzyMode fuzzyMode,
            @Nullable final String oreExpression, @Nullable final NBTFilterConfig nbtFilter) {
        this(
                items,
                fuzzyMode,
                oreExpression != null,
                oreExpression == null ? null : OreFilteredList.makeFilter(oreExpression.trim()),
                nbtFilter);
    }

    CombinedItemFilter(@Nullable final Iterable<IAEItemStack> items, @Nullable final FuzzyMode fuzzyMode,
            final boolean hasOreFilter, @Nullable final Predicate<IAEItemStack> oreFilter,
            @Nullable final NBTFilterConfig nbtFilter) {
        if (items != null) {
            for (final IAEItemStack item : items) {
                if (item != null) this.items.add(item.copy());
            }
        }
        this.fuzzyMode = fuzzyMode;
        this.hasOreFilter = hasOreFilter;
        this.oreFilter = oreFilter;
        this.nbtFilter = nbtFilter == null ? null : nbtFilter.copy();
    }

    @Override
    public boolean isListed(final IAEItemStack input) {
        if (input == null) return false;
        if (this.hasOreFilter && (this.oreFilter == null || !this.oreFilter.test(input))) return false;

        if (!this.items.isEmpty()) {
            boolean matchesItem = false;
            for (final IAEItemStack item : this.items) {
                final boolean matches = this.fuzzyMode != null || this.nbtFilter != null
                        ? NBTItemFilterMatcher.matchesItem(item, input, this.fuzzyMode)
                        : Platform.isSameItemPrecise(item.getItemStack(), input.getItemStack());
                if (matches) {
                    matchesItem = true;
                    break;
                }
            }
            if (!matchesItem) return false;
        }

        return this.nbtFilter == null || this.nbtFilter.matches(input.getItemStack());
    }

    @Override
    public boolean isEmpty() {
        return this.items.isEmpty() && !this.hasOreFilter && this.nbtFilter == null;
    }

    public boolean isFuzzy() {
        return this.fuzzyMode != null && !this.items.isEmpty();
    }

    @Override
    public Iterable<IAEItemStack> getItems() {
        return Collections.unmodifiableList(this.items);
    }
}
