//
// $Id: Coordinates.java 21291 2009-07-29 16:23:21Z swalker $
//
package edu.gemini.skycalc;

import java.text.ParseException;

import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.shared.util.immutable.Option;
import static edu.gemini.skycalc.Angle.Unit.DEGREES;

/**
 * An RA/Dec pair.
 */
public final class Coordinates {
    private final Angle _ra;
    private final Angle _dec;

    public static Coordinates create(String raStr, String decStr)
            throws ParseException {
        double ra = HHMMSS.parse(raStr).toDegrees().getMagnitude();
        double dec = DDMMSS.parse(decStr).toDegrees().getMagnitude();
        return new Coordinates(ra, dec);
    }

    public static Coordinates fromCoreCoordinates(edu.gemini.spModel.core.Coordinates c) {
        return new Coordinates(c.ra().toDegrees(), c.dec().toDegrees());
    }

    public Option<edu.gemini.spModel.core.Coordinates> toCoreCoordinates() {
        return ImOption.fromScalaOpt(
            edu.gemini.spModel.core.Coordinates$.MODULE$.fromDegrees(
                getRaDeg(),
                getDecDeg()
            )
        );
    }

    public Coordinates(double ra, double dec) {
        this(new Angle(ra, DEGREES), new Angle(dec, DEGREES));
    }

    public Coordinates(Angle ra, Angle dec) {
        _ra  = ra.toDegrees();
        _dec = dec.toDegrees();
    }

    public Angle getRa() {
        return _ra;
    }

    public Angle getDec() {
        return _dec;
    }

    public double getRaDeg() {
        return _ra.getMagnitude();
    }

    public double getDecDeg() {
        return _dec.getMagnitude();
    }

    public boolean equals(Object other) {
        if (other == null) return false;
        if (other.getClass() != getClass()) return false;

        Coordinates that = (Coordinates) other;
        if (!_ra.equals(that._ra)) return false;
        return _dec.equals(that._dec);
    }

    public int hashCode() {
        int res = _ra.hashCode();
        res = res*37 + _dec.hashCode();
        return res;
    }

    public String toString() {
        return HHMMSS.valStr(_ra.getMagnitude()) + " " +
                DDMMSS.valStr(_dec.getMagnitude());
    }

    /**
     * Converts a cartesian coordinate triplet back to a standard ra and dec.
     */
    public static Coordinates xyzToCoordinates(double x, double y, double z) {
        // skycal: xyz_cel
       double mod;    // modulus
       double xy;     // component in xy plane
       double radian_ra, radian_dec;

       // this taken directly from pl1 routine - no acos or asin available there,
       // as it is in c. Easier just to copy, though

       mod = Math.sqrt(x*x + y*y + z*z);
       x = x / mod;
       y = y / mod;
       z = z / mod;   // normalize 'em explicitly first.

       xy = Math.sqrt(x*x + y*y);

       if (xy < 1.0e-10) {
          radian_ra = 0.;  // too close to pole
          radian_dec = Math.PI / 2.;
          if (z < 0.) radian_dec = radian_dec * -1.;
       } else {
          if (Math.abs(z/xy) < 3.0) {
              radian_dec = Math.atan(z / xy);
          } else if (z >= 0.) {
              radian_dec = Math.PI / 2. - Math.atan(xy / z);
          } else {
              radian_dec = -1. * Math.PI / 2. - Math.atan(xy / z);
          }
          if (Math.abs(x) > 1.0e-10) {
             if (Math.abs(y / x) < 3.0) {
                 radian_ra = Math.atan(y/x);
             } else if ((x * y ) >= 0.0) {
                 radian_ra = Math.PI / 2. - Math.atan(x/y);
             } else {
                 radian_ra = -1.0 *  Math.PI / 2. - Math.atan(x / y);
             }
          } else {
             radian_ra = Math.PI / 2.;
             if((x * y)<= 0.) radian_ra = radian_ra * -1.;
          }
          if (x <0.0) {
              radian_ra = radian_ra + Math.PI;
          }
          if (radian_ra < 0.0) {
              radian_ra = radian_ra + 2.0 * Math.PI;
          }
       }

        double ra  = radian_ra * ImprovedSkyCalcMethods.HRS_IN_RADIAN;
        double dec = radian_dec * ImprovedSkyCalcMethods.DEG_IN_RADIAN;
        return new Coordinates(ra, dec);
    }



