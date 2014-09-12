package edu.gemini.spModel.gemini.gmos.blueprint;

import edu.gemini.spModel.gemini.altair.blueprint.SpAltair;
import edu.gemini.spModel.gemini.gmos.GmosNorthType.DisperserNorth;
import edu.gemini.spModel.gemini.gmos.GmosNorthType.FilterNorth;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;

public abstract class SpGmosNBlueprintSpectroscopyBase extends SpGmosNBlueprintBase {

    public static final String DISPERSER_PARAM_NAME = "disperser";
    public static final String FILTER_PARAM_NAME    = "filter";

    public final DisperserNorth disperser;
    public final FilterNorth filter;

    protected SpGmosNBlueprintSpectroscopyBase(SpAltair altair, DisperserNorth disperser, FilterNorth filter) {
        super(altair);
        this.disperser = disperser;
        this.filter    = filter;
    }

    protected SpGmosNBlueprintSpectroscopyBase(ParamSet paramSet) {
        super(paramSet);
        disperser = Pio.getEnumValue(paramSet, DISPERSER_PARAM_NAME, DisperserNorth.DEFAULT);
        filter    = Pio.getEnumValue(paramSet, FILTER_PARAM_NAME, FilterNorth.DEFAULT);
    }

    @Override
    public ParamSet toParamSet(PioFactory factory) {
        ParamSet paramSet = super.toParamSet(factory);
        Pio.addEnumParam(factory, paramSet, DISPERSER_PARAM_NAME, disperser);
        Pio.addEnumParam(factory, paramSet, FILTER_PARAM_NAME, filter);
        return paramSet;
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) return false;

        SpGmosNBlueprintSpectroscopyBase that = (SpGmosNBlueprintSpectroscopyBase) o;
        if (disperser != that.disperser) return false;
        if (filter != that.filter) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int res = super.hashCode();
        res = res*31 + filter.hashCode();
        res = res*31 + disperser.hashCode();
        return res;
    }
}
