package edu.gemini.spModel.target.env;

import edu.gemini.pot.sp.SPComponentBroadType;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.shared.util.immutable.DefaultImList;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.spModel.gemini.ghost.GhostAsterism$;

public enum AsterismType {
    Single("single") {
        @Override
        public Asterism createEmptyAsterism() {
            return Asterism$.MODULE$.createSingleAsterism();
        }
    },
    GhostStandardResolution("ghostStandardRes") {
        @Override
        public Asterism createEmptyAsterism() {
            return GhostAsterism$.MODULE$.createEmptyStandardResolutionAsterism();
        }
    },
    GhostHighResolution("ghostHighRes") {
        @Override
        public Asterism createEmptyAsterism() {
            return GhostAsterism$.MODULE$.createEmptyHighResolutionAsterism();
        }
    },
    ;

    public final String tag;

    AsterismType(final String tag) {
        this.tag = tag;
    }

    public abstract Asterism createEmptyAsterism();

    // Return the asterism types supported by the different instruments.
    // We need to do this here because we want these statically accessible.
    // Assume the head of the list is the default for the instrument.
    public static ImList<AsterismType> supportedTypesForInstrument(final SPComponentType instType) {
        if (!instType.broadType.equals(SPComponentBroadType.INSTRUMENT))
            throw new RuntimeException("Can only look up supported asterism types for instruments");

        switch (instType) {
            case INSTRUMENT_GHOST:
                return DefaultImList.create(GhostStandardResolution, GhostHighResolution);
            default:
                return DefaultImList.create(Single);
        }
    }

    // The default asterism type per instrument.
    public static AsterismType defaultTypeForInstrument(final SPComponentType instType) {
        return supportedTypesForInstrument(instType).head();
    }
}
