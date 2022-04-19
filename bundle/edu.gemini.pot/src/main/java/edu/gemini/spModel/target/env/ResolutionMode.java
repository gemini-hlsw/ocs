package edu.gemini.spModel.target.env;

import edu.gemini.spModel.type.DisplayableSpType;

import java.util.*;

public enum ResolutionMode implements DisplayableSpType {
    // This will be used by every instrument other than GHOST and thus can be considered disjoint;
    // the overlapping name should not make a difference.
    Standard("standard", "Standard Resolution", Collections.emptyList()),

    GhostStandard("ghostStandard", "Standard Resolution",
            Arrays.asList(
                    AsterismType.GhostSingleTarget,
                    AsterismType.GhostDualTarget,
                    AsterismType.GhostTargetPlusSky,
                    AsterismType.GhostSkyPlusTarget)
    ),
    GhostHigh("ghostHigh", "High Resolution",
            Collections.singletonList(
                    AsterismType.GhostHighResolutionTargetPlusSky
            )),
    GhostPRV("ghostPRV", "Precision Radial Velocity",
            Collections.singletonList(
                    AsterismType.GhostHighResolutionTargetPlusSky
            ));

    public final String tag;
    public final String displayName;
    public final List<AsterismType> asterismTypes;

    ResolutionMode(final String tag,
                   final String displayName,
                   final List<AsterismType> asterismTypes) {
        this.tag           = tag;
        this.displayName   = displayName;
        this.asterismTypes = asterismTypes;
    }

    @Override
    public String toString() {
        return displayName;
    }

    @Override
    public String displayValue() {
        return displayName;
    }
}
