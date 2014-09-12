package edu.gemini.spModel.gemini.gmos.blueprint;

import edu.gemini.spModel.gemini.gmos.GmosSouthType.DisperserSouth;
import edu.gemini.spModel.gemini.gmos.GmosSouthType.FPUnitSouth;
import edu.gemini.spModel.gemini.gmos.GmosSouthType.FilterSouth;
import edu.gemini.spModel.pio.ParamSet;

public final class SpGmosSBlueprintIfu extends SpGmosSBlueprintSpectroscopyFpuBase {

    public static final String PARAM_SET_NAME = "gmosSBlueprintIfu";

    public SpGmosSBlueprintIfu(DisperserSouth disperser, FilterSouth filter, FPUnitSouth fpu) {
        super(disperser, filter, fpu);
    }

    public SpGmosSBlueprintIfu(ParamSet paramSet) {
        super(paramSet);
    }

    public String paramSetName() { return PARAM_SET_NAME; }

    @Override
    public String blueprintTypeLabel() { return "IFU"; }
}
