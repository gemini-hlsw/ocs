package edu.gemini.spModel.gemini.nifs.blueprint;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.gemini.nifs.InstNIFS;
import edu.gemini.spModel.gemini.nifs.NIFSParams.Disperser;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.template.SpBlueprint;

public abstract class SpNifsBlueprintBase extends SpBlueprint {
    public static final String DISPERSER_PARAM_NAME = "disperser";

    public final Disperser disperser;

    protected SpNifsBlueprintBase(Disperser disperser) {
        this.disperser = disperser;
    }

    protected SpNifsBlueprintBase(ParamSet paramSet) {
        this.disperser = Pio.getEnumValue(paramSet, DISPERSER_PARAM_NAME, Disperser.DEFAULT);
    }

    public SPComponentType instrumentType() { return InstNIFS.SP_TYPE; }

    public ParamSet toParamSet(PioFactory factory) {
        ParamSet paramSet = factory.createParamSet(paramSetName());
        Pio.addEnumParam(factory, paramSet, DISPERSER_PARAM_NAME, disperser);
        return paramSet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SpNifsBlueprintBase that = (SpNifsBlueprintBase) o;

        if (disperser != that.disperser) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return disperser.hashCode();
    }
}
