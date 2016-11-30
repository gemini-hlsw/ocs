package edu.gemini.itc.base;

/**
 * This interface plays the role of Visitor in the Visitor pattern.
 * See Design Patterns by GoF.
 * Background on the Visitor pattern:
 * This pattern separates operations from the elements they operate on.
 * It is useful when the elements are more stable than the operations.
 * By putting the operations in Visitors instead of in the Elements,
 * operations can be added and changed freely without modifying the
 * Elements.
 * The Visitor and each Concrete Visitor must know about every
 * Concrete Element.
 * In this case we have only one Concrete Element, the SED.
 * But the motivation for using Visitor is that we want to make
 * "recipes" of operations consisting of a list of Visitors.
 * So the operations themselves are an abstraction in this problem.
 */
@FunctionalInterface
public interface SampledSpectrumVisitor {
    void visit(SampledSpectrum spectrum);
}
