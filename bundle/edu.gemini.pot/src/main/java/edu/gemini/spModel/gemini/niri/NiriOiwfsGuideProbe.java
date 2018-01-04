package edu.gemini.spModel.gemini.niri;

import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.spModel.core.BandsList;
import edu.gemini.spModel.core.MagnitudeBand;
import edu.gemini.spModel.core.SingleBand;
import edu.gemini.spModel.guide.*;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.target.SPTarget;

import java.awt.geom.Ellipse2D;

/**
 * NIRI on-instrument guide probe.
 */
public enum NiriOiwfsGuideProbe implements ValidatableGuideProbe {
    instance;

    public String getKey() {
        return "NIRI OIWFS";
    }

    public String toString() {
        return getKey();
    }

    public Type getType() {
        return Type.OIWFS;
    }

    public String getDisplayName() {
        return "NIRI On-instrument WFS";
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

    // Unobscured NIRI OIWFS patrol field area (circle with a radius of 105 arcseconds)
    final private static PatrolField patrolField = new PatrolField(new Ellipse2D.Double(-105, -105, 210, 210));

    @Override public PatrolField getPatrolField() {
        return patrolField;
    }

    @Override
    public Option<PatrolField> getCorrectedPatrolField(ObsContext ctx) {
        return (ctx.getInstrument() instanceof InstNIRI) ? new Some<>(patrolField) : None.instance();
    }

    @Override
    public GuideStarValidation validate(SPTarget guideStar, ObsContext ctx) {
        return GuideProbeUtil.instance.validate(guideStar, this, ctx);
    }

    public BandsList getBands() { return SingleBand.apply(MagnitudeBand.K$.MODULE$); }
}
