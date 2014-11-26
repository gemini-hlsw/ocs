// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
// $Id: VisitableSampledSpectrum.java,v 1.2 2003/11/21 14:31:02 shane Exp $
//
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
     *
     * Example:
     *
     * SampledSpectrum s = SampledSpectrumFactory.
     *    getSampledSpectrum("SampledSpectrumFILE");
     * SampledSpectrumVisitor r = new ResampleVisitor();
     * s.Accept(r);
     *
     */
    void accept(SampledSpectrumVisitor v) throws Exception;
}
