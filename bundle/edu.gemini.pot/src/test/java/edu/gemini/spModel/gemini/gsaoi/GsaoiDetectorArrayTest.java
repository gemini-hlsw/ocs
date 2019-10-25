//
// $
//

package edu.gemini.spModel.gemini.gsaoi;

import edu.gemini.skycalc.Angle;
import static edu.gemini.skycalc.Angle.Unit.ARCSECS;
import static edu.gemini.skycalc.Angle.Unit.DEGREES;
import edu.gemini.skycalc.Coordinates;
import edu.gemini.shared.util.immutable.Option;
import static edu.gemini.spModel.gemini.gsaoi.GsaoiDetectorArray.DETECTOR_GAP_ARCSEC;
import static edu.gemini.spModel.gemini.gsaoi.GsaoiDetectorArray.DETECTOR_SIZE_ARCSEC;

import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.telescope.IssPort;
import junit.framework.TestCase;
import org.junit.Test;

import edu.gemini.shared.util.immutable.None;
import edu.gemini.spModel.core.Site;

/**
 * Test cases for the {@link GsaoiDetectorArray}.
 */
public class GsaoiDetectorArrayTest extends TestCase {

    private ObsContext baseContext;
    private static final double midDetector   = (DETECTOR_GAP_ARCSEC + DETECTOR_SIZE_ARCSEC)/2.0;
    private static final double detectorLimit = DETECTOR_GAP_ARCSEC/2.0 + DETECTOR_SIZE_ARCSEC;

    protected void setUp() throws Exception{
        // (OT-8) Account for ODGW hotspot (and convert arcsec value to deg)
        final double p = GsaoiDetectorArray.ODGW_HOTSPOT_OFFSET_P/3600.;
        final double q = GsaoiDetectorArray.ODGW_HOTSPOT_OFFSET_Q/3600.;

        SPTarget base         = new SPTarget(p, q);
        TargetEnvironment env = TargetEnvironment.create(base);

        Gsaoi inst = new Gsaoi();
        inst.setPosAngle(0);
        inst.setIssPort(IssPort.SIDE_LOOKING);

        baseContext = ObsContext.create(env, inst, None.instance(), SPSiteQuality.Conditions.BEST, null, null, None.instance());
    }

    private void assertEmpty(Coordinates[] coordsArray) {
        Option<GsaoiDetectorArray.Id> idOpt;
        for (Coordinates coords : coordsArray) {
            idOpt = GsaoiDetectorArray.instance.getId(coords, baseContext);
            assertTrue(idOpt.isEmpty());
        }
    }

    private void assertOrdered(Coordinates[] coordsArray) {
        assertOrdered(coordsArray, baseContext);
    }

    private void assertOrdered(Coordinates[] coordsArray, ObsContext ctx) {
        int index = 1;
        Option<GsaoiDetectorArray.Id> idOpt;
        for (Coordinates coords : coordsArray) {
            idOpt = GsaoiDetectorArray.instance.getId(coords, ctx);
            assertEquals(idOpt.getValue().index(), index++);
        }
    }

    @Test
    public void testGaps() throws Exception {
        Coordinates[] coordsArray = new Coordinates[] {
                new Coordinates(new Angle(0, ARCSECS), new Angle(midDetector, ARCSECS)),
                new Coordinates(new Angle(0, ARCSECS), new Angle(-midDetector, ARCSECS)),
                new Coordinates(new Angle(midDetector, ARCSECS), new Angle(0, ARCSECS)),
                new Coordinates(new Angle(-midDetector, ARCSECS), new Angle(0, ARCSECS)),
        };
        assertEmpty(coordsArray);
    }


