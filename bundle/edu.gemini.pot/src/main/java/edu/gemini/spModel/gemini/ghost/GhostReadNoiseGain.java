package edu.gemini.spModel.gemini.ghost;

import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.gemini.flamingos2.Flamingos2;
import edu.gemini.spModel.type.SpTypeUtil;
import edu.gemini.spModel.type.StandardSpType;

import static edu.gemini.spModel.seqcomp.SeqConfigNames.INSTRUMENT_KEY;

public enum GhostReadNoiseGain implements StandardSpType {
    SLOW_LOW("Slow / Low", "Slow Read / Low Gain: Standard Science Mode"),
    FAST_LOW("Fast / Low", "Fast Read / Low Gain: Rapid Readout"),
    FAST_HIGH("Fast / High", "Fast Read / High Gain: Bright Target Spectroscopy")
    ;

    private final String displayValue;
    private final String description;

    GhostReadNoiseGain(final String displayValue, final String description) {
        this.displayValue = displayValue;
        this.description = description;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public String displayValue() {
        return displayValue;
    }

    @Override
    public String logValue() {
        return displayValue;
    }

    @Override
    public String sequenceValue() {
        return displayValue;
    }

    @Override
    public String toString() {
        return description;
    }

    public static final GhostReadNoiseGain DEFAULT = SLOW_LOW;

    public static Option<GhostReadNoiseGain> valueOf(String tag, Option<GhostReadNoiseGain> def) {
        return SpTypeUtil.optionValueOf(GhostReadNoiseGain.class, tag).orElse(def);
    }

    public static Option<GhostReadNoiseGain> byName(String name) {
        for (final GhostReadNoiseGain m: values()) {
            if (m.displayValue().equals(name)) {
                return new Some<>(m);
            }
        }
        return None.instance();
    }
}
