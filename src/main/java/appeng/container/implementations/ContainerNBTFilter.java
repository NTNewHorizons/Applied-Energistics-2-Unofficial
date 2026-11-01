package appeng.container.implementations;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

import appeng.api.config.Upgrades;
import appeng.api.implementations.tiles.ICellWorkbench;
import appeng.container.ContainerSubGui;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketNBTFilterConfig;
import appeng.helpers.INBTFilterable;
import appeng.util.Platform;
import appeng.util.nbt.NBTFilterConfig;

public class ContainerNBTFilter extends ContainerSubGui {

    private final INBTFilterable filterHost;
    private final NBTFilterConfig config;
    private final ItemStack editingCell;
    private boolean initialConfigSent;
    private int revision;

    public ContainerNBTFilter(final InventoryPlayer ip, final INBTFilterable filterHost) {
        super(ip, filterHost);
        this.filterHost = filterHost;
        this.config = filterHost.getNBTFilterConfig().copy();
        this.editingCell = filterHost instanceof ICellWorkbench workbench
                ? workbench.getInventoryByName("cell").getStackInSlot(0)
                : null;
    }

    public NBTFilterConfig getConfig() {
        return this.config;
    }

    public int getRevision() {
        return this.revision;
    }

    public void setConfig(final NBTFilterConfig config) {
        if (Platform.isServer() && !this.isFilterHostValid()) return;
        this.config.replaceWith(config);
        this.revision++;
        if (Platform.isServer()) this.filterHost.setNBTFilterConfig(this.config.copy());
    }

    @Override
    public void detectAndSendChanges() {
        if (Platform.isServer() && !this.isFilterHostValid()) {
            this.setValidContainer(false);
            return;
        }
        if (Platform.isServer() && !this.initialConfigSent) {
            this.initialConfigSent = true;
            NetworkHandler.instance.sendTo(
                    new PacketNBTFilterConfig(this.filterHost.getNBTFilterConfig()),
                    (EntityPlayerMP) this.getInventoryPlayer().player);
        }
        super.detectAndSendChanges();
    }

    @Override
    public boolean canInteractWith(final EntityPlayer player) {
        return super.canInteractWith(player) && this.isFilterHostValid();
    }

    private boolean isFilterHostValid() {
        if (!(this.filterHost instanceof ICellWorkbench workbench)) return true;
        return workbench.isSame(workbench) && this.editingCell != null
                && workbench.getInventoryByName("cell").getStackInSlot(0) == this.editingCell
                && workbench.getInstalledUpgrades(Upgrades.NBT_FILTER) > 0;
    }

}
