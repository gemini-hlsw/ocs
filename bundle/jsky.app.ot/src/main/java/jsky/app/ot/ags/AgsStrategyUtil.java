package jsky.app.ot.ags;

import edu.gemini.ags.api.AgsStrategy;
import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.spModel.ags.AgsStrategyKey;
import edu.gemini.spModel.obs.SPObservation;

public final class AgsStrategyUtil {
    private AgsStrategyUtil() {}

    /**
     * Sets the observation's selected AgsStrategy.
     * @param selectedStrategy  if defined, the strategy to use for this
     *                          observation; if None use the default strategy
     */
    public static void setSelection(final ISPObservation obs, final Option<AgsStrategy> selectedStrategy) {
        if (obs != null) {
            final Option<AgsStrategyKey> selectedStrategyKey = strategyToKey(selectedStrategy);

            // The data object does this check too but we have no way of knowing
            // whether the data object ends up assigning a new strategy or not
            // without explicitly checking. We want to avoid the data object
            // update unless necessary.
            final SPObservation spObs = (SPObservation) obs.getDataObject();
            if (!spObs.getAgsStrategyOverride().equals(selectedStrategyKey)) {
                spObs.setAgsStrategyOverride(selectedStrategyKey);
                obs.setDataObject(spObs);
            }
        }
    }

    public static Option<AgsStrategyKey> strategyToKey(Option<AgsStrategy> so) {
        return so.map(AgsStrategy::key);
    }

    static <T> Option<T> toJavaOption(scala.Option<T> optT) {
        return optT.isDefined() ? new Some<>(optT.get()) : None.<T>instance();
    }
}
