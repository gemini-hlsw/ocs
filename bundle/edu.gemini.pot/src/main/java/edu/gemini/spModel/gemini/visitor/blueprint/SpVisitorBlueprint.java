package edu.gemini.spModel.gemini.visitor.blueprint;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.gemini.visitor.VisitorInstrument;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.template.SpBlueprint;

public final class SpVisitorBlueprint extends SpBlueprint {
    public static final String PARAM_SET_NAME  = "visitorBlueprint";
    public static final String NAME_PARAM_NAME = "name";

    public final String name;

    public SpVisitorBlueprint(String name) {
        this.name = name;
    }

    public SpVisitorBlueprint(ParamSet paramSet) {
        this.name = Pio.getValue(paramSet, NAME_PARAM_NAME, "Unknown");
    }

    public String paramSetName() { return PARAM_SET_NAME; }
    public SPComponentType instrumentType() { return VisitorInstrument.SP_TYPE; }

    @Override
    public String toString() {
        return String.format("Visitor %s", name);
    }

    public ParamSet toParamSet(PioFactory factory) {
        ParamSet paramSet = factory.createParamSet(PARAM_SET_NAME);
        Pio.addParam(factory, paramSet, NAME_PARAM_NAME, name);
        return paramSet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SpVisitorBlueprint that = (SpVisitorBlueprint) o;

        if (!name.equals(that.name)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
