package edu.gemini.spModel.gemini.texes.blueprint;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.gemini.texes.InstTexes;
import edu.gemini.spModel.gemini.texes.TexesParams;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.template.SpBlueprint;

public final class SpTexesBlueprint extends SpBlueprint {
    public static final String PARAM_SET_NAME     = "texesBlueprint";
    public static final String DISPERSER_PARAM_NAME = "disperser";

    public final TexesParams.Disperser disperser;

    public SpTexesBlueprint(TexesParams.Disperser disperser) {
        this.disperser = disperser;
    }

    public SpTexesBlueprint(ParamSet paramSet) {
        this.disperser = Pio.getEnumValue(paramSet, DISPERSER_PARAM_NAME, TexesParams.Disperser.DEFAULT);
    }

    public String paramSetName() { return PARAM_SET_NAME; }
    public SPComponentType instrumentType() { return InstTexes.SP_TYPE; }

    @Override
    public String toString() {
        return String.format("TEXES %s", disperser);
    }

    public ParamSet toParamSet(PioFactory factory) {
        ParamSet paramSet = factory.createParamSet(PARAM_SET_NAME);
        Pio.addEnumParam(factory, paramSet, DISPERSER_PARAM_NAME, disperser);
        return paramSet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SpTexesBlueprint that = (SpTexesBlueprint) o;

        if (!disperser.equals(that.disperser)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return disperser.hashCode();
    }
}