    @Test
    public void testCenters() throws Exception {
        Coordinates[] coordsArray = new Coordinates[] {
                new Coordinates(new Angle( midDetector, ARCSECS), new Angle(-midDetector, ARCSECS)), // 1
                new Coordinates(new Angle(-midDetector, ARCSECS), new Angle(-midDetector, ARCSECS)), // 2
                new Coordinates(new Angle(-midDetector, ARCSECS), new Angle( midDetector, ARCSECS)), // 3
                new Coordinates(new Angle( midDetector, ARCSECS), new Angle( midDetector, ARCSECS)), // 4
        };
        assertOrdered(coordsArray);
    }

    @Test
    public void testInsideFarCorners() throws Exception {
        final double limit = detectorLimit - 0.000001; // just inside
        Coordinates[] coordsArray = new Coordinates[] {
                new Coordinates(new Angle( limit, ARCSECS), new Angle(-limit, ARCSECS)), // 1
                new Coordinates(new Angle(-limit, ARCSECS), new Angle(-limit, ARCSECS)), // 2
                new Coordinates(new Angle(-limit, ARCSECS), new Angle( limit, ARCSECS)), // 3
                new Coordinates(new Angle( limit, ARCSECS), new Angle( limit, ARCSECS)), // 4
        };
        assertOrdered(coordsArray);
    }

    @Test
    public void testOnCorners() throws Exception {
        Coordinates[] coordsArray = new Coordinates[] {
                new Coordinates(new Angle(-detectorLimit, ARCSECS), new Angle(-detectorLimit, ARCSECS)),
                new Coordinates(new Angle( detectorLimit, ARCSECS), new Angle(-detectorLimit, ARCSECS)),
                new Coordinates(new Angle( detectorLimit, ARCSECS), new Angle( detectorLimit, ARCSECS)),
                new Coordinates(new Angle(-detectorLimit, ARCSECS), new Angle( detectorLimit, ARCSECS)),
        };
        assertEmpty(coordsArray);
    }

    @Test
    public void testRotation() throws Exception {
        Coordinates[] coordsArray = new Coordinates[] {
                new Coordinates(new Angle(-midDetector, ARCSECS), new Angle(-midDetector, ARCSECS)), // 1
                new Coordinates(new Angle(-midDetector, ARCSECS), new Angle( midDetector, ARCSECS)), // 2
                new Coordinates(new Angle( midDetector, ARCSECS), new Angle( midDetector, ARCSECS)), // 3
                new Coordinates(new Angle( midDetector, ARCSECS), new Angle(-midDetector, ARCSECS)), // 4
        };
        assertOrdered(coordsArray, baseContext.withPositionAngleJava(new Angle(90, DEGREES)));

        // These fall in the gaps when the pos angle is 0, but should be in the
        // detector when rotated 45 degrees.
        coordsArray = new Coordinates[] {
                new Coordinates(new Angle(0, ARCSECS),            new Angle(-midDetector, ARCSECS)), // 1
                new Coordinates(new Angle(-midDetector, ARCSECS), new Angle(0, ARCSECS)),            // 2
                new Coordinates(new Angle(0, ARCSECS),            new Angle( midDetector, ARCSECS)), // 3
                new Coordinates(new Angle( midDetector, ARCSECS), new Angle(0, ARCSECS)),            // 4
        };
        assertOrdered(coordsArray, baseContext.withPositionAngleJava(new Angle(45, DEGREES)));
    }

    @Test
    public void testPort() throws Exception {
        Coordinates[] coordsArray = new Coordinates[] {
                new Coordinates(new Angle( midDetector, ARCSECS), new Angle(-midDetector, ARCSECS)), // 1
                new Coordinates(new Angle(-midDetector, ARCSECS), new Angle(-midDetector, ARCSECS)), // 2
                new Coordinates(new Angle(-midDetector, ARCSECS), new Angle( midDetector, ARCSECS)), // 3
                new Coordinates(new Angle( midDetector, ARCSECS), new Angle( midDetector, ARCSECS)), // 4
        };
        assertOrdered(coordsArray, baseContext.withIssPort(IssPort.UP_LOOKING));
    }
}
