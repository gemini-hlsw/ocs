// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
// $Id: SampledSpectrumVisitor.java,v 1.2 2003/11/21 14:31:02 shane Exp $
//
package edu.gemini.itc.shared;

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
public interface SampledSpectrumVisitor {
    void visit(SampledSpectrum spectrum) throws Exception;
}
