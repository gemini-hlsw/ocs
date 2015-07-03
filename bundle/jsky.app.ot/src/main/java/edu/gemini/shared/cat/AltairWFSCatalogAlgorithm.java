package edu.gemini.shared.cat;

import edu.gemini.shared.skyobject.Magnitude;

/**
 * Algorithm for suggesting Hokupaa AOWFS star.
 *
 */
@Deprecated
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
