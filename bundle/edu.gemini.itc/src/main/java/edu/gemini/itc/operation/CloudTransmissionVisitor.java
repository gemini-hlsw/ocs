package edu.gemini.itc.operation;

import edu.gemini.itc.shared.ITCConstants;
import edu.gemini.itc.shared.TransmissionElement;

/**
 * The CloudTransmissionVisitor is designed to adjust the SED for
 * clouds in the atmosphere.
 */
public final class CloudTransmissionVisitor {
    private static final String FILENAME = "cloud_trans";

    private CloudTransmissionVisitor() {
    }

    /**
     * Constructs transmission visitor for clouds.
     */
    public static TransmissionElement create(int skyTransparencyCloud) throws Exception {
        return new TransmissionElement(ITCConstants.TRANSMISSION_LIB + "/" + FILENAME +
                skyTransparencyCloud + ITCConstants.DATA_SUFFIX);
    }
}
