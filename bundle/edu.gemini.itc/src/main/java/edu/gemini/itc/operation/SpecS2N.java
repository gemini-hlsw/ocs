package edu.gemini.itc.operation;

import edu.gemini.itc.shared.VisitableSampledSpectrum;

/**
 * A set of common values that are needed by the SpectroscopyResult objects.
 */
public interface SpecS2N {

    VisitableSampledSpectrum getSignalSpectrum();
    VisitableSampledSpectrum getBackgroundSpectrum();
    VisitableSampledSpectrum getExpS2NSpectrum();
    VisitableSampledSpectrum getFinalS2NSpectrum();

    double getImageQuality();
    double getSpecFracWithSource();
    double getSpecNpix();

}
