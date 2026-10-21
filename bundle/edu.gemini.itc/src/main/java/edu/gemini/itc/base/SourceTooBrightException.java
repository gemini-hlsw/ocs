package edu.gemini.itc.base;

/**
 * Thrown when the detector well fills to half capacity faster than the shortest exposure allowed.
 * GPP reads the times from this exception instead of parsing the message, so the wording can change.
 */
public final class SourceTooBrightException extends RuntimeException {
    private final double halfWellTime;
    private final double minExposureTime;

    public SourceTooBrightException(final double halfWellTime, final double minExposureTime) {
        super(String.format("This target is too bright for this configuration.\n" +
                "The detector well is half filled in %.2f seconds (minimum is %.2f seconds).", halfWellTime, minExposureTime));
        this.halfWellTime    = halfWellTime;
        this.minExposureTime = minExposureTime;
    }

    public double getHalfWellTime() {
        return halfWellTime;
    }

    public double getMinExposureTime() {
        return minExposureTime;
    }
}
