// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.

package edu.gemini.itc.shared;

/**
 * This class supplies utility routines for dealing with gaussian curves.
 */
public class Gaussian {
    // Values of a 2-D gaussian integral over a square region.
    private static final double[] _2D_INTEGRAL =
            {0, 0.031, 0.118, 0.245, 0.393, 0.542, 0.675, 0.784, 0.865, 0.920, 0.956};

    // This is a table of the integral of a 2-D gaussian over a square region.
    // X is the length of the square as a fraction of sigma.
    // Y is the value of the double integral.
    private static DefaultSampledSpectrum _2d_integralTable = new
            DefaultSampledSpectrum(_2D_INTEGRAL, 0, 0.5);

    /**
     * This method returns the value of a 2-D circularly-symmetric gaussian
     * over a square region centered about the mean.
     * @param sigmaFraction The length of the square as a fraction of sigma.
     */
    public static double get2DIntegral(double sigmaFraction) {
        // Use linear interpolation on the table of pre-calculated results.
        return _2d_integralTable.getY(sigmaFraction);
    }
}
