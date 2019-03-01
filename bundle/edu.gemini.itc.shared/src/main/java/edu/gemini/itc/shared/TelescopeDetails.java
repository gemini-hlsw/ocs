package edu.gemini.itc.shared;

import edu.gemini.spModel.guide.GuideProbe;
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

    // The telescope size
    private static final double telescopeDiameter = 3.95 + 3.95;

    // Data members
    private final Coating           mirrorCoating;  // aluminum or silver
    private final IssPort           instrumentPort; // up or side
    private final GuideProbe.Type   wfs;

    public TelescopeDetails(final Coating mirrorCoating, final IssPort instrumentPort, final GuideProbe.Type wfs) {
        this.mirrorCoating  = mirrorCoating;
        this.instrumentPort = instrumentPort;
        this.wfs            = wfs;
    }

    public Coating getMirrorCoating() {
        return mirrorCoating;
    }

    public IssPort getInstrumentPort() {
        return instrumentPort;
    }

    public GuideProbe.Type getWFS() {
        return wfs;
    }

    public double getTelescopeDiameter() {
        return telescopeDiameter;
    }

    public String toString() {
        return String.format("TelescopeDetails(%s, %s, %s)", mirrorCoating, instrumentPort, wfs);
    }

    public int hashCode() {
        return mirrorCoating.hashCode() ^ instrumentPort.hashCode() ^ wfs.hashCode();
    }

    public boolean equals(Object o) {
        if (o instanceof TelescopeDetails) {
            final TelescopeDetails td = (TelescopeDetails) o;
            return td.mirrorCoating  == mirrorCoating  &&
                   td.instrumentPort == instrumentPort &&
                   td.wfs            == wfs;
        } else {
            return false;
        }
    }

}
