package appeng.util.nbt;

import java.math.BigDecimal;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagString;

public final class CompiledNBTFilterEntry {

    private final NBTPath path;
    private final NBTComparison comparison;
    @Nullable
    private final NBTFilterValue value;

    private CompiledNBTFilterEntry(final NBTPath path, final NBTComparison comparison,
            @Nullable final NBTFilterValue value) {
        this.path = path;
        this.comparison = comparison;
        this.value = value;
    }

    @Nullable
    public static CompiledNBTFilterEntry compile(final NBTFilterEntry entry) {
        if (entry == null || entry.isCompletelyBlank()) return null;
        final NBTPath path = NBTPath.compile(entry.getPath());
        if (path == null) return null;

        final NBTComparison comparison = entry.getComparison();
        if (!comparison.requiresValue()) return new CompiledNBTFilterEntry(path, comparison, null);
        if (entry.getValue().isEmpty()) return null;

        return new CompiledNBTFilterEntry(path, comparison, NBTFilterValue.compile(entry.getValue()));
    }

    public boolean matches(final ItemStack stack) {
        return stack != null && this.matches(stack.getTagCompound());
    }

    public boolean matches(final NBTTagCompound root) {
        final NBTBase resolved = this.path.resolve(root);
        if (this.comparison == NBTComparison.EXISTS) return resolved != null;
        if (this.comparison == NBTComparison.NOT_EXISTS) return resolved == null;
        if (resolved == null || this.value == null) return false;

        if (isOrderingComparison(this.comparison) && !this.value.isNumeric()) return false;

        final Integer result = this.value.compare(resolved);
        if (result == null) return false;
        return switch (this.comparison) {
            case EQUALS -> result == 0;
            case NOT_EQUALS -> result != 0;
            case GREATER_THAN -> result > 0;
            case GREATER_THAN_OR_EQUAL -> result >= 0;
            case LESS_THAN -> result < 0;
            case LESS_THAN_OR_EQUAL -> result <= 0;
            default -> false;
        };
    }

    private static boolean isOrderingComparison(final NBTComparison comparison) {
        return comparison == NBTComparison.GREATER_THAN || comparison == NBTComparison.GREATER_THAN_OR_EQUAL
                || comparison == NBTComparison.LESS_THAN
                || comparison == NBTComparison.LESS_THAN_OR_EQUAL;
    }

    private interface NBTFilterValue {

        @Nullable
        Integer compare(NBTBase actual);

        default boolean isNumeric() {
            return false;
        }

        static NBTFilterValue compile(final String source) {
            try {
                return new NumericValue(new BigDecimal(source.trim()));
            } catch (final NumberFormatException ignored) {
                return new StringValue(source);
            }
        }
    }

    private static final class NumericValue implements NBTFilterValue {

        private final BigDecimal expected;

        private NumericValue(final BigDecimal expected) {
            this.expected = expected;
        }

        @Override
        public Integer compare(final NBTBase actual) {
            if (!(actual instanceof NBTBase.NBTPrimitive primitive)) return null;
            if (actual.getId() >= 1 && actual.getId() <= 4) {
                return BigDecimal.valueOf(primitive.func_150291_c()).compareTo(this.expected);
            }
            final double actualValue = primitive.func_150286_g();
            if (Double.isNaN(actualValue) || Double.isInfinite(actualValue)) return null;
            return BigDecimal.valueOf(actualValue).compareTo(this.expected);
        }

        @Override
        public boolean isNumeric() {
            return true;
        }
    }

    private static final class StringValue implements NBTFilterValue {

        private final String expected;

        private StringValue(final String expected) {
            this.expected = expected;
        }

        @Override
        public Integer compare(final NBTBase actual) {
            if (!(actual instanceof NBTTagString string)) return null;
            return string.func_150285_a_().compareTo(this.expected);
        }
    }
}
