package edu.gemini.spModel.gemini.graces.blueprint;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.gemini.visitor.VisitorInstrument;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.template.SpBlueprint;

public final class SpGracesBlueprint extends SpBlueprint {
    public static final String PARAM_SET_NAME  = "GracesBlueprint";

    public SpGracesBlueprint() {
        // no state
    }

    // found via reflection :/  must exist
    public SpGracesBlueprint(ParamSet paramSet) {
    }

    public String paramSetName() { return PARAM_SET_NAME; }
    public SPComponentType instrumentType() { return VisitorInstrument.SP_TYPE; }

    @Override public String toString() {
        return String.format("GRACES");
    }

    public ParamSet toParamSet(PioFactory factory) {
        return factory.createParamSet(PARAM_SET_NAME);
    }

    @Override public boolean equals(Object o) {
        return (o != null) && getClass() == o.getClass();
    }

    @Override public int hashCode() {
        return getClass().hashCode();
    }
}
