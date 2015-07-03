package edu.gemini.shared.cat;

import edu.gemini.shared.skyobject.Magnitude;

/**
 * Algorithm for suggesting a GMOS AO On Instrument WFS star.
 *
 */
@Deprecated
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
