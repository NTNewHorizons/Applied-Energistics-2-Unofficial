package appeng.util.prioitylist;

import appeng.api.storage.data.IAEItemStack;
import appeng.util.nbt.NBTFilterConfig;

/** NBT partition list backed only by a compiled configuration; intentionally has no item result cache. */
public final class NBTFilteredList implements IPartitionList<IAEItemStack> {

    private final NBTFilterConfig config;

    public NBTFilteredList(final NBTFilterConfig config) {
        this.config = config.copy();
    }

    @Override
    public boolean isListed(final IAEItemStack input) {
        return input != null && this.config.matches(input.getItemStack());
    }

    @Override
    public boolean isEmpty() {
        return this.config.getFilters().isEmpty();
    }

    @Override
    public Iterable<IAEItemStack> getItems() {
        return null;
    }
}
