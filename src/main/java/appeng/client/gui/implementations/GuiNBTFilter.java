package appeng.client.gui.implementations;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;

import org.lwjgl.input.Mouse;

import appeng.client.gui.GuiSub;
import appeng.client.gui.widgets.MEGuiTextField;
import appeng.container.implementations.ContainerNBTFilter;
import appeng.core.localization.ColorUtils;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketNBTFilterConfig;
import appeng.helpers.INBTFilterable;
import appeng.util.nbt.NBTFilterConfig;
import appeng.util.nbt.NBTFilterEntry;
import cpw.mods.fml.client.config.GuiButtonExt;

public class GuiNBTFilter extends GuiSub {

    private static final int MAX_VISIBLE_FILTERS = 5;
    private static final int ROW_TOP = 31;
    private static final int ROW_HEIGHT = 14;
    private static final int ROW_CONTROL_OFFSET = 4;
    private static final int ROW_CONTROL_HEIGHT = 12;
    private static final int TEXTURE_TOP_HEIGHT = 33;
    private static final int TEXTURE_ROW_HEIGHT = 14;
    private static final int TEXTURE_BOTTOM_Y = TEXTURE_TOP_HEIGHT + TEXTURE_ROW_HEIGHT;
    private static final int TEXTURE_HEIGHT = 61;

    private final ContainerNBTFilter containerNBTFilter;
    private final List<FilterRow> rows = new ArrayList<>();
    private GuiButton modeButton;
    private GuiButton addButton;
    private int observedRevision = -1;
    private boolean controlsDirty;

    public GuiNBTFilter(final InventoryPlayer ip, final INBTFilterable host) {
        super(new ContainerNBTFilter(ip, host));
        this.containerNBTFilter = (ContainerNBTFilter) this.inventorySlots;
        this.xSize = 256;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.rebuildControls();
    }

    private void rebuildControls() {
        this.buttonList.removeIf(button -> button != this.originalGuiBtn);
        this.rows.clear();

        final NBTFilterConfig config = this.containerNBTFilter.getConfig();
        this.modeButton = new GuiButton(0, this.guiLeft + 174, this.guiTop + 5, 52, 20, config.getMode().name());
        this.buttonList.add(this.modeButton);

        final int visibleRows = Math.min(config.getFilters().size(), MAX_VISIBLE_FILTERS);
        for (int i = 0; i < visibleRows; i++) this.addRowControls(i, config.getFilters().get(i));

        this.addButton = new GuiButton(100, this.guiLeft + 85, this.guiTop + 145, 86, 20, "+ Add Filter");
        this.addButton.enabled = config.getFilters().size() < MAX_VISIBLE_FILTERS;
        this.buttonList.add(this.addButton);
        this.observedRevision = this.containerNBTFilter.getRevision();
        this.controlsDirty = false;
    }

    private void addRowControls(final int index, final NBTFilterEntry entry) {
        final int y = this.guiTop + ROW_TOP + index * ROW_HEIGHT;
        final FilterRow row = new FilterRow(index, y, entry);
        this.rows.add(row);
        this.buttonList.add(row.comparisonButton);
        this.buttonList.add(row.deleteButton);
    }

    private void configurationChanged() {
        NetworkHandler.instance.sendToServer(new PacketNBTFilterConfig(this.containerNBTFilter.getConfig()));
    }

