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
import java.util.ListIterator;

/**
 * This is the concrete class that plays the role of Composite in the
 * composite pattern from GoF book.  This allows an aperture or a group of
 * apertures to be treated the same by the program.  This will be helpful
 * when we implement multiple IFU's.
 * The class also plays the role of Visitor to a morphology.  This allows
 * the class to calculate different values of the SourceFraction for different
 * types of Morphologies.
 */
public class ApertureComposite extends ApertureComponent {
    List apertureList = new ArrayList();

    //Use the following to traverse a list;

    //ListIterator iter = apertureList.listIterator();
    //while (iter1.hasNext()) {}

    //Methods for creating and removing apertures
    public void addAperture(ApertureComponent ap) {
        apertureList.add(ap);
    }

    public void removeAperture(int x) {
        if (x < apertureList.size() && x >= 0)
            apertureList.remove(x);
    }

    public void removeAllApertures() {
        apertureList.clear();
    }

    //public ApertureComponent getAperture(int x, int y)  // might use this someday

    public ApertureComponent getAperture(int x) {
        if (x < apertureList.size() && x >= 0)
            return (ApertureComponent) apertureList.get(x);
        else
            return null;
    }

    private int getNumberOfApertures() {
        return apertureList.size();
    }

    //Methods for visiting with a group of apertures
    public void visitGaussian(Morphology3D morphology) {
        ListIterator apIter = apertureList.listIterator();
        while (apIter.hasNext()) {
            try {
                ((ApertureComponent) apIter.next()).visitGaussian(morphology);
            } catch (Exception e) {
                System.out.println(" Could Not visit Gaussian Morpology with an aperture.");
            }

        }
    }
    
    public void visitAO(Morphology3D morphology) {
        ListIterator apIter = apertureList.listIterator();
        while (apIter.hasNext()) {
            try {
                ((ApertureComponent) apIter.next()).visitAO(morphology);
            } catch (Exception e) {
                System.out.println(" Could Not visit AO Morpology with an aperture.");
            }

        }
    }

    public void visitUSB(Morphology3D morphology) {
        ListIterator apIter = apertureList.listIterator();
        while (apIter.hasNext()) {
            try {
                ((ApertureComponent) apIter.next()).visitUSB(morphology);
            } catch (Exception e) {
                System.out.println(" Could Not visit USB morphology with an aperture.");
            }

        }
    }

    public void visitExponential(Morphology3D morphology) {
        ListIterator apIter = apertureList.listIterator();
        while (apIter.hasNext()) {
            try {
                ((ApertureComponent) apIter.next()).visitExponential(morphology);
            } catch (Exception e) {
                System.out.println(" Could Not visit Exponential Morphology with an aperture.");
            }

        }
    }

    public void visitElliptical(Morphology3D morphology) {
        ListIterator apIter = apertureList.listIterator();
        while (apIter.hasNext()) {
            try {
                ((ApertureComponent) apIter.next()).visitElliptical(morphology);
            } catch (Exception e) {
                System.out.println(" Could Not visit Elliptical Morphology with an aperture.");
            }

        }
    }

    //Methods for distributing a getSourceFraction query to multiple apertures
    public List getFractionOfSourceInAperture() {
        List sourceFractionsList = new ArrayList();

        ListIterator apIter = apertureList.listIterator();
        while (apIter.hasNext()) {
            // there should only one source fraction per aperture.  Code might need to be
            // changed if we implement different clusters other than in a line.
            try {
                sourceFractionsList.add(
                        (((ApertureComponent) apIter.next())
                         .getFractionOfSourceInAperture()).get(0));
            } catch (Exception e) {
                System.out.println(" Could Not get Source Fraction.");
            }
        }

        return sourceFractionsList;
    }
    
    public void clearFractionOfSourceInAperture() {
        ListIterator apIter = apertureList.listIterator();
        
        while (apIter.hasNext()) {
            // there should only one source fraction per aperture.  Code might need to be
            // changed if we implement different clusters other than in a line.
            try {
                ((ApertureComponent) apIter.next()).clearFractionOfSourceInAperture();
            } catch (Exception e) {
                System.out.println(" Could Not get Source Fraction.");
            }
        }
    }

}
