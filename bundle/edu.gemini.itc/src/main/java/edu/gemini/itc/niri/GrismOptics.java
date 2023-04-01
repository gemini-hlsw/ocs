package edu.gemini.itc.niri;

import edu.gemini.itc.base.DatFile;
import edu.gemini.itc.base.Disperser;
import edu.gemini.itc.base.Instrument;
import edu.gemini.itc.base.TransmissionElement;
import edu.gemini.itc.operation.Slit;

import java.util.*;

/**
 * This represents the transmission of the Grism optics.
 */
public final class GrismOptics extends TransmissionElement implements Disperser {

    private static final String JGRISM = "J-grism";
    private static final String HGRISM = "H-grism";
    private static final String KGRISM = "K-grism";
    private static final String LGRISM = "L-grism";
    private static final String MGRISM = "M-grism";


    // === static code for caching of coverage and resolution data files
    private static final class Coverage {
        final double start;
        final double end;
        final double pixelWidth;
        Coverage(final double start, final double end, final double pixelWidth) {
            this.start = start;
            this.end = end;
            this.pixelWidth = pixelWidth;
        }
    }
    private static final Map<String, List<Integer>> resolutions = new HashMap<>();
    private static final Map<String, List<Coverage>> coverages = new HashMap<>();

    private static List<Integer> loadResolutions(final String file) {
        final List<Integer> resolvingPower = new ArrayList<>();
        try (final Scanner scan = DatFile.scanFile(file)) {
            while (scan.hasNext()) {
                resolvingPower.add(scan.nextInt());
            }
        }
        return resolvingPower;
    }
    private static synchronized List<Integer> getResolution(final String file) {
        if (!resolutions.containsKey(file)) {
            resolutions.put(file, loadResolutions(file));
        }
        return resolutions.get(file);
    }

    private static List<Coverage> loadCoverages(final String file) {
        final List<Coverage> coverages = new ArrayList<>();
        try (final Scanner scan = DatFile.scanFile(file)) {
            while (scan.hasNext()) {
                final double start = scan.nextDouble();
                final double end   = scan.nextDouble();
                final double pixW  = scan.nextDouble();
                coverages.add(new Coverage(start, end, pixW));
            }
        }
        return coverages;
    }
    private static synchronized List<Coverage> getCoverage(final String file) {
        if (!coverages.containsKey(file)) {
            coverages.put(file, loadCoverages(file));
        }
        return coverages.get(file);
    }


    // == non static stuff
    private final List<Coverage> coverage;
    private final List<Integer> resolution;
    private final String grismName;

    public GrismOptics(final String directory,
                       final String grismName,
                       final String cameraName,
                       final String focalPlaneMaskOffset,
                       final String stringSlitWidth) {

        super(directory + Niri.getPrefix() + grismName + "_" + cameraName + Instrument.getSuffix());

        final String grismFile    = directory + Niri.getPrefix() + "grism-resolution-" + stringSlitWidth + "_" + cameraName + Instrument.getSuffix();
        final String coverageFile = directory + Niri.getPrefix() + "grism-coverage-" + focalPlaneMaskOffset + Instrument.getSuffix();

        this.grismName  = grismName;
        this.resolution = getResolution(grismFile);
        this.coverage   = getCoverage(coverageFile);
    }

    public double getStart() {
        return coverage.get(getGrismNumber()).start;
    }

    public double getEnd() {
        return coverage.get(getGrismNumber()).end;
    }

    public double getEffectiveWavelength() {
        return (getStart() + getEnd()) / 2;
    }

    public double getPixelWidth() {
        return coverage.get(getGrismNumber()).pixelWidth;
    }

    public int getGrismNumber() {
        switch (grismName) {
            case JGRISM:    return 0;
            case HGRISM:    return 1;
            case KGRISM:    return 2;
            case LGRISM:    return 3;
            case MGRISM:    return 4;
            default:        throw new Error();
        }
    }

    // For NIRI we can not use the extrapolation from the half arcsec slit as we do for other instruments.
    // Therefore this method is undefined and must not be used for NIRI!
    public double resolutionHalfArcsecSlit() {
        throw new Error("not implemented for NIRI");
    }

    // Calculates the resolution for the current grism and wavelength.
    public double resolution(final Slit slit) {
        return getEffectiveWavelength() / resolution.get(getGrismNumber());
    }

    // Calculates the resolution for the current grism and wavelength. The image quality is not taken into account;
    // for NIRI the resolution in the files assumes that the target fills the slit (IQ>slit width).
    public double resolution(final Slit slit, final double imgQuality) {
        return resolution(slit);
    }

    public double dispersion(double wv) {
        return coverage.get(getGrismNumber()).pixelWidth;
    }

    public String toString() {
        return "Grism Optics: " + grismName;
    }

}
