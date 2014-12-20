// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
package edu.gemini.itc.gnirs;

import edu.gemini.itc.shared.TransmissionElement;
import edu.gemini.itc.shared.Instrument;

/**
 * This represents the transmission of the optics native to the camera.
 */
public class LongCameraRedOptics extends LongCameraOptics {
    private static final String CAMERA_FILENAME = FILENAME +
    GnirsParameters.getRedCameraName() + Instrument.getSuffix();
    
    public LongCameraRedOptics(String directory) throws Exception {
        super(directory + "/" + CAMERA_FILENAME);
    }
    
    public String toString() { return super.toString() + "Red)"; }
}

