package edu.gemini.spModel.gemini.ghost;

import edu.gemini.spModel.type.DisplayableSpType;
import edu.gemini.spModel.type.LoggableSpType;
import edu.gemini.spModel.type.SequenceableSpType;
import edu.gemini.spModel.type.SpTypeUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum GhostSpatialBinning implements DisplayableSpType, LoggableSpType, SequenceableSpType {
    /** We disallow 1x2 spectral / spatial binning. **/
    ONE(1, Arrays.asList(GhostSpectralBinning.ONE, GhostSpectralBinning.TWO)),
    TWO(2, Collections.singletonList(GhostSpectralBinning.ONE)),
    FOUR(4, Arrays.asList(GhostSpectralBinning.ONE, GhostSpectralBinning.TWO)),
    EIGHT(8, Arrays.asList(GhostSpectralBinning.ONE, GhostSpectralBinning.TWO))
    ;

    public static final GhostSpatialBinning DEFAULT = GhostSpatialBinning.ONE;

    private final int value;
    private final List<GhostSpectralBinning> compatibleSpectralBinning;

    GhostSpatialBinning(final int value, final List<GhostSpectralBinning> compatibleSpectralBinning) {
        this.value = value;
        this.compatibleSpectralBinning = Collections.unmodifiableList(compatibleSpectralBinning);
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

    public List<GhostSpectralBinning> getCompatibleSpectralBinning() {
        return compatibleSpectralBinning;
    }

    public static GhostSpectralBinning getBinning(String name, GhostSpectralBinning nvalue) {
        return SpTypeUtil.oldValueOf(GhostSpectralBinning.class, name, nvalue);
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
