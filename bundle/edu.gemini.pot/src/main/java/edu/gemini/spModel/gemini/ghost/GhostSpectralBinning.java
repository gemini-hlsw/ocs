package edu.gemini.spModel.gemini.ghost;

import edu.gemini.spModel.type.DisplayableSpType;
import edu.gemini.spModel.type.LoggableSpType;
import edu.gemini.spModel.type.SequenceableSpType;
import edu.gemini.spModel.type.SpTypeUtil;

public enum GhostSpectralBinning implements DisplayableSpType, LoggableSpType, SequenceableSpType {
    ONE(1),
    TWO(2)
    ;

    public static final GhostSpectralBinning DEFAULT = GhostSpectralBinning.ONE;

    private final int value;

    GhostSpectralBinning(final int value) {
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

    public static GhostSpectralBinning getBinning(String name, GhostSpectralBinning nvalue) {
        return SpTypeUtil.oldValueOf(GhostSpectralBinning.class, name, nvalue);
    }

    public static GhostSpectralBinning getBinning(String name) {
        return getBinning(name, GhostSpectralBinning.DEFAULT);
    }

    public static GhostSpectralBinning getBinningByValue(int value) {
        for (final GhostSpectralBinning constant: GhostSpectralBinning.class.getEnumConstants()) {
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
