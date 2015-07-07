package edu.gemini.shared.cat;

import edu.gemini.shared.skyobject.Magnitude;

/**
 * Algorithm for suggesting GNIRS OI WFS stars.
 */
@Deprecated
public class GnirsOIWFSCatalogAlgorithm extends OIWFSCatalogAlgorithm {
    private static final String NAME = "GNIRS OIWFS";
    private static final String DESC = "Selects target for the GNIRS OIWFS";

    /**
     * Default constructor.
     */
    public GnirsOIWFSCatalogAlgorithm() {
        super(NAME, DESC, 0.2, 1.5, 0.0, 14.0, Magnitude.Band.K);
    }


    @Override
    public ICatalogAlgorithm.SensorType getType() {
        return ICatalogAlgorithm.SensorType.NIR;
    }
}
