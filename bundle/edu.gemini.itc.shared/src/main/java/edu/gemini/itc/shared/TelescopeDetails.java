package edu.gemini.itc.shared;

import edu.gemini.spModel.telescope.IssPort;
import edu.gemini.spModel.type.DisplayableSpType;

import java.io.Serializable;

/**
 * Container for telescope parameters.
 */
public final class TelescopeDetails implements Serializable {

    public enum Coating implements DisplayableSpType {
        ALUMINIUM("aluminium"),
        SILVER("silver"),
        ;
        private String displayValue;
        Coating(String displayValue) {
            this.displayValue = displayValue;
        }
        public String displayValue() {
            return displayValue;
        }
    }
    public enum Wfs implements DisplayableSpType {
        OIWFS("oiwfs"),
        PWFS("pwfs"),
        AOWFS("aowfs"),
        ;
        private String displayValue;
        Wfs(String displayValue) {
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
        return _wfs;
    }

    public double getTelescopeDiameter() {
        return _telescopeDiameter;
    }


}
