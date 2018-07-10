package edu.gemini.qpt.shared.sp;

import edu.gemini.qpt.shared.util.PioSerializable;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;

import java.io.Serializable;

/**
 * Mini-model representation of site conditions.
 * <p>
 * Represents an immutable set of site conditions, in probability percentiles between 0 and 
 * 100. 
 * @author rnorris
 */
public final class Conds implements Serializable, PioSerializable {

    private static final long serialVersionUID = 1L;

    public static final Conds ANY = new Conds((byte) 100, (byte) 100, (byte) 100, (byte) 100);
    
    public final byte sb;
    public final byte cc;
    public final byte iq;
    public final byte wv;
    
    public Conds(SPSiteQuality spsq) {
        this(spsq.getSkyBackground().getPercentage(),
                spsq.getCloudCover().getPercentage(),
                spsq.getImageQuality().getPercentage(),
                spsq.getWaterVapor().getPercentage());
    }
    
    public Conds(byte sb, byte cc, byte iq, byte wv) {
        
        assert sb >= 0 && sb <= 100;
        assert cc >= 0 && cc <= 100;
        assert iq >= 0 && iq <= 100;
        assert wv >= 0 && wv <= 100;

        this.sb = sb;
        this.cc = cc;
        this.iq = iq;
        this.wv = wv;

    }
    
    public Conds(ParamSet params) {
        this(Pio.getByteValue(params, "sb", (byte) -1),
             Pio.getByteValue(params, "cc", (byte) -1),
             Pio.getByteValue(params, "iq", (byte) -1),
             Pio.getByteValue(params, "wv", (byte) -1));
    }

    public ParamSet getParamSet(PioFactory factory, String name) {
        ParamSet params = factory.createParamSet(name);
        Pio.addByteParam(factory, params, "cc", cc);
        Pio.addByteParam(factory, params, "iq", iq);
        Pio.addByteParam(factory, params, "sb", sb);
        Pio.addByteParam(factory, params, "wv", wv);
        return params;
    }

    
    public boolean meetsConstraints(Conds constraints) {
        boolean ret = 
            constraints.sb <= sb &&
            constraints.iq <= iq &&
            constraints.cc <= cc &&
            constraints.wv <= wv;
        return ret;
        
    }

    public boolean meetsIQConstraint(Conds constraints) {
        boolean ret = constraints.iq <= iq;
        return ret;
        
    }

    public boolean meetsSBConstraint(Conds constraints) {
        boolean ret = constraints.sb <= sb;
        return ret;
        
    }

    public boolean meetsCCConstraint(Conds constraints) {
        boolean ret = constraints.cc <= cc;
        return ret;
    }

    public boolean meetsWVConstraint(Conds constraints) {
        boolean ret = constraints.wv <= wv;
        return ret;
        
    }

    @Override
    public String toString() {
        // TODO: this should be final
        StringBuilder buf = new StringBuilder();
        buf.append("SB = ").append(anyIf100(sb)).append(" / ");
        buf.append("CC = ").append(anyIf100(cc)).append(" / ");
        buf.append("IQ = ").append(anyIf100(iq)).append(" / ");
        buf.append("WV = ").append(anyIf100(wv));
        return buf.toString();
    }
    
    private Object anyIf100(byte b) {
        switch (b) {
        case 0: return "unconstrained";
        case 100: return "any";
        default: return b;
        }
    }
    
    public String toShortString() {
        // TODO: this should be final
        StringBuilder buf = new StringBuilder();
        buf.append("CC").append(shortAnyIf100(cc));
        buf.append("IQ").append(shortAnyIf100(iq));
        buf.append("WV").append(shortAnyIf100(wv));
        return buf.toString();
    }
    
    private Object shortAnyIf100(byte b) {
        switch (b) {
        case 0: return "-";
        case 100: return "A";
        default: return b;
        }
    }
    
    public static byte getPercentileForSkyBrightness(Double mag) {
        if (mag == null || mag > 21.37) return 20;
        if (mag > 20.78) return 50;
        if (mag > 19.61) return 80;
        return 100;
    }

    public double getBrightestMagnitude() {
        return getBrightestMagnitude(sb);
    }
    
    public static double getBrightestMagnitude(byte sb) {
        switch (sb) {
        case 20: return 21.37;
        case 50: return 20.78;
        case 80: return 19.61;
        default: return 0;
        }
    }
    
    public boolean containsSkyBrightness(Double mag) {
        return sb >= getPercentileForSkyBrightness(mag);
    }

    public boolean meetsConstraintsEasily(Conds constraints) {
        return 
            meetsConstraints(constraints) && (
                (constraints.sb != 0 && constraints.sb < sb) ||
                (constraints.wv != 0 && constraints.wv < wv) ||
                (constraints.cc != 0 && constraints.cc < cc) ||
                (constraints.iq != 0 && constraints.iq < iq)
            );
    }

    public byte getCC() {
        return cc;
    }
    
    public byte getIQ() {
        return iq;
    }
    
    public byte getWV() {
        return wv;
    }
    
    public byte getSB() {
        return sb;
    }
    
}
