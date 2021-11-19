package edu.gemini.spModel.target.env;

import edu.gemini.pot.sp.Instrument;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.gemini.ghost.AsterismConverters;
import edu.gemini.spModel.gemini.ghost.GhostAsterism$;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;

public enum AsterismType {
    Single("single", "Single Target", None.instance(), Asterism$.MODULE$::createSingleAsterism),

    GhostSingleTarget("ghostSingleTarget", "Single Target",
            new Some<>(AsterismConverters.GhostSingleTargetConverter$.MODULE$),
            GhostAsterism$.MODULE$::createEmptySingleTargetAsterism),

    GhostDualTarget("ghostDualTarget", "Dual Target",
            new Some<>(AsterismConverters.GhostDualTargetConverter$.MODULE$),
            GhostAsterism$.MODULE$::createEmptyDualTargetAsterism),

    GhostTargetPlusSky("ghostTargetPlusSky", "SRIFU1 Target, SRIFU2 Sky",
            new Some<>(AsterismConverters.GhostTargetPlusSkyConverter$.MODULE$),
            GhostAsterism$.MODULE$::createEmptyTargetPlusSkyAsterism),

    GhostSkyPlusTarget("ghostSkyPlusTarget", "SRIFU1 Sky, SRIFU2 Target",
            new Some<>(AsterismConverters.GhostSkyPlusTargetConverter$.MODULE$),
            GhostAsterism$.MODULE$::createEmptySkyPlusTargetAsterism),

    GhostHighResolutionTargetPlusSky("ghostHRTargetPlusSky", "HRIFU Target, Sky",
            new Some<>(AsterismConverters.GhostHRTargetPlusSkyConverter$.MODULE$),
            GhostAsterism$.MODULE$::createEmptyTargetPlusSkyAsterism)

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
        final Set<AsterismType> result;
        if (instType == Instrument.Ghost) {
            final Set<AsterismType> s = new TreeSet<>();
            s.add(GhostSingleTarget);
            s.add(GhostDualTarget);
            s.add(GhostTargetPlusSky);
            s.add(GhostSkyPlusTarget);
            s.add(GhostHighResolutionTargetPlusSky);
            result = Collections.unmodifiableSet(s);
        } else {
            result = Collections.singleton(Single);
        }
        return result;
    }
}