package edu.gemini.shared.cat;

import edu.gemini.shared.skyobject.Magnitude;

/**
 * Algorithm for suggesting Niri OI WFS star using the f/14 camera.
 *
 */
@Deprecated
public final class NiriOIWFSCatalogAlgorithm extends OIWFSCatalogAlgorithm {
    private static final String NAME = "NIRI OIWFS";
    private static final String DESC = "Selects target for the NIRI OIWFS using the f/14 camera.";

    /**
     * Default constructor.
     */
    public NiriOIWFSCatalogAlgorithm() {
        super(NAME, DESC, 0.5, 1.4, -2.0, 14, Magnitude.Band.K);
    }

    /**
     * This algorithm is for a NIR Sensor, so let's override the default type
     */
    public SensorType getType() {
        return SensorType.NIR;
    }
}
