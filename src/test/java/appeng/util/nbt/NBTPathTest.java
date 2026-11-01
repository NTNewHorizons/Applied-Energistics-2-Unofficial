package appeng.util.nbt;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;

import org.junit.Test;

public class NBTPathTest {

    @Test
    public void resolvesCompoundPathsAndBracketedListIndices() {
        final NBTTagCompound root = new NBTTagCompound();
        final NBTTagCompound stats = new NBTTagCompound();
        stats.setInteger("damage", 3);
        root.setTag("stats", stats);
        final NBTTagList inventory = new NBTTagList();
        final NBTTagCompound entry = new NBTTagCompound();
        entry.setInteger("count", 4);
        inventory.appendTag(entry);
        root.setTag("inventory", inventory);

        assertSame(stats.getTag("damage"), NBTPath.compile("stats.damage").resolve(root));
        assertSame(entry.getTag("count"), NBTPath.compile("inventory[0].count").resolve(root));
        assertNull(NBTPath.compile("inventory[1].count").resolve(root));
        assertNull(NBTPath.compile("inventory.\"0\".count").resolve(root));
    }

    @Test
    public void quotedSegmentsSelectLiteralCompoundKeys() {
        final NBTTagCompound root = new NBTTagCompound();
        final NBTTagCompound nested = new NBTTagCompound();
        final NBTTagInt value = new NBTTagInt(7);
        nested.setTag("0", value);
        nested.setTag("fuel.level", value);
        nested.setTag("a\"a", value);
        root.setTag("stats", nested);
        root.setTag("stats.damage", nested);
        root.setTag("a\\b\"c", value);
        root.setTag("a[0]", value);
        root.setTag("a]b", value);

        assertSame(value, NBTPath.compile("\"stats.damage\".\"0\"").resolve(root));
        assertSame(value, NBTPath.compile("\"a\\\\b\\\"c\"").resolve(root));
        assertSame(value, NBTPath.compile("stats.\"fuel.level\"").resolve(root));
        assertSame(value, NBTPath.compile("stats.\"a\\\"a\"").resolve(root));
        assertSame(value, NBTPath.compile("\"a[0]\"").resolve(root));
        assertSame(value, NBTPath.compile("\"a]b\"").resolve(root));
        // Bare backslashes retain their previous literal meaning.
        root.setTag("a\\b", value);
        assertSame(value, NBTPath.compile("a\\b").resolve(root));
    }

    @Test
    public void combinesListIndicesWithQuotedKeysAndNestedLists() {
        final NBTTagCompound root = new NBTTagCompound();
        final NBTTagInt value = new NBTTagInt(7);
        final NBTTagCompound entry = new NBTTagCompound();
        entry.setTag("fuel.level", value);
        final NBTTagList inventory = new NBTTagList();
        inventory.appendTag(entry);
        root.setTag("inventory", inventory);
        final NBTTagList inner = new NBTTagList();
        inner.appendTag(new NBTTagInt(3));
        inner.appendTag(value);
        final NBTTagList items = new NBTTagList();
        items.appendTag(inner);
        root.setTag("items", items);

        assertSame(value, NBTPath.compile("inventory[0].\"fuel.level\"").resolve(root));
        assertSame(value, NBTPath.compile("items[0][1]").resolve(root));
        assertNull(NBTPath.compile("items[0][2]").resolve(root));
        assertNull(NBTPath.compile("items[0][1][0]").resolve(root));
    }

    @Test
    public void rejectsMalformedPaths() {
        for (final String path : new String[] { "", " ", ".a", "a.", "a..b", "\"a", "\"a\"b", "\"a\\", "\"a\\q\"",
                "\"a\".", "0", "2147483648", "inventory.0.count", "items.0.1", "inventory[0].0", "inventory.[0]", "[0]",
                "inventory[]", "inventory[-1]", "inventory[+1]", "inventory[ 0]", "inventory[0 ]", "inventory[0",
                "inventory0]", "inventory[0]count", "inventory[0].", "items[0]..count", "items[0][1",
                "inventory[2147483648]" }) {
            assertNull(path, NBTPath.compile(path));
        }
    }
}
