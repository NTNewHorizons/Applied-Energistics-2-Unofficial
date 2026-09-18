package appeng.util.nbt;

import javax.annotation.Nullable;

import appeng.api.config.FuzzyMode;
import appeng.api.storage.data.IAEItemStack;

/** Matches an item filter independently from the NBT rules applied to the candidate. */
public final class NBTItemFilterMatcher {

    private NBTItemFilterMatcher() {}

    public static boolean matchesItem(final IAEItemStack filter, final IAEItemStack candidate,
            @Nullable final FuzzyMode fuzzyMode) {
        if (filter == null || candidate == null) return false;
        if (fuzzyMode != null) return filter.fuzzyComparison(candidate, fuzzyMode);
        return filter.getItem() == candidate.getItem() && filter.getItemDamage() == candidate.getItemDamage();
    }
}
