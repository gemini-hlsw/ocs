package edu.gemini.spModel.gemini.niri.blueprint;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.gemini.altair.blueprint.SpAltair;
import edu.gemini.spModel.gemini.altair.blueprint.SpAltairReaders;
import edu.gemini.spModel.gemini.niri.InstNIRI;
import edu.gemini.spModel.gemini.niri.Niri.Camera;
import edu.gemini.spModel.gemini.niri.Niri.Filter;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.template.SpBlueprint;
import edu.gemini.spModel.template.SpBlueprintUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SpNiriBlueprint extends SpBlueprint {
    public static final String PARAM_SET_NAME     = "niriBlueprint";
    public static final String CAMERA_PARAM_NAME  = "camera";
    public static final String FILTERS_PARAM_NAME = "filters";

    public final SpAltair altair;
    public final Camera camera;
    public final List<Filter> filters;

    public SpNiriBlueprint(SpAltair altair, Camera camera, List<Filter> filters) {
        this.altair  = altair;
        this.camera  = camera;
        this.filters = Collections.unmodifiableList(new ArrayList<Filter>(filters));
    }

    public SpNiriBlueprint(ParamSet paramSet) {
        this.altair  = SpAltairReaders.read(paramSet);
        this.camera  = Pio.getEnumValue(paramSet, CAMERA_PARAM_NAME, Camera.DEFAULT);
        this.filters = Pio.getEnumValues(paramSet, FILTERS_PARAM_NAME, Filter.class);
    }

    public String paramSetName() { return PARAM_SET_NAME; }
    public SPComponentType instrumentType() { return InstNIRI.SP_TYPE; }

    @Override
    public String toString() {
        return String.format("NIRI %s %s %s",
                altair.shortName(),
                camera.displayValue(),
                SpBlueprintUtil.mkString(filters, "+"));
    }

    public ParamSet toParamSet(PioFactory factory) {
        ParamSet paramSet = factory.createParamSet(PARAM_SET_NAME);
        paramSet.addParamSet(altair.toParamSet(factory));
        Pio.addEnumParam(factory, paramSet, CAMERA_PARAM_NAME, camera);
        Pio.addEnumListParam(factory, paramSet, FILTERS_PARAM_NAME, filters);
        return paramSet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SpNiriBlueprint that = (SpNiriBlueprint) o;

        if (!altair.equals(that.altair)) return false;
        if (camera != that.camera) return false;
        if (!filters.equals(that.filters)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = altair.hashCode();
        result = 31 * result + camera.hashCode();
        result = 31 * result + filters.hashCode();
        return result;
    }
}
