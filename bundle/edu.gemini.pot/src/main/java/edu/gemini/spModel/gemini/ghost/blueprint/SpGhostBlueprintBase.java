package edu.gemini.spModel.gemini.ghost.blueprint;

import edu.gemini.pot.sp.Instrument;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.target.env.AsterismType;
import edu.gemini.spModel.template.SpBlueprint;

import java.util.Objects;


public abstract class SpGhostBlueprintBase extends SpBlueprint {

    public static final String ASTERISM_TYPE_PARAM_NAME = "asterismType";

    private final AsterismType asterismType;

    protected SpGhostBlueprintBase(AsterismType asterismType) {
        assert AsterismType.supportedTypesForInstrument(Instrument.Ghost).contains(asterismType) :
                "Ghost blueprints must be created with a GHOST asterism type, not " + asterismType.tag;

        this.asterismType = asterismType;
    }

    @Override public SPComponentType instrumentType() {
        return SPComponentType.INSTRUMENT_GHOST;
    }

    @Override public AsterismType asterismType() {
       return asterismType;
    }

    @Override public ParamSet toParamSet(PioFactory factory) {
        final ParamSet paramSet = factory.createParamSet(paramSetName());
        Pio.addEnumParam(factory, paramSet, ASTERISM_TYPE_PARAM_NAME, asterismType);
        return paramSet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpGhostBlueprintBase that = (SpGhostBlueprintBase) o;
        return asterismType == that.asterismType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(asterismType);
    }
}
