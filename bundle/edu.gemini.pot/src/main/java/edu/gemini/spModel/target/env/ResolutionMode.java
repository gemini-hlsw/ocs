package edu.gemini.spModel.target.env;

import java.util.*;

public enum ResolutionMode {
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
            Arrays.asList(
                    AsterismType.GhostHighResolutionTarget,
                    AsterismType.GhostHighResolutionTargetPlusSky
            )),
    GhostPRV("ghostPRV", "Precision Radial Velocity",
            Collections.singletonList(AsterismType.GhostHighResolutionTarget));

    public final String tag;
    public final String name;
    public final List<AsterismType> asterismTypes;

    ResolutionMode(final String tag,
                   final String name,
                   final List<AsterismType> asterismTypes) {
        this.tag = tag;
        this.name = name;
        this.asterismTypes = asterismTypes;
    }

    @Override
    public String toString() {
        return name;
    }
}
