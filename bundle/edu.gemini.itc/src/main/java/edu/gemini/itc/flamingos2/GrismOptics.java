package edu.gemini.itc.flamingos2;

import edu.gemini.itc.base.Disperser;
import edu.gemini.itc.base.Instrument;
import edu.gemini.itc.base.DatFile;
import edu.gemini.itc.base.TransmissionElement;

import java.util.Hashtable;
import java.util.Scanner;

/**
 * This represents the transmission of the Grism optics.
 * See REL-557
 */
public class GrismOptics extends TransmissionElement implements Disperser {

    private static class CoverageEntry {
        public final double start;
        public final double end;
        public final double width;
        public final double resolution;
        public final String name;
        public final String grismDataFileName;

        CoverageEntry(final String name, final double start, final double end, final double width, final double res, final String grismDataFileName) {
            this.name       = name;
            this.start      = start;
            this.end        = end;
            this.width      = width;
            this.resolution = res;
            this.grismDataFileName = grismDataFileName;
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


    private final String grismName;
    private final double plateScale;
    private final CoverageEntry coverage;

    public GrismOptics(final String directory, final String grismName, final String filterBand, final double plateScale) {
        super(directory + getGrismDataFileName(grismName, filterBand));

        this.plateScale = plateScale;
        this.grismName = grismName;
        final String coverageDataName = buildCoverageDataName(grismName, filterBand);
        coverage = _coverage.get(coverageDataName);
        if (coverage == null) throw new RuntimeException("No coverage entry for " + coverageDataName);
    }

    public double getStart() {
        return coverage.start;
    }

    public double getEnd() {
        return coverage.end;
    }

    public double getEffectiveWavelength() {
        return (getStart() + getEnd()) / 2;
    }

    public double getPixelWidth() {
        return coverage.width;
    }

    public double resolutionHalfArcsecSlit() {
        return 0.5 / plateScale * dispersion(-1);
    }

    public double dispersion(double wv) {
        return getPixelWidth();
    }

    public String toString() {
        return "Grism Optics: " + grismName;
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
    private static String getGrismDataFileName(final String grismName, final String filter) {
        final String coverageDataName = buildCoverageDataName(grismName, filter);
        final CoverageEntry ce = _coverage.get(coverageDataName);
        if (ce == null) throw new RuntimeException("No coverage entry for " + coverageDataName);
        return ce.grismDataFileName;
    }

    private static String buildCoverageDataName(final String grismName, final String filter) {
        return grismName + "-" + filter;
    }

}
