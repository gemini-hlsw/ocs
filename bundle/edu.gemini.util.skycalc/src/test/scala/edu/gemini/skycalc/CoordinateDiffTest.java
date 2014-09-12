//
// $
//

package edu.gemini.skycalc;

import static edu.gemini.skycalc.Angle.Unit.ARCSECS;

import junit.framework.TestCase;
import org.junit.Test;

/**
 */
public class CoordinateDiffTest extends TestCase {

    private void compareAngles(double expected, double actual) {
        String expectedStr = String.format("%.4f", expected);
        String actualStr   = String.format("%.4f", actual);
        assertEquals(expectedStr, actualStr);
    }

    private void verifyDiff(double posAngle, double dist, double p, double q, CoordinateDiff cd) {
        compareAngles(posAngle, cd.getPositionAngle().getMagnitude());
        compareAngles(dist, cd.getDistance().getMagnitude());
        compareAngles(p, cd.getOffset().p().getMagnitude());
        compareAngles(q, cd.getOffset().q().getMagnitude());
    }

    private void testEquator(double posAngle, double raArcsec, double decArcsec) throws Exception {
        double baseRA  = 0.0;
        double baseDec = 0.0;

        CoordinateDiff cd;
        double ra, dec, dist;

        ra  = (new Angle(raArcsec,  ARCSECS)).toDegrees().getMagnitude();
        dec = (new Angle(decArcsec, ARCSECS)).toDegrees().getMagnitude();

        // The distance is close enough to a straightforward calculation at the
        // equator and prime meridian.
        dist = Math.sqrt(Math.pow(raArcsec, 2) + Math.pow(decArcsec, 2));

        cd = new CoordinateDiff(baseRA, baseDec, ra, dec);
        verifyDiff(posAngle, dist, raArcsec, decArcsec, cd);

        cd = new CoordinateDiff(baseRA, baseDec, ra, -dec);
        verifyDiff(180-posAngle, dist, raArcsec, -decArcsec, cd);

        cd = new CoordinateDiff(baseRA, baseDec, -ra, -dec);
        verifyDiff(180+posAngle, dist, -raArcsec, -decArcsec, cd);

        cd = new CoordinateDiff(baseRA, baseDec, -ra, dec);
        verifyDiff(360-posAngle, dist, -raArcsec, decArcsec, cd);
    }

    @Test
    public void testEquator() throws Exception{
        testEquator(45.0, 10.0,       10.0);
        testEquator(30.0, 10.0 * 0.5, 10.0 * Math.sqrt(3)/2.0);
    }

    /*
    public void testX() throws Exception {
        double baseRA  = 0.0;
        double baseDec = 0.0;

        CoordinateDiff cd;
        double ra, dec, dist;

        ra = dec = (new Angle(10, ARCSECS)).toDegrees().getMagnitude();
        dist = Math.sqrt(200); // sqrt(10^2 + 10^2)

        cd = new CoordinateDiff(baseRA, baseDec, ra, dec);
        verifyDiff(45.0, dist, 10.0, 10.0, cd);

        cd = new CoordinateDiff(baseRA, baseDec, ra, -dec);
        System.out.println(cd);
        System.out.println(cd.getOffset().toString());

        cd = new CoordinateDiff(baseRA, baseDec, -ra, -dec);
        System.out.println(cd);
        System.out.println(cd.getOffset().toString());

        cd = new CoordinateDiff(baseRA, baseDec, -ra, dec);
        System.out.println(cd);
        System.out.println(cd.getOffset().toString());
    }

    public void testY() throws Exception {
        double baseRA  = 0.0;
        double baseDec = 0.0;

        CoordinateDiff cd;
        double ra, dec;

        ra  = (new Angle(10, ARCSECS)).toDegrees().getMagnitude();
        dec = (new Angle(20, ARCSECS)).toDegrees().getMagnitude();

        cd = new CoordinateDiff(baseRA, baseDec, ra, dec);
        System.out.println(cd);
        System.out.println(cd.getOffset().toString());

        cd = new CoordinateDiff(baseRA, baseDec, ra, -dec);
        System.out.println(cd);
        System.out.println(cd.getOffset().toString());

        cd = new CoordinateDiff(baseRA, baseDec, -ra, -dec);
        System.out.println(cd);
        System.out.println(cd.getOffset().toString());

        cd = new CoordinateDiff(baseRA, baseDec, -ra, dec);
        System.out.println(cd);
        System.out.println(cd.getOffset().toString());
    }
    */

    /*
    public void testPole() throws Exception {
        double baseRA  = 0.0;
        double baseDec = 90.0;

        CoordinateDiff cd;
        double dec;

        double[] ras = new double[] { 0.0, 45.0, 90.0, 135.0, 180.0, 225.0, 270.0, 315.0 };

        for (double ra : ras) {
            dec = 90 - Angle.Unit.ARCSECS.toDegrees(10);

            cd = new CoordinateDiff(baseRA, baseDec, ra, dec);
            compareAngles(10.0, cd.getDistance().toArcsecs().getMagnitude());

            Angle posAngle = (new Angle(180.0 - ra, Angle.Unit.DEGREES)).toPositive();

            compareAngles(posAngle.getMagnitude(), cd.getPositionAngle().toDegrees().getMagnitude());

            Offset offset = cd.getOffset();


            cd.getDistance().getMagnitude()

            System.out.println("------ RA " + ra);
            System.out.println(cd);
            System.out.println(cd.getOffset().toString());
        }
    }
    */

    public void testZ0() throws Exception {
        double baseRA  = 0.0;
        double baseDec = 90.0;

        CoordinateDiff cd;
        double dec = 90 - Angle.Unit.ARCSECS.toDegrees(10);

        double[] ras = new double[] { 0.0, 45.0, 90.0, 135.0, 180.0, 225.0, 270.0, 315.0 };


        cd = new CoordinateDiff(baseRA, baseDec,   0.0, dec);
        verifyDiff(180.0, 10.0,   0.0,      -10.0,      cd);

        cd = new CoordinateDiff(baseRA, baseDec,  45.0, dec);
        verifyDiff(135.0, 10.0,   7.071068,  -7.071068, cd);

        cd = new CoordinateDiff(baseRA, baseDec,  90.0, dec);
        verifyDiff( 90.0, 10.0,  10.0,        0.0,      cd);

        cd = new CoordinateDiff(baseRA, baseDec, 135.0, dec);
        verifyDiff( 45.0, 10.0,   7.071068,   7.071068, cd);

        cd = new CoordinateDiff(baseRA, baseDec, 180.0, dec);
        verifyDiff(  0.0, 10.0,   0.0,       10.0,      cd);
    }
}
