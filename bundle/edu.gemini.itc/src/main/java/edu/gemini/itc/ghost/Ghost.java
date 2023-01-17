package edu.gemini.itc.ghost;

import edu.gemini.itc.base.*;
import edu.gemini.itc.operation.DetectorsTransmissionVisitor;
import edu.gemini.itc.shared.GhostParameters;
import edu.gemini.itc.shared.IfuMethod;
import edu.gemini.itc.shared.ObservationDetails;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.gemini.ghost.GhostType;
import org.jfree.util.Log;
import scala.Option;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Ghost specification class
 */
public final class Ghost extends Instrument implements BinningProvider, SpectroscopyInstrument {

    private static final Logger log = Logger.getLogger(Ghost.class.getName());

    public static final String INSTR_DIR = "ghost";

    public static final double PLATE_SCALE = 1.64;  // arcsec/mm

    private static final double WellDepth = 350000;  // VENU has to confirm the correct value.
    private final TransmissionElement _blazeThrougthput;
    //private final IFU_Trans _ifuTrans;
    private final IFUComponent _ifu;

    protected DetectorsTransmissionVisitor _dtv;

    /**
     * Related files will be in this subdir of lib
     */
    public static final String INSTR_PREFIX = "ghost_";

    // Instrument reads its configuration from here.
    private static final double AD_SATURATION = 65535;

    protected Ghost[] _instruments;

    protected final GhostParameters gp;
    protected final ObservationDetails odp;

    protected GhostGratingOptics _gratingOptics;
    protected Detector _detector;
    protected double _sampling;

    /*
      Provide value(s) of throughput for GHOST Cass unit and science cable, based on
      results from Ross Zhelem 21 May 2019. The throughput includes Cass unit
      (telecentricity lens and ADC) and fibre cable + microlens arrays at both ends,
      but not any aperture losses
     */
    protected TransmissionElement _cableThrougthput;

    protected TransmissionElement _fixedOptics;

    protected TransmissionElement _resolutionElement;

    private static final String[] DETECTOR_CCD_NAMES = {"BLUE", "RED"};

    private static final String FILENAME = "ghost" + getSuffix();

    // These are the limits of observable wavelength with this configuration.

    private GhostType.DetectorManufacturer _ccd;


    private GhostType.DetectorManufacturer _ccdColor;

    private GhostSaturLimitRule _ghostSaturLimitWarning;  // GHOST-specific saturation limit warning

    public Ghost(final GhostParameters gp, final ObservationDetails odp, final GhostType.DetectorManufacturer ccdColor) {
        super(Site.GS, Bands.VISIBLE, INSTR_DIR, FILENAME);

        Log.info("###### New ghost resolution: "+gp.resolution() + " ccd_color: "+ ccdColor.displayValue() +  " ##### ");
        this.odp    = odp;
        this.gp     = gp;

        _ccdColor = ccdColor;
        _detector = new Detector(getDirectory() + "/", getPrefix(),  _ccdColor.getManufacter(), _ccdColor.getManufacter());

        _detector.setDetectorPixels(_ccdColor.getXsize());
        addComponent(_detector);
        _cableThrougthput = new TransmissionElement(getDirectory()+"/"+ Ghost.INSTR_PREFIX + "cable" +getSuffix());
        _cableThrougthput.setDescription("GHOST Cass unit and science cable");
        addComponent(_cableThrougthput);
        _fixedOptics = new TransmissionElement(getDirectory()+"/" + Ghost.INSTR_PREFIX + "fixedOptics"+getSuffix());
        _fixedOptics.setDescription("Fix Optics");
        addComponent(_fixedOptics);
        _blazeThrougthput = new TransmissionElement(getDirectory()+"/" + Ghost.INSTR_PREFIX + "blaze"+getSuffix());
        _blazeThrougthput.setDescription("Echelle Blaze function for individuals orders");
        addComponent(_blazeThrougthput);
        Log.info("*********** Directory "+ getDirectory() + "/ghost_" + gp.resolution().get_displayValue() + Instrument.getSuffix());
        _gratingOptics = new GhostGratingOptics(
                getDirectory() + "/" +Ghost.INSTR_PREFIX,
                gp.resolution().get_displayValue() + "_dispersion",
                "gratings",
                gp.centralWavelength().toNanometers(), _detector.getDetectorPixels(), gp.spectralBinning().getValue());
        _gratingOptics.setDescription("Grating Resolution");
        addDisperser(_gratingOptics);
        _sampling = super.getSampling();
        _ifu = new IFUComponent(gp.resolution());
        addComponent(_ifu);
        // TODO. REMOVE after talking with Andy
        _resolutionElement = new TransmissionElement (getDirectory()+"/" + Ghost.INSTR_PREFIX + "resElement_"+ gp.resolution().get_displayValue() +getSuffix());
        addComponent(_resolutionElement);
        _ghostSaturLimitWarning = new GhostSaturLimitRule(AD_SATURATION, WellDepth, getSpatialBinning(), getSpectralBinning(), gain() , 0.90);
    }

