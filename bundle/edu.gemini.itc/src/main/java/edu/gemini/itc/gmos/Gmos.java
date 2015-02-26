package edu.gemini.itc.gmos;

import edu.gemini.itc.operation.DetectorsTransmissionVisitor;
import edu.gemini.itc.parameters.ObservationDetailsParameters;
import edu.gemini.itc.shared.*;
import edu.gemini.spModel.gemini.gmos.GmosNorthType;
import edu.gemini.spModel.gemini.gmos.GmosSouthType;

import java.awt.*;

/**
 * Gmos specification class
 */
public abstract class Gmos extends Instrument {

    //Plate scales for original and Hamamatsu CCD's (temporary)
    public static final double ORIG_PLATE_SCALE = 0.0727;
    public static final double HAM_PLATE_SCALE = 0.080778;

    protected DetectorsTransmissionVisitor _dtv;

    // Colors to use for charts corresponding to the detectorCcdIndex
    protected static final Color[] DETECTOR_CCD_COLORS = {Color.blue, Color.green, Color.red};

    /**
     * Related files will be in this subdir of lib
     */
    public static final String INSTR_DIR = "gmos";

    // Instrument reads its configuration from here.
    private static final double WELL_DEPTH = 125000.0;
    private static final double AD_SATURATION = 56636;
    private static final double HIGH_GAIN = 4.4;
    private static final double LOW_GAIN = 2.18;
    private static final int DETECTOR_PIXELS = 6218;

    // Used as a desperate solution when multiple detectors need to be handled differently (See REL-478).
    // For EEV holds the one instance one the Gmos instrument, for Hamamatsu, contains 3 one Gmos instance for
    // each of the three detectors.
    protected Gmos[] _instruments;

    protected final GmosParameters gp;
    protected final ObservationDetailsParameters odp;

    // Keep a reference to the color filter to ask for effective wavelength
    protected Filter _Filter;
    protected IFUComponent _IFU;
    protected GmosGratingOptics _gratingOptics;
    protected Detector _detector;
    protected double _sampling;

    // These are the limits of observable wavelength with this configuration.

    private int _detectorCcdIndex = 0; // 0, 1, or 2 when there are multiple CCDs in the detector

    public Gmos(GmosParameters gp, ObservationDetailsParameters odp, String FILENAME, int detectorCcdIndex) throws Exception {
        super(INSTR_DIR, FILENAME);

        this.odp    = odp;
        this.gp     = gp;

        _detectorCcdIndex = detectorCcdIndex;

        _sampling = super.getSampling();

        // TODO: filter is not yet defined, need to work with filter from gp, clean this up
        if (!gp.getFilter().equals(GmosNorthType.FilterNorth.NONE) && !gp.getFilter().equals(GmosSouthType.FilterSouth.NONE)) {
            _Filter = Filter.fromWLFile(getPrefix(), gp.getFilter().name(), getDirectory() + "/");
            addFilter(_Filter);
        }


        FixedOptics _fixedOptics = new FixedOptics(getDirectory() + "/", getPrefix());
        addComponent(_fixedOptics);


        //Choose correct CCD QE curve
        // REL-760, REL-478
        // The following QE files correspond to each option
        // 0. gmos_n_E2V4290DDmulti3.dat      => EEV DD array
        // 1. gmos_n_cdd_red.dat              => EEV legacy
        // 2. gmos_n_CCD-{R,G,B}.dat          =>  Hamamatsu (R,G,B)
        if (gp.getCCDtype().equals("0")) {
            _detector = new Detector(getDirectory() + "/", getPrefix(), "E2V4290DDmulti3", "EEV DD array");
            _detector.setDetectorPixels(DETECTOR_PIXELS);
            if (detectorCcdIndex == 0) _instruments = new Gmos[]{this};
        } else if (gp.getCCDtype().equals("1")) {
            _detector = new Detector(getDirectory() + "/", getPrefix(), "ccd_red", "EEV legacy array");
            _detector.setDetectorPixels(DETECTOR_PIXELS);
            if (detectorCcdIndex == 0) _instruments = new Gmos[]{this};
        } else if (gp.getCCDtype().equals("2")) {
            String fileName = getCcdFiles()[detectorCcdIndex];
            String name = getCcdNames()[detectorCcdIndex];
            Color color = DETECTOR_CCD_COLORS[detectorCcdIndex];
            _detector = new Detector(getDirectory() + "/", getPrefix(), fileName, "Hamamatsu array", name, color);
            _detector.setDetectorPixels(DETECTOR_PIXELS);
            if (detectorCcdIndex == 0)
                _instruments = createCcdArray();
        }

        if (detectorCcdIndex == 0) {
            _dtv = new DetectorsTransmissionVisitor(gp.getSpectralBinning(),
                    getDirectory() + "/" + getPrefix() + "ccdpix_red" + Instrument.getSuffix());
        }

        if (isIfuUsed()) {
            if (gp.getIFUMethod().equals(GmosParameters.SINGLE_IFU)) {
                _IFU = new IFUComponent(getPrefix(), gp.getIFUOffset());
            }
            if (gp.getIFUMethod().equals(GmosParameters.RADIAL_IFU)) {
                _IFU = new IFUComponent(getPrefix(), gp.getIFUMinOffset(), gp.getIFUMaxOffset());
            }
            addComponent(_IFU);
        }


        if (!(gp.getGrating().equals("none"))) {
            _gratingOptics = new GmosGratingOptics(getDirectory() + "/" + getPrefix(), gp.getGrating(), _detector,
                    gp.getCentralWavelength(),
                    _detector.getDetectorPixels(),
                    gp.getSpectralBinning());
            _sampling = _gratingOptics.getGratingDispersion_nmppix();
            addGrating(_gratingOptics);
        }


        addComponent(_detector);


        // validate the current configuration
        validate();

    }

