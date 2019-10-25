package edu.gemini.spModel.gemini.gsaoi;

import edu.gemini.skycalc.Angle;
import static edu.gemini.skycalc.Angle.Unit.ARCSECS;
import edu.gemini.skycalc.Coordinates;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.shared.util.immutable.Option;
import static edu.gemini.spModel.gemini.gsaoi.GsaoiDetectorArray.DETECTOR_GAP_ARCSEC;
import static edu.gemini.spModel.gemini.gsaoi.GsaoiDetectorArray.DETECTOR_SIZE_ARCSEC;

import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.GuideProbeTargets;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.telescope.IssPort;
import junit.framework.TestCase;
import org.junit.Test;

import edu.gemini.shared.util.immutable.None;
import edu.gemini.spModel.core.Site;

/**
 * Test cases for the GsaoiOdgw group.
 */
public class GsaoiOdgwGroupTest extends TestCase {

    private ObsContext baseContext;
    private static final GsaoiOdgw.Group group = GsaoiOdgw.Group.instance;
    private static final double midDetector   = (DETECTOR_GAP_ARCSEC + DETECTOR_SIZE_ARCSEC)/2.0;

    protected void setUp() throws Exception{
        // (OT-8) Account for ODGW hotspot (and convert arcsec value to deg)
        final double p = GsaoiDetectorArray.ODGW_HOTSPOT_OFFSET_P/3600.;
        final double q = GsaoiDetectorArray.ODGW_HOTSPOT_OFFSET_Q/3600.;

        final SPTarget base         = new SPTarget(p, q);
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
        final TargetEnvironment env = group.add(guideTarget, baseContext);

        // Adds an ODGW1 target by default.
        final ImList<GuideProbeTargets> col = env.getPrimaryGuideGroup().getAll();
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

            final TargetEnvironment env = group.add(guideTarget, baseContext);

            // Should have just one set of GuideTargets for the new guide star.
            assertEquals(1, env.getPrimaryGuideGroup().getAll().size());

            // Should be guide targets for the expected guide window.
            final Option<GuideProbeTargets> gtOpt = env.getPrimaryGuideProbeTargets(odgw);
            assertEquals(1, gtOpt.getValue().getTargets().size());

            // Should be the new target that was added.
            assertEquals(guideTarget, gtOpt.getValue().getTargets().head());
        }
    }


}
