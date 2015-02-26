package edu.gemini.itc.parameters;

import edu.gemini.itc.shared.ITCParameters;
import edu.gemini.spModel.telescope.IssPort;
import edu.gemini.spModel.type.DisplayableSpType;

/**
 * This class holds the information from the Telescope section
 * of an ITC web page.  This object is constructed from a servlet request.
 */
public final class TeleParameters extends ITCParameters {

    public static enum Coating implements DisplayableSpType {
        ALUMINIUM("aluminium"),
        SILVER("silver"),
        ;
        private String displayValue;
        private Coating(String displayValue) {
            this.displayValue = displayValue;
        }
        public String displayValue() {
            return displayValue;
        }
    }
    public static enum Wfs implements DisplayableSpType {
        OIWFS("oiwfs"),
        PWFS("pwfs"),
        AOWFS("aowfs"),
        ;
        private String displayValue;
        private Wfs(String displayValue) {
            this.displayValue = displayValue;
        }
        public String displayValue() {
            return displayValue;
        }
    }

    // The telescope size
    private static final double _telescopeDiameter = 3.95 + 3.95;

    // Data members
    private final Coating _mirrorCoating;  // aluminum or silver
    private final IssPort _instrumentPort; // up or side
    private final Wfs     _wfs;

    public TeleParameters(final Coating mirrorCoating, final IssPort instrumentPort, final Wfs wfs) {
        _mirrorCoating  = mirrorCoating;
        _instrumentPort = instrumentPort;
        _wfs            = wfs;
    }

    public Coating getMirrorCoating() {
        return _mirrorCoating;
    }

    public IssPort getInstrumentPort() {
        return _instrumentPort;
    }

    public Wfs getWFS() {
        if (_wfs == Wfs.AOWFS)
            return Wfs.OIWFS;  //AO/tiptilt will be handled by Altair return something the rest of the code can understand
        else
            return _wfs;
    }

    public double getTelescopeDiameter() {
        return _telescopeDiameter;
    }

    /**
     * Return a human-readable string for debugging
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Mirror Coating:\t" + getMirrorCoating().displayValue() + "\n");
        sb.append("ISS Port:\t" + portToString() + "\n");
        sb.append("WFS:\t" + getWFS().displayValue() + "\n");
        sb.append("\n");
        return sb.toString();
    }

    public String printParameterSummary() {
        return printParameterSummary(getWFS().displayValue());
    }

    public String printParameterSummary(String wfs) {
        StringBuffer sb = new StringBuffer();
        sb.append("Telescope configuration: \n");
        sb.append("<LI>" + getMirrorCoating().displayValue() + " mirror coating.\n");
        sb.append("<LI>" + portToString() + " looking port.\n");
        sb.append("<LI>wavefront sensor: " + wfs + "\n");
        return sb.toString();
    }

    // compatibility for regression testing, can go away after regression tests have passed
    private String portToString() {
        switch (_instrumentPort) {
            case SIDE_LOOKING:  return "side";
            case UP_LOOKING:    return "up";
            default:            throw new IllegalArgumentException("unknown port");
        }
    }

}
