package edu.gemini.spModel.io.impl.migration.to2015B;

import java.awt.geom.*;

// taken from wscon.java from NASA
public class WSCon {

    /*  Constant vector and matrix (by columns)
        These values were obtained by inverting C.Hohenkerk's forward matrix
        (private communication), which agrees with the one given in reference
        2 but which has one additional decimal place.  */
    static double a[] = {-1.62557e-6, -0.31919e-6, -0.13843e-6};
    static double ad[] = {1.245e-3, -1.580e-3, -0.659e-3};

    /* Convert B1950.0 fk4 star data to J2000.0 fk5 */
    static double[][] em = {
        {0.999925678186902, 0.011182059571766, 0.004857946721186,
         -0.000541652366951, 0.237917612131583, -0.436111276039270},

        {-0.011182059642247, 0.999937478448132, -0.000027147426498,
         -0.237968129744288, -0.002660763319071, 0.012259092261564},

        {-0.004857946558960, -0.000027176441185, 0.999988199738770,
         0.436227555856097, -0.008537771074048, 0.002119110818172},

        {0.000002423950176, 0.000000027106627, 0.000000011776559,
         0.999947035154614, 0.011182506007242, 0.004857669948650},

        {-0.000000027106627, 0.000002423978783, -0.000000000065816,
         -0.011182506121805, 0.999958833818833, -0.000027137309539},

        {-0.000000011776558, -0.000000000065874, 0.000002424101735,
         -0.004857669684959, -0.000027184471371, 1.000009560363559}};

    /* Right ascension in degrees (B1950 in, J2000 out) */
    /* Declination in degrees (B1950 in, J2000 out) */
    /* Besselian epoch in years */
    public static Point2D.Double fk425e(Point2D.Double input, double epoch) {
        /* Proper motion in right ascension */
        /* Proper motion in declination  */
        /* In: rad/trop.yr.  Out:  rad/jul.yr. */

        Point2D.Double pm = new Point2D.Double(0.0, 0.0);

        Point2D.Double output = fk425m(input, pm);

        output.x = output.x + (pm.x * (epoch - 2000.0));
        output.y = output.y + (pm.y * (epoch - 2000.0));

        return output;
    }

