package appeng.parts.automation;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

import appeng.api.config.FuzzyMode;
import appeng.api.config.RedstoneMode;
import appeng.api.config.Settings;
import appeng.api.networking.IGridNode;
import appeng.util.nbt.NBTFilterConfig;
import appeng.util.prioitylist.CombinedItemFilter;

public class PartSharedItemBusTest {

    @Test
    public void reusesFilterUntilConfigurationChanges() {
        final TestBus bus = new TestBus();
        CombinedItemFilter filter = bus.createItemFilter();
        assertSame(filter, bus.createItemFilter());

        bus.saveAEStackInv();
        filter = assertRebuilt(bus, filter);

        bus.setFilter("ingotIron");
        filter = assertRebuilt(bus, filter);

        bus.setNBTFilterConfig(new NBTFilterConfig());
        filter = assertRebuilt(bus, filter);

        bus.upgradesChanged();
        filter = assertRebuilt(bus, filter);

        bus.getConfigManager().putSetting(Settings.FUZZY_MODE, FuzzyMode.PERCENT_50);
        filter = assertRebuilt(bus, filter);

        bus.getConfigManager().putSetting(Settings.REDSTONE_CONTROLLED, RedstoneMode.HIGH_SIGNAL);
        assertSame(filter, bus.createItemFilter());
    }

    @Test
    public void loadingConfigurationDiscardsCachedFilter() {
        final TestBus bus = new TestBus();
        final CombinedItemFilter filter = bus.createItemFilter();

        bus.readFromNBT(new NBTTagCompound());

        assertRebuilt(bus, filter);
    }

    private static CombinedItemFilter assertRebuilt(final TestBus bus, final CombinedItemFilter previous) {
        final CombinedItemFilter current = bus.createItemFilter();
        assertNotSame(previous, current);
        assertSame(current, bus.createItemFilter());
        return current;
    }

    private static final class TestBus extends PartImportBus {

        private TestBus() {
            super(new ItemStack(new Item()));
        }

        @Override
        public IGridNode getGridNode() {
            return null;
        }
    }
}
