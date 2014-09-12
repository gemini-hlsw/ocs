//
// Convert.java
//

/*
VisAD system for interactive analysis and visualization of numerical
data.  Copyright (C) 1996 - 2000 Bill Hibbard, Curtis Rueden, Tom
Rink, Dave Glowacki, Steve Emmerson, Tom Whittaker, Don Murray, and
Tommy Jasmin.

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Library General Public
License as published by the Free Software Foundation; either
version 2 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Library General Public License for more details.

You should have received a copy of the GNU Library General Public
License along with this library; if not, write to the Free
Software Foundation, Inc., 59 Temple Place - Suite 330, Boston,
MA 02111-1307, USA
*/

//package visad.browser;

package jsky.util;

/**
 * Utility methods for various conversions between primitive data types.
 */
public class Convert {

    /**
     * Number of significant digits after the decimal point.
     */
    public static final int PLACES = 3;

    /**
     * Gets a reasonably short string representation of a double
     * for use in a graphical user interface.
     */
    public static String shortString(double val) {
        // remember whether or not the number is negative
        boolean negative = (val < 0.0);

        double orig_val = val;

        // now we only need to deal with a positive number
        val = Math.abs(val);

        if (val < 0.001) {
            for (int i = 1; i < 30; i++) {
                val *= 10.0;
                orig_val *= 10.0;
                if (val >= 1.0) {
                    return shortString(orig_val) + "E-" + i;
                }
            }
        }

        // build multiplier for saving significant digits
        // also build value used to round up insignificant digits
        int mult = 1;
        float round = 0.5f;
        for (int p = PLACES; p > 0; p--) {
            mult *= 10;
            round /= 10;
        }

        // break into digits before (preDot) and after (postDot) the decimal point
        long l = (long) ((val + round) * mult);
        long preDot = l / mult;
        int postDot = (int) (l % mult);

        // format the pre-decimal point number
        // Integer.toString() is faster than Long.toString(); use it if possible
        String num;
        if (preDot <= Integer.MAX_VALUE) {
            num = Integer.toString((int) preDot);
        } else {
            num = Long.toString(preDot);
        }

        // if there's nothing after the decimal point, use the whole number
        if (postDot == 0) {

            // make sure we don't return "-0"
            if (negative && preDot != 0) {
                return "-" + num;
            }

            return num;
        }

        // start building the string
        StringBuilder buf = new StringBuilder(num.length() + 5);

        // add sign (if necessary), pre-decimal point digits and decimal point
        if (negative) {
            buf.append('-');
        }
        buf.append(num);
        buf.append('.');

        // format the post-decimal point digits
        num = Integer.toString(postDot);

        // add leading zeros if necessary
        int nlen = num.length();
        for (int p = PLACES; p > nlen; p--) {
            buf.append('0');
        }

        // add actual digits
        buf.append(num);

        // return the final string
        return buf.toString();
    }

}
