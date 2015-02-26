package edu.gemini.itc.altair;

import edu.gemini.itc.shared.SampledSpectrum;
import edu.gemini.itc.shared.SampledSpectrumVisitor;

/**
 * The AltairFluxAttenuationVisitor class is designed to adjust the SED for the
 * by the FluxAttenuation factor of altair.
 */
public class AltairFluxAttenuationVisitor implements SampledSpectrumVisitor {

    private double fluxAttenuationFactor;


    /**
     * Constructs AltairBackgroundVisitor.
     */
    public AltairFluxAttenuationVisitor(double fluxAttenuationFactor) {

        this.fluxAttenuationFactor = fluxAttenuationFactor;
    }


    /**
     * Implements the SampledSpectrumVisitor interface
     */
    public void visit(SampledSpectrum sed) {
        //for (int i=0; i < sed.getLength(); i++) {
        //    sed.setY(i, sed.getY(i)*fluxAttenuationFactor);
        //}

        //use the sed provided rescale Y instead of above equivalent algorithm
        sed.rescaleY(fluxAttenuationFactor);
    }


    public String toString() {
        return "AltairFluxAttenuationVisitor :" + fluxAttenuationFactor;
    }
}
