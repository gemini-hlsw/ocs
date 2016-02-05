package edu.gemini.itc.gnirs;

import edu.gemini.itc.base.*;
import edu.gemini.itc.shared.CalculationMethod;
import edu.gemini.itc.shared.GnirsParameters;
import edu.gemini.itc.shared.Imaging;
import edu.gemini.itc.shared.ObservationDetails;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.gemini.gnirs.GNIRSParams;
import edu.gemini.spModel.gemini.gnirs.GNIRSParams.Disperser;
import edu.gemini.spModel.gemini.gnirs.GNIRSParams.SlitWidth;

import java.util.ArrayList;
import java.util.List;


/**
 * Gnirs specification class
 */
public final class Gnirs extends Instrument implements SpectroscopyInstrument {

    private static final double LONG_CAMERA_SCALE_FACTOR = 3.0;

    private static final double XDISP_CENTRAL_WAVELENGTH = 1616.85;

    /**
     * Related files will be in this subdir of lib
     */
    public static final String INSTR_DIR = "gnirs";

    public static final String INSTR_PREFIX = "gnirs_";

    private static final String FILENAME = "gnirs" + getSuffix();

    // Values taken from instrument's web documentation
    private static final double SHALLOW_WELL = 90000.0;
    private static final double DEEP_WELL = 180000.0;
    private static final double SHALLOW_WELL_LINEARITY_LIMIT = 65000;
    private static final double DEEP_WELL_LINEARTY_LIMIT = 130000.0;

    public static final int DETECTOR_PIXELS = 1024;

    // Keep a reference to the color filter to ask for effective wavelength
    private final GnirsParameters params;
    protected Filter _Filter;
    protected GnirsGratingOptics _gratingOptics;
    protected Detector _detector;
    protected double _sampling;
    protected String _filterUsed;
    protected Disperser _grating;
    protected CalculationMethod _mode;
    protected double _centralWavelength;

    protected final TransmissionElement _camera;
    protected final boolean _XDisp;
    protected final double _wellDepth;
    protected final double _linearityLimit;

    public Gnirs(GnirsParameters gp, ObservationDetails odp) {
        super(Site.GN, Bands.NEAR_IR, INSTR_DIR, FILENAME);
        ///
        // The instrument data file gives a start/end wavelength for
        // the instrument.  But with a filter in place, the filter
        // transmits wavelengths that are a subset of the original range.

        _sampling = super.getSampling();

        params = gp;
        _grating = gp.grating();
        _centralWavelength = correctedCentralWavelength(); // correct central wavelength if cross dispersion is used
        _mode = odp.calculationMethod();
        _XDisp = isXDispUsed();

        if (_centralWavelength < 1030 || _centralWavelength > 6000) {
            throw new RuntimeException("Central wavelength must be between 1.03um and 6.0um.");
        }

        //set read noise by exporsure time
        if (odp.exposureTime() <= 1.0) {
            _wellDepth      = DEEP_WELL;
            _linearityLimit = DEEP_WELL_LINEARTY_LIMIT;
        } else {
            _wellDepth      = SHALLOW_WELL;
            _linearityLimit = SHALLOW_WELL_LINEARITY_LIMIT;
        }

        //Select filter depending on if Cross dispersion is used.
        if (_XDisp) {
            _filterUsed = "XD";
            _Filter = Filter.fromFile(getPrefix(), _filterUsed, getDirectory() + "/");
        } else {
            //Use GnirsOrderSelecter to decide which filter to put in
            _filterUsed = "order";
            _Filter = Filter.fromFile(getPrefix(), _filterUsed + GnirsOrderSelector.getOrder(_centralWavelength), getDirectory() + "/");
        }
        addComponent(_Filter);

        //Select Transmission Element depending on if Cross dispersion is used.
        final TransmissionElement selectableTrans;
        if (_XDisp) {
            selectableTrans = new XDispersingPrism(getDirectory(), isLongCamera() ? "LXD" : "SXD");
        } else {
            selectableTrans = new GnirsPickoffMirror(getDirectory(), "mirror");
        }
        addComponent(selectableTrans);

        final FixedOptics _fixedOptics = new FixedOptics(getDirectory() + "/", getPrefix());
        addComponent(_fixedOptics);

        _camera = CameraFactory.camera(params.pixelScale(), _centralWavelength, getDirectory());
        addComponent(_camera);

        // GNIRS is spectroscopy only
        if (_mode instanceof Imaging) {
            throw new RuntimeException("GNIRS does not support imaging.");
        }


        _detector = new Detector(getDirectory() + "/", getPrefix(), "aladdin", "1K x 1K ALADDIN III InSb CCD");
        _detector.setDetectorPixels(DETECTOR_PIXELS);

        _gratingOptics = new GnirsGratingOptics(
                getDirectory() + "/" + getPrefix(), _grating,
                _centralWavelength,
                _detector.getDetectorPixels(),
                1,
                isLongCamera() ? LONG_CAMERA_SCALE_FACTOR : 1,
                1);

        if (_grating.equals(Disperser.D_10) && !isLongCamera())
            throw new RuntimeException("The grating " + _grating + " cannot be used with the " +
                    "0.15\" arcsec/pix (Short) camera.\n" +
                    "  Please either change the camera or the grating.");

        if (!(_filterUsed.equals("none")))
            if ((_Filter.getStart() >= _gratingOptics.getEnd()) ||
                    (_Filter.getEnd() <= _gratingOptics.getStart())) {
                throw new RuntimeException("The " + _filterUsed + " filter" +
                        " and the " + _grating +
                        " do not overlap with the requested wavelength.\n" +
                        " Please select a different filter, grating or wavelength.");
            }


        addComponent(_detector);


    }

