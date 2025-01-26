package edu.gemini.itc.igrins2;

import edu.gemini.itc.base.*;
import edu.gemini.itc.shared.*;
import edu.gemini.spModel.core.MagnitudeBand;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.gemini.igrins2.Igrins2$;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;


// IGRINS2 specification class
public class Igrins2 extends Instrument implements SpectroscopyInstrument {
    private static final Logger Log = Logger.getLogger(Igrins2.class.getName());

    public static final String INSTR_DIR = "igrins2";  // Related files will be in this directory
    public static final String INSTR_PREFIX = "";  // Related files will start with this prefix
    private static final String FILENAME = "igrins2" + getSuffix();  // Instrument configuration file
    private final Igrins2GratingOptics _gratingOptics;
    private final Igrins2Arm _arm;
    private final double _readNoise;
    private final int _fowlerSamples;

    public Igrins2(final Igrins2Parameters igp, final ObservationDetails odp, Igrins2Arm arm) {
        super(Site.GN, Bands.NEAR_IR, INSTR_DIR, FILENAME);
        this._arm = arm;

        Log.fine("Calc method = " + odp.calculationMethod());
        Log.fine("Arm = " + arm.getName());

        _fowlerSamples = Igrins2$.MODULE$.fowlerSamples(odp.exposureTime());
        Log.fine("Fowler Samples = " + _fowlerSamples);

        _readNoise = Igrins2$.MODULE$.readNoise(odp.exposureTime(), arm.getMagnitudeBand());
        Log.fine("Read Noise = " + _readNoise + " e-");

        FixedOptics _fixedOptics = new FixedOptics(
                getDirectory() + "/",
                getPrefix(),
                "_" + arm.getName());
        addComponent(_fixedOptics);

        _gratingOptics = new Igrins2GratingOptics(
                getDirectory() + "/" + getPrefix(),
                "immersion_grating_" + arm.getName(),
                "gratings",
                arm.getWavelengthCentral(),
                arm.getDetXSize(),
                1);
        _gratingOptics.setDescription("Grating Resolution");
        addDisperser(_gratingOptics);

        Detector _detector = new Detector(
                getDirectory() + "/", getPrefix(),
                arm.getDetectorFilename(),
                arm.getName());
        addComponent(_detector);

        if (igp.altair().isDefined()) {
            if (igp.altair().get().guideStarSeparation() < 0 || igp.altair().get().guideStarSeparation() > 45)
                throw new RuntimeException("Altair Guide star distance must be less than 45 arcsecs.\n");
        }

    }

    public int getEffectiveWavelength() { return (int) _arm.getWavelengthCentral(); }  // (nanometers)

    @Override
    public double getReadNoise() { return _readNoise; }  // (electrons)

    public double getReadNoise(double exposureTime) {
        return Igrins2$.MODULE$.readNoise(exposureTime, _arm.getMagnitudeBand());  // (electrons)
    }

    public int getFowlerSamples() { return _fowlerSamples; }

    public String getDirectory() { return ITCConstants.LIB + "/" + INSTR_DIR; }  // the directory with the data files

    public double getWavelengthStart() { return _arm.getWavelengthStart(); }  // (nanometers)

    public double getWavelengthEnd() { return _arm.getWavelengthEnd(); }  // (nanometers)

    public Igrins2Arm getArm() { return _arm; }

    public String getWaveBand() { return _arm.getName(); }

    public double getPixelSize() { return _arm.getPixelSize(); }  // Spatial pixel size (arcseconds)

    public double getSpectralPixelWidth() { return _gratingOptics.getSpectralPixelWidth(); }  // (nanometers)

    public double getSlitWidth() { return 0.33; }  // Slit width (arcseconds)

    public static String getPrefix() { return INSTR_PREFIX; }  // The prefix on data file names for this instrument

    @Override public double wellDepth() { return _arm.getWellDepth(); }  // Detector well-depth (electrons)

    @Override public double gain() { return _arm.getGain(); }  // Detector gain (electrons / ADU)

    @Override public double getDarkCurrent() { return _arm.getDarkCurrent(); }  // Detector dark current (electrons / sec / pixel)

    @Override public double maxFlux() { return _arm.get_maxRecommendedFlux(); }  // Maximum recommended flux (electrons)

    @Override public List<WarningRule> warnings() {
        return new ArrayList<WarningRule>() {{
            add(new LinearityLimitRule(_arm.getLinearityLimit(), 0.80));
            add(new SaturationLimitRule(_arm.getSaturationLimit(), 0.80));
        }};
    }

}
