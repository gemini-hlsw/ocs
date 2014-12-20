// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
// $Id: GemsFluxAttenuationVisitor.java,v 1.1 2004/01/12 16:22:25 bwalls Exp $
//
package edu.gemini.itc.gems;

import edu.gemini.itc.shared.SampledSpectrumVisitor;
import edu.gemini.itc.shared.SampledSpectrum;

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
    public void visit(SampledSpectrum sed) throws Exception {
        //use the sed provided rescale Y instead of above equivalent algorithm
        sed.rescaleY(fluxAttenuationFactor);
    }


    public String toString() {
        return "GemsFluxAttenuationVisitor :" + fluxAttenuationFactor;
    }
}
