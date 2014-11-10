package jsky.app.ot.ags;

import edu.gemini.ags.api.AgsRegistrar;
import edu.gemini.ags.api.AgsStrategy;
import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.obs.context.ObsContext;
import static jsky.app.ot.ags.AgsStrategyUtil.toJavaOption;

import java.util.Comparator;

/**
 * AGS strategy options for an observation, summarized in one convenient place.
 */
public final class AgsContext {
    public static final AgsContext EMPTY = new AgsContext();

    private static final Comparator<AgsStrategy> DISPLAY_NAME_COMPARATOR = new Comparator<AgsStrategy>() {
        @Override public int compare(AgsStrategy as1, AgsStrategy as2) {
            return as1.key().displayName().compareTo(as2.key().displayName());
        }
    };

    public static AgsContext create(final ISPObservation obs) {
        return create(ImOption.apply(obs).flatMap(new MapOp<ISPObservation, Option<ObsContext>>() {
            @Override public Option<ObsContext> apply(ISPObservation o) {
                return ObsContext.create(o);
            }
        }));
    }

    public static AgsContext create(final Option<ObsContext> ctxOpt) {
        return ctxOpt.map(new MapOp<ObsContext, AgsContext>() {
            @Override public AgsContext apply(ObsContext ctx) {
                return create(ctx);
            }
        }).getOrElse(EMPTY);
    }

    public static AgsContext create(final ObsContext ctx) {
        return new AgsContext(
            toJavaOption(AgsRegistrar.defaultStrategy(ctx)),
            DefaultImList.create(AgsRegistrar.validStrategiesAsJava(ctx)).sort(DISPLAY_NAME_COMPARATOR),
            toJavaOption(AgsRegistrar.strategyOverride(ctx)),
            toJavaOption(AgsRegistrar.currentStrategy(ctx))
        );
    }

    public final Option<AgsStrategy> defaultStrategy;
    public final ImList<AgsStrategy> validStrategies;
    public final Option<AgsStrategy> strategyOverride;
    public final Option<AgsStrategy> currentStrategy;

    private AgsContext() {
        this.defaultStrategy  = None.instance();
        this.validStrategies  = ImCollections.emptyList();
        this.strategyOverride = None.instance();
        this.currentStrategy  = None.instance();
    }

    private AgsContext(Option<AgsStrategy> defaultStrategy,
                       ImList<AgsStrategy> validStrategies,
                       Option<AgsStrategy> strategyOverride,
                       Option<AgsStrategy> currentStrategy) {
        this.defaultStrategy  = defaultStrategy;
        this.validStrategies  = validStrategies;
        this.strategyOverride = strategyOverride;
        this.currentStrategy  = currentStrategy;
    }

    public boolean nonEmpty() {
        return currentStrategy.isDefined();
    }

    public boolean usingDefault() {
        return defaultStrategy.equals(currentStrategy) &&
               !strategyOverride.equals(currentStrategy);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final AgsContext that = (AgsContext) o;
        if (!defaultStrategy.equals(that.defaultStrategy)) return false;
        if (!strategyOverride.equals(that.strategyOverride)) return false;
        if (!currentStrategy.equals(that.currentStrategy)) return false;
        return validStrategies.equals(that.validStrategies);
    }

    @Override
    public int hashCode() {
        int result = defaultStrategy.hashCode();
        result = 31 * result + validStrategies.hashCode();
        result = 31 * result + strategyOverride.hashCode();
        result = 31 * result + currentStrategy.hashCode();
        return result;
    }
}
