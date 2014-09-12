package edu.gemini.spModel.gemini.gnirs.blueprint;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.gemini.altair.blueprint.SpAltair;
import edu.gemini.spModel.gemini.altair.blueprint.SpAltairReaders;
import edu.gemini.spModel.gemini.gnirs.InstGNIRS;
import edu.gemini.spModel.gemini.gnirs.GNIRSParams.PixelScale;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.template.SpBlueprint;

public abstract class SpGnirsBlueprintBase extends SpBlueprint {
    public static final String PIXEL_SCALE_PARAM_NAME = "pixelScale";

    public final SpAltair altair;
    public final PixelScale pixelScale;

    protected SpGnirsBlueprintBase(SpAltair altair, PixelScale pixelScale) {
        this.altair     = altair;
        this.pixelScale = pixelScale;
    }

    protected SpGnirsBlueprintBase(ParamSet paramSet) {
        this.altair     = SpAltairReaders.read(paramSet);
        this.pixelScale = Pio.getEnumValue(paramSet, PIXEL_SCALE_PARAM_NAME, PixelScale.DEFAULT);
    }

    public SPComponentType instrumentType() { return InstGNIRS.SP_TYPE; }

    public ParamSet toParamSet(PioFactory factory) {
        ParamSet paramSet = factory.createParamSet(paramSetName());
        paramSet.addParamSet(altair.toParamSet(factory));
        Pio.addEnumParam(factory, paramSet, PIXEL_SCALE_PARAM_NAME, pixelScale);
        return paramSet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SpGnirsBlueprintBase that = (SpGnirsBlueprintBase) o;
        if (!altair.equals(that.altair)) return false;
        if (!pixelScale.equals(that.pixelScale)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = altair.hashCode();
        result = 31 * result + pixelScale.hashCode();
        return result;
    }
}
