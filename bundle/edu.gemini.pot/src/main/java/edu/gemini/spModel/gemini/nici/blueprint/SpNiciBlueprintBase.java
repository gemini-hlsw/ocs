package edu.gemini.spModel.gemini.nici.blueprint;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.gemini.nici.InstNICI;
import edu.gemini.spModel.gemini.nici.NICIParams.DichroicWheel;
import edu.gemini.spModel.gemini.nici.NICIParams.Channel1FW;
import edu.gemini.spModel.gemini.nici.NICIParams.Channel2FW;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.template.SpBlueprint;
import edu.gemini.spModel.template.SpBlueprintUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class SpNiciBlueprintBase extends SpBlueprint {
    public static final String DICHROIC_PARAM_NAME     = "dichroic";
    public static final String RED_FILTERS_PARAM_NAME  = "redFilters";
    public static final String BLUE_FILTERS_PARAM_NAME = "blueFilters";

    public final DichroicWheel dichroic;
    public final List<Channel1FW> redFilters;
    public final List<Channel2FW> blueFilters;

    protected SpNiciBlueprintBase(DichroicWheel dichroic, List<Channel1FW> redFilters, List<Channel2FW> blueFilters) {
        this.dichroic    = dichroic;
        this.redFilters  = Collections.unmodifiableList(new ArrayList<Channel1FW>(redFilters));
        this.blueFilters = Collections.unmodifiableList(new ArrayList<Channel2FW>(blueFilters));
    }

    protected SpNiciBlueprintBase(ParamSet paramSet) {
        this.dichroic    = Pio.getEnumValue(paramSet, DICHROIC_PARAM_NAME, DichroicWheel.DEFAULT);
        this.redFilters  = Pio.getEnumValues(paramSet, RED_FILTERS_PARAM_NAME, Channel1FW.class);
        this.blueFilters = Pio.getEnumValues(paramSet, BLUE_FILTERS_PARAM_NAME, Channel2FW.class);
    }

    public SPComponentType instrumentType() { return InstNICI.SP_TYPE; }

    public ParamSet toParamSet(PioFactory factory) {
        ParamSet paramSet = factory.createParamSet(paramSetName());
        Pio.addEnumParam(factory, paramSet, DICHROIC_PARAM_NAME, dichroic);
        Pio.addEnumListParam(factory, paramSet, RED_FILTERS_PARAM_NAME, redFilters);
        Pio.addEnumListParam(factory, paramSet, BLUE_FILTERS_PARAM_NAME, blueFilters);
        return paramSet;
    }

    protected String formatFilters() {
        StringBuilder buf = new StringBuilder();
        if (redFilters.size() > 0) {
            buf.append(SpBlueprintUtil.mkString(redFilters, " Red(", "+", ")"));
        }
        if (blueFilters.size() > 0) {
            buf.append(SpBlueprintUtil.mkString(blueFilters, " Blue(", "+", ")"));
        }
        return buf.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SpNiciBlueprintBase that = (SpNiciBlueprintBase) o;

        if (!blueFilters.equals(that.blueFilters)) return false;
        if (dichroic != that.dichroic) return false;
        if (!redFilters.equals(that.redFilters)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = dichroic.hashCode();
        result = 31 * result + redFilters.hashCode();
        result = 31 * result + blueFilters.hashCode();
        return result;
    }
}
