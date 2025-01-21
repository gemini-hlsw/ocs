package edu.gemini.itc.flamingos2;

import edu.gemini.itc.base.*;
import edu.gemini.itc.shared.Flamingos2Parameters;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.gemini.flamingos2.Flamingos2.FPUnit;
import edu.gemini.spModel.gemini.flamingos2.Flamingos2.ReadMode;
import scala.Option;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Flamingos 2 specification class
 */
public final class Flamingos2 extends Instrument implements SpectroscopyInstrument {
    private static final Logger Log = Logger.getLogger(Flamingos2.class.getName());

    // values are taken from instrument's web documentation
    private static final double AmpGain        =   4.44; // electrons / ADU
    private static final double WellDepth      = 155400; // electrons = 35000 ADU
    private static final double LinearityLimit =  97680; // electrons = 22000 ADU

    private static final String FILENAME = "flamingos2" + getSuffix();
    public static final String INSTR_DIR = "flamingos2";
    public static final String INSTR_PREFIX = "";
    public static String getPrefix() {
        return INSTR_PREFIX;
    }

    private final Option<Filter> _colorFilter;
    private final Option<GrismOptics> _grismOptics;
    private final Flamingos2Parameters params;

    /**
     * construct a Flamingos2 object with specified color filter and ND filter.
     */
    public Flamingos2(final Flamingos2Parameters fp) {
        super(Site.GS, Bands.NEAR_IR, INSTR_DIR, FILENAME);

        params = fp;
        _colorFilter = addColorFilter(fp);

        addComponent(new FixedOptics(getDirectory() + File.separator, getPrefix()));
        addComponent(new Detector(getDirectory() + File.separator, getPrefix(), "detector", "2048x2048 Hawaii-II (HgCdTe)"));

        _grismOptics = addGrism(fp);
    }

    private Option<Filter> addColorFilter(final Flamingos2Parameters fp) {
        switch (fp.filter()) {
            case OPEN:
                return Option.empty();
            default:
                final Filter filter = Filter.fromFile(getPrefix(), fp.filter().name(), getDirectory() + "/");
                addFilter(filter);
                return Option.apply(filter);
        }
    }

    private Option<GrismOptics> addGrism(final Flamingos2Parameters fp) {
        switch (fp.grism()) {
            case NONE:
                return Option.empty();
            default:
                final GrismOptics grismOptics;
                try {
                    grismOptics = new GrismOptics(getDirectory() + File.separator, fp.grism().name(), fp.filter().name(), getPixelSize());
                } catch (Exception e) {
                    throw new IllegalArgumentException("Grism/filter " + fp.grism() + "+" + fp.filter().name() + " combination is not supported.");
                }
                addComponent(grismOptics);
                return Option.apply(grismOptics);
        }
    }

    /** {@inheritDoc} */
    public double getSlitWidth() {
        // Note: Slit size for F2 is in pixels, not in arcsecs!
        switch (params.mask()) {
            case FPU_NONE:
                return 1 * getPixelSize();
            case CUSTOM_MASK:
                // There are two possible errors here: a programming error in case "Custom Mask" is set but
                // no custom slit width is sent to ITC or the case where the custom mask slit width is set
                // to "Other" (i.e. unknown).
                if (params.customSlitWidth().isEmpty()) {
                    throw new Error("Custom slit width is missing.");
                }
                if (params.customSlitWidth().get().width().isEmpty()) {
                    throw new IllegalArgumentException("Custom masks with unknown slit widths are not supported.");
                }
                return params.customSlitWidth().get().width().getValue() * getPixelSize();
            default:
                return params.mask().getSlitWidth() * getPixelSize();
        }
    }

    /**
     * Returns the subdirectory where this instrument's data files are.
     */
    public String getDirectory() {
        return ITCConstants.LIB + "/" + INSTR_DIR;
    }

    /**
     * Returns the effective observing wavelength. This is properly calculated
     * as a flux-weighted averate of observed spectrum. So this may be
     * temporary.
     *
     * @return Effective wavelength in nm
     */
    public int getEffectiveWavelength() {
        if (_colorFilter.isEmpty()) {
            return (int) (getEnd() + getStart()) / 2;
        } else {
            return (int) _colorFilter.get().getEffectiveWavelength();
        }
    }

    public double getObservingEnd() {
        if (_colorFilter.isDefined()) {
            return _colorFilter.get().getEnd();
        } else {
            return getEnd();
        }
    }

    public double getObservingStart() {
        if (_colorFilter.isDefined()) {
            return _colorFilter.get().getStart();
        } else {
            return getStart();
        }
    }

    @Override
    public double getReadNoise() {
        return params.readMode().readNoise();
    }

    public double getSpectralPixelWidth() {
        assert _grismOptics.isDefined();
        return _grismOptics.get().getPixelWidth();
    }

    public FPUnit getFocalPlaneMask() {
        return params.mask();
    }

    public String getReadNoiseString() {
        switch (params.readMode()) {
            case BRIGHT_OBJECT_SPEC: return "highNoise";
            case MEDIUM_OBJECT_SPEC: return "medNoise";
            case FAINT_OBJECT_SPEC:  return "lowNoise";
            default:                 throw new Error();
        }
    }

    public ReadMode getReadMode() {
        return params.readMode();
    }

    public double getMinExposureTime(ReadMode r) {
        switch (r) {
            case BRIGHT_OBJECT_SPEC: return 2.0;  // seconds
            case MEDIUM_OBJECT_SPEC: return 8.0;
            case FAINT_OBJECT_SPEC:  return 16.0;
            default:                 throw new Error("Unknown readMode");
        }
    }

    public ReadMode getOptimalReadMode(double exposureTime) {
        if (exposureTime < 21.) {
            return ReadMode.BRIGHT_OBJECT_SPEC;
        } else if (exposureTime < 85.) {
            return ReadMode.MEDIUM_OBJECT_SPEC;
        } else {
            return ReadMode.FAINT_OBJECT_SPEC;
        }
    }

    public Disperser disperser() {
        return _grismOptics.get();
    }

    public double maxFlux() {
        return LinearityLimit;
    }

    @Override public double wellDepth() {
        return WellDepth;
    }

    @Override public double gain() {
        return AmpGain;
    }

    @Override public List<WarningRule> warnings() {
        return new ArrayList<WarningRule>() {{
            add(new LinearityLimitRule(LinearityLimit, 0.80));
            add(new SaturationLimitRule(WellDepth, 0.80));
        }};
    }


}
