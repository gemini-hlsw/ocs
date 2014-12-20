// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.

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
    public abstract List getFractionOfSourceInAperture();
    public abstract void clearFractionOfSourceInAperture();
}
