package edu.gemini.spModel.guide;

import edu.gemini.skycalc.Angle;
import edu.gemini.skycalc.Coordinates;
import edu.gemini.spModel.gemini.gmos.GmosOiwfsGuideProbe;
import edu.gemini.spModel.gemini.gmos.InstGmosNorth;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.telescope.IssPort;
import org.junit.Test;

import edu.gemini.shared.util.immutable.None;
import edu.gemini.spModel.core.Site;

import java.awt.geom.Rectangle2D;

import static edu.gemini.skycalc.Angle.Unit.ARCSECS;

/**
 * TODO: Why does this file exist?
 */
public class BoundaryValidatorTest {
    @Test
    public void worksWithGmos() {
        final Rectangle2D bounds2D = GmosOiwfsGuideProbe.instance.getPatrolField().getArea().getBounds2D();
        Coordinates coords = new Coordinates(
                new Angle(-bounds2D.getMinX() + bounds2D.getWidth() / 2.0, ARCSECS),
                new Angle(-bounds2D.getMinY() + bounds2D.getHeight() / 2.0, ARCSECS));


        SPTarget base = new SPTarget(0.0, 0.0);
        TargetEnvironment env = TargetEnvironment.create(base);

        InstGmosNorth inst = new InstGmosNorth();
        inst.setPosAngle(0);
        inst.setIssPort(IssPort.UP_LOOKING);

        ObsContext ctxt = ObsContext.create(env, inst, None.<Site>instance(), SPSiteQuality.Conditions.BEST, null, null, None.instance());


        //Assert.notFalse(BoundaryValidator.instance.validate(coords, ctxt, GmosOiwfsGuideProbe.instance), "Expected valid, got 'false' ");
    }

    @Test
    public void worksWithPwfs() {
        final Rectangle2D bounds2D = GmosOiwfsGuideProbe.instance.getPatrolField().getArea().getBounds2D();

        Coordinates coords = new Coordinates(
                new Angle(-bounds2D.getMinX() + bounds2D.getWidth() / 2.0, ARCSECS),
                new Angle(-bounds2D.getMinY() + bounds2D.getHeight() / 2.0, ARCSECS));


        SPTarget base = new SPTarget(0.0, 0.0);
        TargetEnvironment env = TargetEnvironment.create(base);

        InstGmosNorth inst = new InstGmosNorth();
        inst.setPosAngle(0);
        inst.setIssPort(IssPort.UP_LOOKING);

        ObsContext ctxt = ObsContext.create(env, inst, None.<Site>instance(), SPSiteQuality.Conditions.BEST, null, null, None.instance());


       // Assert.notFalse(BoundaryValidator.instance.validate(coords, ctxt, PwfsGuideProbe.pwfs1), "Expected valid, got 'false' ");
    }

    @Test
    public void pwfsFieldOfViewConstraints() {

    }
}
