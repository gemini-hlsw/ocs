//
// $
//

package edu.gemini.spModel.gemini.gsaoi;

import edu.gemini.skycalc.Angle;
import static edu.gemini.skycalc.Angle.Unit.ARCSECS;
import static edu.gemini.skycalc.Angle.Unit.DEGREES;
import edu.gemini.skycalc.Coordinates;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.shared.util.immutable.Option;
import static edu.gemini.spModel.gemini.gsaoi.GsaoiDetectorArray.DETECTOR_GAP_ARCSEC;
import static edu.gemini.spModel.gemini.gsaoi.GsaoiDetectorArray.DETECTOR_SIZE_ARCSEC;

import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.GuideProbeTargets;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.telescope.IssPort;
import junit.framework.TestCase;
import org.junit.Test;

import edu.gemini.shared.util.immutable.None;
import edu.gemini.spModel.core.Site;

import java.util.Set;

/**
 * Test cases for the GsaoiOdgw group.
 */
public class GsaoiOdgwGroupTest extends TestCase {

    private ObsContext baseContext;
    private static final GsaoiOdgw.Group group = GsaoiOdgw.Group.instance;
    private static final double midDetector   = (DETECTOR_GAP_ARCSEC + DETECTOR_SIZE_ARCSEC)/2.0;

    protected void setUp() throws Exception{
        // (OT-8) Account for ODGW hotspot (and convert arcsec value to deg)
        final double d = GsaoiDetectorArray.ODGW_HOTSPOT_OFFSET/3600.;

        final SPTarget base         = new SPTarget(d, d);
        final TargetEnvironment env = TargetEnvironment.create(base);

        final Gsaoi inst = new Gsaoi();
        inst.setPosAngle(0);
        inst.setIssPort(IssPort.SIDE_LOOKING);

        baseContext = ObsContext.create(env, inst, None.<Site>instance(), SPSiteQuality.Conditions.BEST, null, null, None.instance());
    }

    // Selection and adding essentially wrap the GsaoiDetectorArray.getId
    // method with code to map the id to an Odgw or update a TargetEnvironment.
    // Not going to repeat all the GsaoiDetectorArray.getId tests that have
    // to do with calculating which detector array a coordinate falls in.

    @Test
    public void testEmptySelect() {
        // In the gap between detectors for the baseContext.
        final Coordinates coords = new Coordinates(
            new Angle(0, ARCSECS), new Angle(midDetector, ARCSECS)
        );
        assertTrue(group.select(coords, baseContext).isEmpty());
    }

    @Test
    public void testSelect() {
        final Coordinates[] coordsArray = new Coordinates[] {
                new Coordinates(new Angle( midDetector, ARCSECS), new Angle(-midDetector, ARCSECS)), // 1
                new Coordinates(new Angle(-midDetector, ARCSECS), new Angle(-midDetector, ARCSECS)), // 2
                new Coordinates(new Angle(-midDetector, ARCSECS), new Angle( midDetector, ARCSECS)), // 3
                new Coordinates(new Angle( midDetector, ARCSECS), new Angle( midDetector, ARCSECS)), // 4
        };
        for (int i=0; i<4; ++i) {
            final Coordinates coords = coordsArray[i];
            final GsaoiOdgw expected = GsaoiOdgw.values()[i];
            assertEquals(expected, group.select(coords, baseContext).getValue());
        }
    }

    @Test
    public void testEmptyAdd() {
        // In the gap between detectors for the baseContext.
        final Coordinates coords = new Coordinates(
            new Angle(0, ARCSECS), new Angle(midDetector, ARCSECS)
        );

        final SPTarget guideTarget = new SPTarget(coords.getRaDeg(), coords.getDecDeg());
        final TargetEnvironment env = group.add(guideTarget, false, baseContext);

        // Adds an ODGW1 target by default.
        final ImList<GuideProbeTargets> col = env.getOrCreatePrimaryGuideGroup().getAll();
        assertEquals(1, col.size());

        final Option<GuideProbeTargets> gtOpt = env.getPrimaryGuideProbeTargets(GsaoiOdgw.odgw1);
        assertFalse(gtOpt.isEmpty());

        final GuideProbeTargets gt = gtOpt.getValue();
        assertEquals(1, gt.getTargets().size());
        assertEquals(guideTarget, gt.getTargets().head());
    }

