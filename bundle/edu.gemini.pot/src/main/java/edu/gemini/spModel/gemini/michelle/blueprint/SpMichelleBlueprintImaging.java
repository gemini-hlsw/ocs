package edu.gemini.spModel.gemini.michelle.blueprint;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.gemini.michelle.InstMichelle;
import edu.gemini.spModel.gemini.michelle.MichelleParams.Filter;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.template.SpBlueprint;
import static edu.gemini.spModel.template.SpBlueprintUtil.mkString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Michelle Blueprint Imaging.
 */
public final class SpMichelleBlueprintImaging extends SpBlueprint {
    public static final String PARAM_SET_NAME         = "michelleBlueprintImaging";
    public static final String FILTERS_PARAM_NAME     = "filters";
    public static final String POLARIMETRY_PARAM_NAME = "polarimetry";

    public final List<Filter> filters;
    public final boolean polarimetry;

    public SpMichelleBlueprintImaging(List<Filter> filters, boolean polarimetry) {
        this.filters     = Collections.unmodifiableList(new ArrayList<Filter>(filters));
        this.polarimetry = polarimetry;
    }

    public SpMichelleBlueprintImaging(ParamSet paramSet) {
        this.filters     = Pio.getEnumValues(paramSet, FILTERS_PARAM_NAME, Filter.class);
        this.polarimetry = Pio.getBooleanValue(paramSet, POLARIMETRY_PARAM_NAME, false);
    }

    public SPComponentType instrumentType() { return InstMichelle.SP_TYPE; }
    public String paramSetName() { return PARAM_SET_NAME; }

    public ParamSet toParamSet(PioFactory factory) {
        ParamSet paramSet = factory.createParamSet(PARAM_SET_NAME);
        Pio.addEnumListParam(factory, paramSet, FILTERS_PARAM_NAME, filters);
        Pio.addBooleanParam(factory, paramSet, POLARIMETRY_PARAM_NAME, polarimetry);
        return paramSet;
    }

    @Override
    public String toString() {
        return String.format("Michelle Imaging %s %s", mkString(filters), (polarimetry ? "Polarimetry" : ""));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SpMichelleBlueprintImaging that = (SpMichelleBlueprintImaging) o;
        if (polarimetry != that.polarimetry) return false;
        return filters.equals(that.filters);
    }

    @Override
    public int hashCode() {
        int result = filters.hashCode();
        result = 31 * result + (polarimetry ? 1 : 0);
        return result;
    }
}
