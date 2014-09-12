package edu.gemini.spModel.gemini.gmos.blueprint;

import edu.gemini.spModel.gemini.gmos.GmosCommonType.UseNS;
import edu.gemini.spModel.gemini.gmos.GmosSouthType.DisperserSouth;
import edu.gemini.spModel.gemini.gmos.GmosSouthType.FilterSouth;
import edu.gemini.spModel.gemini.gmos.GmosSouthType.FPUnitSouth;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;

public final class SpGmosSBlueprintMos extends SpGmosSBlueprintSpectroscopyBase {

    private static final long serialVersionUID = 8393899423042661924L;

    public static final String PARAM_SET_NAME               = "gmosSBlueprintMos";
    public static final String NOD_AND_SHUFFLE_PARAM_NAME   = "isNodAndShuffle";
    public static final String PRE_IMAGING_PARAM_NAME       = "isPreImaging";
    public static final String FPU_PARAM_NAME               = "fpu";

    public final UseNS nodAndShuffle;
    public final boolean preImaging;
    public final FPUnitSouth fpu;


    public SpGmosSBlueprintMos(DisperserSouth disperser, FilterSouth filter, UseNS ns, boolean preImaging, FPUnitSouth fpu) {
        super(disperser, filter);
        this.nodAndShuffle = ns;
        this.preImaging    = preImaging;
        this.fpu           = fpu;
    }

    public SpGmosSBlueprintMos(ParamSet paramSet) {
        super(paramSet);
        nodAndShuffle = Pio.getEnumValue(paramSet, NOD_AND_SHUFFLE_PARAM_NAME, UseNS.DEFAULT);
        preImaging    = Pio.getBooleanValue(paramSet, PRE_IMAGING_PARAM_NAME, false);
        fpu           = Pio.getEnumValue(paramSet, FPU_PARAM_NAME, FPUnitSouth.FPU_NONE);
    }

    public String paramSetName() { return PARAM_SET_NAME; }

    @Override
    public String toString() {
        String ns = (nodAndShuffle == UseNS.TRUE) ? "MOS N+S" : "MOS";
        String pi = preImaging ? "+Pre" : "";
        return String.format("GMOS-S %s %s %s %s", ns, disperser.displayValue(), filter.displayValue(), pi);
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

        SpGmosSBlueprintMos that = (SpGmosSBlueprintMos) o;

        if (preImaging != that.preImaging) return false;
        if (nodAndShuffle != that.nodAndShuffle) return false;
        if (fpu != that.fpu) return false;

        return true;
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
