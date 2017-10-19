//
// $
//

package edu.gemini.spModel.gemini.nici;

import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.spModel.core.BandsList;
import edu.gemini.spModel.core.NoBands;
import edu.gemini.spModel.guide.*;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.target.SPTarget;


import java.awt.geom.Rectangle2D;

/**
 * NICI on-instrument guide probe.
 */
public enum NiciOiwfsGuideProbe implements ValidatableGuideProbe {
    instance;

    public String getKey() {
        return "NICI OIWFS";
    }

    public String toString() {
        return getKey();
    }

    public Type getType() {
        return Type.OIWFS;
    }

    public String getDisplayName() {
        return "NICI On-instrument WFS";
    }

    public String getSequenceProp() {
        return "guideWithOIWFS";
    }

    public GuideOptions getGuideOptions() {
        return StandardGuideOptions.instance;
    }

    public Option<GuideProbeGroup> getGroup() {
        return None.instance();
    }

    // UX-800: NICI does not have a guider and therefore no actual patrol field, nevertheless we need an area that
    // will be used to look for guide star candidates for the automated guide star search. Since in most cases for
    // NICI the science target will also be the guide star we return the science area as the patrol field which
    // defines the area where ags looks for candidates.
    final private static PatrolField patrolField = new PatrolField(new Rectangle2D.Double(-9.,-9.,18.,18.));
    @Override public PatrolField getPatrolField() {
        return patrolField;
    }

    @Override public Option<PatrolField> getCorrectedPatrolField(ObsContext ctx) {
        return (ctx.getInstrument() instanceof InstNICI) ? new Some<>(getPatrolField()) : None.<PatrolField>instance();
    }
    @Override
    public GuideStarValidation validate(SPTarget guideStar, ObsContext ctx) {
        return GuideProbeUtil.instance.validate(guideStar, this, ctx);
    }

    @Override
    public BandsList getBands() { return NoBands.instance(); }
}