package edu.gemini.spModel.gemini.ghost;

import edu.gemini.spModel.type.DisplayableSpType;
import edu.gemini.spModel.type.LoggableSpType;
import edu.gemini.spModel.type.SequenceableSpType;

public enum GhostBinning implements DisplayableSpType, LoggableSpType, SequenceableSpType {
    ONE_BY_ONE(1,1),
    ONE_BY_TWO(1,2),
    ONE_BY_FOUR(1, 4),
    ONE_BY_EIGHT(1, 8),
    TWO_BY_TWO(2,2),
    TWO_BY_FOUR(2,4),
    TWO_BY_EIGHT(2,8),
    FOUR_BY_FOUR(4,4)
    ;

    public static final GhostBinning DEFAULT = GhostBinning.ONE_BY_ONE;

    private final int spectralBinning;
    private final int spatialBinning;

    GhostBinning(int spectralBinning, int spatialBinning) {
        this.spectralBinning = spectralBinning;
        this.spatialBinning = spatialBinning;
    }

    @Override
    public String displayValue() {
        return String.format("%d x %d", spectralBinning, spatialBinning);
    }

    @Override
    public String sequenceValue() {
        return displayValue();
    }

    @Override
    public String logValue() {
        return displayValue();
    }

    @Override
    public String toString() {
        return displayValue();
    }

    public int getSpectralBinning() {
        return spectralBinning;
    }

    public int getSpatialBinning() {
        return spatialBinning;
    }
}
