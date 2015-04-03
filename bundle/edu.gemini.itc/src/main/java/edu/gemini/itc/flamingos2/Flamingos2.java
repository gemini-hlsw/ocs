package edu.gemini.itc.flamingos2;

import edu.gemini.itc.operation.DetectorsTransmissionVisitor;
import edu.gemini.itc.shared.*;
import scala.Option;

import java.io.File;
import java.util.Hashtable;
import java.util.Scanner;

/**
 * Flamingos 2 specification class
 */
public final class Flamingos2 extends Instrument {

    private static final class ReadNoiseEntry {
        public final String _name;
        public final double _readNoise;
        ReadNoiseEntry(String name, double readNoise) {
            _name = name;
            _readNoise = readNoise;
        }
    }

    private static Hashtable<String, ReadNoiseEntry> _readNoiseLevels = new Hashtable<>(3);
    static {
        final String file = File.separator + Flamingos2.INSTR_DIR + File.separator + Flamingos2.getPrefix() + "readnoise" + Instrument.getSuffix();
        try (final Scanner scan = DatFile.scanFile(file)) {
            while (scan.hasNext()) {
                final String name = scan.next();
                final double rn = scan.nextDouble();
                final ReadNoiseEntry re = new ReadNoiseEntry(name, rn);
                _readNoiseLevels.put(name, re);
            }
        }
    }

    private static final String FILENAME = "flamingos2" + getSuffix();
    private static final String HIGHNOISE = "highNoise";
    public static final String INSTR_DIR = "flamingos2";
    public static final String INSTR_PREFIX = "";
    public static final String INSTR_PREFIX_2 = "flamingos2_";
    private static final double WELL_DEPTH = 200000.0;
    public static String getPrefix() {
        return INSTR_PREFIX;
    }
    public static String getPrefix2() {
        return INSTR_PREFIX_2;
    }


    private final DetectorsTransmissionVisitor _dtv;
    private final Option<Filter> _colorFilter;
    private final Option<GrismOptics> _grismOptics;
    private final String _filterBand;
    private final String _grism;
    private final String _readNoise;
    private final String _focalPlaneMask;
    private final double _slitSize;

    /**
     * construct a Flamingos2 object with specified color filter and ND filter.
     */
    public Flamingos2(final Flamingos2Parameters fp) {
        super(INSTR_DIR, FILENAME);

        _filterBand = fp.getColorFilter();
        _readNoise = fp.getReadNoise();
        _focalPlaneMask = fp.getFPMask();
        _grism = fp.getGrism();
        _slitSize = getSlitSize() * getPixelSize();
        _colorFilter = addColorFilter(_filterBand);
        _dtv = new DetectorsTransmissionVisitor(1, getDirectory() + "/" + getPrefix2() + "ccdpix" + Instrument.getSuffix());

        addComponent(new FixedOptics(getDirectory() + File.separator, getPrefix()));
        addComponent(new Detector(getDirectory() + File.separator, getPrefix(), "detector", "2048x2048 Hawaii-II (HgCdTe)"));

        _grismOptics = addGrism(_filterBand);
    }

    private Option<Filter> addColorFilter(final String filterBand) {
        if (filterBand.equalsIgnoreCase(Flamingos2Parameters.CLEAR)) {
            return Option.empty();
        } else {
            final Filter filter = Filter.fromFile(getPrefix(), _filterBand, getDirectory() + "/");
            addFilter(filter);
            return Option.apply(filter);
        }
    }

    private Option<GrismOptics> addGrism(final String filterBand) {
        if (_grism.equalsIgnoreCase(Flamingos2Parameters.NOGRISM)) {
            return Option.empty();
        } else {
            final GrismOptics grismOptics;
            try {
                grismOptics = new GrismOptics(getDirectory() + File.separator, _grism, _slitSize * getPixelSize(), filterBand);
            } catch (Exception e) {
                throw new IllegalArgumentException("Grism/filter " + _grism + "+" + filterBand + " combination is not supported.");
            }
            addComponent(grismOptics);
            return Option.apply(grismOptics);
        }
    }

    public double getSlitSize() {
        if (_focalPlaneMask.equalsIgnoreCase("none")) {
            return 1;
        }
        return Double.parseDouble(_focalPlaneMask);
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

    public double getGrismResolution() {
        if (_grismOptics.isDefined()) {
            return _grismOptics.get().getGrismResolution();
        } else {
            return 0;
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

    public DetectorsTransmissionVisitor getDetectorTransmision() {
        return _dtv;
    }

    @Override
    public double getReadNoise() {
        ReadNoiseEntry re = _readNoiseLevels.get(_readNoise);
        if (re == null)
            re = _readNoiseLevels.get(Flamingos2.HIGHNOISE);
        return re._readNoise;
    }

    public double getSpectralPixelWidth() {
        assert _grismOptics.isDefined();
        return _grismOptics.get().getPixelWidth();
    }

    public double getWellDepth() {
        return WELL_DEPTH;
    }

    public String getFocalPlaneMask() {
        return _focalPlaneMask;
    }

    public String getReadNoiseString() {
        return _readNoise;
    }
}
