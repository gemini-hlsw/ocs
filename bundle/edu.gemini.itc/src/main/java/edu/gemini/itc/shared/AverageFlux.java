// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.

package edu.gemini.itc.shared;

/**
 * This base class is used in the normalization process where the client
 * wants to know the average flux of an object in a certain waveband.
 * To keep things consistent all clients and implementers should use
 * units of photons/s/m^2/nm
 */
// Programmer's note: interfaces can't have static methods, so this
// had to be a class.  Static methods can't be abstract, so the method
// had to have a default implementation.
// Subclasses should override getAverageFlux().

public abstract class AverageFlux {
    /**
     * Get average flux in photons/s/m^2/nm in specified waveband.
     */
    public static int getAverageFlux(String waveband) {
        return 0;
    }
}
