package edu.gemini.itc.operation;

import edu.gemini.itc.base.VisitableSampledSpectrum;

import java.util.stream.IntStream;

/**
 * A set of common values that are accessed by service users through the SpectroscopyResult object.
 */
public interface SpecS2N {

    VisitableSampledSpectrum getSignalSpectrum();
    VisitableSampledSpectrum getBackgroundSpectrum();
    VisitableSampledSpectrum getExpS2NSpectrum();
    VisitableSampledSpectrum getFinalS2NSpectrum();

    double getImageQuality();
    double getSpecFracWithSource();
    double getSpecNpix();

    default double getPeakPixelCount() {
        final double[] sig = getSignalSpectrum().getValues();
        final double[] bck = getBackgroundSpectrum().getValues();
        // TODO: check fi these conditions really hold true
        assert getSignalSpectrum().getStart() == getBackgroundSpectrum().getStart();
        assert getSignalSpectrum().getEnd() == getBackgroundSpectrum().getEnd();
        assert sig.length == bck.length;
        return IntStream.range(0, sig.length).mapToDouble(i -> bck[i] + sig[i]).max().getAsDouble(); // TODO how to avoid optional?
    }

}
