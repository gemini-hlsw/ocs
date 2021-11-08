package edu.gemini.spModel.obsrecord;

import edu.gemini.spModel.type.DescribableSpType;
import edu.gemini.spModel.type.DisplayableSpType;

/**
 *
 */
public enum ObsExecStatus implements DisplayableSpType, DescribableSpType {
    PENDING("Ready", "This observation has not been started"),
    ONGOING("Ongoing", "This observation has been partially executed"),
    OBSERVED("Observed", "This observation has been executed.")
    ;

    private final String displayValue;
    private final String description;

    private ObsExecStatus(String dv, String desc) {
        this.displayValue = dv;
        this.description  = desc;
    }

    public String displayValue() { return displayValue; }
    public String description()  { return description;  }
    @Override public String toString() { return displayValue; }
}
