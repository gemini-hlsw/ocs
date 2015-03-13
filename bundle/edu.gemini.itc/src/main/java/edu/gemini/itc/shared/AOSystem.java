package edu.gemini.itc.shared;

/**
 * Common interface for AO systems (Gems and Altair).
 */
public interface AOSystem {

    SampledSpectrumVisitor getBackgroundVisitor();
    SampledSpectrumVisitor getTransmissionVisitor();
    SampledSpectrumVisitor getFluxAttenuationVisitor();
    SampledSpectrumVisitor getHaloFluxAttenuationVisitor();
    double getAOCorrectedFWHM();

}
