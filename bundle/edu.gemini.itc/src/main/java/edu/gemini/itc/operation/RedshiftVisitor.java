// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
// $Id: RedshiftVisitor.java,v 1.3 2003/11/21 14:31:02 shane Exp $
//
package edu.gemini.itc.operation;

import edu.gemini.itc.shared.SampledSpectrumVisitor;
import edu.gemini.itc.shared.SampledSpectrum;

/**
 * This visitor performs a redshift on the spectrum.
 */
public class RedshiftVisitor implements SampledSpectrumVisitor {
    /**
     * For efficiency, no shift will be performed unless z is
     * larger than this value
     */
    public static final double MIN_SHIFT = 0.0001;

    private double _z = 0;  // z = v / c

    /**
     * @param z redshift = velocity / c
     */
    public RedshiftVisitor(double z) {
        _z = z;
    }

    /**
     * Performs the redshift on specified spectrum.
     */
    public void visit(SampledSpectrum sed) {
        // only shift if greater than MIN_SHIFT
        if (getShift() <= MIN_SHIFT) return;  // No scaling to be done.

        sed.rescaleX(1.0 + getShift());
        return;
    }

    /**
     * @return the redshift
     */
    public double getShift() {
        return _z;
    }

    /**
     * Sets the redshift.
     */
    public void setShift(double z) {
        _z = z;
    }

    public String toString() {
        return "Redshift z = " + getShift();
    }
}
