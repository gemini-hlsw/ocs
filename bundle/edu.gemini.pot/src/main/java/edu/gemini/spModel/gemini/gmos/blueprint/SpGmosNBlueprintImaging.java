package edu.gemini.spModel.gemini.gmos.blueprint;

import edu.gemini.spModel.gemini.altair.blueprint.SpAltair;
import edu.gemini.spModel.gemini.gmos.GmosNorthType.FilterNorth;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import static edu.gemini.spModel.template.SpBlueprintUtil.mkString;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SpGmosNBlueprintImaging extends SpGmosNBlueprintBase {

    public static final String PARAM_SET_NAME     = "gmosNBlueprintImaging";
    public static final String FILTERS_PARAM_NAME = "filters";

    public final List<FilterNorth> filters;

    public SpGmosNBlueprintImaging(SpAltair altair, List<FilterNorth> filters) {
        super(altair);
        this.filters = Collections.unmodifiableList(new ArrayList<FilterNorth>(filters));
    }

    public SpGmosNBlueprintImaging(ParamSet paramSet) {
        super(paramSet);
        filters = Pio.getEnumValues(paramSet, FILTERS_PARAM_NAME, FilterNorth.class);
    }

    public String paramSetName() { return PARAM_SET_NAME; }

    @Override
    public String toString() {
        return super.toString("GMOS-N Imaging", mkString(filters, "+"));
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

        SpGmosNBlueprintImaging that = (SpGmosNBlueprintImaging) o;
        return filters.equals(that.filters);
    }

    @Override
    public int hashCode() {
        int res = super.hashCode();
        return 31*res + filters.hashCode();
    }
}
