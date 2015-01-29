package edu.gemini.itc.flamingos2;

import edu.gemini.itc.shared.Instrument;
import edu.gemini.itc.shared.DatFile;
import edu.gemini.itc.shared.TransmissionElement;

import java.util.Hashtable;
import java.util.Scanner;

/**
 * This represents the transmission of the Grism optics.
 * See REL-557
 */
public class GrismOptics extends TransmissionElement {

    private static class CoverageEntry {
        public final double _start, _end, _width;
        public final double _resolution;
        public final String _name;
        public final String _grismDataFileName;

        CoverageEntry(String name, double start, double end, double width,
                      double res, String grismDataFileName) {
            _name = name;
            _start = start;
            _end = end;
            _width = width;
            _resolution = res;
            _grismDataFileName = grismDataFileName;
        }
    }

    private static final Hashtable<String, CoverageEntry> _coverage;

    static {
        _coverage = new Hashtable<>(10);
        try {
            readCoverageData();
        } catch (Exception e) {
            throw new IllegalArgumentException("Coverage data file is invalid: " + e.getMessage(), e);
        }
    }


    private final String _grismName;
    private final String _coverageDataName;
    private final double _slitSize;

    public GrismOptics(String directory, String grismName, double slitSize, String filterBand) throws Exception {
        super(directory + getGrismDataFileName(grismName, filterBand));

        _grismName = grismName;
        _coverageDataName = buildCoverageDataName(grismName, filterBand);
        _slitSize = slitSize;
    }

    public double getStart() {
        CoverageEntry ce = _coverage.get(_coverageDataName);
        if (ce == null)
            return this.get_trans().getStart();
        return ce._start;
    }

    public double getEnd() {
        CoverageEntry ce = _coverage.get(_coverageDataName);
        if (ce == null)
            return this.get_trans().getEnd();
        return ce._end;
    }

    public double getEffectiveWavelength() {
        return (getStart() + getEnd()) / 2;
    }

    public double getPixelWidth() {
        CoverageEntry ce = _coverage.get(_coverageDataName);
        if (ce == null)
            return _slitSize;
        return ce._width;
    }

    public double getGrismResolution() {
        CoverageEntry ce = _coverage.get(_coverageDataName);
        if (ce == null)
            return 0;
        return ce._resolution;
    }

    public String toString() {
        return "Grism Optics: " + _grismName;
    }

    // =====
    private static void readCoverageData() {
        final String file = "/" + Flamingos2.INSTR_DIR + "/grism-coverage" + Instrument.getSuffix();
        try (final Scanner tr = DatFile.scanFile(file)) {
            while (tr.hasNext()) {
                final String grism = tr.next();
                final String filter = tr.next();
                final double start = tr.nextDouble();
                final double end = tr.nextDouble();
                final double width = tr.nextDouble();
                final double res = tr.nextDouble();
                final String grismDataFileName = tr.next();
                final String name = buildCoverageDataName(grism, filter);
                final CoverageEntry ce = new CoverageEntry(name, start, end, width, res, grismDataFileName);
                _coverage.put(name, ce);
            }
        }
    }

    // REL-557: Get grism data file name from coverage table
    private static String getGrismDataFileName(String grismName, String filter) {
        CoverageEntry ce = _coverage.get(buildCoverageDataName(grismName, filter));
        if (ce == null)
            return null;
        return ce._grismDataFileName;
    }

    private static String buildCoverageDataName(String grismName, String filter) {
        return grismName + "-" + filter;
    }

}
