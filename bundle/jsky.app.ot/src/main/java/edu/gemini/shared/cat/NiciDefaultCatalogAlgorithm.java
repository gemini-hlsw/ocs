// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: NiciDefaultCatalogAlgorithm.java 7615 2007-02-13 14:15:37Z anunez $
//

package edu.gemini.shared.cat;

/**
 * Algorithm for suggesting a NICI OIWFS star. NICI OIWFS start will be the same target
 * $Id: NiciDefaultCatalogAlgorithm.java 7615 2007-02-13 14:15:37Z anunez $
 */
public final class NiciDefaultCatalogAlgorithm extends SameTargetCatalogAlgorithm {
    private static final String NAME = "Target is NICI AO Guide Star";
    private static final String DESC = "Set the same target as the Guide Star for the NICI AOWFS.";


    /**
     * Default constructor.
     */
    public NiciDefaultCatalogAlgorithm() {
        super(NAME, DESC);
    }
}