    /**
     * Returns an array containing this instrument, or, if there are multiple detector CCDs,
     * an array containing instances of this instrument with the CCD set differently
     * (Used to implement hamamatsu CCD support).
     */
    public Gmos[] getDetectorCcdInstruments() {
        return _instruments;
    }

    /**
     * Index of current CCD in detector
     *
     * @return 0, 1, or 2 when there are multiple CCDs in the detector (default: 0)
     */
    public int getDetectorCcdIndex() {
        return _detectorCcdIndex;
    }

    /**
     * Returns the name of the detector CCD
     */
    public String getDetectorCcdName() {
        return _detector.getName();
    }

    /**
     * Returns the color to use in plots of the detector CCD
     */
    public Color getDetectorCcdColor() {
        return _detector.getColor();
    }

    /**
     * Returns the effective observing wavelength.
     * This is properly calculated as a flux-weighted averate of
     * observed spectrum.  So this may be temporary.
     *
     * @return Effective wavelength in nm
     */
    public int getEffectiveWavelength() {
        if (gp.getGrating().equals("none")) return (int) _Filter.getEffectiveWavelength();
        else return (int) _gratingOptics.getEffectiveWavelength();

    }

    public double getGratingResolution() {
        return _gratingOptics.getGratingResolution();
    }

    public String getGrating() {
        return gp.getGrating();
    }

    public double getGratingDispersion_nm() {
        return _gratingOptics.getGratingDispersion_nm();
    }

    public double getGratingDispersion_nmppix() {
        return _gratingOptics.getGratingDispersion_nmppix();
    }


    /**
     * Returns the subdirectory where this instrument's data files are.
     */
    public String getDirectory() {
        return ITCConstants.LIB + "/" + INSTR_DIR;
    }

    public double getPixelSize() {
        if (gp.getCCDtype().equals("0") || gp.getCCDtype().equals("1")) {
            return ORIG_PLATE_SCALE * gp.getSpatialBinning();
        } else {
            return HAM_PLATE_SCALE * gp.getSpatialBinning();
        }
    }

    public double getSpectralPixelWidth() {
        return _gratingOptics.getPixelWidth();
    }

    public double getWellDepth() {
        return WELL_DEPTH;
    }

