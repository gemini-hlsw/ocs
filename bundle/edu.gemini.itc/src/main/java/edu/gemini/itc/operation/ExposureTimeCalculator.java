package edu.gemini.itc.operation;

import java.util.logging.Logger;

public final class ExposureTimeCalculator {

    private static final Logger Log = Logger.getLogger(ExposureTimeCalculator.class.getName());

    private ExposureTimeCalculator() {}  // prevent instantiation

    /**
     * Calculate the exposure time required to achieve a desired Signal-to-Noise on a single frame.
     * @param signal     The signal per second summed over the pixels in the aperture.
     * @param background The background per second summed over the pixels in the aperture.
     * @param darkNoise  The dark current per second summed over the pixels in the aperture.
     * @param readNoise  The squared read noise summed over the pixels in the aperture.
     * @param skyAper
     * @param SNR        The desired Signal-to-Noise Ratio.
     * @return The exposure time in seconds.
     */
    public static double calculate(
            double signal,
            double background,
            double darkNoise,
            double readNoise,
            double skyAper,
            double SNR
    ) {
        final double f = 1. + (1. / skyAper);  // noise factor
        // SNR = S / sqrt(S + f(B + D + R)) = st / sqrt(st + f(bt + dt + R)) = x
        // This can be expressed as a quadratic equation:  (ss/xx)t^2 - (s + fb + fd)t - fR = 0
        // and solved using the quadratic formula: t = (-b + sqrt(b^2 - 4ac)) / 2a
        double a = signal * signal / (SNR * SNR);
        double b = -(signal + f * background + f * darkNoise);
        double c = -f * readNoise;
        double exposureTime = (-b + Math.sqrt(b*b - 4.*a*c)) / (2.*a);
        Log.fine(String.format("Exposure time must be %.3f s to achieve S/N = %.2f in 1 exposure", exposureTime, SNR));
        return exposureTime;
    }
}
