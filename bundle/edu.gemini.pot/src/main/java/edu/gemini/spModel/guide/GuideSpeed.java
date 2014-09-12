package edu.gemini.spModel.guide;

import edu.gemini.shared.skyobject.Magnitude;
import edu.gemini.shared.util.immutable.MapOp;

public enum GuideSpeed {
    FAST(0.00),
    MEDIUM(0.75),
    SLOW(1.50)
    ;

    private final double adjustment;

    private GuideSpeed(double adjustment) {
        this.adjustment = adjustment;
    }

    public MapOp<Magnitude, Magnitude> magAdjustOp() {
        return new MapOp<Magnitude, Magnitude>() {
            @Override public Magnitude apply(Magnitude magnitude) {
                return magnitude.add(adjustment);
            }
        };
    }
}
