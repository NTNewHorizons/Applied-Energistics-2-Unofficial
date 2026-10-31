package appeng.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

import appeng.api.config.FuzzyMode;

public class VisualDurabilityFuzzyTest {

    private final Item item = new VisualDurabilityItem();

    @Test
    public void fuzzyComparisonUsesVisualDurability() {
        final ItemStack undamaged = stack(0);
        final ItemStack slightlyDamaged = stack(0.20);
        final ItemStack heavilyDamaged = stack(0.80);
        final ItemStack alsoHeavilyDamaged = stack(0.90);

        assertFalse(FuzzyItemDurability.isComparable(slightlyDamaged, heavilyDamaged, FuzzyMode.PERCENT_50));
        assertTrue(FuzzyItemDurability.isComparable(slightlyDamaged, heavilyDamaged, FuzzyMode.PERCENT_50_VISUAL));
        assertFalse(FuzzyItemDurability.matches(slightlyDamaged, heavilyDamaged, FuzzyMode.PERCENT_50_VISUAL));
        assertTrue(FuzzyItemDurability.matches(heavilyDamaged, alsoHeavilyDamaged, FuzzyMode.PERCENT_50_VISUAL));
        assertTrue(FuzzyItemDurability.matches(slightlyDamaged, heavilyDamaged, FuzzyMode.IGNORE_ALL_VISUAL));
        assertFalse(FuzzyItemDurability.matches(undamaged, slightlyDamaged, FuzzyMode.PERCENT_99_VISUAL));
        assertTrue(FuzzyItemDurability.matches(undamaged, slightlyDamaged, FuzzyMode.PERCENT_75_VISUAL));
    }

    @Test
    public void ordinaryDamageableItemsKeepTheirFuzzyThresholds() {
        final Item damageable = new Item().setMaxDamage(100);
        final ItemStack undamaged = new ItemStack(damageable, 1, 0);
        final ItemStack slightlyDamaged = new ItemStack(damageable, 1, 1);
        final ItemStack heavilyDamaged = new ItemStack(damageable, 1, 80);

        assertTrue(FuzzyItemDurability.isComparable(undamaged, heavilyDamaged, FuzzyMode.PERCENT_50));
        assertTrue(FuzzyItemDurability.matches(undamaged, slightlyDamaged, FuzzyMode.PERCENT_99));
        assertFalse(FuzzyItemDurability.matches(undamaged, slightlyDamaged, FuzzyMode.PERCENT_99_VISUAL));
        assertFalse(FuzzyItemDurability.matches(undamaged, heavilyDamaged, FuzzyMode.PERCENT_50));
    }

    @Test
    public void visualTogglePreservesThresholdAndLegacyModes() {
        assertSame(FuzzyMode.PERCENT_50_VISUAL, FuzzyMode.PERCENT_50.toggleVisualDurability());
        assertSame(FuzzyMode.PERCENT_50, FuzzyMode.PERCENT_50_VISUAL.toggleVisualDurability());
        assertSame(FuzzyMode.PERCENT_25_VISUAL, FuzzyMode.PERCENT_50_VISUAL.rotate(false));
        assertSame(FuzzyMode.IGNORE_ALL, FuzzyMode.PERCENT_1.rotate(false));
        assertSame(FuzzyMode.IGNORE_ALL_VISUAL, FuzzyMode.PERCENT_1_VISUAL.rotate(false));
    }

    private ItemStack stack(final double visualDamage) {
        final ItemStack stack = new ItemStack(this.item);
        final NBTTagCompound tag = new NBTTagCompound();
        tag.setDouble("visualDamage", visualDamage);
        stack.setTagCompound(tag);
        return stack;
    }

    private static final class VisualDurabilityItem extends Item {

        @Override
        public boolean showDurabilityBar(final ItemStack stack) {
            return this.getDurabilityForDisplay(stack) > 0;
        }

        @Override
        public double getDurabilityForDisplay(final ItemStack stack) {
            return stack.hasTagCompound() ? stack.getTagCompound().getDouble("visualDamage") : 0;
        }

        @Override
        public int getDisplayDamage(final ItemStack stack) {
            return (int) (this.getDurabilityForDisplay(stack) * 100);
        }
    }
}
