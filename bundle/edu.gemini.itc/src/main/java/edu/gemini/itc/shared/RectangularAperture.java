package edu.gemini.itc.shared;

import java.util.ArrayList;
import java.util.List;

/**
 * This is the concrete class is one type of aperture supported by the itc
 * The class also plays the role of Visitor to a morphology.  This allows
 * the class to calculate different values of the SourceFraction for different
 * types of Morphologies.
 */
public final class RectangularAperture extends ApertureComponent {
    private final List<Double> sourceFraction = new ArrayList<>();
    private final double IFUlenX;
    private final double IFUlenY;
    private final double IFUposX;
    private final double IFUposY;

    public RectangularAperture(final double IFUlenX, final double IFUlenY, final double IFUposX, final double IFUposY) {
        this.IFUlenX = IFUlenX;
        this.IFUlenY = IFUlenY;
        this.IFUposX = IFUposX;
        this.IFUposY = IFUposY;
    }


    //Methods for visiting a Morphology
    public void visitGaussian(final Morphology3D morphology) {
        final double xLower = IFUposX - IFUlenX / 2;
        final double xUpper = IFUposX + IFUlenX / 2;
        final double yLower = IFUposY - IFUlenY / 2;
        final double yUpper = IFUposY + IFUlenY / 2;
        final double fractionOfSourceInAperture = morphology.get2DSquareIntegral(xLower, xUpper, yLower, yUpper);
        sourceFraction.add(fractionOfSourceInAperture);
    }

    public void visitAO(final Morphology3D morphology) {
        final double xLower = IFUposX - IFUlenX / 2;
        final double xUpper = IFUposX + IFUlenX / 2;
        final double yLower = IFUposY - IFUlenY / 2;
        final double yUpper = IFUposY + IFUlenY / 2;
        final double fractionOfSourceInAperture = morphology.get2DSquareIntegral(xLower, xUpper, yLower, yUpper);
        sourceFraction.add(fractionOfSourceInAperture);
    }

    public void visitUSB(final Morphology3D morphology) {
        // Original ancient comment: "Might work, not sure."
        sourceFraction.add(IFUlenX * IFUlenY * Math.PI / 4);
    }

    //Method for returning the Sourcefraction for this Aperture
    public List<Double> getFractionOfSourceInAperture() {
        return sourceFraction;
    }

    public void clearFractionOfSourceInAperture() {
        sourceFraction.clear();
    }

}
