package edu.gemini.itc.base;

import edu.gemini.itc.shared.*;
import edu.gemini.spModel.core.EmissionLine;
import edu.gemini.spModel.core.GaussianSource;
import edu.gemini.spModel.core.SpatialProfile;



/**
 * This is a collection of validation methods that originally were implemented repeatedly for the different
 * instruments and were rehomed here in a central location. It is questionable if this is the best place for all
 * of these checks, but at least they're all in one place now. Note that currently the error handling is based on
 * exceptions that are thrown if any problem inside the code is detected. For as long as all the central parts
 * of the ITC is Java code, this is probably the way to go.
 */
public final class Validation {

    public static void validate(final Instrument instrument, final ObservationDetails obs, final SourceDefinition source) {
        checkElineWidth(instrument, source);
        checkGaussianFwhm(source.profile());

        if (obs.analysisMethod() instanceof ApertureMethod) {
            checkSkyAperture((ApertureMethod) obs.analysisMethod());
        }
        if (obs.calculationMethod() instanceof S2NMethod) {
            checkSourceFraction(((S2NMethod) obs.calculationMethod()).exposures(), obs.calculationMethod().sourceFraction());
        }

    }

    private static void checkSkyAperture(final ApertureMethod am) {
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
     * Checks the relation between the emission line width and the resolution (sampling rate) of the data
     * files describing the atmospheric properties.
     */
    private static void checkElineWidth(final Instrument instrument, final SourceDefinition source) {

        if (source.distribution() instanceof EmissionLine) {

            // These values reflect the resolution of the data files we are currently using to describe the
            // atmospheric properties for the given wavelength ranges. In case the resolution of those files change,
            // these values should change, too. Ideally there was a way to know the resolution from the files,
            // but right now there is no such mechanism in place.
            final double resolution;
            switch (instrument.getBands()) {
                case VISIBLE: resolution = 0.50 * 2; break; // visible is sampled with 0.5 nm resolution
                case NEAR_IR: resolution = 0.02 * 2; break; // near ir is sampled with 0.02 nm resolution
                case MID_IR:  resolution = 2.00 * 2; break; // mid ir is sampled with 2 nm resolution
                default:      throw new Error();
            }

            final EmissionLine eLine = (EmissionLine) source.distribution();
            final double maxWidth = ITCConstants.C / (eLine.wavelength().toNanometers() * (1.0 + source.redshift().z() / resolution));

            if (eLine.width().toKilometersPerSecond() < maxWidth) {
                throw new IllegalArgumentException(
                    String.format(
                            "Please use a model line width > %.2f nm (or %.2f km/s) " +
                            "to avoid undersampling of the line profile when convolved " +
                            "with the transmission response", 1.0*resolution, maxWidth));
            }
        }
    }
}
