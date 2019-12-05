package edu.gemini.spModel.gemini.ghost;

import edu.gemini.spModel.type.DisplayableSpType;
import edu.gemini.spModel.type.LoggableSpType;
import edu.gemini.spModel.type.SequenceableSpType;
import edu.gemini.spModel.type.SpTypeUtil;

public enum GhostSpatialBinning implements DisplayableSpType, LoggableSpType, SequenceableSpType {
    /** We disallow 1x2 spectral / spatial binning. **/
    ONE(1),
    TWO(2),
    FOUR(4),
    EIGHT(8)
    ;

    public static final GhostSpatialBinning DEFAULT = GhostSpatialBinning.ONE;

    private final int value;

    GhostSpatialBinning(final int value) {
        this.value = value;
    }

    @Override
    public String displayValue() {
        return String.valueOf(value);
    }

    @Override
    public String sequenceValue() {
        return displayValue();
    }

    @Override
    public String logValue() {
        return displayValue();
    }

    public int getValue() {
        return value;
    }

    public static GhostSpatialBinning getBinning(String name, GhostSpatialBinning nvalue) {
        return SpTypeUtil.oldValueOf(GhostSpatialBinning.class, name, nvalue);
    }

    public static GhostSpatialBinning getBinning(String name) {
        return getBinning(name, GhostSpatialBinning.DEFAULT);
    }

    public static GhostSpatialBinning getBinningByValue(int value) {
        for (final GhostSpatialBinning constant: GhostSpatialBinning.class.getEnumConstants()) {
            if (constant.getValue() == value)
                return constant;
        }
        return DEFAULT;
    }

    @Override
    public String toString() {
        return displayValue();
    }
}
