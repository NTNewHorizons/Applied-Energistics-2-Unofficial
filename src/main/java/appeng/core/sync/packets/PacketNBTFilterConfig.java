package appeng.core.sync.packets;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

import appeng.container.implementations.ContainerNBTFilter;
import appeng.core.sync.AppEngPacket;
import appeng.core.sync.network.INetworkInfo;
import appeng.util.nbt.NBTComparison;
import appeng.util.nbt.NBTFilterConfig;
import appeng.util.nbt.NBTFilterEntry;
import appeng.util.nbt.NBTFilterMode;
import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public final class PacketNBTFilterConfig extends AppEngPacket {

    private static final int MAX_FILTERS = 32;
    private static final int MAX_PATH_LENGTH = 256;
    private static final int MAX_VALUE_LENGTH = 1024;

    private final NBTFilterConfig config = new NBTFilterConfig();

    public PacketNBTFilterConfig(final ByteBuf stream) {
        final int modeOrdinal = stream.readUnsignedByte();
        final NBTFilterMode[] modes = NBTFilterMode.values();
        this.config.setMode(modes[Math.min(modeOrdinal, modes.length - 1)]);

        final int count = Math.min(stream.readUnsignedByte(), MAX_FILTERS);
        final NBTComparison[] comparisons = NBTComparison.values();
        for (int i = 0; i < count; i++) {
            final String path = truncate(ByteBufUtils.readUTF8String(stream), MAX_PATH_LENGTH);
            final int comparisonOrdinal = stream.readUnsignedByte();
            final String value = truncate(ByteBufUtils.readUTF8String(stream), MAX_VALUE_LENGTH);
            this.config.addFilter(
                    new NBTFilterEntry(path, comparisons[Math.min(comparisonOrdinal, comparisons.length - 1)], value));
        }
    }

    public PacketNBTFilterConfig(final NBTFilterConfig config) {
        this.config.replaceWith(config);
        final ByteBuf data = Unpooled.buffer();
        data.writeInt(this.getPacketID());
        data.writeByte(this.config.getMode().ordinal());
        final int count = Math.min(this.config.getFilters().size(), MAX_FILTERS);
        data.writeByte(count);
        for (int i = 0; i < count; i++) {
            final NBTFilterEntry entry = this.config.getFilters().get(i);
            ByteBufUtils.writeUTF8String(data, truncate(entry.getPath(), MAX_PATH_LENGTH));
            data.writeByte(entry.getComparison().ordinal());
            ByteBufUtils.writeUTF8String(data, truncate(entry.getValue(), MAX_VALUE_LENGTH));
        }
        this.configureWrite(data);
    }

    private static String truncate(final String value, final int maximumLength) {
        return value.length() <= maximumLength ? value : value.substring(0, maximumLength);
    }

    @Override
    public void serverPacketData(final INetworkInfo manager, final AppEngPacket packet, final EntityPlayer player) {
        final Container container = player.openContainer;
        if (container instanceof ContainerNBTFilter nbtFilter) nbtFilter.setConfig(this.config);
    }

    @Override
    public void clientPacketData(final INetworkInfo manager, final AppEngPacket packet, final EntityPlayer player) {
        final Container container = player.openContainer;
        if (container instanceof ContainerNBTFilter nbtFilter) nbtFilter.setConfig(this.config);
    }
}
