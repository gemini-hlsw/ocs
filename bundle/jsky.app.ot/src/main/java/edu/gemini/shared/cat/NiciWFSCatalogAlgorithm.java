package edu.gemini.shared.cat;

import edu.gemini.shared.skyobject.Magnitude;

/**
 * Algorithm for suggesting a NICI  AOWFS star.
 * $Id: NiciWFSCatalogAlgorithm.java 47126 2012-08-01 15:40:43Z swalker $
 */
@Deprecated
public class NiciWFSCatalogAlgorithm extends OIWFSCatalogAlgorithm {
    private static final String NAME = "NICI AO";
    private static final String DESC = "Selects target for the NICI AO.";


    /**
     * Default constructor.
     */
    public NiciWFSCatalogAlgorithm() {
        // REL-346: NICI+OIWFS, 0.0, 0.15, 15.5 ## Change, mag lim is R
        super(NAME, DESC, 0.0, 0.15, -2.0, 15.5, Magnitude.Band.R);
    }

}
