package edu.gemini.spModel.gemini.nici.blueprint;

import edu.gemini.spModel.gemini.nici.NICIParams.Channel1FW;
import edu.gemini.spModel.gemini.nici.NICIParams.Channel2FW;
import edu.gemini.spModel.gemini.nici.NICIParams.DichroicWheel;
import edu.gemini.spModel.pio.ParamSet;

import java.util.List;

public final class SpNiciBlueprintStandard extends SpNiciBlueprintBase {
    public static final String PARAM_SET_NAME = "niciBlueprintStandard";


    public SpNiciBlueprintStandard(DichroicWheel dichroic, List<Channel1FW> redFilters, List<Channel2FW> blueFilters) {
        super(dichroic, redFilters, blueFilters);
    }

    public SpNiciBlueprintStandard(ParamSet paramSet) {
        super(paramSet);
    }

    public String paramSetName() { return PARAM_SET_NAME; }

    @Override
    public String toString() {
        return String.format("NICI Standard %s%s",
                dichroic.displayValue(),
                formatFilters());
    }
}
