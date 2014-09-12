package edu.gemini.spModel.gemini.gmos.blueprint;

import edu.gemini.spModel.gemini.gmos.GmosSouthType.DisperserSouth;
import edu.gemini.spModel.gemini.gmos.GmosSouthType.FilterSouth;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;

public abstract class SpGmosSBlueprintSpectroscopyBase extends SpGmosSBlueprintBase {

    public static final String DISPERSER_PARAM_NAME = "disperser";
    public static final String FILTER_PARAM_NAME    = "filter";

    public final DisperserSouth disperser;
    public final FilterSouth filter;

    protected SpGmosSBlueprintSpectroscopyBase(DisperserSouth disperser, FilterSouth filter) {
        this.disperser = disperser;
        this.filter    = filter;
    }

    protected SpGmosSBlueprintSpectroscopyBase(ParamSet paramSet) {
        disperser = Pio.getEnumValue(paramSet, DISPERSER_PARAM_NAME, DisperserSouth.DEFAULT);
        filter    = Pio.getEnumValue(paramSet, FILTER_PARAM_NAME, FilterSouth.DEFAULT);
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

        SpGmosSBlueprintSpectroscopyBase that = (SpGmosSBlueprintSpectroscopyBase) o;
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
