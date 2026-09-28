package appeng.me.storage;

import static appeng.util.item.AEItemStackType.ITEM_STACK_TYPE;

import appeng.api.AEApi;
import appeng.api.config.FuzzyMode;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.tile.inventory.IAEStackInventory;
import appeng.util.prioitylist.CombinedItemFilter;
import appeng.util.prioitylist.FuzzyPriorityList;
import appeng.util.prioitylist.PrecisePriorityList;

public class ItemCellInventoryHandler extends CellInventoryHandler<IAEItemStack> {

    public ItemCellInventoryHandler(IMEInventory<IAEItemStack> c) {
        super(c, ITEM_STACK_TYPE);
    }

    @Override
    protected void setPriorityList(boolean hasFuzzy, boolean hasOreFilter, IAEStackInventory config, FuzzyMode fzMode,
            String oreFilter) {
        final IItemList<IAEItemStack> priorityList = AEApi.instance().storage().createItemList();
        for (int x = 0; x < config.getSizeInventory(); x++) {
            final IAEStack<?> aes = config.getAEStackInSlot(x);
            if (aes != null) {
                priorityList.add((IAEItemStack) aes);
            }
        }
        if (hasOreFilter) {
            this.setPartitionList(new CombinedItemFilter(priorityList, hasFuzzy ? fzMode : null, oreFilter, null));
        } else if (!priorityList.isEmpty()) {
            if (hasFuzzy) {
                this.setPartitionList(new FuzzyPriorityList<>(priorityList, fzMode));
            } else {
                this.setPartitionList(new PrecisePriorityList<>(priorityList));
            }
        }
    }

    @Override
    public StorageChannel getStorageChannel() {
        return StorageChannel.ITEMS;
    }
}
