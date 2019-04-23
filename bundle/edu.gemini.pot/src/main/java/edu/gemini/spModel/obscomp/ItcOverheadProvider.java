package edu.gemini.spModel.obscomp;

import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.obs.plannedtime.PlannedTime.CategorizedTimeGroup;
import edu.gemini.spModel.obscomp.ItcOverheadProvider;

import java.time.Duration;

// Moved from edu.gemini.spModel.obs.plannedtime.PlannedTime

/**
 * A parallel setup time calculation interface used by the ITC.
 */
@Deprecated
public interface ItcOverheadProvider {
    Duration getSetupTime(Config conf);
    Duration getReacquisitionTime(Config conf);
    CategorizedTimeGroup calc(Config stepConfig, Option<Config> prevStepConfig);
}
