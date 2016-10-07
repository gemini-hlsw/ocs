package edu.gemini.spModel.gemini.flamingos2.blueprint;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.gemini.flamingos2.Flamingos2;
import edu.gemini.spModel.gemini.flamingos2.Flamingos2.Filter;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.template.SpBlueprint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class SpFlamingos2BlueprintBase extends SpBlueprint {
    public static final String FILTERS_PARAM_NAME = "filters";

    public final List<Filter> filters;

    protected SpFlamingos2BlueprintBase(List<Filter> filters) {
        this.filters = Collections.unmodifiableList(new ArrayList<>(filters));
    }

    protected SpFlamingos2BlueprintBase(ParamSet paramSet) {
        this.filters = Pio.getEnumValues(paramSet, FILTERS_PARAM_NAME, Filter.class);
    }

    public SPComponentType instrumentType() { return Flamingos2.SP_TYPE; }

    public ParamSet toParamSet(PioFactory factory) {
        ParamSet paramSet = factory.createParamSet(paramSetName());
        Pio.addEnumListParam(factory, paramSet, FILTERS_PARAM_NAME, filters);
        return paramSet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SpFlamingos2BlueprintBase that = (SpFlamingos2BlueprintBase) o;
        if (!filters.equals(that.filters)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return filters.hashCode();
    }
}
