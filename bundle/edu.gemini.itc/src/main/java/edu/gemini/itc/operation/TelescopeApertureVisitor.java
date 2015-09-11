// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE included with the distribution package.
//
// $Id: TelescopeApertureVisitor.java,v 1.2 2003/11/21 14:31:02 shane Exp $
//
package edu.gemini.itc.operation;

import edu.gemini.itc.base.SampledSpectrum;
import edu.gemini.itc.base.SampledSpectrumVisitor;

/**
 * This class encapsulates information about the telescope aperture.
 * Other classes can refer to this one for aperture information.
 * It also functions as a visitor to a SED.
 * The main function is to convert a SED from "flux per m^2" to absolute flux.
 */
public final class TelescopeApertureVisitor implements SampledSpectrumVisitor {
    /**
     * Area of 8 meter-diameter mirror minus 1 meter hole in middle.
     * (radius = 4 meters)
     */
    public static final double TELESCOPE_APERTURE = (Math.PI * (3.95 * 3.95)) -
            (Math.PI * (.65 * .65));

    /**
     * Current values in the SED are probably "per m^2".
     * This operation multiplies each spectrum value by the telescope area.
     */
    public void visit(SampledSpectrum sed) {
        sed.rescaleY(TELESCOPE_APERTURE);
        //Total fudge just to do one quick calculation.  CHANGE THIS BACK
        //sed.rescaleY(40.66);
    }

    public String toString() {
        return "TelescopeAperture " + TELESCOPE_APERTURE;
    }
}