    @Test
    public void testAdd() {
        final Coordinates[] coordsArray = new Coordinates[] {
                new Coordinates(new Angle( midDetector, ARCSECS), new Angle(-midDetector, ARCSECS)), // 1
                new Coordinates(new Angle(-midDetector, ARCSECS), new Angle(-midDetector, ARCSECS)), // 2
                new Coordinates(new Angle(-midDetector, ARCSECS), new Angle( midDetector, ARCSECS)), // 3
                new Coordinates(new Angle( midDetector, ARCSECS), new Angle( midDetector, ARCSECS)), // 4
        };
        for (int i=0; i<4; ++i) {
            final Coordinates coords   = coordsArray[i];
            final SPTarget guideTarget = new SPTarget(coords.getRaDeg(), coords.getDecDeg());
            final GsaoiOdgw odgw = GsaoiOdgw.values()[i];

            final TargetEnvironment env = group.add(guideTarget, false, baseContext);

            // Should have just one set of GuideTargets for the new guide star.
            assertEquals(1, env.getOrCreatePrimaryGuideGroup().getAll().size());

            // Should be guide targets for the expected guide window.
            final Option<GuideProbeTargets> gtOpt = env.getPrimaryGuideProbeTargets(odgw);
            assertEquals(1, gtOpt.getValue().getTargets().size());

            // Should be the new target that was added.
            assertEquals(guideTarget, gtOpt.getValue().getTargets().head());
        }
    }

    private GuideProbeTargets create(GsaoiOdgw odgw, Coordinates coords) {
        final SPTarget target = new SPTarget(coords.getRaDeg(), coords.getDecDeg());
        return GuideProbeTargets.create(odgw, target).selectPrimary(target);
    }

    // Simple test case where an ODGW star ends up in another detector due to
    // pos angle rotation.
    @Test
    public void testOptimizeOne() {
        // Setup a target in the detector with id 1.
        final Coordinates coords = new Coordinates(new Angle(midDetector, ARCSECS), new Angle(-midDetector, ARCSECS)); // 1
        final GuideProbeTargets gt = create(GsaoiOdgw.odgw1, coords);
        SPTarget target = gt.getTargets().head();

        final TargetEnvironment env = baseContext.getTargets().putPrimaryGuideProbeTargets(gt);

        // Optimize this context when rotated 90 degrees
        final ObsContext ctx = baseContext.withTargets(env).withPositionAngle(new Angle(90, DEGREES));
        final Option<TargetEnvironment> optEnv = group.optimize(ctx);

        // Should have moved to Id 4.
        final TargetEnvironment newEnv = optEnv.getValue();
        final Set<GuideProbe> guiders = newEnv.getOrCreatePrimaryGuideGroup().getReferencedGuiders();
        assertEquals(1, guiders.size());
        assertEquals(GsaoiOdgw.odgw4, guiders.iterator().next());

        final Option<GuideProbeTargets> gtOpt = newEnv.getPrimaryGuideProbeTargets(GsaoiOdgw.odgw4);
        final GuideProbeTargets gtNew = gtOpt.getValue();
        assertEquals(1, gtNew.getTargets().size());
        assertSame(target, gtNew.getTargets().get(0));
    }

    // Tests that an existing primary guide star is kept when new targets are
    // added to a detector
    @Test
    public void testKeepPrimary() {
        // Create a target in detector 2, just on the border with 3.
        final Coordinates coords2 = new Coordinates(new Angle(-midDetector, ARCSECS), new Angle(-DETECTOR_GAP_ARCSEC, ARCSECS));
        final GuideProbeTargets gt2 = create(GsaoiOdgw.odgw2, coords2);

        // Create a target in detector 3, just on the border with 2.
        final Coordinates coords3 = new Coordinates(new Angle(-midDetector, ARCSECS), new Angle( DETECTOR_GAP_ARCSEC, ARCSECS));
        final GuideProbeTargets gt3 = create(GsaoiOdgw.odgw3, coords3);

        // Set up an obs context with these targets, and optimize when rotated 45 degrees.
        final TargetEnvironment env = baseContext.getTargets().putPrimaryGuideProbeTargets(gt2).putPrimaryGuideProbeTargets(gt3);
        final ObsContext ctx = baseContext.withTargets(env).withPositionAngle(new Angle(45, DEGREES));

        // This will bring the target in detector 4 into detector 1.
        final Option<TargetEnvironment> optEnv = group.optimize(ctx);

        // Should have two targets in detector 2.
        final TargetEnvironment newEnv = optEnv.getValue();
        final Set<GuideProbe> guiders = newEnv.getOrCreatePrimaryGuideGroup().getReferencedGuiders();
        assertEquals(1, guiders.size());
        assertEquals(GsaoiOdgw.odgw2, guiders.iterator().next());

        final Option<GuideProbeTargets> gtOpt = newEnv.getPrimaryGuideProbeTargets(GsaoiOdgw.odgw2);
        final GuideProbeTargets gt = gtOpt.getValue();
        assertEquals(2, gt.getTargets().size());

        // Make sure that the primary star is the primary star from gt2.
        // In other words, it shouldn't change just because a new star was
        // added.
        assertEquals(gt.getPrimary().getValue(), gt2.getPrimary().getValue());
    }

