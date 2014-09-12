package edu.gemini.shared.cat;

import edu.gemini.shared.skyobject.Magnitude;

/**
 * Algorithm for suggesting Altair LGS star.
 *
 */
public final class AltairLGSCatalogAlgorithm extends AOWFSCatalogAlgorithm {
    private static final String NAME = "Altair LGS";
    private static final String DESC = "Selects target for the Altair LGS.";

    /**
     * Default constructor.
     */
    public AltairLGSCatalogAlgorithm() {
        super(NAME, DESC, 0.0, 0.45, -2.0, 18.0, Magnitude.Band.R);
    }
}
