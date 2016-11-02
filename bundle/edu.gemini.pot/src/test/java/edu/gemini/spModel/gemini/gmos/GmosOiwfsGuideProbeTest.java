package edu.gemini.spModel.gemini.gmos;

import edu.gemini.skycalc.Angle;
import edu.gemini.skycalc.Coordinates;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.guide.BoundaryPosition;
import edu.gemini.spModel.guide.GuideStarValidation;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.telescope.IssPort;
import junit.framework.TestCase;
import org.junit.Test;

import edu.gemini.shared.util.immutable.None;

import java.awt.geom.Rectangle2D;

import static edu.gemini.skycalc.Angle.Unit.ARCSECS;
import static edu.gemini.skycalc.Angle.Unit.DEGREES;

public class GmosOiwfsGuideProbeTest extends TestCase {

    // test offsetIntersection
        // tests with multiple offsets, in guide, frozen or park state

    //test checkBoundaries
        // test in the 4 different places(inside, outside, innerBoundary and outerBoundary)
        // test in different rotations and base positions
        // test with empty intersection of offsets

    private ObsContext baseContext;
    private TargetEnvironment env;

    @Override
    protected void setUp() throws Exception{
        SPTarget base         = new SPTarget(0.0, 0.0);
        env = TargetEnvironment.create(base);

        InstGmosNorth inst = new InstGmosNorth();
        inst.setPosAngle(0);
        inst.setIssPort(IssPort.UP_LOOKING);

        baseContext = ObsContext.create(env, inst, None.instance(), SPSiteQuality.Conditions.BEST, null, null, None.instance());
    }

    /**
     * Tests validation before and after moving base and/or guide star
     */
    @Test
    public void testMoveBase(){
        final Rectangle2D bounds2D = GmosOiwfsGuideProbe.instance.getPatrolField().getArea().getBounds2D();
        final double fovX = bounds2D.getX();
        final double fovY = bounds2D.getY();
        final double fovWidth = bounds2D.getWidth();
        final double fovHeight = bounds2D.getHeight();

        //mid point of fov
        Coordinates middle = new Coordinates(
                new Angle(-fovX - fovWidth/2.0, ARCSECS),
                new Angle(-fovY - fovHeight/2.0, ARCSECS));

        Coordinates coords = new Coordinates(
                    new Angle(-(fovWidth+5), ARCSECS),
                    new Angle(-(fovHeight+5), ARCSECS));

        ObsContext ctx = baseContext.withTargets(TargetEnvironment.create(new SPTarget(coords.getRaDeg(),coords.getDecDeg())));

        assertFalse("Mid point of fov, after (-width,-height) movement of base.",
                GmosOiwfsGuideProbe.instance.validate(
                        new SPTarget(middle.getRaDeg(), middle.getDecDeg()),
                        ctx) == GuideStarValidation.VALID);

        //mid point of fov moved
        Coordinates middleMoved = new Coordinates(
                new Angle(-fovX - fovWidth/2.0 -(fovWidth+5), ARCSECS),
                new Angle(-fovY - fovHeight/2.0 -(fovHeight+5), ARCSECS));

        assertTrue("Mid point of fov moved by (-width,-height), after (-width,-height) movement of base.",
                GmosOiwfsGuideProbe.instance.validate(
                        new SPTarget(middleMoved.getRaDeg(), middleMoved.getDecDeg()),
                        ctx) == GuideStarValidation.VALID);

    }

    /**
     * Tests validation before and after rotations
     */
    @Test
    public void testRotate(){
        final Rectangle2D bounds2D = GmosOiwfsGuideProbe.instance.getPatrolField().getArea().getBounds2D();
                final double fovX = bounds2D.getX();
                final double fovY = bounds2D.getY();
                final double fovWidth = bounds2D.getWidth();
                final double fovHeight = bounds2D.getHeight();

        //mid point of fov
        Coordinates coords = new Coordinates(
                new Angle(-fovX - fovWidth/2.0, ARCSECS),
                new Angle(-fovY - fovHeight/2.0, ARCSECS));

        assertTrue("Mid point of fov.",
                GmosOiwfsGuideProbe.instance.validate(
                        new SPTarget(coords.getRaDeg(), coords.getDecDeg()),
                        baseContext) == GuideStarValidation.VALID);

        ObsContext ctx = baseContext.withPositionAngleJava(new Angle(90, DEGREES));
        assertFalse("Mid point of fov, after 90 degree rotation of fov.",
                GmosOiwfsGuideProbe.instance.validate(
                        new SPTarget(coords.getRaDeg(), coords.getDecDeg()),
                        ctx) == GuideStarValidation.VALID);

        ctx = baseContext.withPositionAngleJava(new Angle(45, DEGREES));
        assertTrue("Mid point of fov, after 45 degree rotation of fov.",
                GmosOiwfsGuideProbe.instance.validate(
                        new SPTarget(coords.getRaDeg(), coords.getDecDeg()),
                        ctx) == GuideStarValidation.VALID);

    }

