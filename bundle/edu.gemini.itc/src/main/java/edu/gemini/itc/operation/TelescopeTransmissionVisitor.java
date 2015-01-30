package edu.gemini.itc.operation;

import edu.gemini.itc.shared.ITCConstants;
import edu.gemini.itc.shared.TransmissionElement;

/**
 * The TelescopeTransmissionVisitor is designed to adjust the SED for the
 * Telesope Tranmsission.
 */
public final class TelescopeTransmissionVisitor {
    // These constants define the different mirror surfaces used
    public static final String ALUMINUM = "aluminium";
    public static final String SILVER = "silver";
    public static final String UP = "up";
    public static final String SIDE = "side";

    private static final String _COATING = "_coating_";

    private TelescopeTransmissionVisitor() {
    }

    /**
     * The TelTrans constructor takes two arguments: one detailing what
     * type of coating is used, and the other detailing how many mirrors
     * should be used.
     */
    public static TransmissionElement create(String coating, String issPort) throws Exception {
        String fileName;

        if (coating.equals(ALUMINUM)) {
            fileName = "al" + _COATING;
        } else if (coating.equals(SILVER)) {
            fileName = "ag" + _COATING;
        } else {
            throw new Exception("Unknown mirror material: " + coating);
        }

        if (issPort.equals(UP)) {
            fileName += UP;
        } else if (issPort.equals(SIDE)) {
            fileName += SIDE;
        } else {
            throw new Exception("Unknown iss port: " + issPort);
        }

        return new TransmissionElement(ITCConstants.TRANSMISSION_LIB + "/" + fileName + ITCConstants.DATA_SUFFIX);
    }

}
