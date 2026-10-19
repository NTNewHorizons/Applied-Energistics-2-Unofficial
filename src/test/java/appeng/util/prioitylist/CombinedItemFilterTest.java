package appeng.util.prioitylist;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

import appeng.api.config.FuzzyMode;
import appeng.api.storage.data.IAEItemStack;
import appeng.util.nbt.NBTComparison;
import appeng.util.nbt.NBTFilterConfig;
import appeng.util.nbt.NBTFilterEntry;

public class CombinedItemFilterTest {

    @Test
    public void allInstalledFiltersAreCombinedWithAnd() {
        final Item item = new Item().setMaxDamage(100);

        final CombinedItemFilter filter = new CombinedItemFilter(
                Arrays.asList(stack(item, 0, 99)),
                FuzzyMode.IGNORE_ALL,
                true,
                candidate -> candidate.getItem() == item,
                rules());

        assertTrue(filter.isListed(stack(item, 10, 10)));
        assertFalse(filter.isListed(stack(item, 10, 1)));
        assertFalse(filter.isListed(stack(new Item(), 10, 10)));
    }

    @Test
    public void fuzzyWithoutConfiguredItemsAddsNoRestriction() {
        final CombinedItemFilter filter = new CombinedItemFilter(
                Collections.emptyList(),
                FuzzyMode.IGNORE_ALL,
                null,
                null);

        assertTrue(filter.isEmpty());
        assertTrue(filter.isListed(stack(new Item(), 10, 1)));
    }

    @Test
    public void installedEmptyOreFilterRejectsEverything() {
        final CombinedItemFilter filter = new CombinedItemFilter(Collections.emptyList(), null, "", null);

        assertFalse(filter.isEmpty());
        assertFalse(filter.isListed(stack(new Item(), 0, 1)));
    }

    @Test
    public void installedEmptyNbtFilterPreservesExistingRejectAllBehavior() {
        final CombinedItemFilter filter = new CombinedItemFilter(
                Collections.emptyList(),
                null,
                null,
                new NBTFilterConfig());

        assertFalse(filter.isEmpty());
        assertFalse(filter.isListed(stack(new Item(), 0, 1)));
    }

    private static NBTFilterConfig rules() {
        final NBTFilterConfig config = new NBTFilterConfig();
        config.addFilter(new NBTFilterEntry("level", NBTComparison.GREATER_THAN, "5"));
        return config;
    }

    private static IAEItemStack stack(final Item item, final int damage, final int level) {
        final ItemStack stack = new ItemStack(item, 1, damage);
        final NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("level", level);
        stack.setTagCompound(tag);
        final IAEItemStack[] self = new IAEItemStack[1];
        self[0] = (IAEItemStack) Proxy.newProxyInstance(
                IAEItemStack.class.getClassLoader(),
                new Class<?>[] { IAEItemStack.class },
                (proxy, method, args) -> switch (method.getName()) {
                case "getItemStack" -> stack;
                case "getItem" -> item;
                case "getItemDamage" -> damage;
                case "copy" -> self[0];
                case "fuzzyComparison" -> args[0] instanceof IAEItemStack candidate && candidate.getItem() == item;
                default -> throw new UnsupportedOperationException(method.getName());
                });
        return self[0];
    }
}
