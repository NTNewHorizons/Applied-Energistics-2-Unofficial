package appeng.util;

import net.minecraft.item.ItemStack;

import appeng.api.config.FuzzyMode;

public final class FuzzyItemDurability {

    private FuzzyItemDurability() {}

    public static boolean isComparable(final ItemStack a, final ItemStack b, final FuzzyMode mode) {
        if (a.getItem().isDamageable()) return true;
        if (!mode.usesVisualDurability()) return false;
        try {
            return a.getItem().showDurabilityBar(a) || b.getItem().showDurabilityBar(b);
        } catch (final Throwable ignored) {
            return false;
        }
    }

    public static boolean matches(final ItemStack a, final ItemStack b, final FuzzyMode mode) {
        final FuzzyMode threshold = mode.legacyMode();
        try {
            if (threshold == FuzzyMode.IGNORE_ALL) return true;

            if (threshold == FuzzyMode.PERCENT_99 && !mode.usesVisualDurability()) {
                return (a.getItemDamageForDisplay() > 1) == (b.getItemDamageForDisplay() > 1);
            }

            if (!mode.usesVisualDurability()) {
                final float damageA = (float) a.getItemDamageForDisplay() / (float) a.getMaxDamage();
                final float damageB = (float) b.getItemDamageForDisplay() / (float) b.getMaxDamage();
                return (damageA > mode.breakPoint) == (damageB > mode.breakPoint);
            }

            final double damageA = getVisualDamage(a);
            final double damageB = getVisualDamage(b);
            return (damageA > mode.breakPoint) == (damageB > mode.breakPoint);
        } catch (final Throwable ignored) {
            if (threshold == FuzzyMode.IGNORE_ALL) return true;
            if (threshold == FuzzyMode.PERCENT_99) {
                return (a.getItemDamage() > 1) == (b.getItemDamage() > 1);
            }

            if (!mode.usesVisualDurability()) {
                final float damageA = (float) a.getItemDamage() / (float) a.getMaxDamage();
                final float damageB = (float) b.getItemDamage() / (float) b.getMaxDamage();
                return (damageA > mode.breakPoint) == (damageB > mode.breakPoint);
            }

            final double damageA = (double) a.getItemDamage() / (double) a.getMaxDamage();
            final double damageB = (double) b.getItemDamage() / (double) b.getMaxDamage();
            return (damageA > mode.breakPoint) == (damageB > mode.breakPoint);
        }
    }

    private static double getVisualDamage(final ItemStack stack) {
        final double damage = stack.getItem().getDurabilityForDisplay(stack);
        if (Double.isNaN(damage) || Double.isInfinite(damage)) return 0;
        return Math.max(0, Math.min(1, damage));
    }
}
