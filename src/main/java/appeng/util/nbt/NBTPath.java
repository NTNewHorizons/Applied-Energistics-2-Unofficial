package appeng.util.nbt;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

/**
 * A precompiled path using Minecraft-style NBT path syntax: dot-separated compound keys, bracketed list indices,
 * and double-quoted literal keys with escaped quotes and backslashes. Numeric compound keys must be quoted.
 */
public final class NBTPath {

    private final PathElement[] elements;

    private NBTPath(final PathElement[] elements) {
        this.elements = elements;
    }

    @Nullable
    public static NBTPath compile(final String source) {
        if (source == null || source.trim().isEmpty()) return null;

        final List<PathElement> elements = new ArrayList<>();
        int position = 0;
        while (position < source.length()) {
            if (source.charAt(position) == '"') {
                final StringBuilder key = new StringBuilder();
                boolean closed = false;
                position++;
                while (position < source.length()) {
                    char character = source.charAt(position++);
                    if (character == '"') {
                        closed = true;
                        break;
                    }
                    if (character == '\\') {
                        if (position == source.length()) return null;
                        character = source.charAt(position++);
                        if (character != '"' && character != '\\') return null;
                    }
                    key.append(character);
                }
                if (!closed) return null;
                elements.add(new CompoundKey(key.toString()));
            } else {
                final int start = position;
                while (position < source.length() && source.charAt(position) != '.'
                        && source.charAt(position) != '['
                        && source.charAt(position) != ']')
                    position++;
                if (position == start) return null;
                final String token = source.substring(start, position);
                if (isUnsignedInteger(token)) return null;
                elements.add(new CompoundKey(token));
            }
            while (position < source.length() && source.charAt(position) == '[') {
                final int start = ++position;
                while (position < source.length() && source.charAt(position) >= '0' && source.charAt(position) <= '9')
                    position++;
                if (position == start || position == source.length() || source.charAt(position) != ']') return null;
                try {
                    elements.add(new ListIndex(Integer.parseInt(source.substring(start, position))));
                } catch (final NumberFormatException ignored) {
                    return null;
                }
                position++;
            }
            if (position == source.length()) break;
            if (source.charAt(position++) != '.' || position == source.length()) return null;
        }
        return elements.isEmpty() ? null : new NBTPath(elements.toArray(new PathElement[0]));
    }

    static String appendCompoundKey(final String parentPath, final String key) {
        final String encodedKey = encodeCompoundKey(key);
        return parentPath.isEmpty() ? encodedKey : parentPath + '.' + encodedKey;
    }

    static String appendListIndex(final String parentPath, final int index) {
        return parentPath + '[' + index + ']';
    }

    private static String encodeCompoundKey(final String key) {
        if (!key.trim().isEmpty() && !isUnsignedInteger(key)
                && key.indexOf('.') < 0
                && key.indexOf('[') < 0
                && key.indexOf(']') < 0
                && key.indexOf('"') < 0
                && key.indexOf('\\') < 0)
            return key;
        return '"' + key.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
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
