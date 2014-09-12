package edu.gemini.wdba.tcc;

import edu.gemini.spModel.gemini.acqcam.AcqCamParams;
import edu.gemini.spModel.gemini.acqcam.InstAcqCam;

/**
 *
 */
public class AcquisitionCameraSupport implements edu.gemini.wdba.tcc.ITccInstrumentSupport {
    //private static final Logger LOG = LogUtil.getLogger(AcquisitionCameraSupport.class);

    private ObservationEnvironment _oe;

    // Private constructor
    private AcquisitionCameraSupport(edu.gemini.wdba.tcc.ObservationEnvironment oe) {
        if (oe == null) throw new NullPointerException("observation environment can not be null");
        _oe = oe;
    }

    /**
     * Factory for creating a new Acqusition Camera Instrument Support.
     *
     * @param oe The current ObservationEnvironment
     * @return A new instance
     * @throws NullPointerException returned if the ObservationEnvironment is null.
     */
    static public ITccInstrumentSupport create(edu.gemini.wdba.tcc.ObservationEnvironment oe) throws NullPointerException {
        return new AcquisitionCameraSupport(oe);
    }

    /**
     * Returns the appropriate wavelength for the observation for the TCC config
     *
     * @return Appropriate guide wavelength
     */
    public String getWavelength() {
        // The following should be in the acquisition camera data model AcqCamParams
        // but I'll put it here for now so that it will be moved next time there is a database update
        InstAcqCam inst = (InstAcqCam) _oe.getInstrument();
        AcqCamParams.ColorFilter f = inst.getColorFilter();
        // Use the wavelength in the Filter Params
        return f.getCentralWavelength();
    }

    public String getPositionAngle() {
        InstAcqCam inst = (InstAcqCam) _oe.getInstrument();
        return inst.getPosAngleDegreesStr();
    }

    public String getTccConfigInstrument() {
        InstAcqCam inst = (InstAcqCam) _oe.getInstrument();
        // Check for HR or AC
        return inst.getLens() == AcqCamParams.Lens.HRWFS ? "HRWFS" : "ACQCAM";
    }

    /**
     * Support for instrument origins.
     *
     * @return String that is the name of a TCC config file.  See WDBA-5.
     */
    public String getTccConfigInstrumentOrigin() {
        InstAcqCam inst = (InstAcqCam) _oe.getInstrument();
        // Check for HR or AC
        return inst.getLens() == AcqCamParams.Lens.HRWFS ? "hrwfs" : "acqcam";
    }

    /**
     * Return true if the instrument is using a fixed rotator position.  In this case the pos angle is used
     * in a special rotator config
     *
     * @return String value that is the name of the fixed rotator config or null if no special name is needed
     */
    public String getFixedRotatorConfigName() {
        return null;
    }

    /**
     * Add the GUIDE wavelength
     */
    public void addGuideDetails(ParamSet guideConfig) {
    }

    /**
     * Returns the TCC chop parameter value.
     *
     * @return Chop value or null if there is no chop parameter for this instrument.
     */
    public String getChopState() {
        return TccNames.NOCHOP;
    }
}
