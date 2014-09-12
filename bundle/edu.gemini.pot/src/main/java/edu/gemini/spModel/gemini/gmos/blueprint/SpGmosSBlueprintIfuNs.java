package edu.gemini.spModel.gemini.gmos.blueprint;

import edu.gemini.spModel.gemini.gmos.GmosSouthType.DisperserSouth;
import edu.gemini.spModel.gemini.gmos.GmosSouthType.FPUnitSouth;
import edu.gemini.spModel.gemini.gmos.GmosSouthType.FilterSouth;
import edu.gemini.spModel.pio.ParamSet;

public final class SpGmosSBlueprintIfuNs extends SpGmosSBlueprintSpectroscopyFpuBase {

    public static final String PARAM_SET_NAME = "gmosSBlueprintIfuNs";

    public SpGmosSBlueprintIfuNs(DisperserSouth disperser, FilterSouth filter, FPUnitSouth fpu) {
        super(disperser, filter, fpu);
    }

    public SpGmosSBlueprintIfuNs(ParamSet paramSet) {
        super(paramSet);
    }

    public String paramSetName() { return PARAM_SET_NAME; }

    @Override
    public String blueprintTypeLabel() { return "IFU N+S"; }
}
