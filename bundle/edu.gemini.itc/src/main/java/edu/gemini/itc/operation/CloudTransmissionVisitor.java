package edu.gemini.itc.operation;

import edu.gemini.itc.base.ITCConstants;
import edu.gemini.itc.base.TransmissionElement;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;


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
    public static TransmissionElement create(final SPSiteQuality.CloudCover cc) {
        return new TransmissionElement(ITCConstants.TRANSMISSION_LIB + "/" + FILENAME +
                "_" + cc.sequenceValue() + ITCConstants.DATA_SUFFIX);
    }
}