    /**
     * Tests validation before and after selecting an IFU FPUnit
     */
    @Test
    public void testIFU() {
        final Rectangle2D bounds2D = GmosOiwfsGuideProbe.instance.getPatrolField().getArea().getBounds2D();
                final double fovX = bounds2D.getX();
                final double fovY = bounds2D.getY();
                final double fovWidth = bounds2D.getWidth();
                final double fovHeight = bounds2D.getHeight();

        //point inside of fov
        Coordinates coords = new Coordinates(
                new Angle(-fovX - fovWidth + 10.0, ARCSECS),
                new Angle(-fovY - fovHeight / 2.0, ARCSECS));

        assertTrue("Point in fov.",
                GmosOiwfsGuideProbe.instance.validate(
                        new SPTarget(coords.getRaDeg(), coords.getDecDeg()),
                        baseContext) == GuideStarValidation.VALID);

        InstGmosNorth ign = (InstGmosNorth) baseContext.getInstrument();
        ign.setFPUnit(GmosNorthType.FPUnitNorth.IFU_2);
        ObsContext    ctx = ObsContext.create(env, ign, None.instance(), SPSiteQuality.Conditions.BEST, null, null, None.instance());

        assertFalse("Point outside of fov, after IFU selected.",
                GmosOiwfsGuideProbe.instance.validate(
                        new SPTarget(coords.getRaDeg(), coords.getDecDeg()),
                        ctx) == GuideStarValidation.VALID);


    }

    /**
     * Gets the corners of a Rectangle2D.Double, shifted by a certain amount.
     * Useful to test boundary positions.
     *
     * @param rect where to get the corners from
     * @param shift distance to be moved. >0 means move to the outside
     */
    private Coordinates[] getCorners(Rectangle2D rect, Double shift){
           return new Coordinates[]{
            new Coordinates(
                    new Angle(-rect.getMinX() - shift, ARCSECS),
                    new Angle(-rect.getMinY() - shift, ARCSECS)),
            new Coordinates(
                    new Angle(-rect.getMinX() - rect.getWidth() + shift, ARCSECS),
                    new Angle(-rect.getMinY() - shift, ARCSECS)),
            new Coordinates(
                    new Angle(-rect.getMinX() - rect.getWidth() + shift, ARCSECS),
                    new Angle(-rect.getMinY() - rect.getHeight() + shift, ARCSECS)),
            new Coordinates(
                    new Angle(-rect.getMinX() - shift, ARCSECS),
                    new Angle(-rect.getMinY() - rect.getHeight() + shift, ARCSECS))
        };
    }

    @Test
    public void testCornersValidate(){
        Coordinates[] corners=getCorners(GmosOiwfsGuideProbe.instance.getPatrolField().getArea().getBounds2D(), 0.0001);
        for(Integer i=0;i<4;i++){
            SPTarget guideTarget = new SPTarget(corners[i].getRaDeg(), corners[i].getDecDeg());
            assertTrue("Corner "+i+" failed.",GmosOiwfsGuideProbe.instance.validate(guideTarget,baseContext) == GuideStarValidation.VALID);
        }
    }

    /**
     * zones:
     * 1
     *  |--------------------------- region boundary + 2 arcsec
     *  |2
     *  | 3
     *  |  |------------------------ region boundary
     *  |  |4
     *  |  | 5
     *  |  |  |--------------------- region boundary - 2 arcsec
     *  |  |  |6
     *  |  |  |
     *  |  |  |
     *
     */
    @Test
    public void testCornersCheckBoundaries(){
        for(int zone=1;zone<7;zone++){
            Coordinates[] corners = getZoneCorners(zone);
            for(Integer corner=0;corner<4;corner++){
                SPTarget guideTarget = new SPTarget(corners[corner].getRaDeg(), corners[corner].getDecDeg());
                assertEquals("Zone "+zone+", Corner "+corner+" failed.",getExpectedPosition(zone),
                        GmosOiwfsGuideProbe.instance.checkBoundaries(guideTarget,baseContext).getValue());
            }
        }
    }

    /**
     * Get the expected position of a zone, used to test if GmosOiwfsGuideProbe.checkBoundaries is working OK.
     *
     * @param zone wich zone to look at (check comments in GmosOiwfsGuideProbeTest.testCornersCheckBoundaries)
     * @return position of the zone
     */
    private BoundaryPosition getExpectedPosition(int zone){
        switch(zone){
            case 1:
                return BoundaryPosition.outside;
            case 2:
            case 3:
                return BoundaryPosition.outerBoundary;
            case 4:
            case 5:
                return BoundaryPosition.innerBoundary;
            case 6:
                return BoundaryPosition.inside;
        }
        return null;//shouldn't happen :S
    }

    /**
     *
     * @param zone wich zone to look at (check comments in GmosOiwfsGuideProbeTest.testCornersCheckBoundaries)
     * @return coordinates of the 4 corner points of the zone
     */
    private Coordinates[] getZoneCorners(int zone){
        Rectangle2D rect=null;
        Double shift = 0.0001;
        switch(zone){
            case 1:
                shift = -0.0001;
            case 2:
                rect=GmosOiwfsGuideProbe.instance.getPatrolField().getOuterLimit().getBounds2D();
                break;
            case 3:
                shift = -0.0001;
            case 4:
                rect=GmosOiwfsGuideProbe.instance.getPatrolField().getArea().getBounds2D();
                break;
            case 5:
                shift = -0.0001;
            case 6:
                rect=GmosOiwfsGuideProbe.instance.getPatrolField().getSafe().getBounds2D();
                break;
        }
        return getCorners(rect, shift);

    }


}
