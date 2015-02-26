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
public class PupilStopWheel extends TransmissionElement {

    public PupilStopWheel(String directory, String pupilMask) throws Exception {
        super(directory + "pupil_" + pupilMask + Instrument.getSuffix());
    }

    public String toString() {
        return "Pupil Stop Wheel";
    }

}