    // Test that when two guide stars from one detector are added to an empty
    // one, the previously primary one keeps its designation as primary.
    @Test
    public void testKeepTransferPrimary() {
        // Put two guide stars in the same detector (detector 2).
        final Coordinates coords2a = new Coordinates(new Angle(midDetector, ARCSECS), new Angle(-DETECTOR_GAP_ARCSEC, ARCSECS));
        final Coordinates coords2b = new Coordinates(new Angle(DETECTOR_GAP_ARCSEC, ARCSECS), new Angle(-midDetector, ARCSECS));
        final SPTarget target2a = new SPTarget(coords2a.getRaDeg(), coords2a.getDecDeg());
        final SPTarget target2b = new SPTarget(coords2b.getRaDeg(), coords2b.getDecDeg());

        final GuideProbeTargets gt = GuideProbeTargets.create(GsaoiOdgw.odgw2, target2a, target2b).selectPrimary(target2b);

        // Setup the obs context, rotated 90 degrees.
        final TargetEnvironment env = baseContext.getTargets().putPrimaryGuideProbeTargets(gt);
        final ObsContext ctx = baseContext.withTargets(env).withPositionAngle(new Angle(90, DEGREES));

        // This will put both targets in detector 1.
        final Option<TargetEnvironment> optEnv = group.optimize(ctx);

        // Should have two targets in detector 4.
        final TargetEnvironment newEnv = optEnv.getValue();
        final Set<GuideProbe> guiders = newEnv.getOrCreatePrimaryGuideGroup().getReferencedGuiders();
        assertEquals(1, guiders.size());
        assertEquals(GsaoiOdgw.odgw4, guiders.iterator().next());

        final Option<GuideProbeTargets> gtOpt1 = newEnv.getPrimaryGuideProbeTargets(GsaoiOdgw.odgw4);
        final GuideProbeTargets gt1 = gtOpt1.getValue();
        assertEquals(2, gt1.getTargets().size());

        // Make sure that the second star is still primary in the new
        // environment.
        final Option<SPTarget> actual = gt1.getPrimary();
        assertFalse(actual.isEmpty());
        assertEquals(target2b, actual.getValue());
    }

    // TODO: GuideProbeTargets.isEnabled

/*
    // Test that the enabled state is maintaned when optimizing.
    @Test
    public void testKeepKeepEnabled() {
        // Setup a target in the detector with id 1.
        Coordinates coords = new Coordinates(new Angle(-midDetector, ARCSECS), new Angle(-midDetector, ARCSECS)); // 1
        GuideProbeTargets gt = create(GsaoiOdgw.odgw1, coords).withEnabled(false);
        SPTarget target = gt.imList().head();

        TargetEnvironment env = baseContext.getTargets().withGuideTargets(gt);
        ObsContext ctx = baseContext.withTargets(env);

        // Now, optimize this context when rotated 90 degrees
        ctx = ctx.withPositionAngle(new Angle(90, DEGREES));
        Option<TargetEnvironment> optEnv = group.optimize(ctx);

        Option<GuideProbeTargets> gtOpt = optEnv.getValue().getGuideTargets(GsaoiOdgw.odgw2);
        assertEquals(false, gtOpt.getValue().isEnabled()); // still disabled?
    }
/
    // Test when no updates are made.
    @Test
    public void testNoUpdates() {
        // Setup a target in the detector with id 1.
        Coordinates coords = new Coordinates(new Angle(-midDetector, ARCSECS), new Angle(-midDetector, ARCSECS)); // 1
        GuideProbeTargets gt = create(GsaoiOdgw.odgw1, coords).withEnabled(false);

        TargetEnvironment env = baseContext.getTargets().withGuideTargets(gt);
        ObsContext ctx = baseContext.withTargets(env);

        // Now, optimize this context when rotated 1 degrees
        ctx = ctx.withPositionAngle(new Angle(1, DEGREES));
        Option<TargetEnvironment> optEnv = group.optimize(ctx);

        assertTrue(optEnv.isEmpty());
    }
*/
}
