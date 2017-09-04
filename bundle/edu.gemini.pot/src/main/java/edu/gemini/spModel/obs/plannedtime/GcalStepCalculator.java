package edu.gemini.spModel.obs.plannedtime;

import edu.gemini.shared.util.immutable.MapOp;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.gemini.calunit.calibration.CalConfigBuilderUtil;
import edu.gemini.spModel.gemini.calunit.calibration.CalDictionary;
import edu.gemini.spModel.obs.plannedtime.PlannedTime.Category;
import edu.gemini.spModel.obs.plannedtime.PlannedTime.CategorizedTime;
import edu.gemini.spModel.obs.plannedtime.PlannedTime.CategorizedTimeGroup;
import edu.gemini.spModel.obs.plannedtime.PlannedTime.StepCalculator;

/**
 * A step calculator used to add overhead caused by taking calibration datasets.
 */
public enum GcalStepCalculator implements StepCalculator {
    instance;

    public static final long SCIENCE_FOLD_MOVE_TIME = 15000;
    public static final long CONFIG_CHANGE_TIME     =  5000;

    private static final CategorizedTime SCIENCE_FOLD_MOVE =
            CategorizedTime.apply(Category.CONFIG_CHANGE, SCIENCE_FOLD_MOVE_TIME, "Science Fold");

    private static final CategorizedTime gcalConfigTime(ItemKey key) {
        String name   = key.getName();
        String detail = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        return CategorizedTime.apply(Category.CONFIG_CHANGE, CONFIG_CHANGE_TIME, "GCAL " + detail);
    }

    private static CategorizedTime scienceFoldMove(Option<Config> prev, boolean isCalStep) {
        // Does the science fold have to move?
        boolean wasCalStep = prev.map(new MapOp<Config, Boolean>() {
            @Override public Boolean apply(Config config) {
                return CalConfigBuilderUtil.isCalStep(config);
            }
        }).getOrElse(false);


        // How much time to add for the science fold move.
        return (isCalStep == wasCalStep) ? Category.CONFIG_CHANGE.ZERO : SCIENCE_FOLD_MOVE;
    }

    private static CategorizedTime otherConfigChange(Config cur, Option<Config> prev, final ItemKey key) {

        final Object curVal = cur.getItemValue(key);
        boolean updated = prev.map(new MapOp<Config, Boolean>() {
            @Override public Boolean apply(Config config) {
                Object prevVal = config.getItemValue(key);
                return prevVal == null ? true : !curVal.equals(prevVal);
            }
        }).getOrElse(true);
        return updated ? gcalConfigTime(key) : Category.CONFIG_CHANGE.ZERO;
    }

    private static final ItemKey[] CONFIG_CHANGE_KEYS = new ItemKey[] {
            CalDictionary.DIFFUSER_ITEM.key,
            CalDictionary.FILTER_ITEM.key,
            CalDictionary.SHUTTER_ITEM.key,
    };

    @Override public CategorizedTimeGroup calc(Config cur, Option<Config> prev) {
        boolean isCalStep = CalConfigBuilderUtil.isCalStep(cur);
        CategorizedTime scienceFold = scienceFoldMove(prev, isCalStep);

        CategorizedTimeGroup res = CategorizedTimeGroup.EMPTY;
        if (scienceFold.time > 0) res = res.add(scienceFold);

        if (isCalStep) {
            for (ItemKey key : CONFIG_CHANGE_KEYS) {
                CategorizedTime ct = otherConfigChange(cur, prev, key);
                if (ct.time > 0) res = res.add(ct);
            }
        }
        return res;
    }
}
