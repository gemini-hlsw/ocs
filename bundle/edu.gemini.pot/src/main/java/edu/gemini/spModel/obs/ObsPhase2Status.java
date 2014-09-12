package edu.gemini.spModel.obs;

import edu.gemini.spModel.type.DescribableSpType;
import edu.gemini.spModel.type.DisplayableSpType;

/**
 * Phase 2 status of an observation.
 */
public enum ObsPhase2Status implements DisplayableSpType, DescribableSpType {
    PI_TO_COMPLETE("Phase 2", "PI to complete"),
    NGO_TO_REVIEW("For Review", "Ready for review NGO staff"),
    NGO_IN_REVIEW("In Review", "Under review by NGO staff"),
    GEMINI_TO_ACTIVATE("For Activation", "Checked by NGO staff and ready for final verification by Gemini staff"),
    ON_HOLD("On Hold", "Target of opportunity observations, awaiting target definition"),
    PHASE_2_COMPLETE("Prepared", "Phase 2 process is complete."),
    INACTIVE("Inactive", "Observations that should not be done, but do not want to delete from the programs"),
    ;

    private final String displayValue;
    private final String description;

    private ObsPhase2Status(String dv, String desc) {
        this.displayValue = dv;
        this.description  = desc;
    }

    public String displayValue() { return displayValue; }
    public String description()  { return description;  }
    @Override public String toString() { return displayValue; }
}
