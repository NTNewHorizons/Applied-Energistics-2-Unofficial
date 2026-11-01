package appeng.util.prioitylist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import appeng.api.config.FuzzyMode;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.util.Platform;
import appeng.util.nbt.NBTFilterConfig;
import appeng.util.nbt.NBTItemFilterMatcher;

/** Combines configured items, an ore-dictionary expression and NBT rules using AND semantics. */
public final class CombinedItemFilter implements IPartitionList<IAEItemStack> {

    private final List<IAEItemStack> items = new ArrayList<>();
    // A fluid-only view-cell partition is still restrictive: no item can match it.
    private final boolean hasConfiguredItems;
    @Nullable
    private final FuzzyMode fuzzyMode;
    private final boolean hasOreFilter;
    @Nullable
    private final Predicate<IAEItemStack> oreFilter;
    @Nullable
    private final NBTFilterConfig nbtFilter;

    public CombinedItemFilter(@Nullable final Iterable<? extends IAEStack<?>> items,
            @Nullable final FuzzyMode fuzzyMode, @Nullable final String oreExpression,
            @Nullable final NBTFilterConfig nbtFilter) {
        this(
                items,
                fuzzyMode,
                oreExpression != null,
                oreExpression == null ? null : OreFilteredList.makeFilter(oreExpression.trim()),
                nbtFilter);
    }

    CombinedItemFilter(@Nullable final Iterable<? extends IAEStack<?>> items, @Nullable final FuzzyMode fuzzyMode,
            final boolean hasOreFilter, @Nullable final Predicate<IAEItemStack> oreFilter,
            @Nullable final NBTFilterConfig nbtFilter) {
        boolean hasConfiguredItems = false;
        if (items != null) {
            for (final IAEStack<?> stack : items) {
                if (stack != null) hasConfiguredItems = true;
                if (stack instanceof IAEItemStack item) this.items.add(item.copy());
            }
        }
        this.hasConfiguredItems = hasConfiguredItems;
        this.fuzzyMode = fuzzyMode;
        this.hasOreFilter = hasOreFilter;
        this.oreFilter = oreFilter;
        this.nbtFilter = nbtFilter == null ? null : nbtFilter.copy();
    }

    @Override
    public boolean isListed(final IAEItemStack input) {
        if (input == null) return false;
        if (this.hasOreFilter && (this.oreFilter == null || !this.oreFilter.test(input))) return false;

        if (this.hasConfiguredItems) {
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
        return !this.hasConfiguredItems && !this.hasOreFilter && this.nbtFilter == null;
    }

    public boolean isFuzzy() {
        return this.fuzzyMode != null && !this.items.isEmpty();
    }

    public boolean hasOreFilter() {
        return this.hasOreFilter;
    }

    @Nullable
    public NBTFilterConfig getNBTFilterConfig() {
        return this.nbtFilter == null ? null : this.nbtFilter.copy();
    }

    @Override
    public Iterable<IAEItemStack> getItems() {
        return Collections.unmodifiableList(this.items);
    }
}
