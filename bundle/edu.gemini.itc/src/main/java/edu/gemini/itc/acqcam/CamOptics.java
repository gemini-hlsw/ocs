// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
// $Id: CamOptics.java,v 1.3 2003/11/21 14:31:02 shane Exp $
//
package edu.gemini.itc.acqcam;

import edu.gemini.itc.shared.TransmissionElement;
import edu.gemini.itc.shared.Instrument;

/**
 * This represents the transmission of the optics native to the camera.
 */
@Deprecated
public class CamOptics extends TransmissionElement {
    private static final String FILENAME = AcquisitionCamera.getPrefix() +
            "common_optics" + Instrument.getSuffix();
    
    public CamOptics(String directory) throws Exception {
        super(directory + FILENAME);
    }
    
    public String toString() {
        return "Camera Optics";
    }// + FILENAME; }
}