    @Override
    public void drawFG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        this.fontRendererObj.drawString("NBT Filter", 10, 10, ColorUtils.guiTextColorGray.getColor());
        for (final FilterRow row : this.rows) {
            if (row.pathField.getText().isEmpty() && !row.pathField.isFocused()) {
                this.fontRendererObj.drawString("NBT path", 13, ROW_TOP + row.index * ROW_HEIGHT + 6, 0x777777);
            }
            if (row.valueField.getText().isEmpty() && !row.valueField.isFocused()
                    && row.entry.getComparison().requiresValue()) {
                this.fontRendererObj.drawString("value", 165, ROW_TOP + row.index * ROW_HEIGHT + 6, 0x777777);
            }
        }
    }

    @Override
    public void drawBG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        if (this.controlsDirty || this.observedRevision != this.containerNBTFilter.getRevision()) {
            this.rebuildControls();
        }
        this.bindTexture("guis/nbtFilter.png");
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, TEXTURE_TOP_HEIGHT);

        int y = TEXTURE_TOP_HEIGHT;
        for (int row = 0; row < this.rows.size(); row++) {
            this.drawTexturedModalRect(offsetX, offsetY + y, 0, TEXTURE_TOP_HEIGHT, this.xSize, TEXTURE_ROW_HEIGHT);
            y += TEXTURE_ROW_HEIGHT;
        }

        this.drawTexturedModalRect(
                offsetX,
                offsetY + y,
                0,
                TEXTURE_BOTTOM_Y,
                this.xSize,
                TEXTURE_HEIGHT - TEXTURE_BOTTOM_Y);
        for (final FilterRow row : this.rows) {
            row.pathField.drawTextBox();
            row.valueField.drawTextBox();
        }
    }

    @Override
    protected void actionPerformed(final GuiButton button) {
        super.actionPerformed(button);
        final boolean backwards = Mouse.isButtonDown(1);
        final NBTFilterConfig config = this.containerNBTFilter.getConfig();

        if (button == this.modeButton) {
            config.setMode(config.getMode().rotate(backwards));
            this.modeButton.displayString = config.getMode().name();
            this.configurationChanged();
        } else if (button == this.addButton) {
            config.addFilter(new NBTFilterEntry());
            // GuiScreen is still iterating buttonList here. Replacing the controls immediately would make the
            // newly-created add button receive this same click again.
            this.addButton.enabled = false;
            this.controlsDirty = true;
            this.configurationChanged();
        } else {
            for (final FilterRow row : this.rows) {
                if (button == row.comparisonButton) {
                    row.entry.setComparison(row.entry.getComparison().rotate(backwards));
                    row.comparisonButton.displayString = row.entry.getComparison().getDisplayName();
                    row.valueField.setEnabled(row.entry.getComparison().requiresValue());
                    config.rebuildCompiledFilters();
                    this.configurationChanged();
                    return;
                }
                if (button == row.deleteButton) {
                    config.removeFilter(row.index);
                    row.deleteButton.enabled = false;
                    this.controlsDirty = true;
                    this.configurationChanged();
                    return;
                }
            }
        }
    }

    @Override
    protected void mouseClicked(final int x, final int y, final int button) {
        for (final FilterRow row : this.rows) {
            row.pathField.mouseClicked(x, y, button);
            if (row.entry.getComparison().requiresValue()) row.valueField.mouseClicked(x, y, button);
        }
        super.mouseClicked(x, y, button);
    }

    @Override
    protected void keyTyped(final char character, final int key) {
        for (final FilterRow row : this.rows) {
            if (row.pathField.textboxKeyTyped(character, key) || row.valueField.textboxKeyTyped(character, key)) {
                return;
            }
        }
        super.keyTyped(character, key);
    }

    private final class FilterRow {

        private final int index;
        private final NBTFilterEntry entry;
        private final MEGuiTextField pathField;
        private final GuiButton comparisonButton;
        private final MEGuiTextField valueField;
        private final GuiButton deleteButton;

        private FilterRow(final int index, final int y, final NBTFilterEntry entry) {
            this.index = index;
            this.entry = entry;
            this.pathField = new MEGuiTextField(92, ROW_CONTROL_HEIGHT) {

                @Override
                public void onTextChange(final String oldText) {
                    FilterRow.this.entry.setPath(this.getText());
                    containerNBTFilter.getConfig().rebuildCompiledFilters();
                    configurationChanged();
                }
            };
            this.pathField.x = guiLeft + 10;
            this.pathField.y = y + ROW_CONTROL_OFFSET;
            this.pathField.setMaxStringLength(256);
            this.pathField.setText(entry.getPath(), true);

            this.comparisonButton = new GuiButtonExt(
                    200 + index,
                    guiLeft + 105,
                    y + ROW_CONTROL_OFFSET,
                    51,
                    ROW_CONTROL_HEIGHT,
                    entry.getComparison().getDisplayName());

            this.valueField = new MEGuiTextField(62, ROW_CONTROL_HEIGHT) {

                @Override
                public void onTextChange(final String oldText) {
                    FilterRow.this.entry.setValue(this.getText());
                    containerNBTFilter.getConfig().rebuildCompiledFilters();
                    configurationChanged();
                }
            };
            this.valueField.x = guiLeft + 159;
            this.valueField.y = y + ROW_CONTROL_OFFSET;
            this.valueField.setMaxStringLength(1024);
            this.valueField.setText(entry.getValue(), true);
            this.valueField.setEnabled(entry.getComparison().requiresValue());

            this.deleteButton = new GuiButtonExt(
                    300 + index,
                    guiLeft + 224,
                    y + ROW_CONTROL_OFFSET,
                    21,
                    ROW_CONTROL_HEIGHT,
                    "X");
        }
    }
}
