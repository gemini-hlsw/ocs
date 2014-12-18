// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
// $Id: WavebandDefinition.java,v 1.4 2003/11/21 14:31:02 shane Exp $
//
package edu.gemini.itc.shared;

import java.util.HashMap;

/**
 * This class represents the definition of standard "wavebands",
 * U, B, V, R, I, J, H, K, L',M',N,Q
 * Standard units will be nm.
 * All classes should use this class for waveband information rather
 * than re-implementing the definitions and possibly causing an error.
 *
 * Values for Sloan filters (g', r', i', z') taken from Fukugita et al. (1996)
 */
public final class WavebandDefinition {
    // The names of the standard wavebands in order of wavelength.
    public static final String[]
            //WAVEBAND_NAMES = {"U", "B", "V", "R", "I", "J", "H", "K", "L'", "M'", "N", "Q", "g'", "r'", "i'", "z'"};
            WAVEBAND_NAMES = {"U", "B", "V", "R", "I", "J", "H", "K", "L'", "M'", "N", "Q", "G'", "R'", "I'", "Z'"};

    private static HashMap _bandNameToIndex;

    // static initializer
    static {
        // Fill table of indices into arrays
        _bandNameToIndex = new HashMap();
        for (int i = 0; i < WAVEBAND_NAMES.length; ++i) {
            _bandNameToIndex.put(WAVEBAND_NAMES[i], new Integer(i));
        }
    }

    // Waveband center in nm for     U,   B,   V,    R,    I,    J,    H,    K
    private static int[] _center = {360, 440, 550, 670, 870, 1250, 1650, 2200
            , 3760, 4770, 10470, 20130, 483, 626, 767, 910};  //L' M' N Q g' r' i' z'

    // Waveband width in nm
    private static int[] _width = {75, 90, 85, 100, 100, 240, 300, 410,
            700, 240, 5230, 1650, 99, 96, 106, 125};

    /**
     *
     */
    public static int getWavebandIndex(String wavebandName) {
        wavebandName = wavebandName.toUpperCase();
        Integer index = (Integer) (_bandNameToIndex.get(wavebandName));
        if (index == null) return 0;
        return index.intValue();
    }

    /**
     * Returns the center of specified waveband in nm.
     * If waveband does not exist, returns 0.
     */
    public static int getCenter(String waveband) {
        return _center[getWavebandIndex(waveband)];
    }

    /**
     * Returns the width of specified waveband in nm.
     * If waveband does not exist, returns 0.
     */
    public static int getWidth(String waveband) {
        return _width[getWavebandIndex(waveband)];
    }

    /**
     * Returns the lower limit of specified waveband in nm.
     * If waveband does not exist, returns 0.
     */
    public static int getStart(String waveband) {
        return getCenter(waveband) - (getWidth(waveband) / 2);
    }

    /**
     * Returns the upper limit of specified waveband in nm.
     * If waveband does not exist, returns 0.
     */
    public static int getEnd(String waveband) {
        return getCenter(waveband) + (getWidth(waveband) / 2);
    }
}





