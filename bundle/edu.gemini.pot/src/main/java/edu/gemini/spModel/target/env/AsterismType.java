package edu.gemini.spModel.target.env;

import edu.gemini.pot.sp.SPComponentBroadType;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.gemini.ghost.AsterismConverters;
import edu.gemini.spModel.gemini.ghost.GhostAsterism$;

import java.util.function.Supplier;

public enum AsterismType {
    /**
     * Note all GHOST standard resolution asterisms have the same tag because we use this to output them to XML
     * in a category together.
     */
    Single("single", "Single Target", None.instance(), Asterism$.MODULE$::createSingleAsterism),

    GhostStandardResolutionSingleTarget("ghostStandardRes", "Single Target",
            new Some<>(AsterismConverters.GhostSingleTargetConverter$.MODULE$),
            GhostAsterism$.MODULE$::createEmptyStandardResolutionSingleTargetAsterism),

    GhostStandardResolutionDualTarget("ghostStandardRes", "Dual Target",
            new Some<>(AsterismConverters.GhostDualTargetConverter$.MODULE$),
            GhostAsterism$.MODULE$::createEmptyStandardResolutionDualTargetAsterism),

    GhostStandardResolutionTargetPlusSky("ghostStandardRes", "SRIFU1 Target, SRIFU2 Sky",
            new Some<>(AsterismConverters.GhostTargetPlusSkyConverter$.MODULE$),
            GhostAsterism$.MODULE$::createEmptyStandardResolutionTargetPlusSkyAsterism),

    GhostStandardResolutionSkyPlusTarget("ghostStandardRes", "SRIFU1 Sky, SRIFU2 Target",
            new Some<>(AsterismConverters.GhostSkyPlusTargetConverter$.MODULE$),
            GhostAsterism$.MODULE$::createEmptyStandardResolutionSkyPlusTargetAsterism),

    GhostHighResolution("ghostHighRes", "High Resolution",
            new Some<>(AsterismConverters.GhostHighResolutionConverter$.MODULE$),
            GhostAsterism$.MODULE$::createEmptyHighResolutionAsterism),
    ;

    public final String tag;
    public final Option<AsterismConverters.AsterismConverter> converter;
    public final String name;
    public final Supplier<Asterism> emptyAsterismCreator;

    AsterismType(final String tag, final String name,
                 final Option<AsterismConverters.AsterismConverter> converter,
                 final Supplier<Asterism> emptyAsterismCreator) {
        this.tag = tag;
        this.name = name;
        this.converter = converter;
        this.emptyAsterismCreator = emptyAsterismCreator;
    }

    @Override
    public String toString() {
        return name;
    }

    // Return the asterism types supported by the different instruments.
    // We need to do this here because we want these statically accessible.
    // Assume the head of the list is the default for the instrument.
    public static ImList<AsterismType> supportedTypesForInstrument(final SPComponentType instType) {
        if (!instType.broadType.equals(SPComponentBroadType.INSTRUMENT))
            throw new RuntimeException("Can only look up supported asterism types for instruments");

        switch (instType) {
            case INSTRUMENT_GHOST:
                return DefaultImList.create(
                        GhostStandardResolutionSingleTarget,
                        GhostStandardResolutionDualTarget,
                        GhostStandardResolutionTargetPlusSky,
                        GhostStandardResolutionSkyPlusTarget,
                        GhostHighResolution);
            default:
                return DefaultImList.create(Single);
        }
    }

    // The default asterism type per instrument.
    public static AsterismType defaultTypeForInstrument(final SPComponentType instType) {
        return supportedTypesForInstrument(instType).head();
    }
}