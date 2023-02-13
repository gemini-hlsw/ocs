package edu.gemini.itc.base;

import java.util.ArrayList;
import java.util.List;

/**
 * This is the concrete class is one type of aperture supported by the itc
 * For now we will implement it exactly like a square aperture.
 * The class also plays the role of Visitor to a morphology.  This allows
 * the class to calculate different values of the SourceFraction for different
 * types of Morphologies.
 */
public final class HexagonalAperture extends ApertureComponent {
    private final List<Double> sourceFraction = new ArrayList<>();
    private double ifuDiam;
    private double ifuPosX;
    private double ifuPosY;

    public double getIfuDiam() {
        return ifuDiam;
    }

    public double getIfuPosX() {
        return ifuPosX;
    }

    public double getIfuPosY() {
        return ifuPosY;
    }

    public HexagonalAperture(final double ifuPosX, final double ifuPosY, final double ifuDiam) {
        this.ifuDiam = ifuDiam;
        this.ifuPosX = ifuPosX;
        this.ifuPosY = ifuPosY;
    }


    //Methods for visiting a Morphology
    public void visitGaussian(final Morphology3D morphology) {
        final double xLower = ifuPosX - ifuDiam / 2;
        final double xUpper = ifuPosX + ifuDiam / 2;
        final double yLower = ifuPosY - ifuDiam / 2;
        final double yUpper = ifuPosY + ifuDiam / 2;
        final double fractionOfSourceInAperture = morphology.get2DSquareIntegral(xLower, xUpper, yLower, yUpper);
        sourceFraction.add(fractionOfSourceInAperture);
    }

    public void visitAO(final Morphology3D morphology) {
        final double xLower = ifuPosX - ifuDiam / 2;
        final double xUpper = ifuPosX + ifuDiam / 2;
        final double yLower = ifuPosY - ifuDiam / 2;
        final double yUpper = ifuPosY + ifuDiam / 2;
        final double fractionOfSourceInAperture = morphology.get2DSquareIntegral(xLower, xUpper, yLower, yUpper);
        sourceFraction.add(fractionOfSourceInAperture);
    }

    public void visitUSB(final Morphology3D morphology) {
        // Original ancient comment: "Might work, not sure."
        sourceFraction.add(ifuDiam * ifuDiam);
    }

    //Method for returning the Sourcefraction for this Aperture
    public List<Double> getFractionOfSourceInAperture() {
        return sourceFraction;
    }

    public void clearFractionOfSourceInAperture() {
        sourceFraction.clear();
    }
}
