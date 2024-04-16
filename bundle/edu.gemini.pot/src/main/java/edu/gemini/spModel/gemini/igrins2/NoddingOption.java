package edu.gemini.spModel.gemini.igrins2;

import edu.gemini.spModel.type.DisplayableSpType;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;

public enum NoddingOption implements DisplayableSpType {

    KEEP_TARGET_IN_SLIT("Keep target in slit"),
    NOD_TO_SKY("Nod to sky");

    private final String displayValue;

    NoddingOption(String displayValue) {
        this.displayValue = displayValue;
    }

    public String displayValue() {
        return displayValue;
    }

    public static Option<NoddingOption> fromDisplayValue(String value) {
        for (NoddingOption o : NoddingOption.values()) {
            if (o.displayValue.equals(value)) return new Some<NoddingOption>(o);
        }
        return None.<NoddingOption>instance();
    }
}
