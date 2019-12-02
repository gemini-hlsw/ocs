package edu.gemini.spModel.gemini.ghost;

import edu.gemini.spModel.type.DisplayableSpType;
import edu.gemini.spModel.type.LoggableSpType;
import edu.gemini.spModel.type.SequenceableSpType;
import edu.gemini.spModel.type.SpTypeUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum GhostSpectralBinning implements DisplayableSpType, LoggableSpType, SequenceableSpType {
    ONE(1, Arrays.asList(GhostSpatialBinning.ONE, GhostSpatialBinning.TWO, GhostSpatialBinning.FOUR, GhostSpatialBinning.EIGHT)),
    TWO(2, Arrays.asList(GhostSpatialBinning.TWO, GhostSpatialBinning.FOUR, GhostSpatialBinning.EIGHT));

    public static final GhostSpectralBinning DEFAULT = GhostSpectralBinning.ONE;

    private final int value;
    private final List<GhostSpatialBinning> compatibleSpatialBinning;

    GhostSpectralBinning(final int value, final List<GhostSpatialBinning> compatibleSpatialBinning) {
        this.value = value;
        this.compatibleSpatialBinning = Collections.unmodifiableList(compatibleSpatialBinning);
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

    public List<GhostSpatialBinning> getCompatibleSpatialBinning() {
        return compatibleSpatialBinning;
    }

    public static GhostSpectralBinning getBinning(String name, GhostSpectralBinning nvalue) {
        return SpTypeUtil.oldValueOf(GhostSpectralBinning.class, name, nvalue);
    }
}
