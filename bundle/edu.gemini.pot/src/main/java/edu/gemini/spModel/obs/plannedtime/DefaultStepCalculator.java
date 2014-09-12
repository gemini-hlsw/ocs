package edu.gemini.spModel.obs.plannedtime;

import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.obs.plannedtime.PlannedTime.*;

/**
 * A {@link StepCalculator} that can be used for instruments w/o a more
 * precise definition of how to account for time.  It includes shared common
 * items offered by {@link CommonStepCalculator} and a simple estimate for
 * exposure time based upon exposure time multiplied by coadds.
 */
public enum DefaultStepCalculator implements StepCalculator {
    instance;

    /**
     * Adds a simple exposure time estimate to the common items shared by
     * all instruments.
     * @return CategorizedTimeGroup with the common items for all instruments
     * and a simple exposure time estimate
     */
    @Override public CategorizedTimeGroup calc(Config cur, Option<Config> prev) {
        CategorizedTimeGroup ctg = CommonStepCalculator.instance.calc(cur, prev);
        return ctg.add(ExposureCalculator.instance.totalExposureTime(cur));
    }
}
