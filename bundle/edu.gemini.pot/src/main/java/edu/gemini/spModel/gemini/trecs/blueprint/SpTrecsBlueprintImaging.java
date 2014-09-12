package edu.gemini.spModel.gemini.trecs.blueprint;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.gemini.trecs.InstTReCS;
import edu.gemini.spModel.gemini.trecs.TReCSParams.Filter;

import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.template.SpBlueprint;
import edu.gemini.spModel.template.SpBlueprintUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SpTrecsBlueprintImaging extends SpBlueprint {
    public static final String PARAM_SET_NAME     = "trecsBlueprintImaging";
    public static final String FILTERS_PARAM_NAME = "filters";

    public final List<Filter> filters;

    public SpTrecsBlueprintImaging(List<Filter> filters) {
        this.filters = Collections.unmodifiableList(new ArrayList<Filter>(filters));
    }

    public SpTrecsBlueprintImaging(ParamSet paramSet) {
        this.filters = Pio.getEnumValues(paramSet, FILTERS_PARAM_NAME, Filter.class);
    }

    public String paramSetName() { return PARAM_SET_NAME; }
    public SPComponentType instrumentType() { return InstTReCS.SP_TYPE; }

    @Override
    public String toString() {
        return String.format("T-ReCS Imaging %s",
                SpBlueprintUtil.mkString(filters, "+"));
    }

    public ParamSet toParamSet(PioFactory factory) {
        ParamSet paramSet = factory.createParamSet(PARAM_SET_NAME);
        Pio.addEnumListParam(factory, paramSet, FILTERS_PARAM_NAME, filters);
        return paramSet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SpTrecsBlueprintImaging that = (SpTrecsBlueprintImaging) o;

        if (!filters.equals(that.filters)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return filters.hashCode();
    }
}
