package edu.gemini.shared.gui;

import java.io.Serializable;
import java.awt.image.RGBImageFilter;
import java.awt.*;
import java.util.Arrays;

/**
 * Converts RGB to HSB and back for filtering.  This class may not be strictly
 * necessary, since one can always manipulate the RGB values directly, but it is
 * sometimes easier to think in terms of manipulating the hue,
 * saturation, or brightness of a color.
 */
public final class HsbImageFilter extends RGBImageFilter {

    /**
     * An immutable value that represents an HSB color.
     */
    public static final class Hsb implements Serializable {
        private final float[] hsb;

        /**
         * Constructs with the three components.
         *
         * @param hue a fraction between 0 and 1 multiplied by 360 to get the
         * hue angle of the color
         *
         * @param saturation value between 0 and 1, where 0 is fully saturated
         * (white)
         * @param brightness value between 0 and 1, where 0 is fully dark
         * (black)
         */
        public Hsb(float hue, float saturation, float brightness) {
            hsb = new float[3];
            hsb[0] = hue;
            hsb[1] = saturation;
            hsb[2] = brightness;
        }

        private Hsb(float[] hsb) {
            this.hsb = hsb;
        }

        public float getHue() { return hsb[0]; }
        public float getSaturation() { return hsb[1]; }
        public float getBrightness() { return hsb[2]; }

        public Hsb withHue(float hue) {
            return new Hsb(hue, hsb[1], hsb[2]);
        }

        public Hsb withSaturation(float sat) {
            return new Hsb(hsb[0], sat, hsb[2]);
        }

        public Hsb withBrightness(float bright) {
            return new Hsb(hsb[0], hsb[1], bright);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            return Arrays.equals(hsb, ((Hsb) o).hsb);

        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(hsb);
        }
    }

    /**
     * Operation to perform on the HSB color.
     */
    public interface Op {
        Hsb apply(int x, int y, Hsb in);
    }

    /**
     * Applies all the provided operations one after another.
     */
    public static class OpChain implements Op {
        private Op[] ops;

        public OpChain(Op... ops) {
            this.ops = ops;
        }

        @Override
        public Hsb apply(int x, int y, Hsb in) {
            Hsb res = in;
            for (Op op : ops) {
                res = op.apply(x, y, res);
            }
            return res;
        }
    }

    /**
     * An HSB operation to apply to adjust the brightness.  An ajustment to the
     * brightness value should be specified as a floating point value in the
     * range of 0 to 1.  The brightness is adjusted by adding this value to the
     * color's current brightness value.  Use negative values to make the color
     * darker, and positive values to make the color brighter.
     */
    public static class AdjustBrightnessOp implements Op {
        private final float adj;

        /**
         * Creates with the adjustment to apply to the HSB color.
         *
         * @param adj value to be added to the HSB brightness value to compute
         * the new HSB color; should be in the range 0 to 1; use negative values
         * to make the color darker
         */
        public AdjustBrightnessOp(float adj) {
            this.adj = adj;
        }

        @Override public Hsb apply(int x, int y, Hsb in) {
            float newb = in.getBrightness() + adj;
            return in.withBrightness(Math.min(Math.max(0, newb), 1.0f));
        }
    }

    /**
     * Creates an HsbImageFilter which will adjust the brightness of the image
     * according to the provided adjustment.
     *
     * @param adj value to be added to the HSB brightness value to compute the
     * new HSB color; should be in the range 0 to 1; use negative values to
     * make the color darker
     *
     * @return an HsbImageFilter that will adjust the brightness value of the
     * colors that make up the image
     */
    public static HsbImageFilter createBrightnessFilter(float adj) {
        return new HsbImageFilter(new AdjustBrightnessOp(adj), true);
    }

    /**
     * Creates an HsbImageFilter that can be used to darken image icons for
     * buttons.  This is subjective, but it is intended to creates a noticeably
     * darker image to give feedback for rollovers or button presses, but not
     * too much darker.
     *
     * It is a shortcut for
     * <code>{@link #createBrightnessFilter(float) createBrightnessFilter(0.15f)}</code>
     */
    public static HsbImageFilter createImageIconDarkener() {
        return createBrightnessFilter(-0.15f);
    }

    private final Op op;

    public HsbImageFilter(Op op) {
        this(op, false);
    }

    public HsbImageFilter(Op op, boolean canFilterIndexColorModel) {
        if (op == null) throw new IllegalArgumentException();
        this.op = op;
        this.canFilterIndexColorModel = canFilterIndexColorModel;
    }

    public final int filterRGB(int x, int y, int rgb) {
        // Separate out the alpha, red, green, and blue components.
        int a = (rgb >> 24) & 0xFF;
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >>  8) & 0xFF;
        int b = (rgb      ) & 0xFF;

        // Convert this to HSB.
        float[] hsb = Color.RGBtoHSB(r, g, b, null);

        // Filter HSB.
        Hsb res = op.apply(x, y, new Hsb(hsb));

        // Combine with the same hue and saturation and convert back to RGB
        int color = Color.HSBtoRGB(res.getHue(), res.getSaturation(), res.getBrightness());

        // Strip off whatever alpha component this has.
        color = 0x00FFFFFF & color;

        // Put the real alpha back on it and we're done.
        return (a << 24) | color;
    }
}
