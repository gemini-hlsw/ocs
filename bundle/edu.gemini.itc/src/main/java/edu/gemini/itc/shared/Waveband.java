// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
// $Id: Waveband.java,v 1.2 2003/11/21 14:31:02 shane Exp $
//
package edu.gemini.itc.shared;

/**
 * This class represents a standard "waveband", one of
 * U, B, V, R, I, J, H, K
 * Standard units will be nm.
 * This class does not enforce that the band name be a valid one.
 */
public final class Waveband {
    private String _bandName;

    public Waveband(String bandName) {
        _bandName = bandName;
    }

    /**
     * Returns the center of specified waveband in nm.
     * If waveband does not exist, returns 0.
     */
    public int getCenter() {
        return WavebandDefinition.getCenter(_bandName);
    }

    /**
     * Returns the width of specified waveband in nm.
     * If waveband does not exist, returns 0.
     */
    public int getWidth(String waveband) {
        return WavebandDefinition.getWidth(_bandName);
    }

    /**
     * Returns the lower limit of specified waveband in nm.
     * If waveband does not exist, returns 0.
     */
    public int getStart(String waveband) {
        return WavebandDefinition.getStart(_bandName);
    }

    /**
     * Returns the upper limit of specified waveband in nm.
     * If waveband does not exist, returns 0.
     */
    public int getEnd(String waveband) {
        return WavebandDefinition.getEnd(_bandName);
    }
}
