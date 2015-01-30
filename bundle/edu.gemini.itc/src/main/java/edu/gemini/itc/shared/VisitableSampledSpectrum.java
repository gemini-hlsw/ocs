package edu.gemini.itc.shared;

/**
 * The VisitableSampledSpectrum plays the role of Element in a visitor pattern.
 * This pattern is used to separate operations from the elements.
 * Because of this separation, Concrete Elements must offer enough
 * accessors for the separate Concrete Visitor class to perform the
 * manipulation.
 * This interface adds one new method to the SampledSpectrum interface
 * to allow visitors.
 */
public interface VisitableSampledSpectrum extends SampledSpectrum {
    /**
     * The accept(SampledSpectrumVisitor) method is used by Visitors to
     * visit the SampledSpectrum.
     * This is the way a SampledSpectrum is manipulated.
     * <p/>
     * Example:
     * <p/>
     * SampledSpectrum s = SampledSpectrumFactory.
     * getSampledSpectrum("SampledSpectrumFILE");
     * SampledSpectrumVisitor r = new ResampleVisitor();
     * s.Accept(r);
     */
    void accept(SampledSpectrumVisitor v);
}
