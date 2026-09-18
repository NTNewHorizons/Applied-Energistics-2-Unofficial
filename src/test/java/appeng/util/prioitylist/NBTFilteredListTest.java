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

public class NBTFilteredListTest {

    @Test
    public void preciseItemFilterIgnoresFilterStackNbtButEnforcesRules() {
        final Item item = new Item();
        final IAEItemStack filter = stack(item, 0, 99);
        final NBTFilteredList list = new NBTFilteredList(rules(), Arrays.asList(filter), null);
        assertTrue(list.isListed(stack(item, 0, 10)));
        assertFalse(list.isListed(stack(item, 0, 1)));
        assertFalse(list.isListed(stack(new Item(), 0, 10)));
        assertFalse(list.isListed(null));
    }

    @Test
    public void fuzzyItemFilterStillEnforcesNbtRules() {
        final Item item = new Item();
        final IAEItemStack filter = stack(item, 0, 99);
        final NBTFilteredList list = new NBTFilteredList(rules(), Arrays.asList(filter), FuzzyMode.IGNORE_ALL);
        assertTrue(list.isListed(stack(item, 10, 10)));
        assertFalse(list.isListed(stack(item, 10, 1)));
    }

    @Test
    public void emptyItemPartitionAllowsNbtOnlyFiltering() {
        final NBTFilteredList list = new NBTFilteredList(rules(), Collections.emptyList(), null);
        assertTrue(list.isListed(stack(new Item(), 0, 10)));
        assertFalse(list.isListed(stack(new Item(), 0, 1)));
        assertFalse(list.isEmpty());
    }

    @Test
    public void emptyNbtRulesRejectAllItems() {
        final Item item = new Item();
        final IAEItemStack candidate = stack(item, 0, 1);
        final NBTFilteredList withoutItems = new NBTFilteredList(
                new NBTFilterConfig(),
                Collections.emptyList(),
                null);
        final NBTFilteredList withItems = new NBTFilteredList(
                new NBTFilterConfig(),
                Arrays.asList(stack(item, 0, 99)),
                null);

        assertFalse(withoutItems.isEmpty());
        assertFalse(withItems.isEmpty());
        assertFalse(withoutItems.isListed(candidate));
        assertFalse(withItems.isListed(candidate));
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