    public edu.gemini.itc.base.Disperser disperser(final int order) {
        return new GnirsGratingOptics(
                getDirectory() + "/" + getPrefix(), _grating,
                _centralWavelength,
                _detector.getDetectorPixels(),
                order,
                isLongCamera() ? LONG_CAMERA_SCALE_FACTOR : 1,
                1);
    }

    /** {@inheritDoc} */
    public double getSlitWidth() {
        return params.slitWidth().getValue();
    }

    /**
     * Returns the effective observing wavelength.
     * This is properly calculated as a flux-weighted averate of
     * observed spectrum.  So this may be temporary.
     *
     * @return Effective wavelength in nm
     */
    public int getEffectiveWavelength() {
        return (int) _gratingOptics.getEffectiveWavelength();

    }

    /**
     * Returns the subdirectory where this instrument's data files are.
     */
    public String getDirectory() {
        return ITCConstants.LIB + "/" + INSTR_DIR;
    }

    public double getPixelSize() {
        return params.pixelScale().getValue();
    }

    public double getSpectralPixelWidth() {
        if (isLongCamera()) {
            return _gratingOptics.getPixelWidth() / 3.0;
        } else {
            return _gratingOptics.getPixelWidth();
        }
    }

    public double getWellDepth() {
        return _wellDepth;
    }

    public double getSampling() {
        return _sampling;
    }

    /**
     * The prefix on data file names for this instrument.
     */
    public static String getPrefix() {
        return INSTR_PREFIX;
    }

    public Disperser getGrating() {
        return _grating;
    }

    public double getGratingDispersion() {
        return _gratingOptics.dispersion();
    }

    public double getReadNoise() {
        return params.readMode().getReadNoise();
    }

    public double getObservingStart() {
        return _centralWavelength - (getGratingDispersion() / getOrder() * _detector.getDetectorPixels() / 2);
    }

    public double getObservingEnd() {
        return _centralWavelength + (getGratingDispersion() / getOrder() * _detector.getDetectorPixels() / 2);
    }

    public boolean XDisp_IsUsed() {
        return _XDisp;
    }

    public int getOrder() {
        try {
            return GnirsOrderSelector.getOrder(_centralWavelength);
        } catch (Exception e) {
            System.out.println("Cannot find Order setting to 1.");
            return 1;
        }
    }

    public SlitWidth getFocalPlaneMask() {
        return params.slitWidth();
    }

    public double getCentralWavelength() {
        return _centralWavelength;
    }

    public TransmissionElement getGratingOrderNTransmission(int order) {
        return GnirsGratingsTransmission.getOrderNTransmission(_grating, order);
    }

    private double correctedCentralWavelength() {
        if (!isXDispUsed()) {
            return params.centralWavelength().toNanometers();
        } else {
            return XDISP_CENTRAL_WAVELENGTH;
        }
    }

    private boolean isLongCamera() {
        return params.pixelScale().equals(GNIRSParams.PixelScale.PS_005);
    }

    private boolean isXDispUsed() {
        return !params.crossDispersed().equals(GNIRSParams.CrossDispersed.NO);
    }


    @Override public double wellDepth() {
        return _wellDepth;
    }

    @Override public double gain() {
        return 13.5;
    }

    @Override public List<WarningRule> warnings() {
        return new ArrayList<WarningRule>() {{
            add(new LinearityLimitRule(_linearityLimit, 0.80));
            add(new SaturationLimitRule(_wellDepth, 0.80));
        }};
    }


}
