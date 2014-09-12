package edu.gemini.shared.cat;

// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: AltairWFSCatalogAlgorithm.java 47126 2012-08-01 15:40:43Z swalker $
//


import edu.gemini.shared.skyobject.Magnitude;

/**
 * Algorithm for suggesting Hokupaa AOWFS star.
 *
 */
public final class AltairWFSCatalogAlgorithm extends AOWFSCatalogAlgorithm {
    private static final String NAME = "Altair NGS";
    private static final String DESC = "Selects target for the Altair NGS.";

    /**
     * Default constructor.
     */
    public AltairWFSCatalogAlgorithm() {
        super(NAME, DESC, 0.0, 0.45, -2.0, 15.0, Magnitude.Band.R);
    }
}
