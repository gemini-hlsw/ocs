package edu.gemini.itc.gems;

import edu.gemini.itc.shared.SampledSpectrum;
import edu.gemini.itc.shared.SampledSpectrumVisitor;

/**
 * The GemsFluxAttenuationVisitor class is designed to adjust the SED for the
 * by the FluxAttenuation factor of gems.
 */
public class GemsFluxAttenuationVisitor implements SampledSpectrumVisitor {

    private double fluxAttenuationFactor;


    /**
     * Constructs GemsBackgroundVisitor.
     */
    public GemsFluxAttenuationVisitor(double fluxAttenuationFactor) {

        this.fluxAttenuationFactor = fluxAttenuationFactor;
    }


    /**
     * Implements the SampledSpectrumVisitor interface
     */
    public void visit(SampledSpectrum sed) {
        //use the sed provided rescale Y instead of above equivalent algorithm
        sed.rescaleY(fluxAttenuationFactor);
    }


    public String toString() {
        return "GemsFluxAttenuationVisitor :" + fluxAttenuationFactor;
    }
}
