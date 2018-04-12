package edu.gemini.spModel.target.env;

import edu.gemini.pot.sp.Instrument;
import edu.gemini.pot.sp.SPComponentBroadType;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.gemini.ghost.AsterismConverters;
import edu.gemini.spModel.gemini.ghost.GhostAsterism$;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
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
    public static Set<AsterismType> supportedTypesForInstrument(final Instrument instType) {
        switch (instType) {
            case Ghost:
                final Set<AsterismType> s = new TreeSet<>();
                s.add(GhostStandardResolutionSingleTarget);
                s.add(GhostStandardResolutionDualTarget);
                s.add(GhostStandardResolutionTargetPlusSky);
                s.add(GhostStandardResolutionSkyPlusTarget);
                s.add(GhostHighResolution);
                return Collections.unmodifiableSet(s);
            default:
                return Collections.singleton(Single);
        }
    }
}