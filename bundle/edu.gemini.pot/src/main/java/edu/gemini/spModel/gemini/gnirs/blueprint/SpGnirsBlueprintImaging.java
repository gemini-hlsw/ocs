package edu.gemini.spModel.gemini.gnirs.blueprint;

import edu.gemini.spModel.gemini.altair.blueprint.SpAltair;
import edu.gemini.spModel.gemini.gnirs.GNIRSParams.Filter;
import edu.gemini.spModel.gemini.gnirs.GNIRSParams.PixelScale;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;

public final class SpGnirsBlueprintImaging extends SpGnirsBlueprintBase {
    public static final String PARAM_SET_NAME    = "gnirsBlueprintImaging";
    public static final String FILTER_PARAM_NAME = "filter";

    public final Filter filter;

    public SpGnirsBlueprintImaging(SpAltair altair, PixelScale pixelScale, Filter filter) {
        super(altair, pixelScale);
        this.filter = filter;
    }

    public SpGnirsBlueprintImaging(ParamSet paramSet) {
        super(paramSet);
        this.filter = Pio.getEnumValue(paramSet, FILTER_PARAM_NAME, Filter.DEFAULT);
    }

    public String paramSetName() { return PARAM_SET_NAME; }

    @Override
    public String toString() {
        return String.format("GNIRS Imaging %s %s %s", altair.shortName(), pixelScale.displayValue(), filter.displayValue());
    }

    public ParamSet toParamSet(PioFactory factory) {
        ParamSet paramSet = super.toParamSet(factory);
        Pio.addEnumParam(factory, paramSet, FILTER_PARAM_NAME, filter);
        return paramSet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        SpGnirsBlueprintImaging that = (SpGnirsBlueprintImaging) o;

        if (filter != that.filter) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + filter.hashCode();
        return result;
    }
}
