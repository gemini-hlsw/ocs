package edu.gemini.itc.base;

import java.util.ArrayList;
import java.util.List;

/**
 * This is the concrete class that plays the role of Composite in the
 * composite pattern from GoF book.  This allows an aperture or a group of
 * apertures to be treated the same by the program.  This will be helpful
 * when we implement multiple IFU's.
 * The class also plays the role of Visitor to a morphology.  This allows
 * the class to calculate different values of the SourceFraction for different
 * types of Morphologies.
 */
public final class ApertureComposite extends ApertureComponent {
    private final List<ApertureComponent> apertureList = new ArrayList<>();


    //Methods for creating and removing apertures
    public void addAperture(final ApertureComponent ap) {
        apertureList.add(ap);
    }

    //Methods for visiting with a group of apertures
    public void visitGaussian(final Morphology3D morphology) {
        for (final ApertureComponent ac : apertureList) {
            ac.visitGaussian(morphology);
        }
    }

    public void visitAO(final Morphology3D morphology) {
        for (final ApertureComponent ac : apertureList) {
            ac.visitAO(morphology);
        }
    }

    public void visitUSB(final Morphology3D morphology) {
        for (final ApertureComponent ac : apertureList) {
            ac.visitUSB(morphology);
        }
    }

    //Methods for distributing a sourceFraction query to multiple apertures
    public List<Double> getFractionOfSourceInAperture() {
        final List<Double> sourceFractionsList = new ArrayList<>();
        for (final ApertureComponent ac : apertureList) {
            // there should only one source fraction per aperture.  Code might need to be
            // changed if we implement different clusters other than in a line.
            sourceFractionsList.add(ac.getFractionOfSourceInAperture().get(0));
        }
        return sourceFractionsList;
    }

    public void clearFractionOfSourceInAperture() {
        for (final ApertureComponent ac : apertureList) {
            // there should only one source fraction per aperture.  Code might need to be
            // changed if we implement different clusters other than in a line.
            ac.clearFractionOfSourceInAperture();
        }
    }

    public List<ApertureComponent> getApertureList(){
        return apertureList;
    }

}
