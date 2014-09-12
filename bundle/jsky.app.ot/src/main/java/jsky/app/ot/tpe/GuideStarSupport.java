package jsky.app.ot.tpe;

import edu.gemini.ags.api.AgsRegistrar;
import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.gemini.gpi.Gpi;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.system.NonSiderealTarget;

/**
 * Support methods for the tpe guide star dialogs
 */
public class GuideStarSupport {
    public static boolean supportsAutoGuideStarSelection(ISPNode node) {
        return supportsAutoGuideStarSelection(TpeContext.apply(node));
    }

    static boolean supportsAutoGuideStarSelection(TpeContext ctx) {
        if (ctx.isEmpty() || ctx.instrument().isEmpty()) return false;

        //Special handling for GEMS
        if (hasGemsComponent(ctx)) return true;

        if (!ctx.instrument().isDefined()) return false;

        ObsContext obsCtx = ctx.obsContextJava().getOrNull();
        if (obsCtx == null) return false;

        return AgsRegistrar.defaultStrategy(obsCtx).isDefined();
    }

    // Returns true if the instrument supports manual guide star selection
    public static boolean supportsManualGuideStarSelection(ISPNode node) {
        return supportsManualGuideStarSelection(TpeContext.apply(node));
    }

    static boolean supportsManualGuideStarSelection(TpeContext ctx) {
        return !ctx.instrument().is(Gpi.SP_TYPE);
    }

    // Returns true if the instrument is an instance of GuideStarByPWFSOnSidereal
    public static boolean isSiderealGmos(ISPNode node) {
        return isSiderealGmos(TpeContext.apply(node));
    }

    static boolean isSiderealGmos(TpeContext ctx) {
        if (ctx.instrument().is(SPComponentType.INSTRUMENT_GMOS) ||
            ctx.instrument().is(SPComponentType.INSTRUMENT_GMOSSOUTH)) {

            SPTarget base = ctx.targets().baseOrNull();
            return (base != null) && !(base.getTarget() instanceof NonSiderealTarget);
        }
        return false;
    }

    // Returns true if the observation has a gems component
    public static boolean hasGemsComponent(ISPNode node) {
        return hasGemsComponent(TpeContext.apply(node));
    }

    static boolean hasGemsComponent(TpeContext ctx) {
        return ctx.gems().isDefined();
    }

}
