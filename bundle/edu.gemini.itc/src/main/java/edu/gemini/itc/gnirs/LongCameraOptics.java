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
public abstract class LongCameraOptics extends TransmissionElement implements CameraOptics{
    protected static final String FILENAME = Gnirs.getPrefix() + GnirsParameters.getLongCameraName();
    
    public LongCameraOptics(String cameraTransmissionFile) throws Exception {
        super(cameraTransmissionFile);
    }
    public double getPixelScale() { return Gnirs.LONG_CAMERA_PIXEL_SCALE; }
    
    public String toString() { return "Camera: " + getPixelScale() +"arcsec/pix (Long "; }
}

