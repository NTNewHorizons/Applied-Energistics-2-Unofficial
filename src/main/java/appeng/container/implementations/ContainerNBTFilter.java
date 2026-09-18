package appeng.container.implementations;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;

import appeng.container.ContainerSubGui;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketNBTFilterConfig;
import appeng.helpers.INBTFilterable;
import appeng.util.Platform;
import appeng.util.nbt.NBTFilterConfig;

public class ContainerNBTFilter extends ContainerSubGui {

    private final INBTFilterable filterHost;
    private final NBTFilterConfig config;
    private boolean initialConfigSent;
    private int revision;

    public ContainerNBTFilter(final InventoryPlayer ip, final INBTFilterable filterHost) {
        super(ip, filterHost);
        this.filterHost = filterHost;
        this.config = filterHost.getNBTFilterConfig().copy();
    }

    public NBTFilterConfig getConfig() {
        return this.config;
    }

    public int getRevision() {
        return this.revision;
    }

    public void setConfig(final NBTFilterConfig config) {
        this.config.replaceWith(config);
        this.revision++;
        if (Platform.isServer()) this.filterHost.setNBTFilterConfig(this.config.copy());
    }

    @Override
    public void detectAndSendChanges() {
        if (Platform.isServer() && !this.initialConfigSent) {
            this.initialConfigSent = true;
            NetworkHandler.instance.sendTo(
                    new PacketNBTFilterConfig(this.filterHost.getNBTFilterConfig()),
                    (EntityPlayerMP) this.getInventoryPlayer().player);
        }
        super.detectAndSendChanges();
    }
}
