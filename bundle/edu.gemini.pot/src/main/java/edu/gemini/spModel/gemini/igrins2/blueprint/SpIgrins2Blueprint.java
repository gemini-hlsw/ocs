package edu.gemini.spModel.gemini.igrins2.blueprint;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.gemini.igrins2.Igrins2;
import edu.gemini.spModel.gemini.igrins2.NoddingOption;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.template.SpBlueprint;


public final class SpIgrins2Blueprint extends SpBlueprint {

    public static final String PARAM_SET_NAME = "Igrins2Blueprint";
    public static final String PARAM_NODDING  = "noddingOption";

    private NoddingOption noddingOption;

    public SpIgrins2Blueprint(NoddingOption noddingOption) {
        this.noddingOption = noddingOption;
    }

    public NoddingOption getNoddingOption() {
        return noddingOption;
    }

    public SpIgrins2Blueprint(ParamSet paramSet) {
        noddingOption = Pio.getEnumValue(paramSet, PARAM_NODDING, NoddingOption.KEEP_TARGET_IN_SLIT);
    }

    @Override public String paramSetName() { return PARAM_SET_NAME; }

    @Override public SPComponentType instrumentType() { return Igrins2.SP_TYPE(); }

    @Override public String toString() {
        return String.format("Igrins2(%s)", noddingOption.displayValue());
    }

    @Override public ParamSet toParamSet(PioFactory factory) {
        final ParamSet ps = factory.createParamSet(PARAM_SET_NAME);
        Pio.addEnumParam(factory, ps, PARAM_NODDING, noddingOption);
        return ps;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final SpIgrins2Blueprint that = (SpIgrins2Blueprint) o;
        return (noddingOption == that.noddingOption);
    }

    @Override public int hashCode() {
        return noddingOption.hashCode();
    }
}