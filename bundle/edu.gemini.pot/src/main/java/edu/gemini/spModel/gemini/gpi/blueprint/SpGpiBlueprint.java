package edu.gemini.spModel.gemini.gpi.blueprint;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.gemini.gpi.Gpi;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.template.SpBlueprint;

public class SpGpiBlueprint extends SpBlueprint {
    public static final String PARAM_SET_NAME     = "gpiBlueprint";
    public static final String DISPERSER_PARAM_NAME = "disperser";
    public static final String OBSERVING_MODE_PARAM_NAME = "observingMode";

    public final Gpi.Disperser disperser;
    public final Gpi.ObservingMode observingMode;

    public SpGpiBlueprint(Gpi.Disperser disperser, Gpi.ObservingMode observingMode) {
        this.disperser = disperser;
        this.observingMode = observingMode;
    }

    public SpGpiBlueprint(ParamSet paramSet) {
        this.disperser = Pio.getEnumValue(paramSet, DISPERSER_PARAM_NAME, Gpi.Disperser.DEFAULT);
        this.observingMode = Pio.getEnumValue(paramSet, OBSERVING_MODE_PARAM_NAME, Gpi.ObservingMode.CORON_H_BAND);
    }

    public String paramSetName() { return PARAM_SET_NAME; }
    public SPComponentType instrumentType() { return Gpi.SP_TYPE; }

    @Override
    public String toString() {
        return String.format("GPI %s %s", disperser, observingMode);
    }

    public ParamSet toParamSet(PioFactory factory) {
        ParamSet paramSet = factory.createParamSet(PARAM_SET_NAME);
        Pio.addEnumParam(factory, paramSet, DISPERSER_PARAM_NAME, disperser);
        Pio.addEnumParam(factory, paramSet, OBSERVING_MODE_PARAM_NAME, observingMode);
        return paramSet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SpGpiBlueprint that = (SpGpiBlueprint) o;

        if (!disperser.equals(that.disperser)) return false;
        if (!observingMode.equals(that.observingMode)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return disperser.hashCode() + 31 * observingMode.hashCode();
    }
}
