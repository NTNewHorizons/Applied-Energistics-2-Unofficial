package appeng.parts.automation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.lang.reflect.Proxy;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import org.junit.Test;

import appeng.api.config.Settings;
import appeng.api.config.Upgrades;
import appeng.api.config.YesNo;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.StorageName;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.util.IConfigManager;
import appeng.me.helpers.AENetworkProxy;
import appeng.tile.inventory.IAEStackInventory;
import appeng.util.nbt.NBTFilterConfig;
import appeng.util.prioitylist.CombinedItemFilter;

public class PartExportBusCraftingTest {

    @Test
    public void filterCardsExportStockThenRequestCraftingWhenStockRunsOut() {
        for (final Upgrades card : new Upgrades[] { Upgrades.ORE_FILTER, Upgrades.NBT_FILTER }) {
            final TestBus bus = new TestBus(card);
            bus.stock = 1;
            bus.doBusWork();
            assertEquals(1, bus.exported);
            assertEquals(0, bus.craftingRequests);

            bus.doBusWork();
            assertEquals(1, bus.craftingRequests);
            assertSame(bus.config.getAEStackInSlot(0), bus.requestedItem);
        }
    }

    @Test
    public void filterCardsRejectCraftingForNonmatchingOrMissingConfiguredItems() {
        for (final Upgrades card : new Upgrades[] { Upgrades.ORE_FILTER, Upgrades.NBT_FILTER }) {
            final TestBus bus = new TestBus(card);
            bus.rejectItems = true;
            bus.doBusWork();
            assertEquals(0, bus.craftingRequests);

            bus.rejectItems = false;
            bus.config.putAEStackInSlot(0, null);
            bus.doBusWork();
            assertEquals(0, bus.craftingRequests);
        }
    }

    @Test
    public void craftOnlyWithFilterCardsRequestsCraftingWithoutExportingStock() {
        for (final Upgrades card : new Upgrades[] { Upgrades.ORE_FILTER, Upgrades.NBT_FILTER }) {
            final TestBus bus = new TestBus(card);
            bus.getConfigManager().putSetting(Settings.CRAFT_ONLY, YesNo.YES);
            bus.stock = 1;
            bus.doBusWork();
            assertEquals(0, bus.exported);
            assertEquals(1, bus.craftingRequests);
        }
    }

    private static final class TestBus extends PartExportBus {

        private final IAEStackInventory config = new IAEStackInventory(null, 1);
        private final Upgrades card;
        private final AENetworkProxy network;
        private int stock;
        private int exported;
        private int craftingRequests;
        private IAEItemStack requestedItem;
        private boolean rejectItems;

        private TestBus(final Upgrades card) {
            super(new ItemStack(new Item()));
            this.card = card;
            final ItemStack stack = new ItemStack(new Item());
            final IAEItemStack item = (IAEItemStack) Proxy.newProxyInstance(
                    IAEItemStack.class.getClassLoader(),
                    new Class<?>[] { IAEItemStack.class },
                    (proxy, method, args) -> switch (method.getName()) {
                    case "copy", "setStackSize" -> proxy;
                    case "getItemStack" -> stack;
                    default -> throw new UnsupportedOperationException(method.getName());
                    });
            this.config.putAEStackInSlot(0, item);
            final ICraftingGrid crafting = (ICraftingGrid) Proxy.newProxyInstance(
                    ICraftingGrid.class.getClassLoader(),
                    new Class<?>[] { ICraftingGrid.class },
                    (proxy, method, args) -> {
                        if (!method.getName().equals("beginCraftingJob"))
                            throw new UnsupportedOperationException(method.getName());
                        this.craftingRequests++;
                        this.requestedItem = (IAEItemStack) args[3];
                        return null;
                    });
            this.network = new AENetworkProxy(this, "test", null, false) {

                @Override
                public boolean isActive() {
                    return true;
                }

                @Override
                public IEnergyGrid getEnergy() {
                    return null;
                }

                @Override
                public ICraftingGrid getCrafting() {
                    return crafting;
                }

                @Override
                public IGrid getGrid() {
                    return null;
                }
            };
        }

        @Override
        public IGridNode getGridNode() {
            return null;
        }

        @Override
        public AENetworkProxy getProxy() {
            return this.network == null ? super.getProxy() : this.network;
        }

        @Override
        public TileEntity getTile() {
            return new TileEntity();
        }

        @Override
        public int getInstalledUpgrades(final Upgrades upgrade) {
            return upgrade == this.card || upgrade == Upgrades.CRAFTING ? 1 : 0;
        }

        @Override
        public void updateSetting(final IConfigManager manager, final Enum settingName, final Enum newValue) {}

        @Override
        protected boolean canDoBusWork() {
            return true;
        }

        @Override
        protected Object getTarget() {
            return this;
        }

        @Override
        protected IMEMonitor<IAEItemStack> getMonitor() {
            return null;
        }

        @Override
        public IAEStackInventory getAEInventoryByName(final StorageName name) {
            return this.config;
        }

        @Override
        protected CombinedItemFilter createItemFilter() {
            return new CombinedItemFilter(null, null, null, this.rejectItems ? new NBTFilterConfig() : null);
        }

        @Override
        protected boolean canInjectStackToTarget(final IAEItemStack item) {
            return item != null;
        }

        @Override
        protected void doOreDict(final IEnergyGrid energy, final IMEMonitor<IAEItemStack> gridInv) {
            final int sent = (int) Math.min(this.stock, this.itemToSend);
            this.stock -= sent;
            this.exported += sent;
            this.itemToSend -= sent;
        }
    }
}
