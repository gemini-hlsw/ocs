// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
// $Id: DustScreenVisitor.java,v 1.2 2003/11/21 14:31:02 shane Exp $
//
package edu.gemini.itc.operation;

import edu.gemini.itc.shared.SampledSpectrumVisitor;
import edu.gemini.itc.shared.SampledSpectrum;

/**
 * This visitor acts as a dust screen for the spectrum.
 */
public class DustScreenVisitor implements SampledSpectrumVisitor {

    private double _av = 0;  // Visual extinction

    /**
     * @param av visual extinction used in dust extinction law
     */
    public DustScreenVisitor(double av) {
        _av = av;
    }

    /**
     * Performs the extinction on specified spectrum.
     */
    public void visit(SampledSpectrum sed) {
        for (int i = 0; i < sed.getLength(); i++) {
            sed.setY(i, sed.getY(i) * Math.pow(10, (-0.4 * _av)));
        }
        return;
    }

    public String toString() {
        return "Dust Screen Visual Extinction (Av) = " + _av;
    }
}
