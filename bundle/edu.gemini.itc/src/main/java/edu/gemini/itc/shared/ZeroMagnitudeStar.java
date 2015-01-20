// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
// $Id: ZeroMagnitudeStar.java,v 1.3 2003/11/21 14:31:02 shane Exp $
//
package edu.gemini.itc.shared;

/**
 * This class encapsulates the photon flux density (in photons/s/m^2/nm)
 * for a zero-magnitude star.
 */
public final class ZeroMagnitudeStar {
    // flux densities for each waveband for zero-magnitude star (of some type)
    // units are photons/s/m^2/nm
    public static int[] FLUX_DENSITY = {
        75900000, // U
        146100000, // B
        97100000, // V
        64600000, // R
        39000000, // I
        19700000, // J
        9600000, // H
        4500000, // K
        990000, //L'
        510000, //M'
        51000, //N
        7700, //Q
        // Values for Sloan filters taken from Schneider, Gunn, & Hoessel (1983)
        117000000, //g'
        108000000, //r'
        93600000, //i'
        79800000 //z'
    };

    // Keep anyone from instantiating this object.
    private ZeroMagnitudeStar() {
    }

    /**
     * Get average flux in photons/s/m^2/nm in specified waveband.
     * Overrides method in base class AverageFlux.
     */
    public static int getAverageFlux(String waveband) {
        return FLUX_DENSITY[WavebandDefinition.getWavebandIndex(waveband)];
    }
}
