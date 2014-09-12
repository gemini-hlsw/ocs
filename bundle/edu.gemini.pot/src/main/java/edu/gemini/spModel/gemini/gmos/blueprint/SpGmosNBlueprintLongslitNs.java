package edu.gemini.spModel.gemini.gmos.blueprint;

import edu.gemini.spModel.gemini.altair.blueprint.SpAltair;
import edu.gemini.spModel.gemini.gmos.GmosNorthType.DisperserNorth;
import edu.gemini.spModel.gemini.gmos.GmosNorthType.FPUnitNorth;
import edu.gemini.spModel.gemini.gmos.GmosNorthType.FilterNorth;
import edu.gemini.spModel.pio.ParamSet;

public final class SpGmosNBlueprintLongslitNs extends SpGmosNBlueprintSpectroscopyFpuBase {

    public static final String PARAM_SET_NAME = "gmosNBlueprintLongslitNs";

    public SpGmosNBlueprintLongslitNs(SpAltair altair, DisperserNorth disperser, FilterNorth filter, FPUnitNorth fpu) {
        super(altair, disperser, filter, fpu);
    }

    public SpGmosNBlueprintLongslitNs(ParamSet paramSet) {
        super(paramSet);
    }

    public String paramSetName() { return PARAM_SET_NAME; }

    @Override
    public String blueprintTypeLabel() { return "LongSlit N+S"; }
}
