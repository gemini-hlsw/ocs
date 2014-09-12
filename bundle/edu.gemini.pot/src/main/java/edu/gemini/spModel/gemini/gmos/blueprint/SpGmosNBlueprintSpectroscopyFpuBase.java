package edu.gemini.spModel.gemini.gmos.blueprint;

import edu.gemini.spModel.gemini.altair.blueprint.SpAltair;
import edu.gemini.spModel.gemini.gmos.GmosNorthType.DisperserNorth;
import edu.gemini.spModel.gemini.gmos.GmosNorthType.FPUnitNorth;
import edu.gemini.spModel.gemini.gmos.GmosNorthType.FilterNorth;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;

public abstract class SpGmosNBlueprintSpectroscopyFpuBase extends SpGmosNBlueprintSpectroscopyBase {
    public static final String FPU_PARAM_NAME = "fpu";

    public final FPUnitNorth fpu;

    protected SpGmosNBlueprintSpectroscopyFpuBase(SpAltair altair, DisperserNorth disperser, FilterNorth filter, FPUnitNorth fpu) {
        super(altair, disperser, filter);
        this.fpu = fpu;
    }

    protected SpGmosNBlueprintSpectroscopyFpuBase(ParamSet paramSet) {
        super(paramSet);
        fpu = Pio.getEnumValue(paramSet, FPU_PARAM_NAME, FPUnitNorth.DEFAULT);
    }

    @Override
    public String toString() {
        return super.toString(
                String.format("GMOS-N %s", blueprintTypeLabel()),
                String.format("%s %s %s", disperser.displayValue(), filter.displayValue(), fpu.displayValue()));
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

        SpGmosNBlueprintSpectroscopyFpuBase that = (SpGmosNBlueprintSpectroscopyFpuBase) o;
        return fpu.equals(that.fpu);
    }

    @Override
    public int hashCode() {
        int res = super.hashCode();
        return res*31 + fpu.hashCode();
    }
}
