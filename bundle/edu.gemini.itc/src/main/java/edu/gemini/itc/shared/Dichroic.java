// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.

package edu.gemini.itc.shared;

/**
 * This represents the transmission of the optics native to the camera.
 */
public class Dichroic extends TransmissionElement {

    private String _position;

    public Dichroic(String directory, String position, String channel) throws Exception {
        super(directory + "dichroic_" + position + "_" + channel + Instrument.getSuffix());
        _position = position;
    }

    public String toString() {
        return "Dichroic Position: " + _position;
    }

}