    public GhostType.DetectorManufacturer getDetManufacture() {
        return _ccdColor;
    }




    /**
     * Returns an array containing this instrument, or, if there are multiple detector CCDs,
     * an array containing instances of this instrument with the CCD set differently
     * (Used to implement hamamatsu CCD support).
     */
    public Ghost[] getDetectorCcdInstruments() {
        return _instruments;
    }

    /**
     * Index of current CCD in detector
     *
     * @return 0, 1, or 2 when there are multiple CCDs in the detector (default: 0)
     */

    /**
     * Returns the name of the detector CCD
     */
    public String getDetectorCcdName() {
        return _detector.getName();
    }

    /**
     * Returns the effective observing wavelength.
     * This is properly calculated as a flux-weighted averate of
     * observed spectrum.  So this may be temporary.
     *
     * @return Effective wavelength in nm
     */
    public int getEffectiveWavelength() {
        Log.info("ghost getEffectiveWavelength: "+ _gratingOptics.getEffectiveWavelength());
        return (int) _gratingOptics.getEffectiveWavelength();

    }

    public double getGratingDispersion() {
        System.out.println("getGratingDispersionnnnnnnnnnnnnnnnn " + _gratingOptics.dispersion(-1));
        return _gratingOptics.dispersion(-1);
    }

    /**
     * Returns the subdirectory where this instrument's data files are.
     */
    public String getDirectory() {
        return ITCConstants.LIB + "/" + INSTR_DIR;
    }

    @Override
    public double wellDepth() {
        Log.info("TODO. VENU has to give thist information. Not implemented yet Ghost.java wellDepth ");
        return WellDepth;
    }


    @Override
    public double gain() {
        Log.info("TODO. Venu has to confirm the gain for each read Mode");
        switch (gp.readMode()) {
            case BRIGTHTARGETS:
                return 1;
            case FAST:
                return 2;
            case STANDARD:
                return 3;
            default:
                log.warning("Bad definition of the readMode");
                return 0;
        }
    }

    public double getPixelSize() {
        //return (double) super.getPixelSize() * gp.spectralBinning().getValue();
        // The E2V_PIXEL_SIZE should be in arcsecs/pixel.
        return GhostType.E2V_PIXEL_SIZE * gp.spectralBinning().getValue();  // Both detectors has the same pixel size.
    }

    public double getSpectralPixelWidth() {
        log.info("getSpectralPixelWidth, getSampling: " + getSampling());
        return _gratingOptics.getPixelWidth();
    }

    public double getSampling() {
        return _sampling;
    }

    public int getSpectralBinning() {
        return gp.spectralBinning().getValue();
    }

    public int getSpatialBinning() {
        return gp.spatialBinning().getValue();
    }

    public double getADSaturation() {
        return AD_SATURATION;
    }


    /*public IFU_Trans getIFU_trans() {
        return _ifuTrans;
    }*/

