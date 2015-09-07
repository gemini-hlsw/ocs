/* Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 * See the file LICENSE for complete details.
 *
 * $Id: AngleMathCase.java 18053 2009-02-20 20:16:23Z swalker $
 */
package edu.gemini.spModel.target.system.test;

import edu.gemini.spModel.target.system.AngleMath;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Class AngleMathTest tests the basic AngleMath class
 * utility routines.
 */
public final class AngleMathCase {
    // The error value used to check "closeness"
    private static final double _ERROR = 0.000001;

    @Test
    public void testNormalizeRa() {
        assertEquals(100.0, AngleMath.normalizeRa(100.0), _ERROR);
        assertEquals(350.0, AngleMath.normalizeRa(-10), _ERROR);
        assertEquals(179.5, AngleMath.normalizeRa(-180.5), _ERROR);
        assertEquals(0.0, AngleMath.normalizeRa(360.0), _ERROR);
        assertEquals(1.0, AngleMath.normalizeRa(721.0), _ERROR);
        assertEquals(359.0, AngleMath.normalizeRa(-721.0), _ERROR);
        assertEquals(358.74, AngleMath.normalizeRa(-721.26), _ERROR);
        assertEquals(140, AngleMath.normalizeRa(500), _ERROR);
    }

    @Test
    public void testNormalizeDec() {
        assertEquals(80.0, AngleMath.normalizeDec(100.0), _ERROR);
        assertEquals(-10.0, AngleMath.normalizeDec(-10), _ERROR);
        assertEquals(0.5, AngleMath.normalizeDec(-180.5), _ERROR);
        assertEquals(0.0, AngleMath.normalizeDec(360.0), _ERROR);
        assertEquals(1.0, AngleMath.normalizeDec(721.0), _ERROR);
        assertEquals(-1.0, AngleMath.normalizeDec(359.0), _ERROR);
        assertEquals(-1.26, AngleMath.normalizeDec(358.74), _ERROR);
    }
}
