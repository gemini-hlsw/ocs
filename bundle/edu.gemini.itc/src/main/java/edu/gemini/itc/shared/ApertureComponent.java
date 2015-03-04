package edu.gemini.itc.shared;

import java.util.List;


/**
 * This is the abstract class that plays the role of Component in the
 * composite pattern from GoF book.  This allows an aperture or a group of
 * apertures to be treated the same by the program.  This will be helpful
 * when we implement multiple IFU's.
 * The class also plays the role of Visitor to a morphology.  This allows
 * the class to calculate different values of the SourceFraction for different
 * types of Morphologies.
 */
public abstract class ApertureComponent implements MorphologyVisitor {
    public abstract List<Double> getFractionOfSourceInAperture();

    public abstract void clearFractionOfSourceInAperture();
}
