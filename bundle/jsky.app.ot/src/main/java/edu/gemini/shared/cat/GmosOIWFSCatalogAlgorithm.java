// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: GmosOIWFSCatalogAlgorithm.java 47186 2012-08-02 16:54:23Z swalker $
//

package edu.gemini.shared.cat;


import edu.gemini.shared.skyobject.Magnitude;

/**
 * Algorithm for suggesting a GMOS AO On Instrument WFS star.
 *
 */
public final class GmosOIWFSCatalogAlgorithm extends OIWFSCatalogAlgorithm {
    private static final String NAME = "GMOS OIWFS";
    private static final String DESC = "Selects target for the GMOS OIWFS.";

    /**
     * Default constructor.
     */
    public GmosOIWFSCatalogAlgorithm() {
        super(NAME, DESC, 0.33, 4.8, 9.5, 15.5, Magnitude.Band.R);
    }
}
