package edu.gemini.spModel.gemini.nifs.blueprint;

import edu.gemini.spModel.gemini.nifs.NIFSParams.Disperser;
import edu.gemini.spModel.pio.ParamSet;

public final class SpNifsBlueprint extends SpNifsBlueprintBase {
    public static final String PARAM_SET_NAME = "nifsBlueprint";

    public SpNifsBlueprint(Disperser disperser) {
        super(disperser);
    }

    public SpNifsBlueprint(ParamSet paramSet) {
        super(paramSet);
    }

    public String paramSetName() { return PARAM_SET_NAME; }

    @Override
    public String toString() {
        return String.format("NIFS %s", disperser.displayValue());
    }
}
