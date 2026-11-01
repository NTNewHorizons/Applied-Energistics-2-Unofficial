package appeng.me.storage;

import static appeng.util.item.AEItemStackType.ITEM_STACK_TYPE;

import net.minecraft.item.ItemStack;

import appeng.api.AEApi;
import appeng.api.config.FuzzyMode;
import appeng.api.storage.ICellWorkbenchItem;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.tile.inventory.IAEStackInventory;
import appeng.util.nbt.NBTFilterConfig;
import appeng.util.prioitylist.CombinedItemFilter;
import appeng.util.prioitylist.FuzzyPriorityList;
import appeng.util.prioitylist.PrecisePriorityList;

public class ItemCellInventoryHandler extends CellInventoryHandler<IAEItemStack> {

    public ItemCellInventoryHandler(IMEInventory<IAEItemStack> c) {
        super(c, ITEM_STACK_TYPE);
    }

    @Override
    protected void setPriorityList(boolean hasFuzzy, boolean hasOreFilter, boolean hasNBTFilter,
            IAEStackInventory config, FuzzyMode fzMode, String oreFilter) {
        final IItemList<IAEItemStack> priorityList = AEApi.instance().storage().createItemList();
        for (int x = 0; x < config.getSizeInventory(); x++) {
            final IAEStack<?> aes = config.getAEStackInSlot(x);
            if (aes != null) {
                priorityList.add((IAEItemStack) aes);
            }
        }
        if (hasOreFilter || hasNBTFilter) {
            NBTFilterConfig nbtFilter = null;
            if (hasNBTFilter) {
                final ItemStack stack = this.getCellInv().getItemStack();
                nbtFilter = stack.getItem() instanceof ICellWorkbenchItem cell ? cell.getNBTFilterConfig(stack)
                        : new NBTFilterConfig();
            }
            this.setPartitionList(
                    new CombinedItemFilter(
                            priorityList,
                            hasFuzzy ? fzMode : null,
                            hasOreFilter ? oreFilter : null,
                            nbtFilter));
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
