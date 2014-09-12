package edu.gemini.spModel.obs.plannedtime;

import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.obs.plannedtime.PlannedTime.CategorizedTimeGroup;
import edu.gemini.spModel.obs.plannedtime.PlannedTime.StepCalculator;

/**
 * Creates a step containing all the items that are common across most
 * instruments.
 */
public enum CommonStepCalculator implements StepCalculator {
    instance;

    /**
     * Creates a CategorizedTimeGroup with
     * <ul>
     *     <li>offset overhead (if applicable)</li>
     *     <li>calibration overhead (if applicable)</li>
     * </ul>
     *
     * @return CategorizedTimeGroup containing all common items
     */
    @Override public CategorizedTimeGroup calc(Config cur, Option<Config> prev) {
        double secs = OffsetOverheadCalculator.instance.calc(cur, prev);
        CategorizedTimeGroup offset = (secs == 0.0) ? CategorizedTimeGroup.EMPTY :
                CategorizedTimeGroup.apply(OffsetOverheadCalculator.instance.categorize(secs));
        return offset.add(GcalStepCalculator.instance.calc(cur, prev));
    }
}
