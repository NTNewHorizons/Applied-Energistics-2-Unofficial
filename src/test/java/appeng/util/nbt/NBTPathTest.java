package appeng.util.nbt;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;

import org.junit.Test;

public class NBTPathTest {

    @Test
    public void preservesCompoundPathsAndListIndices() {
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
        assertSame(entry.getTag("count"), NBTPath.compile("inventory.0.count").resolve(root));
        assertNull(NBTPath.compile("inventory.1.count").resolve(root));
        assertNull(NBTPath.compile("inventory.\"0\".count").resolve(root));
    }

    @Test
    public void quotedSegmentsSelectLiteralCompoundKeys() {
        final NBTTagCompound root = new NBTTagCompound();
        final NBTTagCompound nested = new NBTTagCompound();
        final NBTTagInt value = new NBTTagInt(7);
        nested.setTag("0", value);
        root.setTag("stats.damage", nested);
        root.setTag("a\\b\"c", value);

        assertSame(value, NBTPath.compile("\"stats.damage\".\"0\"").resolve(root));
        assertSame(value, NBTPath.compile("\"a\\\\b\\\"c\"").resolve(root));
        // Bare backslashes retain their previous literal meaning.
        root.setTag("a\\b", value);
        assertSame(value, NBTPath.compile("a\\b").resolve(root));
    }

    @Test
    public void rejectsMalformedPaths() {
        for (final String path : new String[] { "", " ", ".a", "a.", "a..b", "\"a", "\"a\"b", "\"a\\", "\"a\\q\"",
                "\"a\".", "2147483648" }) {
            assertNull(path, NBTPath.compile(path));
        }
    }
}
