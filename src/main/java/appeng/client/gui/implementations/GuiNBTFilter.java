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

    private static final int MAX_VISIBLE_FILTERS = 999;
    private static final int ROW_TOP = 33;
    private static final int ROW_HEIGHT = 14;
    private static final int ROW_CONTROL_HEIGHT = 12;
    private static final int ROW_BOTTOM_Y = ROW_TOP + ROW_HEIGHT;
    private static final int TEXTURE_HEIGHT = 81;
    private static final int FOOTER_HEIGHT = TEXTURE_HEIGHT - ROW_BOTTOM_Y;
    private static final int PADDING = 2;
    private static final int BORDER_SIZE = 10;
    private static final int PATH_FIELD_WIDTH = 100;
    private static final int COMPARISON_FIELD_WIDTH = 48;
    private static final int VALUE_FIELD_WIDTH = 70;
    private static final int DELETE_BUTTON_WIDTH = ROW_CONTROL_HEIGHT;

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
        this.updateGuiGeometry();
        super.initGui();
        this.rebuildControls();
    }

    private void rebuildControls() {
        this.updateGuiGeometry();
        this.buttonList.removeIf(button -> button != this.originalGuiBtn);
        this.rows.clear();

        if (this.originalGuiBtn != null) {
            this.originalGuiBtn.xPosition = this.guiLeft + this.xSize - 22;
            this.originalGuiBtn.yPosition = this.guiTop;
        }

        final NBTFilterConfig config = this.containerNBTFilter.getConfig();
        this.modeButton = new GuiButtonExt(0, this.guiLeft + 174, this.guiTop + 5, 52, 20, config.getMode().name());
        this.buttonList.add(this.modeButton);

        final int visibleRows = Math.min(config.getFilters().size(), MAX_VISIBLE_FILTERS);
        for (int i = 0; i < visibleRows; i++) this.addRowControls(i, config.getFilters().get(i));

        this.addButton = new GuiButtonExt(
                100,
                this.guiLeft + BORDER_SIZE,
                this.guiTop + ROW_TOP + visibleRows * ROW_HEIGHT + PADDING,
                this.xSize - BORDER_SIZE * 2,
                20,
                "Add Filter");
        this.addButton.enabled = config.getFilters().size() < MAX_VISIBLE_FILTERS;
        this.buttonList.add(this.addButton);
        this.observedRevision = this.containerNBTFilter.getRevision();
        this.controlsDirty = false;
    }

    private void updateGuiGeometry() {
        final int visibleRows = Math.min(this.containerNBTFilter.getConfig().getFilters().size(), MAX_VISIBLE_FILTERS);
        this.ySize = ROW_TOP + visibleRows * ROW_HEIGHT + FOOTER_HEIGHT;
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;
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
    public void drawScreen(final int mouseX, final int mouseY, final float partialTicks) {
        if (this.controlsDirty || this.observedRevision != this.containerNBTFilter.getRevision()) {
            this.rebuildControls();
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void drawFG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        this.fontRendererObj.drawString("NBT Filter", 10, 10, ColorUtils.guiTextColorGray.getColor());

        // Draw field placeholders
        for (final FilterRow row : this.rows) {
            if (row.pathField.getText().isEmpty() && !row.pathField.isFocused()) {
                this.fontRendererObj.drawString(
                        "NBT path",
                        row.pathField.x - guiLeft + 2,
                        ROW_TOP + row.index * ROW_HEIGHT + PADDING + 2,
                        0x777777);
            }
            if (row.valueField.getText().isEmpty() && !row.valueField.isFocused()
                    && row.entry.getComparison().requiresValue()) {
                this.fontRendererObj.drawString(
                        "value",
                        row.valueField.x - guiLeft + 2,
                        ROW_TOP + row.index * ROW_HEIGHT + PADDING + 2,
                        0x777777);
            }
        }
    }

    @Override
    public void drawBG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        this.bindTexture("guis/nbtFilter.png");
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, ROW_TOP);

        int y = ROW_TOP;
        for (int row = 0; row < this.rows.size(); row++) {
            this.drawTexturedModalRect(offsetX, offsetY + y, 0, ROW_TOP, this.xSize, ROW_HEIGHT);
            y += ROW_HEIGHT;
        }

        this.drawTexturedModalRect(offsetX, offsetY + y, 0, ROW_BOTTOM_Y, this.xSize, TEXTURE_HEIGHT - ROW_BOTTOM_Y);
        for (final FilterRow row : this.rows) {
            row.pathField.drawTextBox();
            if (row.entry.getComparison().requiresValue()) row.valueField.drawTextBox();
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
            this.pathField = new MEGuiTextField(PATH_FIELD_WIDTH, ROW_CONTROL_HEIGHT) {

                @Override
                public void onTextChange(final String oldText) {
                    FilterRow.this.entry.setPath(this.getText());
                    containerNBTFilter.getConfig().rebuildCompiledFilters();
                    configurationChanged();
                }
            };
            this.pathField.x = guiLeft + BORDER_SIZE;
            this.pathField.y = y + PADDING;
            this.pathField.setMaxStringLength(256);
            this.pathField.setText(entry.getPath(), true);

            this.comparisonButton = new GuiButtonExt(
                    200 + index,
                    this.pathField.x + PADDING + PATH_FIELD_WIDTH,
                    y + PADDING,
                    COMPARISON_FIELD_WIDTH,
                    ROW_CONTROL_HEIGHT,
                    entry.getComparison().getDisplayName());

            this.valueField = new MEGuiTextField(VALUE_FIELD_WIDTH, ROW_CONTROL_HEIGHT) {

                @Override
                public void onTextChange(final String oldText) {
                    FilterRow.this.entry.setValue(this.getText());
                    containerNBTFilter.getConfig().rebuildCompiledFilters();
                    configurationChanged();
                }
            };
            this.valueField.x = this.comparisonButton.xPosition + PADDING + COMPARISON_FIELD_WIDTH;
            this.valueField.y = y + PADDING;
            this.valueField.setMaxStringLength(1024);
            this.valueField.setText(entry.getValue(), true);
            this.valueField.setEnabled(entry.getComparison().requiresValue());

            this.deleteButton = new GuiButtonExt(
                    300 + index,
                    this.valueField.x + PADDING + VALUE_FIELD_WIDTH,
                    y + PADDING,
                    DELETE_BUTTON_WIDTH,
                    ROW_CONTROL_HEIGHT,
                    "X");
        }
    }
}
