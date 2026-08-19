package edu.gemini.spModel.gemini.ghost.blueprint;

import edu.gemini.pot.sp.Instrument;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.target.env.AsterismType;

public final class SpGhostBlueprint extends SpGhostBlueprintBase {

    public static final String PARAM_SET_NAME = "ghost";

    private SpGhostBlueprint(AsterismType asterismType) {
        super(asterismType);
    }

    /** Required by SpBlueprintFactory, which looks it up reflectively. */
    public SpGhostBlueprint(ParamSet paramSet) {
        super(Pio.getEnumValue(paramSet, ASTERISM_TYPE_PARAM_NAME, AsterismType.GhostSingleTarget));
    }

    public static Option<SpGhostBlueprint> fromAsterismType(AsterismType asterismType) {
        return ImOption.when(
            AsterismType.supportedTypesForInstrument(Instrument.Ghost).contains(asterismType),
            () -> new SpGhostBlueprint(asterismType)
        );
    }

    @Override public String paramSetName() {
        return PARAM_SET_NAME;
    }

    public String toString() {
        return String.format("Ghost %s", asterismType().displayName);
    }

}
