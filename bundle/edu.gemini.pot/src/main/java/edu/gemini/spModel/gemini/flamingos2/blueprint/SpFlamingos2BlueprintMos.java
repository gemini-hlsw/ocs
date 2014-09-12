package edu.gemini.spModel.gemini.flamingos2.blueprint;

import edu.gemini.spModel.gemini.flamingos2.Flamingos2.Disperser;
import edu.gemini.spModel.gemini.flamingos2.Flamingos2.Filter;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;

import java.util.List;

import static edu.gemini.spModel.template.SpBlueprintUtil.mkString;

public final class SpFlamingos2BlueprintMos extends SpFlamingos2BlueprintSpectroscopyBase {

    public static final String PARAM_SET_NAME         = "flamingos2BlueprintMos";
    public static final String PRE_IMAGING_PARAM_NAME = "preImaging";

    public final boolean preImaging;

    public SpFlamingos2BlueprintMos(List<Filter> filters, Disperser disperser, boolean preImaging) {
        super(filters, disperser);
        this.preImaging = preImaging;
    }

    public SpFlamingos2BlueprintMos(ParamSet paramSet) {
        super(paramSet);
        preImaging = Pio.getBooleanValue(paramSet, PRE_IMAGING_PARAM_NAME, false);
    }

    public String paramSetName() { return PARAM_SET_NAME; }

    @Override
    public String toString() {
        return String.format("Flamingos2 MOS%s %s %s",
                (preImaging ? "+Pre" : ""),
                disperser.displayValue(), mkString(filters, "+"));
    }

    @Override
    public ParamSet toParamSet(PioFactory factory) {
        ParamSet paramSet = super.toParamSet(factory);
        Pio.addBooleanParam(factory, paramSet, PRE_IMAGING_PARAM_NAME, preImaging);
        return paramSet;
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) return false;

        SpFlamingos2BlueprintMos that = (SpFlamingos2BlueprintMos) o;
        return preImaging == that.preImaging;
    }

    @Override
    public int hashCode() {
        int res = super.hashCode();
        res = res*31 + Boolean.valueOf(preImaging).hashCode();
        return res;
    }
}
