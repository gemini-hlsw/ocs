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

    private TelescopeTransmissionVisitor() {}

    /**
     * The TelTrans constructor takes two arguments: one detailing what
     * type of coating is used, and the other detailing how many mirrors
     * should be used.
     */
    public static TransmissionElement create(final TeleParameters tp) {

        final String coating;
        switch (tp.getMirrorCoating()) {
            case ALUMINIUM:     coating = "al" + _COATING; break;
            case SILVER:        coating = "ag" + _COATING; break;
            default:            throw new IllegalArgumentException("Unknown mirror material: " + tp.getMirrorCoating());
        }

        final String port;
        switch (tp.getInstrumentPort()) {
            case UP_LOOKING:    port = "up";   break;
            case SIDE_LOOKING:  port = "side"; break;
            default:            throw new IllegalArgumentException("Unknown iss port: " + tp.getInstrumentPort());
        }

        return new TransmissionElement(ITCConstants.TRANSMISSION_LIB + "/" + coating + port + ITCConstants.DATA_SUFFIX);
    }

}
