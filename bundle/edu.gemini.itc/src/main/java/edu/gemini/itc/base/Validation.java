package edu.gemini.itc.base;

import edu.gemini.itc.shared.AnalysisMethod;
import edu.gemini.itc.shared.ObservationDetails;
import edu.gemini.itc.shared.SourceDefinition;
import edu.gemini.spModel.target.GaussianSource;
import edu.gemini.spModel.target.SpatialProfile;

/**
 * This is a collection of validation methods that originally were implemented repeatedly for the different
 * instruments and were rehomed here in a central location. It is questionable if this is the best place for all
 * of these checks, but at least they're all in one place now. Note that currently the error handling is based on
 * exceptions that are thrown if any problem inside the code is detected. For as long as all the central parts
 * of the ITC is Java code, this is probably the way to go.
 */
public final class Validation {

    public static void validate(final ObservationDetails obs, final SourceDefinition source, final double elineFudge) {
        checkElineWidth(source, elineFudge);
        checkSourceFraction(obs.getNumExposures(), obs.getSourceFraction());
        checkGaussianFwhm(source.profile);
        checkSkyAperture(obs.getAnalysis());
    }

    private static void checkSkyAperture(final AnalysisMethod am) {
        if (am.skyAperture() < 1.0) {
            throw new IllegalArgumentException("The sky aperture must be 1.0 or greater.");
        }
    }

    private static void checkGaussianFwhm(final SpatialProfile sp) {
        if (sp instanceof GaussianSource) {
            if (((GaussianSource) sp).fwhm() < 0.1) {
                throw new IllegalArgumentException("Please use a Gaussian FWHM of 0.1 or greater.");
            }
        }
    }

    private static void checkSourceFraction(final double nExp, final double fracSource) {
        final double epsilon = 0.2;
        final double number_source_exposures = nExp * fracSource;
        final int iNumExposures = (int) (number_source_exposures + 0.5);
        final double diff = number_source_exposures - iNumExposures;

        if (Math.abs(diff) > epsilon) {
            throw new IllegalArgumentException(
                String.format(
                        "Fraction with source value produces non-integral number of " +
                        "source exposures with source (%.2f vs. %d).", number_source_exposures, iNumExposures));
        }
    }

    /**
     * Checks the relation between the emission line width and the emission line wavelength.
     * It is not clear to me what the purpose/origin of the seemingly instrument dependent "fudge" factor is,
     * I am just centralising and reproducing the original functionality here. The original code contained the
     * comments "fudge x b/c of increased resolution of transmission files" next to those numbers.
     * @param source
     * @param fudge
     */
    private static void checkElineWidth(final SourceDefinition source, final double fudge) {
        if (source.getDistributionType().equals(SourceDefinition.Distribution.ELINE)) {

            final double maxWidth = 3E5 / (source.getELineWavelength().toNanometers() * fudge);

            if (source.getELineWidth().toKilometersPerSecond() < maxWidth) {
                throw new IllegalArgumentException(
                    String.format(
                            "Please use a model line width > %.2f nm (or %.2f km/s) " +
                            "to avoid undersampling of the line profile when convolved " +
                            "with the transmission response", 1.0/fudge, maxWidth));
            }
        }
    }
}
