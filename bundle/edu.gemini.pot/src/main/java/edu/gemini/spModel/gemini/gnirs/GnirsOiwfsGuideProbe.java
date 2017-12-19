package edu.gemini.spModel.gemini.gnirs;

import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.spModel.core.BandsList;
import edu.gemini.spModel.core.MagnitudeBand;
import edu.gemini.spModel.core.SingleBand;
import edu.gemini.spModel.guide.*;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.telescope.IssPort;

import java.awt.geom.*;

/**
 * GNIRS on-instrument guide probe.
 */
public enum GnirsOiwfsGuideProbe implements ValidatableGuideProbe {
    instance;

    private static final PatrolField patrolField;
    static{

        // The FOV is 3 arcminutes in diameter, with an inner boundary that
        // is a "keyhole" pattern: 100 arcsecs long by 10 arcsecs high
        // with a 15 arcsec semi-circle in the center.

        // ------------------ the actual probe range
        double d = 3 * 60;
        Ellipse2D.Double area = new Ellipse2D.Double(- d / 2, - d / 2, d, d);

        // ------------------ blocked area (keyhole)
        // rectangle 100 times 10 arcseconds
        double h = 100;
        double w =  10;
        Rectangle2D.Double rect = new Rectangle2D.Double(- w / 2, - h / 2, w, h);

        // 5 arcsec semi-circle in the center
        double r = 15;
        Arc2D.Double arc = new Arc2D.Double();
        arc.setArcByCenter(0.0, 0.0, r, 90.0, 180, Arc2D.OPEN);

        // combine rectangle and semi-circle
        Area blocked = new Area(rect);
        blocked.add(new Area(arc));

        patrolField = new PatrolField(area, blocked);
    }

    public String getKey() {
        return "GNIRS OIWFS";
    }

    public String toString() {
        return getKey();
    }

    public Type getType() {
        return Type.OIWFS;
    }

    public String getDisplayName() {
        return "GNIRS On-instrument WFS";
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

    @Override public PatrolField getPatrolField() {
        return patrolField;
    }

    @Override public Option<PatrolField> getCorrectedPatrolField(ObsContext ctx) {
        if (ctx.getInstrument() instanceof InstGNIRS) {
            // flip X if altair is used (AO component installed) AND flip X again if side looking port; no flipRA
            double flipX = 1.0;
            flipX *= (ctx.getIssPort() == IssPort.SIDE_LOOKING) ? -1. : 1.;
    //        flipX *= (!ctx.getAOComponent().isEmpty()) ? -1. : 1.;
            final AffineTransform correction = AffineTransform.getScaleInstance(flipX, 1.0);
            return new Some<>(patrolField.getTransformed(correction));
        } else {
            return None.instance();
        }
    }

    @Override
    public GuideStarValidation validate(SPTarget guideStar, ObsContext ctx) {
        return GuideProbeUtil.instance.validate(guideStar, this, ctx);
    }

    @Override
    public BandsList getBands() { return SingleBand.apply(MagnitudeBand.K$.MODULE$); }
}
