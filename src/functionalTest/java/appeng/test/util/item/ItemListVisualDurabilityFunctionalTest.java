package appeng.test.util.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

import appeng.api.config.FuzzyMode;
import appeng.api.storage.data.IAEItemStack;
import appeng.util.item.AEItemStack;
import appeng.util.item.ItemList;

public class ItemListVisualDurabilityFunctionalTest {

    @Test
    public void visualLookupAgreesWithDirectComparisonAcrossMetadata() {
        final Item item = new Item() {

            @Override
            public boolean showDurabilityBar(final ItemStack stack) {
                return getDurabilityForDisplay(stack) > 0;
            }

            @Override
            public double getDurabilityForDisplay(final ItemStack stack) {
                return stack.getTagCompound().getDouble("visualDamage");
            }

            @Override
            public int getDisplayDamage(final ItemStack stack) {
                return (int) (getDurabilityForDisplay(stack) * 100);
            }
        };
        assertFalse(item.isDamageable());

        final AEItemStack filter = stack(item, 0, 0.8);
        final AEItemStack matching = stack(item, 1, 0.9);
        final AEItemStack otherSide = stack(item, 1, 0.2);
        final ItemList items = new ItemList();
        items.addStorage(matching);
        items.addStorage(otherSide);

        assertTrue(filter.fuzzyComparison(matching, FuzzyMode.PERCENT_50_VISUAL));
        assertFalse(filter.fuzzyComparison(otherSide, FuzzyMode.PERCENT_50_VISUAL));
        for (final FuzzyMode mode : FuzzyMode.values()) {
            final Collection<IAEItemStack> matches = items.findFuzzy(filter, mode);
            assertEquals(filter.fuzzyComparison(matching, mode), matches.contains(matching), mode.name());
            assertEquals(filter.fuzzyComparison(otherSide, mode), matches.contains(otherSide), mode.name());
        }
    }

    private static AEItemStack stack(final Item item, final int metadata, final double visualDamage) {
        final ItemStack stack = new ItemStack(item, 1, metadata);
        final NBTTagCompound tag = new NBTTagCompound();
        tag.setDouble("visualDamage", visualDamage);
        stack.setTagCompound(tag);
        return AEItemStack.create(stack);
    }
}
