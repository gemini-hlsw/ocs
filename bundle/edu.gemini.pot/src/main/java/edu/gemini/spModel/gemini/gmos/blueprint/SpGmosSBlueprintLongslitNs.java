package edu.gemini.spModel.gemini.gmos.blueprint;

import edu.gemini.spModel.gemini.gmos.GmosSouthType.DisperserSouth;
import edu.gemini.spModel.gemini.gmos.GmosSouthType.FPUnitSouth;
import edu.gemini.spModel.gemini.gmos.GmosSouthType.FilterSouth;
import edu.gemini.spModel.pio.ParamSet;

public final class SpGmosSBlueprintLongslitNs extends SpGmosSBlueprintSpectroscopyFpuBase {

    public static final String PARAM_SET_NAME = "gmosSBlueprintLongslitNs";

    public SpGmosSBlueprintLongslitNs(DisperserSouth disperser, FilterSouth filter, FPUnitSouth fpu) {
        super(disperser, filter, fpu);
    }

    public SpGmosSBlueprintLongslitNs(ParamSet paramSet) {
        super(paramSet);
    }

    public String paramSetName() { return PARAM_SET_NAME; }

    @Override
    public String blueprintTypeLabel() { return "LongSlit N+S"; }
}
