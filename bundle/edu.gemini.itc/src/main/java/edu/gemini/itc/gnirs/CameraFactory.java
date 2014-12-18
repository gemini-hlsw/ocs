// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
// * CameraFactory.java
// *
// * Created on January 13, 2004, 3:15 PM
// */

package edu.gemini.itc.gnirs;

/**
 *
 * @author  bwalls
 */
public class CameraFactory {
    
    private CameraOptics _camera;
    
    /** Creates a new instance of CameraFactory */
    public CameraFactory(String cameraLength, String cameraColor, String directory) throws Exception {
        if (cameraLength.equals(GnirsParameters.LONG) && cameraColor.equals(GnirsParameters.BLUE)){
            _camera = new LongCameraBlueOptics(directory);
        }
        if (cameraLength.equals(GnirsParameters.LONG) && cameraColor.equals(GnirsParameters.RED)){
            _camera = new LongCameraRedOptics(directory);
        }
        if (cameraLength.equals(GnirsParameters.SHORT) && cameraColor.equals(GnirsParameters.BLUE)){
            _camera = new ShortCameraBlueOptics(directory);
        }
        if (cameraLength.equals(GnirsParameters.SHORT) && cameraColor.equals(GnirsParameters.RED)){
            _camera = new ShortCameraRedOptics(directory);
        }
    }
    
    CameraOptics getCamera() { return _camera; }
    
}
