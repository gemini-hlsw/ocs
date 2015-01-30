package edu.gemini.itc.operation;

import edu.gemini.itc.parameters.TeleParameters;
import edu.gemini.itc.shared.ITCConstants;
import edu.gemini.itc.shared.TransmissionElement;

/**
 * The TelescopeTransmissionVisitor is designed to adjust the SED for the
 * Telesope Tranmsission.
 */
public final class TelescopeTransmissionVisitor {

    private static final String _COATING = "_coating_";

    private TelescopeTransmissionVisitor() {
    }

    /**
     * The TelTrans constructor takes two arguments: one detailing what
     * type of coating is used, and the other detailing how many mirrors
     * should be used.
     */
    public static TransmissionElement create(final TeleParameters tp) throws Exception {
        String fileName;

        switch (tp.getMirrorCoating()) {
            case TeleParameters.ALUMINIUM:
                fileName = "al" + _COATING;
                break;
            case TeleParameters.SILVER:
                fileName = "ag" + _COATING;
                break;
            default:
                throw new Exception("Unknown mirror material: " + tp.getMirrorCoating());
        }

        switch (tp.getInstrumentPort()) {
            case TeleParameters.UP:
                fileName += TeleParameters.UP;
                break;
            case TeleParameters.SIDE:
                fileName += TeleParameters.SIDE;
                break;
            default:
                throw new Exception("Unknown iss port: " + tp.getInstrumentPort());
        }

        return new TransmissionElement(ITCConstants.TRANSMISSION_LIB + "/" + fileName + ITCConstants.DATA_SUFFIX);
    }

}
