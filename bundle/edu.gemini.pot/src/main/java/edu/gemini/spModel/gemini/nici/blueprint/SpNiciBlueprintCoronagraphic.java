package edu.gemini.spModel.gemini.nici.blueprint;

import edu.gemini.spModel.gemini.nici.NICIParams.DichroicWheel;
import edu.gemini.spModel.gemini.nici.NICIParams.Channel1FW;
import edu.gemini.spModel.gemini.nici.NICIParams.Channel2FW;
import edu.gemini.spModel.gemini.nici.NICIParams.FocalPlaneMask;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;

import java.util.List;

public final class SpNiciBlueprintCoronagraphic extends SpNiciBlueprintBase {
    public static final String PARAM_SET_NAME = "niciBlueprintCoronagraphic";
    public static final String FOCAL_PLANE_MASK_PARAM_NAME = "fpm";

    public final FocalPlaneMask fpm;

    public SpNiciBlueprintCoronagraphic(FocalPlaneMask fpm, DichroicWheel dichroic, List<Channel1FW> redFilters, List<Channel2FW> blueFilters) {
        super(dichroic, redFilters, blueFilters);
        this.fpm = fpm;
    }

    public SpNiciBlueprintCoronagraphic(ParamSet paramSet) {
        super(paramSet);
        this.fpm = Pio.getEnumValue(paramSet, FOCAL_PLANE_MASK_PARAM_NAME, FocalPlaneMask.DEFAULT);
    }

    public String paramSetName() { return PARAM_SET_NAME; }

    public ParamSet toParamSet(PioFactory factory) {
        ParamSet paramSet = super.toParamSet(factory);
        Pio.addEnumParam(factory, paramSet, FOCAL_PLANE_MASK_PARAM_NAME, fpm);
        return paramSet;
    }

    @Override
    public String toString() {
        return String.format("NICI Coronagraphic %s %s%s",
                fpm.displayValue(),
                dichroic.displayValue(),
                formatFilters());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        SpNiciBlueprintCoronagraphic that = (SpNiciBlueprintCoronagraphic) o;

        if (fpm != that.fpm) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + fpm.hashCode();
        return result;
    }
}
