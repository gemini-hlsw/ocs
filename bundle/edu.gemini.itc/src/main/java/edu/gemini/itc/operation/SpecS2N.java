package edu.gemini.itc.operation;

import edu.gemini.itc.base.VisitableSampledSpectrum;

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

}