    public Option<IfuMethod> getIfuMethod() {
        return (odp.analysisMethod() instanceof IfuMethod) ? Option.apply((IfuMethod) odp.analysisMethod()): Option.empty();
    }

    public double getCentralWavelength() {
        return gp.centralWavelength().toNanometers();
    }


    protected String getPrefix() {
        return INSTR_PREFIX;
    }
    protected  String[] getCcdNames() {
        return DETECTOR_CCD_NAMES;
    }

    private void validate() {
        log.info("Not implemented because Ghost form doesn't allow a bad configuration");
    }


    @Override public List<WarningRule> warnings() {
        return new ArrayList<WarningRule>() {{
            add(_ghostSaturLimitWarning);
        }};
    }


    public GhostSaturLimitRule getGhostSaturLimitWarning() {
        return _ghostSaturLimitWarning;
    }

    @Override
    public double getSlitWidth() {
        switch (gp.resolution()) {
            case STANDARD:
                return  0.32;
            case HIGH:
                return 0.19;
            default:
                log.warning("Incorrect option defined in the GhostParameter resolution, please check this issue. It is used the DEFAULT value (0.32)");
                return 0.32;
        }
    }

    public double getSlitLength() {
        double slitLength=0.0;

        switch (gp.resolution()) {
            case STANDARD:
                slitLength = GhostType.SIZE_ONE_FIBER_SR_PIXELS * 7;  // SIZE_ONE_FIBER_SR_PIXELS = 2.7
                break;
            case HIGH:
                slitLength = GhostType.SIZE_ONE_FIBER_HR_PIXELS * 19;  // SIZE_ONE_FIBER_HR_PIXELS = 1.62
                break;
            default:
                log.warning("Incorrect option defined in the GhostParameter resolution, please check this issue. It is used the DEFAULT value ("+slitLength+")");
        }
        Log.info("getSlitLength " + slitLength );
        return slitLength;
    }


    public double getDarkCurrent() {
        if (_ccdColor == GhostType.DetectorManufacturer.RED)
            return GhostType.DetectorManufacturer.RED.get_darkCurrent(); //  0.00022916666666666666; // red dark noise, e- /pix /s (NRC rep Jun 2019; av of 4)
        return GhostType.DetectorManufacturer.BLUE.get_darkCurrent();    // 0.0003263888888888889   blue dark noise, e- /pix /s (NRC rep Jun 2019; av of 4)
    }

    /*

        READ MODES- Standard science mode (slow read, low gain) Default: Standard science mode
	    Fast read (fast read, low gain)
	    Bright targets (fast read, fast gain)
     */
    public double getReadNoise() {
        switch (_ccdColor) {
            case RED: {
                switch (gp.readMode()) {
                    case BRIGTHTARGETS:
                        return 4.5;
                    case FAST:
                        return 4.5;
                    case STANDARD:
                        return 2.3;
                    default:
                        Log.error("Bad option provided by GhostParamenter read Mode, return 0 for read noise in the Detector Red");
                        return 0;
                }
            }
            case BLUE:
                switch (gp.readMode()) {
                    case BRIGTHTARGETS:
                        return 4.5;
                    case FAST:
                        return 4.5;
                    case STANDARD:
                        return 2.3;
                    default:
                        Log.error("Bad option provided by GhostParamenter read Mode, return 0 for read noise in the Detector Blue");
                        return 0;
                }
            default:
                Log.error("Bad CCD field built. Please check the class definition. ");
                return 0;
        }
    }

    public void applySkyCoeff(VisitableSampledSpectrum sky) {
        double skyCoeff = 1 + 7/ gp.nSkyMicrolens();
        if (gp.resolution() == GhostType.Resolution.HIGH)
            skyCoeff = 3.714;  // i.e 1 + 19/7
        for (int i = 0; i < sky.getLength(); i++) {
            sky.setY(i, sky.getY(i) * skyCoeff);
        }
    }

    public IFUComponent getIFU() {
        Log.info("getIFUUUUUUU ");
        return _ifu;
    }

    public String getCCDType() {
        return _ccdColor.displayValue();
    }
}
