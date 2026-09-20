package com.pagesofatlas;

import java.util.function.IntBinaryOperator;
import java.util.function.IntSupplier;

/** GL-independent validation so missing lower mips and GL allocation errors can be tested offline. */
public final class AllocationChecks {
    private AllocationChecks() {}
    public static void validate(int root, int texture, PageLayout layout, int mip,
                                IntBinaryOperator dimensions, IntSupplier error) {
        for (int level = 0; level <= mip; level++) {
            int width = dimensions.applyAsInt(level, 0), height = dimensions.applyAsInt(level, 1);
            int glError = error.getAsInt();
            if (glError != 0 || width != (layout.width() >> level) || height != (layout.height() >> level)) {
                throw new IllegalStateException("Pages of Atlas allocation failed: atlas=" + root + ", texture=" + texture
                        + ", mip=" + level + ", expected=" + (layout.width() >> level) + "x" + (layout.height() >> level)
                        + ", actual=" + width + "x" + height + ", GL error=0x" + Integer.toHexString(glError)
                        + ". Insufficient VRAM or a GL allocation error; incomplete materials will not be rendered.");
            }
        }
    }
}
