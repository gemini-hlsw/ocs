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
    void accept(MorphologyVisitor v) throws Exception;
}
