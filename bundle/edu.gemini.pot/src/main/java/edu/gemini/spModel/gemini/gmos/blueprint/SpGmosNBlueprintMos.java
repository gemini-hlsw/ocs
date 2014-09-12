package edu.gemini.spModel.gemini.gmos.blueprint;

import edu.gemini.spModel.gemini.altair.blueprint.SpAltair;
import edu.gemini.spModel.gemini.gmos.GmosNorthType.DisperserNorth;
import edu.gemini.spModel.gemini.gmos.GmosCommonType.UseNS;
import edu.gemini.spModel.gemini.gmos.GmosNorthType.FilterNorth;
import edu.gemini.spModel.gemini.gmos.GmosNorthType.FPUnitNorth;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;

public final class SpGmosNBlueprintMos extends SpGmosNBlueprintSpectroscopyBase {

    private static final long serialVersionUID = -395867508373517518L;

    public static final String PARAM_SET_NAME             = "gmosNBlueprintMos";
    public static final String NOD_AND_SHUFFLE_PARAM_NAME = "isNodAndShuffle";
    public static final String PRE_IMAGING_PARAM_NAME     = "isPreImaging";
    public static final String FPU_PARAM_NAME             = "fpu";

    public final UseNS nodAndShuffle;
    public final boolean preImaging;
    public final FPUnitNorth fpu;

    public SpGmosNBlueprintMos(SpAltair altair, DisperserNorth disperser, FilterNorth filter, UseNS ns, boolean preImaging, FPUnitNorth fpu) {
        super(altair, disperser, filter);
        this.nodAndShuffle = ns;
        this.preImaging    = preImaging;
        this.fpu           = fpu;
    }

    public SpGmosNBlueprintMos(ParamSet paramSet) {
        super(paramSet);
        nodAndShuffle = Pio.getEnumValue(paramSet, NOD_AND_SHUFFLE_PARAM_NAME, UseNS.DEFAULT);
        preImaging    = Pio.getBooleanValue(paramSet, PRE_IMAGING_PARAM_NAME, false);
        fpu           = Pio.getEnumValue(paramSet, FPU_PARAM_NAME, FPUnitNorth.FPU_NONE);
    }

    public String paramSetName() { return PARAM_SET_NAME; }

    @Override
    public String toString() {
        String ns = (nodAndShuffle == UseNS.TRUE) ? "MOS N+S" : "MOS";
        final String name   = String.format("GMOS-N %s", ns);
        final String pi     = preImaging ? "+Pre" : "";
        final String suffix = String.format("%s %s %s", disperser.displayValue(), filter.displayValue(), pi);
        return super.toString(name, suffix);
    }

    @Override
    public ParamSet toParamSet(PioFactory factory) {
        final ParamSet paramSet = super.toParamSet(factory);
        Pio.addEnumParam(factory, paramSet, NOD_AND_SHUFFLE_PARAM_NAME, nodAndShuffle);
        Pio.addBooleanParam(factory, paramSet, PRE_IMAGING_PARAM_NAME, preImaging);
        Pio.addEnumParam(factory, paramSet, FPU_PARAM_NAME, fpu);
        return paramSet;
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) return false;

        SpGmosNBlueprintMos that = (SpGmosNBlueprintMos) o;

        if (preImaging != that.preImaging) return false;
        if (nodAndShuffle != that.nodAndShuffle) return false;
        return fpu == that.fpu;
    }

    @Override
    public int hashCode() {
        int res = super.hashCode();
        res = 31*res + nodAndShuffle.hashCode();
        res = 31*res + (preImaging ? 1 : 0);
        res = 31*res + fpu.hashCode();
        return res;
    }
}
