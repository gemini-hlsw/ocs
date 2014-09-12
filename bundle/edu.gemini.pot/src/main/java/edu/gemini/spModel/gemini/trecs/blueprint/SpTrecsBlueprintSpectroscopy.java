package edu.gemini.spModel.gemini.trecs.blueprint;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.gemini.trecs.InstTReCS;
import edu.gemini.spModel.gemini.trecs.TReCSParams.Disperser;
import edu.gemini.spModel.gemini.trecs.TReCSParams.Mask;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.template.SpBlueprint;

public final class SpTrecsBlueprintSpectroscopy extends SpBlueprint {
    public static final String PARAM_SET_NAME       = "trecsBlueprintSpectroscopy";
    public static final String DISPERSER_PARAM_NAME = "disperser";
    public static final String FPU_PARAM_NAME       = "fpu";

    public final Disperser disperser;
    public final Mask fpu;

    public SpTrecsBlueprintSpectroscopy(Disperser disperser, Mask fpu) {
        this.disperser = disperser;
        this.fpu       = fpu;
    }

    public SpTrecsBlueprintSpectroscopy(ParamSet paramSet) {
        this.disperser = Pio.getEnumValue(paramSet, DISPERSER_PARAM_NAME, Disperser.DEFAULT);
        this.fpu       = Pio.getEnumValue(paramSet, FPU_PARAM_NAME, Mask.DEFAULT);
    }

    public String paramSetName() { return PARAM_SET_NAME; }
    public SPComponentType instrumentType() { return InstTReCS.SP_TYPE; }

    @Override
    public String toString() {
        return String.format("T-ReCS Spectroscopy %s %s",
                disperser.displayValue(),
                fpu.displayValue());
    }

    public ParamSet toParamSet(PioFactory factory) {
        ParamSet paramSet = factory.createParamSet(PARAM_SET_NAME);
        Pio.addEnumParam(factory, paramSet, DISPERSER_PARAM_NAME, disperser);
        Pio.addEnumParam(factory, paramSet, FPU_PARAM_NAME, fpu);
        return paramSet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SpTrecsBlueprintSpectroscopy that = (SpTrecsBlueprintSpectroscopy) o;

        if (disperser != that.disperser) return false;
        if (fpu != that.fpu) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = disperser.hashCode();
        result = 31 * result + fpu.hashCode();
        return result;
    }
}
