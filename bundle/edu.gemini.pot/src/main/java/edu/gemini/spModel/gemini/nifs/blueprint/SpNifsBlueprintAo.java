package edu.gemini.spModel.gemini.nifs.blueprint;

import edu.gemini.spModel.gemini.altair.blueprint.SpAltair;
import edu.gemini.spModel.gemini.altair.blueprint.SpAltairReaders;
import edu.gemini.spModel.gemini.nifs.NIFSParams.Disperser;
import edu.gemini.spModel.gemini.nifs.NIFSParams.Mask;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;

public final class SpNifsBlueprintAo extends SpNifsBlueprintBase {
    public static final String PARAM_SET_NAME = "nifsBlueprintAo";
    public static final String OCCULTING_DISK_PARAM_NAME = "occultingDisk";

    public final SpAltair altair;
    public final Mask occultingDisk;

    public SpNifsBlueprintAo(SpAltair altair, Mask occultingDisk, Disperser disperser) {
        super(disperser);
        this.altair        = altair;
        this.occultingDisk = occultingDisk;
    }

    public SpNifsBlueprintAo(ParamSet paramSet) {
        super(paramSet);
        this.altair = SpAltairReaders.read(paramSet);
        this.occultingDisk = Pio.getEnumValue(paramSet, OCCULTING_DISK_PARAM_NAME, Mask.DEFAULT);
    }

    public String paramSetName() { return PARAM_SET_NAME; }

    @Override
    public ParamSet toParamSet(PioFactory factory) {
        ParamSet paramSet = super.toParamSet(factory);
        paramSet.addParamSet(altair.toParamSet(factory));
        Pio.addEnumParam(factory, paramSet, OCCULTING_DISK_PARAM_NAME, occultingDisk);
        return paramSet;
    }

    @Override
    public String toString() {
        return String.format("NIFS %s %s %s",
                altair.shortName(),
                occultingDisk.displayValue(),
                disperser.displayValue());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        SpNifsBlueprintAo that = (SpNifsBlueprintAo) o;

        if (!altair.equals(that.altair)) return false;
        if (occultingDisk != that.occultingDisk) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + altair.hashCode();
        result = 31 * result + occultingDisk.hashCode();
        return result;
    }
}
