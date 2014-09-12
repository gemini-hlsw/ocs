package edu.gemini.spModel.gemini.gmos.blueprint;

import edu.gemini.spModel.gemini.gmos.GmosSouthType.DisperserSouth;
import edu.gemini.spModel.gemini.gmos.GmosSouthType.FPUnitSouth;
import edu.gemini.spModel.gemini.gmos.GmosSouthType.FilterSouth;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;

public abstract class SpGmosSBlueprintSpectroscopyFpuBase extends SpGmosSBlueprintSpectroscopyBase {
    public static final String FPU_PARAM_NAME = "fpu";

    public final FPUnitSouth fpu;

    protected SpGmosSBlueprintSpectroscopyFpuBase(DisperserSouth disperser, FilterSouth filter, FPUnitSouth fpu) {
        super(disperser, filter);
        this.fpu = fpu;
    }

    protected SpGmosSBlueprintSpectroscopyFpuBase(ParamSet paramSet) {
        super(paramSet);
        fpu = Pio.getEnumValue(paramSet, FPU_PARAM_NAME, FPUnitSouth.DEFAULT);
    }

    @Override
    public String toString() {
        return String.format("GMOS-S %s %s %s %s", blueprintTypeLabel(), disperser.displayValue(), filter.displayValue(), fpu.displayValue());
    }

    public abstract String blueprintTypeLabel();

    @Override
    public ParamSet toParamSet(PioFactory factory) {
        ParamSet paramSet = super.toParamSet(factory);
        Pio.addEnumParam(factory, paramSet, FPU_PARAM_NAME, fpu);
        return paramSet;
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) return false;

        SpGmosSBlueprintSpectroscopyFpuBase that = (SpGmosSBlueprintSpectroscopyFpuBase) o;
        return fpu.equals(that.fpu);
    }

    @Override
    public int hashCode() {
        int res = super.hashCode();
        return res*31 + fpu.hashCode();
    }
}
