package edu.gemini.spModel.gemini.gsaoi.blueprint;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.gemini.gsaoi.Gsaoi;
import edu.gemini.spModel.gemini.gsaoi.Gsaoi.Filter;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.template.SpBlueprint;
import edu.gemini.spModel.template.SpBlueprintUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SpGsaoiBlueprint extends SpBlueprint {
    public static final String PARAM_SET_NAME     = "gsoaiBlueprint";
    public static final String FILTERS_PARAM_NAME = "filters";

    public final List<Filter> filters;

    public SpGsaoiBlueprint(List<Filter> filters) {
        this.filters = Collections.unmodifiableList(new ArrayList<Filter>(filters));
    }

    public SpGsaoiBlueprint(ParamSet paramSet) {
        this.filters = Pio.getEnumValues(paramSet, FILTERS_PARAM_NAME, Filter.class);
    }

    public String paramSetName() { return PARAM_SET_NAME; }
    public SPComponentType instrumentType() { return Gsaoi.SP_TYPE; }

    @Override
    public String toString() {
        return String.format("GSAOI %s",
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

        SpGsaoiBlueprint that = (SpGsaoiBlueprint) o;

        if (!filters.equals(that.filters)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return filters.hashCode();
    }
}
