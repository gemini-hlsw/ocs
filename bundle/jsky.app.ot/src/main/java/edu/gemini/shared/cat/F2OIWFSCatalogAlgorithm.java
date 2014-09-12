// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: F2OIWFSCatalogAlgorithm.java 47186 2012-08-02 16:54:23Z swalker $
//

package edu.gemini.shared.cat;


import edu.gemini.shared.skyobject.Magnitude;

/**
 * Algorithm for suggesting a Flamingos 2 On Instrument WFS star.
 * $Id: F2OIWFSCatalogAlgorithm.java 47186 2012-08-02 16:54:23Z swalker $
 */
public class F2OIWFSCatalogAlgorithm extends OIWFSCatalogAlgorithm {
    private static final String NAME = "Flamingos 2 OIWFS";
    private static final String DESC = "Selects target for the Flamingos 2 OIWFS.";


    /**
     * Default constructor.
     */
    public F2OIWFSCatalogAlgorithm() {
        // REL-346:  F2+OIWFS, 0.33, 6.1, 15.0 ## Change min radius
        super(NAME, DESC, 0.33, 6.1, 9.5, 15, Magnitude.Band.R);
    }
}