    public double getSampling() {
        return _sampling;
    }

    public int getSpectralBinning() {
        return gp.getSpectralBinning();
    }

    public int getSpatialBinning() {
        return gp.getSpatialBinning();
    }

    public double getADSaturation() {
        return AD_SATURATION;
    }

    public double getHighGain() {
        return HIGH_GAIN;
    }

    public double getLowGain() {
        return LOW_GAIN;
    }

    public IFUComponent getIFU() {
        return _IFU;
    }

    public boolean isIfuUsed() {
        return gp.getFocalPlaneMask().equals(GmosParameters.IFU);
    }

    //Abstract class for Detector Pixel Transmission  (i.e.  Create Detector gaps)
    public DetectorsTransmissionVisitor getDetectorTransmision() {
        return _dtv;
    }

    public String toString() {

        String s = "Instrument configuration: \n";
        s += super.opticalComponentsToString();

        if (!gp.getFocalPlaneMask().equals(GmosParameters.NO_SLIT))
            s += "<LI> Focal Plane Mask: " + gp.getFocalPlaneMask() + "\n";
        s += "\n";
        if (odp.getMethod().isSpectroscopy())
            s += "<L1> Central Wavelength: " + gp.getCentralWavelength() + " nm" + "\n";
        s += "Spatial Binning: " + getSpatialBinning() + "\n";
        if (odp.getMethod().isSpectroscopy())
            s += "Spectral Binning: " + getSpectralBinning() + "\n";
        s += "Pixel Size in Spatial Direction: " + getPixelSize() + "arcsec\n";
        if (odp.getMethod().isSpectroscopy())
            s += "Pixel Size in Spectral Direction: " + getGratingDispersion_nmppix() + "nm\n";
        if (isIfuUsed()) {
            s += "IFU is selected,";
            if (gp.getIFUMethod().equals(GmosParameters.SINGLE_IFU))
                s += "with a single IFU element at " + gp.getIFUOffset() + "arcsecs.";
            else
                s += "with mulitple IFU elements arranged from " + gp.getIFUMinOffset() + " to " + gp.getIFUMaxOffset() + "arcsecs.";
            s += "\n";
        }
        return s;
    }

    protected abstract Gmos[] createCcdArray() throws Exception;
    protected abstract String getPrefix();
    protected abstract String[] getCcdFiles();
    protected abstract String[] getCcdNames();

    private void validate() {
        //Test to see that all conditions for Spectroscopy are met
        if (odp.getMethod().isSpectroscopy()) {
            if (grating.isEmpty())
                throw new RuntimeException("Spectroscopy calculation method is selected but a grating" +
                        " is not.\nPlease select a grating and a " +
                        "focal plane mask in the Instrument " +
                        "configuration section.");
            if (gp.getFocalPlaneMask().equals(GmosParameters.NO_SLIT))
                throw new RuntimeException("Spectroscopy calculation method is selected but a focal" +
                        " plane mask is not.\nPlease select a " +
                        "grating and a " +
                        "focal plane mask in the Instrument " +
                        "configuration section.");
        }

        if (odp.getMethod().isImaging()) {
            if (filter.isEmpty())
                throw new RuntimeException("Imaging calculation method is selected but a filter" +
                        " is not.\n  Please select a filter and resubmit the " +
                        "form to continue.");
            if (grating.isDefined())
                throw new RuntimeException("Imaging calculation method is selected but a grating" +
                        " is also selected.\nPlease deselect the " +
                        "grating or change the method to spectroscopy.");
            if (!gp.getFocalPlaneMask().equals("none"))
                throw new RuntimeException("Imaging calculation method is selected but a Focal" +
                        " Plane Mask is also selected.\nPlease " +
                        "deselect the Focal Plane Mask" +
                        " or change the method to spectroscopy.");
            if (isIfuUsed())
                throw new RuntimeException("Imaging calculation method is selected but an IFU" +
                        " is also selected.\nPlease deselect the IFU or" +
                        " change the method to spectroscopy.");
        }

    }
}
