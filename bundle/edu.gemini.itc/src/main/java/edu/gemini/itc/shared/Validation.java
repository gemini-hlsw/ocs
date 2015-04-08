package edu.gemini.itc.shared;

public final class Validation {

    public static void checkSourceFraction(double nExp, double fracSource) {
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
