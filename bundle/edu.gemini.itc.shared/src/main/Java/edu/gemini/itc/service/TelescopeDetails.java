package edu.gemini.itc.service;

import edu.gemini.spModel.telescope.IssPort;
import edu.gemini.spModel.type.DisplayableSpType;

/**
 * Container for telescope parameters.
 */
public final class TelescopeDetails {

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

    public TelescopeDetails(final Coating mirrorCoating, final IssPort instrumentPort, final Wfs wfs) {
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
        sb.append("ISS Port:\t" + getInstrumentPort().displayValue() + "\n");
        sb.append("WFS:\t" + getWFS().displayValue() + "\n");
        sb.append("\n");
        return sb.toString();
    }

}
