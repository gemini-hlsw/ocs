package edu.gemini.spModel.target.env;

import edu.gemini.pot.sp.Instrument;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.gemini.ghost.AsterismConverters;
import edu.gemini.spModel.type.DisplayableSpType;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

public enum AsterismType implements DisplayableSpType {

    Single(
        "single",
        "Single Target",
        ResolutionMode.Standard,
        None.instance()
    ),

    GhostSingleTarget(
        "ghostSingleTarget",
         "Single Target",
         ResolutionMode.GhostStandard,
         new Some<>(AsterismConverters.GhostSingleTargetConverter$.MODULE$)
    ),

    GhostDualTarget(
        "ghostDualTarget",
        "Dual Target",
        ResolutionMode.GhostStandard,
        new Some<>(AsterismConverters.GhostDualTargetConverter$.MODULE$)
    ),

    GhostTargetPlusSky(
        "ghostTargetPlusSky",
        "SRIFU1 Target, SRIFU2 Sky",
        ResolutionMode.GhostStandard,
        new Some<>(AsterismConverters.GhostTargetPlusSkyConverter$.MODULE$)
    ),

    GhostSkyPlusTarget(
        "ghostSkyPlusTarget",
        "SRIFU1 Sky, SRIFU2 Target",
        ResolutionMode.GhostStandard,
        new Some<>(AsterismConverters.GhostSkyPlusTargetConverter$.MODULE$)
    ),

    GhostHighResolutionTargetPlusSky(
        "ghostHRTargetPlusSky",
        "HRIFU Target, Sky",
        ResolutionMode.GhostHigh,
        new Some<>(AsterismConverters.GhostHRTargetPlusSkyConverter$.MODULE$)
    ),

    GhostHighResolutionTargetPlusSkyPrv(
        "ghostHRTargetPlusSkyPrv",
        "HRIFU Target, Sky (PRV)",
        ResolutionMode.GhostPRV,
        new Some<>(AsterismConverters.GhostHRTargetPlusSkyPrvConverter$.MODULE$)
    )
    ;

    public final String tag;
    public final String displayName;
    public final ResolutionMode resolutionMode;
    public final Option<AsterismConverters.AsterismConverter> converter;

    AsterismType(
        String tag,
        String displayName,
        ResolutionMode resolutionMode,
        Option<AsterismConverters.AsterismConverter> converter
    ) {
        this.tag            = tag;
        this.displayName    = displayName;
        this.resolutionMode = resolutionMode;
        this.converter      = converter;
    }

    @Override
    public String toString() {
        return displayName;
    }

    @Override
    public String displayValue() {
        return displayName;
    }

    public static final SortedSet<AsterismType> DEFAULT_SET;

    static {
        final SortedSet<AsterismType> s = new TreeSet<>();
        s.add(Single);
        DEFAULT_SET = Collections.unmodifiableSortedSet(s);
    }

    public static final SortedSet<AsterismType> GHOST_SET;

    static {
        final SortedSet<AsterismType> s = new TreeSet<>();
        s.add(GhostSingleTarget);
        s.add(GhostDualTarget);
        s.add(GhostTargetPlusSky);
        s.add(GhostSkyPlusTarget);
        s.add(GhostHighResolutionTargetPlusSky);
        s.add(GhostHighResolutionTargetPlusSkyPrv);
        GHOST_SET = Collections.unmodifiableSortedSet(s);
    }

    // Return the asterism types supported by the different instruments.
    // We need to do this here because we want these statically accessible.
    public static SortedSet<AsterismType> supportedTypesForInstrument(final Instrument instType) {
        return (instType == Instrument.Ghost) ? GHOST_SET : DEFAULT_SET;
    }

    public static Option<SortedSet<AsterismType>> supportedTypesForComponent(final SPComponentType compType) {
        return Instrument
                .fromComponentType(compType)
                .map(AsterismType::supportedTypesForInstrument);
    }
}