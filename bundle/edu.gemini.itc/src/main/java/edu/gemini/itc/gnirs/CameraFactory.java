package edu.gemini.itc.gnirs;

import edu.gemini.itc.base.TransmissionElement;

/**
 * @author bwalls
 */
public class CameraFactory {

    private TransmissionElement _camera;

    /**
     * Creates a new instance of CameraFactory
     */
    public CameraFactory(String cameraLength, String cameraColor, String directory) {
        if (cameraLength.equals(GnirsParameters.LONG) && cameraColor.equals(GnirsParameters.BLUE)) {
            _camera = new LongCameraBlueOptics(directory);
        }
        if (cameraLength.equals(GnirsParameters.LONG) && cameraColor.equals(GnirsParameters.RED)) {
            _camera = new LongCameraRedOptics(directory);
        }
        if (cameraLength.equals(GnirsParameters.SHORT) && cameraColor.equals(GnirsParameters.BLUE)) {
            _camera = new ShortCameraBlueOptics(directory);
        }
        if (cameraLength.equals(GnirsParameters.SHORT) && cameraColor.equals(GnirsParameters.RED)) {
            _camera = new ShortCameraRedOptics(directory);
        }
    }

    TransmissionElement getCamera() {
        return _camera;
    }

}
