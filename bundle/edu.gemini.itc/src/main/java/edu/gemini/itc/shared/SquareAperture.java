// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.

package edu.gemini.itc.shared;

import java.util.List;
import java.util.ArrayList;

/**
 * This is the concrete class is one type of aperture supported by the itc
 * The class also plays the role of Visitor to a morphology.  This allows
 * the class to calculate different values of the SourceFraction for different
 * types of Morphologies.
 */
public class SquareAperture extends ApertureComponent {
    List sourceFraction = new ArrayList();
    int IFUdiam, IFUposX, IFUposY;

    public SquareAperture(int IFUdiam, int IFUposX, int IFUposY) {
        this.IFUdiam = IFUdiam;
        this.IFUposX = IFUposX;
        this.IFUposY = IFUposY;
    }


    //Methods for visiting a Morphology
    public void visitGaussian(Morphology3D morphology) {
        int xLower = IFUposX - IFUdiam / 2;
        int xUpper = IFUposX + IFUdiam / 2;
        int yLower = IFUposY - IFUdiam / 2;
        int yUpper = IFUposY + IFUdiam / 2;

        double fractionOfSourceInAperture;

        fractionOfSourceInAperture = morphology.get2DSquareIntegral(xLower, xUpper, yLower, yUpper);

        sourceFraction.add(new Double(fractionOfSourceInAperture));


    }
    
    public void visitAO(Morphology3D morphology) {
        int xLower = IFUposX - IFUdiam / 2;
        int xUpper = IFUposX + IFUdiam / 2;
        int yLower = IFUposY - IFUdiam / 2;
        int yUpper = IFUposY + IFUdiam / 2;

        double fractionOfSourceInAperture;

        fractionOfSourceInAperture = morphology.get2DSquareIntegral(xLower, xUpper, yLower, yUpper);

        sourceFraction.add(new Double(fractionOfSourceInAperture));

    }

    public void visitUSB(Morphology3D morphology) {
        // Might work, not sure.
        sourceFraction.add(new Double(IFUdiam * IFUdiam * Math.PI / 4));
    }

    public void visitExponential(Morphology3D morphology) {
        // not implemented
    }

    public void visitElliptical(Morphology3D morphology) {
        // not implemented
    }

    //Method for returning the Sourcefraction for this Aperture
    public List getFractionOfSourceInAperture() {
        return sourceFraction;
    }
    
    public void clearFractionOfSourceInAperture() {
        sourceFraction.clear();
    }

}