    /**
     * Takes a coordinate pair and precesses it using matrix procedures
     * as outlined in Taff's Computational Spherical Astronomy book.
     * This is the so-called 'rigorous' method which should give very
     * accurate answers all over the sky over an interval of several
     * centuries.  Naked eye accuracy holds to ancient times, too.
     * Precession constants used are the new IAU1976 -- the 'J2000'
     * system.
     */
    public Coordinates precess(double orig_epoch, double final_epoch) {
        // skycalc: precrot

       double ti, tf, zeta, z, theta;  /* all as per  Taff */
       double cosz, coszeta, costheta, sinz, sinzeta, sintheta;  /* ftns */
       double p11, p12, p13, p21, p22, p23, p31, p32, p33;
          /* elements of the rotation matrix */
       double radian_ra, radian_dec;
       double orig_x, orig_y, orig_z;
       double fin_x, fin_y, fin_z;   /* original and final unit ectors */

       ti = (orig_epoch - 2000.) / 100.;
       tf = (final_epoch - 2000. - 100. * ti) / 100.;

       zeta = (2306.2181 + 1.39656 * ti + 0.000139 * ti * ti) * tf +
        (0.30188 - 0.000344 * ti) * tf * tf + 0.017998 * tf * tf * tf;
       z = zeta + (0.79280 + 0.000410 * ti) * tf * tf + 0.000205 * tf * tf * tf;
       theta = (2004.3109 - 0.8533 * ti - 0.000217 * ti * ti) * tf
         - (0.42665 + 0.000217 * ti) * tf * tf - 0.041833 * tf * tf * tf;

       /* convert to radians */

       zeta = zeta / ImprovedSkyCalcMethods.ARCSEC_IN_RADIAN;
       z = z / ImprovedSkyCalcMethods.ARCSEC_IN_RADIAN;
       theta = theta / ImprovedSkyCalcMethods.ARCSEC_IN_RADIAN;

       /* compute the necessary trig functions for speed and simplicity */

       cosz     = Math.cos(z);
       coszeta  = Math.cos(zeta);
       costheta = Math.cos(theta);
       sinz     = Math.sin(z);
       sinzeta  = Math.sin(zeta);
       sintheta = Math.sin(theta);

       /* compute the elements of the precession matrix */

       p11 = coszeta * cosz * costheta - sinzeta * sinz;
       p12 = -1. * sinzeta * cosz * costheta - coszeta * sinz;
       p13 = -1. * cosz * sintheta;

       p21 = coszeta * sinz * costheta + sinzeta * cosz;
       p22 = -1. * sinzeta * sinz * costheta + coszeta * cosz;
       p23 = -1. * sinz * sintheta;

       p31 = coszeta * sintheta;
       p32 = -1. * sinzeta * sintheta;
       p33 = costheta;

       /* transform original coordinates */

       radian_ra  = getRaDeg()  / ImprovedSkyCalcMethods.HRS_IN_RADIAN;
       radian_dec = getDecDeg() / ImprovedSkyCalcMethods.DEG_IN_RADIAN;

       orig_x = Math.cos(radian_dec) * Math.cos(radian_ra);
       orig_y = Math.cos(radian_dec) * Math.sin(radian_ra);
       orig_z = Math.sin(radian_dec);

          /* (hard coded matrix multiplication ...) */
       fin_x = p11 * orig_x + p12 * orig_y + p13 * orig_z;
       fin_y = p21 * orig_x + p22 * orig_y + p23 * orig_z;
       fin_z = p31 * orig_x + p32 * orig_y + p33 * orig_z;

       /* convert back to spherical polar coords */
       return xyzToCoordinates(fin_x, fin_y, fin_z);
    }
}
