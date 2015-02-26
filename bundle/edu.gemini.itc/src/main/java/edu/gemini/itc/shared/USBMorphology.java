// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
//
package edu.gemini.itc.shared;

/**
 * The USBMorphology concreate class implements the operations that all
 * that are defined in it's abstract parent class Morphology2d.
 */
public class USBMorphology extends Morphology3D {


    /**
     * We should provide methods that allow the calculation of
     * integrals for square, circular.
     * <p/>
     * Method to get the 2D square integral on a USB source.  Easy! It is always 1
     *
     * @param xMin -x side of the square region
     * @param xMax +x side of the square region
     * @param yMin -y side of the square region
     * @param yMax +y side of the square region
     * @return returns the amount of the source in the 2D square.  Always 1 for this source.
     */


    public double get2DSquareIntegral(double xMin, double xMax, double yMin, double yMax) {
        return 1;
    }


    /**
     * The accept method for the Visitor pattern
     *
     * @param v Visitor
     * @throws java.lang.Exception thown in something goes wrong
     */
    public void accept(MorphologyVisitor v) throws Exception {
        v.visitUSB(this);
    }


}
