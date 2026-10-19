/*
 * The MIT License (MIT) Copyright (c) 2013 AlgorithmX2 Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions: The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software. THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package appeng.api.config;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;

import com.gtnewhorizon.gtnhlib.item.ItemStackNBT;

public enum FuzzyMode {

    // Note that percentage damaged, is the inverse of percentage durability.
    IGNORE_ALL(-1),
    PERCENT_99(0),
    PERCENT_75(25),
    PERCENT_50(50),
    PERCENT_25(75),
    PERCENT_10(90),
    PERCENT_1(99),
    // Keep the original seven ordinals and names unchanged for existing saved settings.
    IGNORE_ALL_VISUAL(-1),
    PERCENT_99_VISUAL(0),
    PERCENT_75_VISUAL(25),
    PERCENT_50_VISUAL(50),
    PERCENT_25_VISUAL(75),
    PERCENT_10_VISUAL(90),
    PERCENT_1_VISUAL(99);

    private static final int MODES_PER_STYLE = 7;
    private static final FuzzyMode[] MODES = values();

    public final float breakPoint;
    public final float percentage;

    FuzzyMode(final float p) {
        this.percentage = p;
        this.breakPoint = p / 100.0f;
    }

    public int calculateBreakPoint(final int maxDamage) {
        return (int) ((this.percentage * maxDamage) / 100.0f);
    }

    public boolean usesVisualDurability() {
        return this.ordinal() >= MODES_PER_STYLE;
    }

    public FuzzyMode legacyMode() {
        return MODES[this.ordinal() % MODES_PER_STYLE];
    }

    public FuzzyMode toggleVisualDurability() {
        return MODES[(this.ordinal() + MODES_PER_STYLE) % MODES.length];
    }

    public FuzzyMode rotate(final boolean backwards) {
        final int style = this.usesVisualDurability() ? MODES_PER_STYLE : 0;
        final int next = (this.ordinal() % MODES_PER_STYLE + (backwards ? MODES_PER_STYLE - 1 : 1)) % MODES_PER_STYLE;
        return MODES[style + next];
    }

    @Nonnull
    public static FuzzyMode fromItemStack(@Nonnull ItemStack is) {
        final String fz = ItemStackNBT.getString(is, "FuzzyMode");
        if (fz == null || fz.isEmpty()) {
            return FuzzyMode.IGNORE_ALL;
        }
        try {
            return FuzzyMode.valueOf(fz);
        } catch (IllegalArgumentException ignored) {
            return FuzzyMode.IGNORE_ALL;
        }
    }
}
