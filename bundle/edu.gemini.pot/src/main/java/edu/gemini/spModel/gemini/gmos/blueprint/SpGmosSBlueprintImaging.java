package edu.gemini.spModel.gemini.gmos.blueprint;

import edu.gemini.spModel.gemini.gmos.GmosSouthType.FilterSouth;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static edu.gemini.spModel.template.SpBlueprintUtil.mkString;

public final class SpGmosSBlueprintImaging extends SpGmosSBlueprintBase {

    public static final String PARAM_SET_NAME     = "gmosSBlueprintImaging";
    public static final String FILTERS_PARAM_NAME = "filters";

    public final List<FilterSouth> filters;

    public SpGmosSBlueprintImaging(List<FilterSouth> filters) {
        this.filters = Collections.unmodifiableList(new ArrayList<FilterSouth>(filters));
    }

    public SpGmosSBlueprintImaging(ParamSet paramSet) {
        filters = Pio.getEnumValues(paramSet, FILTERS_PARAM_NAME, FilterSouth.class);
    }

    public String paramSetName() { return PARAM_SET_NAME; }

    @Override
    public String toString() {
        return String.format("GMOS-S Imaging %s", mkString(filters, "+"));
    }

    @Override
    public ParamSet toParamSet(PioFactory factory) {
        ParamSet paramSet = super.toParamSet(factory);
        Pio.addEnumListParam(factory, paramSet, FILTERS_PARAM_NAME, filters);
        return paramSet;
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) return false;

        SpGmosSBlueprintImaging that = (SpGmosSBlueprintImaging) o;
        return filters.equals(that.filters);
    }

    @Override
    public int hashCode() {
        int res = super.hashCode();
        return 31*res + filters.hashCode();
    }
}
