// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: SameTargetCatalogAlgorithm.java 7615 2007-02-13 14:15:37Z anunez $
//

package edu.gemini.shared.cat;

/**
 * Base class for algorithms that returns the same target as guide star
 */
public abstract class SameTargetCatalogAlgorithm extends AbstractCatalogAlgorithm {
    /**
     * Base class for setting guide stars that are the same as the target
     *
     * @param name display name
     * @param desc description
     */
    protected SameTargetCatalogAlgorithm(String name, String desc) {
        super(name, desc);
        setStarTypeOptions(new String[]{"WFS", "OIWFS", "AOWFS"});
    }
}
