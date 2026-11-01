package appeng.parts.automation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import java.util.EnumSet;
import java.util.Set;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

import appeng.api.config.FuzzyMode;
import appeng.api.config.RedstoneMode;
import appeng.api.config.Settings;
import appeng.api.config.Upgrades;
import appeng.api.networking.IGridNode;
import appeng.util.nbt.NBTComparison;
import appeng.util.nbt.NBTFilterConfig;
import appeng.util.nbt.NBTFilterEntry;
import appeng.util.nbt.NBTFilterMode;
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

    @Test
    public void removingAndReinstallingNbtCardDoesNotRestoreBusRules() {
        final Set<Upgrades> upgrades = EnumSet.of(Upgrades.NBT_FILTER);
        final PartSharedItemBus<?>[] buses = { new TestBus(upgrades), new TestExportBus(upgrades) };
        for (final PartSharedItemBus<?> bus : buses) {
            upgrades.add(Upgrades.NBT_FILTER);
            final NBTFilterConfig config = new NBTFilterConfig();
            config.setMode(NBTFilterMode.ANY);
            config.addFilter(new NBTFilterEntry("level", NBTComparison.EXISTS, ""));
            bus.setNBTFilterConfig(config);
            bus.upgradesChanged();
            assertEquals(1, bus.getNBTFilterConfig().getFilters().size());

            upgrades.remove(Upgrades.NBT_FILTER);
            bus.upgradesChanged();
            assertEquals(0, bus.getNBTFilterConfig().getFilters().size());
            assertEquals(NBTFilterMode.ALL, bus.getNBTFilterConfig().getMode());

            upgrades.add(Upgrades.NBT_FILTER);
            bus.upgradesChanged();
            assertEquals(0, bus.getNBTFilterConfig().getFilters().size());
            final NBTTagCompound candidate = new NBTTagCompound();
            candidate.setInteger("level", 1);
            assertFalse(bus.getNBTFilterConfig().matches(candidate));
        }
    }

    private static final class TestBus extends PartImportBus {

        private final Set<Upgrades> upgrades;

        private TestBus() {
            this(EnumSet.noneOf(Upgrades.class));
        }

        private TestBus(final Set<Upgrades> upgrades) {
            super(new ItemStack(new Item()));
            this.upgrades = upgrades;
        }

        @Override
        public int getInstalledUpgrades(final Upgrades upgrade) {
            return this.upgrades.contains(upgrade) ? 1 : 0;
        }

        @Override
        public IGridNode getGridNode() {
            return null;
        }
    }

    private static final class TestExportBus extends PartExportBus {

        private final Set<Upgrades> upgrades;

        private TestExportBus(final Set<Upgrades> upgrades) {
            super(new ItemStack(new Item()));
            this.upgrades = upgrades;
        }

        @Override
        public int getInstalledUpgrades(final Upgrades upgrade) {
            return this.upgrades.contains(upgrade) ? 1 : 0;
        }

        @Override
        public IGridNode getGridNode() {
            return null;
        }
    }
}
