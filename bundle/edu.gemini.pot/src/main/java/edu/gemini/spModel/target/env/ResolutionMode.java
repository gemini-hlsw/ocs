package edu.gemini.spModel.target.env;

import edu.gemini.spModel.type.DisplayableSpType;

import java.util.*;
import java.util.stream.Collectors;

public enum ResolutionMode implements DisplayableSpType {
    // This will be used by every instrument other than GHOST and thus can be considered disjoint;
    // the overlapping name should not make a difference.
    Standard("standard", "Standard Resolution"),

    GhostStandard("ghostStandard", "Standard Resolution"),
    GhostHigh("ghostHigh", "High Resolution"),
    GhostPRV("ghostPRV", "Precision Radial Velocity")
    ;

    public final String tag;
    public final String displayName;

    ResolutionMode(final String tag,
                   final String displayName) {
        this.tag           = tag;
        this.displayName   = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    @Override
    public String displayValue() {
        return displayName;
    }

    public SortedSet<AsterismType> asterismTypes() {
        return Arrays
            .stream(AsterismType.values())
            .filter(a -> a.resolutionMode == this)
            .collect(Collectors.toCollection(() -> new TreeSet<AsterismType>()));
    }
}
