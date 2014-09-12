package edu.gemini.shared.cat;

import edu.gemini.shared.skyobject.Magnitude;

/**
 * Algorithm for suggesting a NIFS OI WFS star w/field lens.
 * <p>
 */
public class NifsOIWFSFieldCatalogAlgorithm extends OIWFSCatalogAlgorithm {
    private static final String NAME = "NIFS OIWFS w/ field lens";
    private static final String DESC = "Selects target for the NIFS OIWFS w/ field lens.";

    /**
     * Default constructor.
     */
    public NifsOIWFSFieldCatalogAlgorithm() {
        this(false);
    }

    /**
     * Initialize based on the given flag.
     * @param isAltair true if the observation contains an Altair component
     */
    public NifsOIWFSFieldCatalogAlgorithm(boolean isAltair) {
        super(NAME, DESC, 0.22, 0.5, -2.0, 14.5, Magnitude.Band.K);
    }

    /**
     * This algorithm is for a NIR Sensor, so let's override the default type
     */
    public SensorType getType() {
        return SensorType.NIR;
    }

}