    /* Right ascension and declination in degrees
                               input:  B1950.0,fk4  returned:  J2000.0,fk5 */
    /* Proper motion in right ascension and declination
                               input:  B1950.0,fk4  returned:  J2000.0,fk5
                                       deg/trop.yr.            deg/jul.yr.  */
    /* This routine converts stars from the old, Bessel-Newcomb, FK4
       system to the new, IAU 1976, FK5, Fricke system, using Yallop's
       implementation (see ref 2) of a matrix method due to Standish
       (see ref 3).  The numerical values of ref 2 are used canonically.

       Notes:

          1)  The proper motions in ra are dra/dt rather than
               cos(dec)*dra/dt, and are per year rather than per century.

          2)  Conversion from besselian epoch 1950.0 to Julian epoch
               2000.0 only is provided for.  Conversions involving other
               epochs will require use of the appropriate precession,
               proper motion, and e-terms routines before and/or
               after fk425 is called.

          3)  In the FK4 catalogue the proper motions of stars within
               10 degrees of the poles do not embody the differential
               e-term effect and should, strictly speaking, be handled
               in a different manner from stars outside these regions.
               However, given the general lack of homogeneity of the star
               data available for routine astrometry, the difficulties of
               handling positions that may have been determined from
               astrometric fields spanning the polar and non-polar regions,
               the likelihood that the differential e-terms effect was not
               taken into account when allowing for proper motion in past
               astrometry, and the undesirability of a discontinuity in
               the algorithm, the decision has been made in this routine to
               include the effect of differential e-terms on the proper
               motions for all stars, whether polar or not.  At epoch 2000,
               and measuring on the sky rather than in terms of dra, the
               errors resulting from this simplification are less than
               1 milliarcsecond in position and 1 milliarcsecond per
               century in proper motion.

       References:

          1  "Mean and apparent place computations in the new IAU System.
              I. The transformation of astrometric catalog systems to the
              equinox J2000.0." Smith, C.A.; Kaplan, G.H.; Hughes, J.A.;
              Seidelmann, P.K.; Yallop, B.D.; Hohenkerk, C.Y.
              Astronomical Journal vol. 97, Jan. 1989, p. 265-273.

          2  "Mean and apparent place computations in the new IAU System.
              II. Transformation of mean star places from FK4 B1950.0 to
              FK5 J2000.0 using matrices in 6-space."  Yallop, B.D.;
              Hohenkerk, C.Y.; Smith, C.A.; Kaplan, G.H.; Hughes, J.A.;
              Seidelmann, P.K.; Astronomical Journal vol. 97, Jan. 1989,
              p. 274-279.

          3  "Conversion of positions and proper motions from B1950.0 to the
              IAU system at J2000.0", Standish, E.M.  Astronomy and
              Astrophysics, vol. 115, no. 1, Nov. 1982, p. 20-22.

       P.T.Wallace   Starlink   27 October 1987
       Doug Mink     Smithsonian Astrophysical Observatory  7 June 1995 */
    public static Point2D.Double fk425m(Point2D.Double input, Point2D.Double pm) {
        double r1950,d1950; /* B1950.0 ra,dec (rad) */
        double dr1950,dd1950; /* B1950.0 proper motions (rad/trop.yr) */
        double r2000,d2000; /* J2000.0 ra,dec (rad) */
        double dr2000,dd2000; /*J2000.0 proper motions (rad/jul.yr) */

        /* Miscellaneous */
        double ur,ud,sr,cr,sd,cd,w,wd;
        double x,y,z,xd,yd,zd, dra,ddec,scon,tcon;
        double rxysq,rxyzsq,rxy,spxy;
        int i,j;
        int diag = 0;

        double r0[] = new double[3], r1[] = new double[3];  /* star position and velocity vectors */
        double v1[] = new double[6], v2[] = new double[6];  /* combined position and velocity vectors */

        /* Constants */
        double d2pi = 6.283185307179586476925287; /* two PI */
        double pmf; /* radians per year to arcsec per century */
        double tiny = 1.e-30; /* small number to avoid arithmetic problems */
        double zero = 0.0;

        pmf = 100 * 60 * 60 * 360 / d2pi;

        /* Pick up B1950 data (units radians and arcsec / tc) */
        r1950 = ((input.x) * Math.PI / 180.0);
        d1950 = ((input.y) * Math.PI / 180.0);
        dr1950 = ((pm.x) * Math.PI / 180.0);
        dd1950 = ((pm.y) * Math.PI / 180.0);
        ur = dr1950 * pmf;
        ud = dd1950 * pmf;

        /* Spherical to cartesian */
        sr = Math.sin(r1950);
        cr = Math.cos(r1950);
        sd = Math.sin(d1950);
        cd = Math.cos(d1950);

        r0[0] = cr * cd;
        r0[1] = sr * cd;
        r0[2] = sd;

        r1[0] = -sr * cd * ur - cr * sd * ud;
        r1[1] = cr * cd * ur - sr * sd * ud;
        r1[2] = cd * ud;

        /* Allow for e-terms and express as position + velocity 6-vector */
        w = r0[0] * a[0] + r0[1] * a[1] + r0[2] * a[2];
        wd = r0[0] * ad[0] + r0[1] * ad[1] + r0[2] * ad[2];
        for (i = 0; i < 3; i++) {
            v1[i] = r0[i] - a[i] + w * r0[i];
            v1[i + 3] = r1[i] - ad[i] + wd * r0[i];
        }

        /* Convert position + velocity vector to Fricke system */
        for (i = 0; i < 6; i++) {
            w = zero;
            for (j = 0; j < 6; j++) {
                w = w + em[j][i] * v1[j];
            }
            v2[i] = w;
        }

        /* Revert to spherical coordinates */
        x = v2[0];
        y = v2[1];
        z = v2[2];
        xd = v2[3];
        yd = v2[4];
        zd = v2[5];

        rxysq = x * x + y * y;
        rxyzsq = rxysq + z * z;
        rxy = Math.sqrt(rxysq);

        spxy = x * xd + y * yd;

        if (x == zero && y == zero)
            r2000 = zero;
        else {
            r2000 = Math.atan2(y, x);
            if (r2000 < zero)
                r2000 = r2000 + d2pi;
        }
        d2000 = Math.atan2(z, rxy);

        if (rxy > tiny) {
            ur = (x * yd - y * xd) / rxysq;
            ud = (zd * rxysq - z * spxy) / (rxyzsq * rxy);
        }
        dr2000 = ur / pmf;
        dd2000 = ud / pmf;

        /* Return results */
        input.x = ((r2000) * 180.0 / Math.PI);
        input.y = ((d2000) * 180.0 / Math.PI);
        pm.x = ((dr2000) * 180.0 / Math.PI);
        pm.y = ((dd2000) * 180.0 / Math.PI);

        if (diag > 0) {
            scon = ((3.6e3) * 180.0 / Math.PI);
            tcon = ((2.4e2) * 180.0 / Math.PI);
            dra = tcon * (r2000 - r1950);
            ddec = scon * (d2000 - d1950);
            //printf("J2000-B1950: dra= %11.5f sec  ddec= %f11.5f arcsec\n",
            //dra, ddec);
        }

        return input;
    }

}
