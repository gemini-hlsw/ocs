package edu.gemini.itc.base;

/**
 * The VisitableMorphology plays the role of Element in a visitor pattern.
 * This pattern is used to separate operations from the elements.
 * Because of this separation, Concrete Elements must offer enough
 * accessors for the separate Concrete Visitor class to perform the
 * manipulation.
 */
public interface VisitableMorphology {
    /**
     * The accept(MorphologyVisitor) method is used by Visitors to
     * visit the Morphology.
     * This is the way a Morphology is manipulated.
     */
    void accept(MorphologyVisitor v);
}
