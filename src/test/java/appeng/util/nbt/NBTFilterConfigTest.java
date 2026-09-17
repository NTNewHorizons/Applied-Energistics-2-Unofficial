package appeng.util.nbt;

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

        final NBTFilterConfig config = config("ench.0.lvl", NBTComparison.GREATER_THAN_OR_EQUAL, "5");
        assertTrue(config.matches(root));
        root.setTag("ench", new NBTTagList());
        assertFalse(config.matches(root));
    }

    @Test
    public void combinesCompiledEntriesUsingGlobalMode() {
        final NBTTagCompound root = new NBTTagCompound();
        root.setString("name", "wrench");
        root.setInteger("damage", 500);

        final NBTFilterConfig config = config("name", NBTComparison.EQUALS, "wrench");
        config.addFilter(new NBTFilterEntry("damage", NBTComparison.GREATER_THAN, "900"));
        assertFalse(config.matches(root));
        config.setMode(NBTFilterMode.ANY);
        assertTrue(config.matches(root));
    }

    @Test
    public void existenceChecksDoNotRequireValues() {
        final NBTTagCompound root = new NBTTagCompound();
        root.setTag("present", new NBTTagString(""));
        assertTrue(config("present", NBTComparison.EXISTS, "").matches(root));
        assertTrue(config("missing", NBTComparison.NOT_EXISTS, "").matches(root));
    }

    @Test
    public void orderingNeverFallsBackToLexicographicStrings() {
        final NBTTagCompound root = new NBTTagCompound();
        root.setString("number", "10");
        assertFalse(config("number", NBTComparison.GREATER_THAN, "2").matches(root));
    }

    @Test
    public void invalidAndIncompleteRowsAreExcludedWithoutThrowing() {
        final NBTTagCompound root = new NBTTagCompound();
        root.setTag("value", new NBTTagInt(1));
        final NBTFilterConfig config = config("bad..path", NBTComparison.EQUALS, "1");
        config.addFilter(new NBTFilterEntry("value", NBTComparison.EQUALS, ""));
        assertTrue(config.matches(root));
    }

    @Test
    public void structuredNbtRoundTripRebuildsCompiledFilters() {
        final NBTFilterConfig original = config("stats.damage", NBTComparison.LESS_THAN_OR_EQUAL, "12");
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
    }

    private static NBTFilterConfig config(final String path, final NBTComparison comparison, final String value) {
        final NBTFilterConfig config = new NBTFilterConfig();
        config.addFilter(new NBTFilterEntry(path, comparison, value));
        return config;
    }
}
