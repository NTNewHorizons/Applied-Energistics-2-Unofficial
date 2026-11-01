package appeng.util.nbt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

import org.junit.Test;

public class NBTFilterConfigTest {

    @Test
    public void resolvesNestedCompoundsAndListIndices() {
        final NBTTagCompound root = new NBTTagCompound();
        final NBTTagList enchantments = new NBTTagList();
        final NBTTagCompound enchantment = new NBTTagCompound();
        enchantment.setInteger("lvl", 5);
        enchantments.appendTag(enchantment);
        root.setTag("ench", enchantments);

        final NBTFilterConfig config = config("ench[0].lvl", NBTComparison.GREATER_THAN_OR_EQUAL, "5");
        assertTrue(config.matches(root));
        assertFalse(config("ench.0.lvl", NBTComparison.NOT_EXISTS, "").matches(root));
        root.setTag("ench", new NBTTagList());
        assertFalse(config.matches(root));
    }

    @Test
    public void existenceChecksDoNotRequireValues() {
        final NBTTagCompound root = new NBTTagCompound();
        root.setTag("present", new NBTTagString(""));
        assertTrue(config("present", NBTComparison.EXISTS, "").matches(root));
        assertFalse(config("missing", NBTComparison.EXISTS, "").matches(root));
        assertTrue(config("missing", NBTComparison.NOT_EXISTS, "").matches(root));
        assertFalse(config("present", NBTComparison.NOT_EXISTS, "").matches(root));
    }

    @Test
    public void orderingNeverFallsBackToLexicographicStrings() {
        final NBTTagCompound root = new NBTTagCompound();
        root.setString("number", "9");
        assertFalse(config("number", NBTComparison.GREATER_THAN, "2").matches(root));
        assertFalse(config("number", NBTComparison.GREATER_THAN, "\"2\"").matches(root));
    }

    @Test
    public void invalidAndIncompleteRowsFailAllMode() {
        final NBTTagCompound root = new NBTTagCompound();
        root.setTag("value", new NBTTagInt(1));
        final NBTFilterConfig config = config("value", NBTComparison.EQUALS, "1");
        config.addFilter(new NBTFilterEntry("bad..path", NBTComparison.EQUALS, "1"));
        assertFalse(config.matches(root));
        config.updateFilter(1, "value", NBTComparison.EQUALS, "");
        assertFalse(config.matches(root));
    }

    @Test
    public void invalidRowsAreFalseButAnyModeCanMatchAnotherRow() {
        final NBTTagCompound root = new NBTTagCompound();
        root.setTag("value", new NBTTagInt(1));
        final NBTFilterConfig config = config("bad..path", NBTComparison.EQUALS, "1");
        config.addFilter(new NBTFilterEntry("value", NBTComparison.EQUALS, "1"));
        config.setMode(NBTFilterMode.ANY);
        assertTrue(config.matches(root));

        config.updateFilter(1, "value", NBTComparison.EQUALS, "2");
        assertFalse(config.matches(root));
    }

    @Test
    public void structuredNbtRoundTripRebuildsCompiledFilters() {
        final NBTFilterConfig original = config("stats.damage", NBTComparison.LESS_THAN_OR_EQUAL, "12");
        original.addFilter(new NBTFilterEntry("name", NBTComparison.EQUALS, "wrench"));
        original.setMode(NBTFilterMode.ANY);
        final NBTTagCompound serialized = new NBTTagCompound();
        original.writeToNBT(serialized);

        final NBTFilterConfig restored = new NBTFilterConfig();
        restored.readFromNBT(serialized);
        final NBTTagCompound stats = new NBTTagCompound();
        stats.setInteger("damage", 10);
        final NBTTagCompound root = new NBTTagCompound();
        root.setTag("stats", stats);
        assertTrue(restored.matches(root));
        stats.setInteger("damage", 13);
        assertFalse(restored.matches(root));
        root.setString("name", "wrench");
        assertTrue(restored.matches(root));
    }

    @Test
    public void generatesEqualityFiltersFromNestedNbt() {
        final NBTTagCompound stats = new NBTTagCompound();
        stats.setLong("energy", 9_007_199_254_740_993L);
        final NBTTagList names = new NBTTagList();
        names.appendTag(new NBTTagString("first"));
        names.appendTag(new NBTTagString("second"));
        final NBTTagCompound root = new NBTTagCompound();
        root.setTag("stats", stats);
        root.setTag("names", names);
        root.setString("numericString", "123");

        final NBTFilterConfig config = new NBTFilterConfig();
        config.setMode(NBTFilterMode.ANY);
        config.replaceWithNBT(root, 25);

        assertEquals(NBTFilterMode.ALL, config.getMode());
        assertEquals(4, config.getFilters().size());
        assertEquals("names[0]", config.getFilters().get(0).getPath());
        assertEquals("names[1]", config.getFilters().get(1).getPath());
        assertTrue(config.matches(root));

        stats.setLong("energy", 9_007_199_254_740_992L);
        assertFalse(config.matches(root));
    }

    private static NBTFilterConfig config(final String path, final NBTComparison comparison, final String value) {
        final NBTFilterConfig config = new NBTFilterConfig();
        config.addFilter(new NBTFilterEntry(path, comparison, value));
        return config;
    }

    @Test
    public void copyNbtPreservesLiteralKeysInsideCompoundsAndLists() {
        final String[] keys = { "", " ", "0", "001", "2147483648", "a.b", "a\\b", "a\"b", "a.\\\"b", "a[0]", "a]b",
                "ordinary" };
        final NBTTagCompound nested = new NBTTagCompound();
        for (final String key : keys) nested.setInteger(key, 7);
        final NBTTagList list = new NBTTagList();
        list.appendTag(nested.copy());
        final NBTTagCompound root = new NBTTagCompound();
        root.setTag("compound.with.dots", nested);
        root.setTag("list\\name", list);
        root.setTag("empty.compound", new NBTTagCompound());
        root.setTag("empty.list", new NBTTagList());

        final NBTFilterConfig generated = new NBTFilterConfig();
        generated.replaceWithNBT(root, 32);
        assertEquals(keys.length * 2 + 2, generated.getFilters().size());
        assertTrue(generated.matches(root));

        final NBTTagCompound saved = new NBTTagCompound();
        generated.writeToNBT(saved);
        final NBTFilterConfig restored = new NBTFilterConfig();
        restored.readFromNBT(saved);
        assertTrue(restored.matches(root));
        for (final NBTTagCompound compound : new NBTTagCompound[] { nested, list.getCompoundTagAt(0) }) {
            for (final String key : keys) {
                compound.setInteger(key, 8);
                assertFalse(key, restored.matches(root));
                compound.setInteger(key, 7);
            }
        }
    }

    @Test
    public void copyNbtGeneratesConsecutiveListIndices() {
        final NBTTagList inner = new NBTTagList();
        inner.appendTag(new NBTTagInt(3));
        inner.appendTag(new NBTTagInt(7));
        final NBTTagList outer = new NBTTagList();
        outer.appendTag(inner);
        final NBTTagCompound root = new NBTTagCompound();
        root.setTag("items", outer);
        final NBTFilterConfig config = new NBTFilterConfig();

        config.replaceWithNBT(root, 25);

        assertEquals(2, config.getFilters().size());
        assertEquals("items[0][0]", config.getFilters().get(0).getPath());
        assertEquals("items[0][1]", config.getFilters().get(1).getPath());
        assertTrue(config.matches(root));
        inner.tagList.set(1, new NBTTagInt(8));
        assertFalse(config.matches(root));
    }
}
