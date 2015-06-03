package edu.gemini.itc.base;

import edu.gemini.itc.shared.AnalysisMethod;
import edu.gemini.itc.shared.ObservationDetails;
import edu.gemini.itc.shared.SourceDefinition;
import edu.gemini.spModel.target.GaussianSource;
import edu.gemini.spModel.target.SpatialProfile;

public final class Validation {

    public static void validate(ObservationDetails obs, SourceDefinition source) {
        checkSourceFraction(obs.getNumExposures(), obs.getSourceFraction());
        checkGaussianFwhm(source.profile);
        checkSkyAperture(obs.getAnalysis());
    }

    private static void checkSkyAperture(AnalysisMethod am) {
        if (am.skyAperture() < 1.0) throw new IllegalArgumentException("The sky aperture must be 1.0 or greater.");
    }

    private static void checkGaussianFwhm(SpatialProfile sp) {
        if (sp instanceof GaussianSource) {
            if (((GaussianSource) sp).fwhm() < 0.1) {
                throw new IllegalArgumentException("Please use a Gaussian FWHM of 0.1 or greater.");
            }
        }
    }

    private static void checkSourceFraction(double nExp, double fracSource) {
        double epsilon = 0.2;
        double number_source_exposures = nExp * fracSource;
        int iNumExposures = (int) (number_source_exposures + 0.5);
        double diff = number_source_exposures - iNumExposures;
        if (Math.abs(diff) > epsilon) {
            throw new IllegalArgumentException(
                    "Fraction with source value produces non-integral number of source exposures with source (" +
                            number_source_exposures + " vs. " + iNumExposures + ").");
        }
    }

}
