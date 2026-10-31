package appeng.util.nbt;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

/** A deliberately small, precompiled path made of compound keys and list indices. */
public final class NBTPath {

    private final PathElement[] elements;

    private NBTPath(final PathElement[] elements) {
        this.elements = elements;
    }

    @Nullable
    public static NBTPath compile(final String source) {
        if (source == null || source.trim().isEmpty()) return null;

        final List<PathElement> elements = new ArrayList<>();
        int start = 0;
        for (int i = 0; i <= source.length(); i++) {
            if (i != source.length() && source.charAt(i) != '.') continue;
            if (i == start) return null;

            final String token = source.substring(start, i);
            if (isUnsignedInteger(token)) {
                try {
                    elements.add(new ListIndex(Integer.parseInt(token)));
                } catch (final NumberFormatException ignored) {
                    return null;
                }
            } else {
                elements.add(new CompoundKey(token));
            }
            start = i + 1;
        }
        return elements.isEmpty() ? null : new NBTPath(elements.toArray(new PathElement[0]));
    }

    private static boolean isUnsignedInteger(final String token) {
        for (int i = 0; i < token.length(); i++) {
            if (!Character.isDigit(token.charAt(i))) return false;
        }
        return !token.isEmpty();
    }

    @Nullable
    public NBTBase resolve(final NBTTagCompound root) {
        if (root == null) return null;
        NBTBase current = root;
        for (final PathElement element : this.elements) {
            current = element.resolve(current);
            if (current == null) return null;
        }
        return current;
    }

    private interface PathElement {

        @Nullable
        NBTBase resolve(NBTBase current);
    }

    private static final class CompoundKey implements PathElement {

        private final String key;

        private CompoundKey(final String key) {
            this.key = key;
        }

        @Override
        public NBTBase resolve(final NBTBase current) {
            if (!(current instanceof NBTTagCompound compound) || !compound.hasKey(this.key)) return null;
            return compound.getTag(this.key);
        }
    }

    private static final class ListIndex implements PathElement {

        private final int index;

        private ListIndex(final int index) {
            this.index = index;
        }

        @Override
        public NBTBase resolve(final NBTBase current) {
            if (!(current instanceof NBTTagList list) || this.index < 0 || this.index >= list.tagCount()) return null;
            return (NBTBase) list.tagList.get(this.index);
        }
    }
}
