package edu.gemini.spModel.gemini.flamingos2.blueprint;

import edu.gemini.spModel.gemini.flamingos2.Flamingos2.Filter;
import edu.gemini.spModel.pio.ParamSet;
import static edu.gemini.spModel.template.SpBlueprintUtil.mkString;

import java.util.List;

public final class SpFlamingos2BlueprintImaging extends SpFlamingos2BlueprintBase {
    public static final String PARAM_SET_NAME = "flamingos2BlueprintImaging";

    public SpFlamingos2BlueprintImaging(List<Filter> filters) {
        super(filters);
    }

    public SpFlamingos2BlueprintImaging(ParamSet paramSet) {
        super(paramSet);
    }

    public String paramSetName() { return PARAM_SET_NAME; }

    @Override
    public String toString() {
        return String.format("Flamingos2 Imaging %s", mkString(filters));
    }
}
