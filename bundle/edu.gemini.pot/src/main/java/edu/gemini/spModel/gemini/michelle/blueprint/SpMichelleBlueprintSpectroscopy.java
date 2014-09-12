package edu.gemini.spModel.gemini.michelle.blueprint;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.gemini.michelle.InstMichelle;
import edu.gemini.spModel.gemini.michelle.MichelleParams.Disperser;
import edu.gemini.spModel.gemini.michelle.MichelleParams.Mask;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.template.SpBlueprint;

/**
 * Michelle Blueprint Spectroscopy.
 */
public final class SpMichelleBlueprintSpectroscopy extends SpBlueprint {
    public static final String PARAM_SET_NAME       = "michelleBlueprintSpectroscopy";
    public static final String FPU_PARAM_NAME       = "fpu";
    public static final String DISPERSER_PARAM_NAME = "disperser";

    public final Mask fpu;
    public final Disperser disperser;

    public SpMichelleBlueprintSpectroscopy(Mask fpu, Disperser disperser) {
        if ((fpu == null) || (disperser == null)) throw new IllegalArgumentException();
        this.fpu       = fpu;
        this.disperser = disperser;
    }

    public SpMichelleBlueprintSpectroscopy(ParamSet paramSet) {
        this.fpu       = Pio.getEnumValue(paramSet, FPU_PARAM_NAME, Mask.DEFAULT);
        this.disperser = Pio.getEnumValue(paramSet, DISPERSER_PARAM_NAME, Disperser.DEFAULT);
    }

    public SPComponentType instrumentType() { return InstMichelle.SP_TYPE; }
    public String paramSetName() { return PARAM_SET_NAME; }

    public ParamSet toParamSet(PioFactory factory) {
        ParamSet paramSet = factory.createParamSet(PARAM_SET_NAME);
        Pio.addEnumParam(factory, paramSet, FPU_PARAM_NAME, fpu);
        Pio.addEnumParam(factory, paramSet, DISPERSER_PARAM_NAME, disperser);
        return paramSet;
    }

    @Override
    public String toString() {
        return String.format("Michelle Spectroscopy %s %s", fpu.displayValue(), disperser.displayValue());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SpMichelleBlueprintSpectroscopy that = (SpMichelleBlueprintSpectroscopy) o;

        if (disperser != that.disperser) return false;
        if (fpu != that.fpu) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = fpu.hashCode();
        result = 31 * result + disperser.hashCode();
        return result;
    }
}
