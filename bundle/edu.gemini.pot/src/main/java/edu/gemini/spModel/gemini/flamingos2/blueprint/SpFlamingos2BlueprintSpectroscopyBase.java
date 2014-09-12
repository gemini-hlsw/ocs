package edu.gemini.spModel.gemini.flamingos2.blueprint;

import edu.gemini.spModel.gemini.flamingos2.Flamingos2.Disperser;
import edu.gemini.spModel.gemini.flamingos2.Flamingos2.Filter;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;

import java.util.List;

import static edu.gemini.spModel.template.SpBlueprintUtil.mkString;

public abstract class SpFlamingos2BlueprintSpectroscopyBase extends SpFlamingos2BlueprintBase {

    public static final String DISPERSER_PARAM_NAME = "disperser";

    public final Disperser disperser;

    protected SpFlamingos2BlueprintSpectroscopyBase(List<Filter> filters, Disperser disperser) {
        super(filters);
        this.disperser = disperser;
    }

    protected SpFlamingos2BlueprintSpectroscopyBase(ParamSet paramSet) {
        super(paramSet);
        disperser = Pio.getEnumValue(paramSet, DISPERSER_PARAM_NAME, Disperser.DEFAULT);
    }

    @Override
    public ParamSet toParamSet(PioFactory factory) {
        ParamSet paramSet = super.toParamSet(factory);
        Pio.addEnumParam(factory, paramSet, DISPERSER_PARAM_NAME, disperser);
        return paramSet;
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) return false;

        SpFlamingos2BlueprintSpectroscopyBase that = (SpFlamingos2BlueprintSpectroscopyBase) o;
        return disperser == that.disperser;
    }

    @Override
    public int hashCode() {
        int res = super.hashCode();
        res = res*31 + disperser.hashCode();
        return res;
    }
}
