package edu.gemini.qpt.core.util;

import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.acos;
import static java.lang.Math.asin;
import static java.lang.Math.cos;
import static java.lang.Math.log;
import static java.lang.Math.pow;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class ImprovedSkyCalcMethods {

    // defined quantities for apparent place transforms ..
    protected static final int XFORM_FROMSTD = 1;
    protected static final int XFORM_TOSTDEP = -1;
    protected static final int XFORM_JUSTPRE = 1;
    protected static final int XFORM_DOAPPAR = 0;

    // some (not all) physical, mathematical, and astronomical constants used are defined here.
    protected static final double TWOPI = 6.28318530717959;
    protected static final double PI_OVER_2 = 1.57079632679490; // From Abramowitz & Stegun
    protected static final double ARCSEC_IN_RADIAN = 206264.8062471;
    protected static final double DEG_IN_RADIAN = 57.2957795130823;
    protected static final double HRS_IN_RADIAN = 3.819718634205;
    protected static final double KMS_AUDAY = 1731.45683633;  // km per sec in 1 AU/day
    protected static final double SPEED_OF_LIGHT = 299792.458;  // in km per sec ... exact.
    protected static final double J2000 = 2451545.; // Julian date at standard epoch
    protected static final double SEC_IN_DAY = 86400.;
    protected static final double FLATTEN = 0.003352813; // flattening of earth, 1/298.257
    protected static final double EQUAT_RAD = 6378137.;  // equatorial radius of earth, meters
    protected static final double ASTRO_UNIT = 1.4959787066e11; // 1 AU in meters
    protected static final double KZEN = 0.172; // zenith extinction, mag, for use in lunar sky brightness calculations.
    protected static final double EARTH_DIFF = 0.05; // used in numerical differentiation to find earth velocity

    // Constants needed in etcorr method
    protected static final double[] DELTS = new double[] { -2.72, 3.86, 10.46,
            17.20, 21.16, 23.62, 24.02, 23.93, 24.33, 26.77, 29.15, 31.07,
            33.15, 35.73, 40.18, 45.48, 50.54, 54.34, 56.86, 60.78, 62.97, };

    protected static final TimeZone UT = TimeZone.getTimeZone("UT");

    protected static final class DoubleRef {
        double d;
        DoubleRef() {
            this(0.0);
        }
        DoubleRef(double d) {
            this.d = d;
        }
        @Override
        public String toString() {
            return Double.toString(d);
        }
    }
    
    protected static final class DateTime {
        
        final short y;
        final short mo;
        final short d;
        final short h;
        final short mn;
        final double s;

        private static final Calendar cal = Calendar.getInstance(UT);
        
        DateTime(Date date) {
            synchronized (cal) { // v. important! [QPT-206]
                cal.setTime(date);
                y = (short)cal.get(Calendar.YEAR);
                mo = (short)(cal.get(Calendar.MONTH) + 1);
                d = (short)cal.get(Calendar.DAY_OF_MONTH);
                h = (short)cal.get(Calendar.HOUR_OF_DAY);
                mn = (short)cal.get(Calendar.MINUTE);
                s = cal.get(Calendar.SECOND) + cal.get(Calendar.MILLISECOND)/1000.;
            }
        }
    }

    /**
     * Return the LST time as a Date object for the given LST hours and date.
     */
    protected static Date getLst(double lstHours, Date date) {
        Calendar cal = Calendar.getInstance(UT);
        cal.setTime(date);
        int h = cal.get(Calendar.HOUR_OF_DAY);
        boolean nextDay = (lstHours < h);
        setHours(cal, lstHours, nextDay);
        return cal.getTime();
    }

    protected static void setHours(Calendar cal, double hours, boolean nextDay) {
        int h = (int) hours;
        double md = (hours - h) * 60.;
        int min = (int) md;
        double sd = (md - min) * 60.;
        int sec = (int)sd;
        int ms = (int)((sd - sec) * 1000);
    
        cal.set(Calendar.HOUR_OF_DAY, h);
        cal.set(Calendar.MINUTE, min);
        cal.set(Calendar.SECOND, sec);
        cal.set(Calendar.MILLISECOND, ms);
    
        if (nextDay) {
            cal.add(Calendar.HOUR_OF_DAY, 24);
        }
    }

    /**
     * Return the airmass for the given altitude in degrees.
     */
    public static double getAirmass(double alt) {
        double secz = secant_z(alt);
        if (secz >= 0.) {
            if (secz < 12.) {
               return true_airmass(secz);
            } else if (secz <= 99.) {
                return secz;
            }
        }
        return 0.;
    }

    /**
     * This takes the date (which contains the time), and the site parameters,
     * and prints out a banner giving the various dates and times; also
     * computes and returns various jd's, the sidereal time, and the epoch.
     * Returns negative number to signal error if date is out of range of
     * validity of algorithms, or if you specify a bad time during daylight-time
     * change; returns zero if successful.
     */
    protected static short setup_time_place(DateTime date, double longit, DoubleRef jdut, DoubleRef sid, DoubleRef curepoch) {
        double jd = date_to_jd(date);
        sid.d = lst(jd, longit);
        jdut.d = jd;
        curepoch.d = 2000. + (jd - J2000) / 365.25;
        return (0);
    }

    protected static void cooxform(double rin, double din, double std_epoch, double date_epoch, DoubleRef rout, DoubleRef dout, int just_precess, int from_std) {
    
        /* all the 3-d stuff is declared as [4] 'cause I'm not using the
          zeroth element. */
    
        double ti, tf, zeta, z, theta;  /* all as per  Taff */
        double cosz, coszeta, costheta, sinz, sinzeta, sintheta;  /* ftns */
        double[][] p = new double[4][4];
        /* elements of the rotation matrix */
        double[][] n = new double[4][4];
        /* elements of the nutation matrix */
        double[][] r = new double[4][4];
        /* their product */
        double[][] t = new double[4][4];  /* temporary matrix for inversion .... */
        double radian_ra, radian_dec;
    
        /* nutation angles in radians */
        DoubleRef del_psi = new DoubleRef();
        DoubleRef del_eps = new DoubleRef();
        double eps;
    
        double[] orig = new double[4];   /* original unit vector */
        double[] fin = new double[4];   /* final unit vector */
        int i, j, k;
    
    
        ti = (std_epoch - 2000.) / 100.;
        tf = (date_epoch - 2000. - 100. * ti) / 100.;
    
        zeta = (2306.2181 + 1.39656 * ti + 0.000139 * ti * ti) * tf +
                (0.30188 - 0.000344 * ti) * tf * tf + 0.017998 * tf * tf * tf;
        z = zeta + (0.79280 + 0.000410 * ti) * tf * tf + 0.000205 * tf * tf * tf;
        theta = (2004.3109 - 0.8533 * ti - 0.000217 * ti * ti) * tf
                - (0.42665 + 0.000217 * ti) * tf * tf - 0.041833 * tf * tf * tf;
    
        /* convert to radians */
    
        zeta = zeta / ARCSEC_IN_RADIAN;
        z = z / ARCSEC_IN_RADIAN;
        theta = theta / ARCSEC_IN_RADIAN;
    
        /* compute the necessary trig functions for speed and simplicity */
    
        cosz = Math.cos(z);
        coszeta = Math.cos(zeta);
        costheta = Math.cos(theta);
        sinz = Math.sin(z);
        sinzeta = Math.sin(zeta);
        sintheta = Math.sin(theta);
    
        /* compute the elements of the precession matrix -- set up
           here as *from* standard epoch *to* input jd. */
    
        p[1][1] = coszeta * cosz * costheta - sinzeta * sinz;
        p[1][2] = -1. * sinzeta * cosz * costheta - coszeta * sinz;
        p[1][3] = -1. * cosz * sintheta;
    
        p[2][1] = coszeta * sinz * costheta + sinzeta * cosz;
        p[2][2] = -1. * sinzeta * sinz * costheta + coszeta * cosz;
        p[2][3] = -1. * sinz * sintheta;
    
        p[3][1] = coszeta * sintheta;
        p[3][2] = -1. * sinzeta * sintheta;
        p[3][3] = costheta;
    
        if (just_precess == XFORM_DOAPPAR) {  /* if apparent place called for */
    
            /* do the same for the nutation matrix. */
    
            nutation_params(date_epoch, del_psi, del_eps);
            eps = 0.409105;  /* rough obliquity of ecliptic in radians */
    
            n[1][1] = 1.;
            n[2][2] = 1.;
            n[3][3] = 1.;
            n[1][2] = -1. * del_psi.d * Math.cos(eps);
            n[1][3] = -1. * del_psi.d * Math.sin(eps);
            n[2][1] = -1. * n[1][2];
            n[2][3] = -1. * del_eps.d;
            n[3][1] = -1. * n[1][3];
            n[3][2] = -1. * n[2][3];
    
            /* form product of precession and nutation matrices ... */
            for (i = 1; i <= 3; i++) {
                for (j = 1; j <= 3; j++) {
                    r[i][j] = 0.;
                    for (k = 1; k <= 3; k++) {
                        r[i][j] += p[i][k] * n[k][j];
                    }
                }
            }
        } else {  /* if you're just precessing .... */
            for (i = 1; i <= 3; i++) {
                for (j = 1; j <= 3; j++) {
                    r[i][j] = p[i][j];  /* simply copy precession matrix */
                }
            }
        }
    
        /* The inverse of a rotation matrix is its transpose ... */
    
        if (from_std == XFORM_TOSTDEP) {    /* if you're transforming back to std
                         epoch, rather than forward from std */
            for (i = 1; i <= 3; i++) {
                for (j = 1; j <= 3; j++) {
                    t[i][j] = r[j][i];  /* store transpose ... */
                }
            }
            for (i = 1; i <= 3; i++) {
                for (j = 1; j <= 3; j++) {
                    r[i][j] = t[i][j];  /* replace original w/ transpose.*/
                }
            }
        }
    
        /* finally, transform original coordinates */
    
        radian_ra = rin / HRS_IN_RADIAN;
        radian_dec = din / DEG_IN_RADIAN;
    
        orig[1] = Math.cos(radian_dec) * Math.cos(radian_ra);
        orig[2] = Math.cos(radian_dec) * Math.sin(radian_ra);
        orig[3] = Math.sin(radian_dec);
    
    
        if (from_std == XFORM_TOSTDEP && just_precess == XFORM_DOAPPAR)
        /* if you're transforming from jd to std epoch, and doing apparent place,
       first step is to de-aberrate while still in epoch of date ... */ {
            aberrate(date_epoch, orig, from_std);
        }
    
    
        for (i = 1; i <= 3; i++) {
            fin[i] = 0.;
            for (j = 1; j <= 3; j++) {
                fin[i] += r[i][j] * orig[j];
            }
        }
    
        if (from_std == XFORM_FROMSTD && just_precess == XFORM_DOAPPAR)
        /* if you're transforming from std epoch to jd,
             last step is to apply aberration correction once you're in
             equinox of that jd. */ {
            aberrate(date_epoch, fin, from_std);
        }
    
        /* convert back to spherical polar coords */
    
        xyz_cel(fin[1], fin[2], fin[3], rout, dout);
    
        return;
    }

    /**
     * computes the nutation parameters delta psi and
     * delta epsilon at julian epoch (in years) using approximate
     * formulae given by Jean Meeus, Astronomical Formulae for
     * Calculators, Willman-Bell, 1985, pp. 69-70. Accuracy
     * appears to be a few hundredths of an arcsec or better
     * and numerics have been checked against his example.
     * Nutation parameters are returned in radians.
     */
    protected static void nutation_params(double date_epoch, DoubleRef del_psi, DoubleRef del_ep) {
    
        double T, jd, L, Lprime, M, Mprime, Omega;
    
        jd = (date_epoch - 2000.) * 365.25 + J2000;
        T = (jd - 2415020.0) / 36525.;
    
        L = 279.6967 + (36000.7689 + 0.000303 * T) * T;
        Lprime = 270.4342 + (481267.8831 - 0.001133 * T) * T;
        M = 358.4758 + (35999.0498 - 0.000150 * T) * T;
        Mprime = 296.1046 + (477198.8491 + 0.009192 * T) * T;
        Omega = 259.1833 - (1934.1420 - 0.002078 * T) * T;
    
        L = L / DEG_IN_RADIAN;
        Lprime = Lprime / DEG_IN_RADIAN;
        M = M / DEG_IN_RADIAN;
        Mprime = Mprime / DEG_IN_RADIAN;
        Omega = Omega / DEG_IN_RADIAN;
    
    
        del_psi.d = -1. * (17.2327 + 0.01737 * T) * Math.sin(Omega)
                - (1.2729 + 0.00013 * T) * Math.sin(2. * L)
                + 0.2088 * Math.sin(2 * Omega)
                - 0.2037 * Math.sin(2 * Lprime)
                + (0.1261 - 0.00031 * T) * Math.sin(M)
                + 0.0675 * Math.sin(Mprime)
                - (0.0497 - 0.00012 * T) * Math.sin(2 * L + M)
                - 0.0342 * Math.sin(2 * Lprime - Omega)
                - 0.0261 * Math.sin(2 * Lprime + Mprime)
                + 0.0214 * Math.sin(2 * L - M)
                - 0.0149 * Math.sin(2 * L - 2 * Lprime + Mprime)
                + 0.0124 * Math.sin(2 * L - Omega)
                + 0.0114 * Math.sin(2 * Lprime - Mprime);
    
        del_ep.d = (9.2100 + 0.00091 * T) * Math.cos(Omega)
                + (0.5522 - 0.00029 * T) * Math.cos(2 * L)
                - 0.0904 * Math.cos(2 * Omega)
                + 0.0884 * Math.cos(2. * Lprime)
                + 0.0216 * Math.cos(2 * L + M)
                + 0.0183 * Math.cos(2 * Lprime - Omega)
                + 0.0113 * Math.cos(2 * Lprime + Mprime)
                - 0.0093 * Math.cos(2 * L - M)
                - 0.0066 * Math.cos(2 * L - Omega);
    
        del_psi.d = del_psi.d / ARCSEC_IN_RADIAN;
        del_ep.d = del_ep.d / ARCSEC_IN_RADIAN;
    }

    /**
     * A much cleaner rewrite of the original skycalc code for this,
     * which was transcribed from a PL/I routine ....
     */
    protected static void xyz_cel(double x, double y, double z, DoubleRef ra, DoubleRef dec) { /* corresponding right ascension and declination,
                                          returned in decimal hours and decimal degrees. */
        double mod;    /* modulus */
        double xy;     /* component in xy plane */
    
        /* normalize explicitly and check for bad input */
    
        mod = Math.sqrt(x * x + y * y + z * z);
        if (mod > 0.) {
            x = x / mod;
            y = y / mod;
            z = z / mod;
        } else {   /* this has never happened ... */
            System.out.println(
                    "Bad data in xyz_cel .... zero modulus position vector.\n");
            ra.d = 0.;
            dec.d = 0.;
            return;
        }
    
        xy = Math.sqrt(x * x + y * y);
    
        if (xy < 1.0e-11) {   /* practically on a pole -- limit is arbitrary ...  */
            ra.d = 0.;   /* degenerate anyway */
            dec.d = PI_OVER_2;
            if (z < 0.) {
                dec.d *= -1.;
            }
        } else { /* in a normal part of the sky ... */
            dec.d = Math.asin(z);
            ra.d = atan_circ(x, y);
        }
    
        ra.d *= HRS_IN_RADIAN;
        dec.d *= DEG_IN_RADIAN;
    }

    /**
     * corrects celestial unit vector for aberration due to earth's motion.
     * Uses accurate sun position ... replace with crude one for more speed if
     * needed.
     */
    protected static void aberrate(double epoch, double[] vec, int from_std) {  /* 1 = apply aberration, -1 = take aberration out. */
    
        double jd, jd1, jd2, Xdot, Ydot, Zdot;   /* page C24 */
    
        /* throwaways */
        DoubleRef ras = new DoubleRef();
        DoubleRef decs = new DoubleRef();
        DoubleRef dists = new DoubleRef();
        DoubleRef topora = new DoubleRef();
        DoubleRef topodec = new DoubleRef();
    
        DoubleRef x = new DoubleRef();
        DoubleRef y = new DoubleRef();
        DoubleRef z = new DoubleRef();
        DoubleRef x1 = new DoubleRef();
        DoubleRef y1 = new DoubleRef();
        DoubleRef z1 = new DoubleRef();
        DoubleRef x2 = new DoubleRef();
        DoubleRef y2 = new DoubleRef();
        DoubleRef z2 = new DoubleRef();
    
        double norm;
    
        /* find heliocentric velocity of earth as a fraction of the speed of light ... */
    
        jd = J2000 + (epoch - 2000.) * 365.25;
        jd1 = jd - EARTH_DIFF;
        jd2 = jd + EARTH_DIFF;
    
        accusun(jd1, 0., 0., ras, decs, dists, topora, topodec, x1, y1, z1);
        accusun(jd2, 0., 0., ras, decs, dists, topora, topodec, x2, y2, z2);
        accusun(jd, 0., 0., ras, decs, dists, topora, topodec, x, y, z);
    
        Xdot = KMS_AUDAY * (x2.d - x1.d) / (2. * EARTH_DIFF * SPEED_OF_LIGHT);  /* numerical differentiation */
        Ydot = KMS_AUDAY * (y2.d - y1.d) / (2. * EARTH_DIFF * SPEED_OF_LIGHT);  /* crude but accurate */
        Zdot = KMS_AUDAY * (z2.d - z1.d) / (2. * EARTH_DIFF * SPEED_OF_LIGHT);
    
        /* approximate correction ... non-relativistic but very close.  */
    
        vec[1] += from_std * Xdot;
        vec[2] += from_std * Ydot;
        vec[3] += from_std * Zdot;
    
        norm = Math.pow((vec[1] * vec[1] + vec[2] * vec[2] + vec[3] * vec[3]), 0.5);
    
        vec[1] = vec[1] / norm;
        vec[2] = vec[2] / norm;
        vec[3] = vec[3] / norm;
    }

    /**
     * returns radian angle 0 to 2pi for coords x, y --
     * get that quadrant right !!
     */
    protected static double atan_circ(double x, double y) {
        double theta;
    
        if ((x == 0.) && (y == 0.)) {
            return (0.);  /* guard ... */
        }
    
        theta = Math.atan2(y, x);
        while (theta < 0.) {
            theta += TWOPI;
        }
        return (theta);
    }

    /**
     * implemenataion of Jean Meeus' more accurate solar
     * ephemeris.  For ultimate use in helio correction! From
     * Astronomical Formulae for Calculators, pp. 79 ff.  This
     * gives sun's position wrt *mean* equinox of date, not
     * apparent*.  Accuracy is << 1 arcmin.  Positions given are
     * geocentric ... parallax due to observer's position on earth is
     * ignored. This is up to 8 arcsec; routine is usually a little
     * better than that.
     * // -- topocentric correction *is* included now. -- //
     * Light travel time is apparently taken into
     * account for the ra and dec, but I don't know if aberration is
     * and I don't know if distance is simlarly antedated.
     * <p/>
     * x, y, and z are heliocentric equatorial coordinates of the
     * EARTH, referred to mean equator and equinox of date.
     */
    protected static void accusun(double jd, double lst, double geolat, DoubleRef ra, DoubleRef dec, DoubleRef dist, DoubleRef topora, DoubleRef topodec, DoubleRef x, DoubleRef y, DoubleRef z) {
    
            double L, T, Tsq, Tcb;
            double M, e, Cent, nu, sunlong;
            double Mrad, nurad, R;
            double A, B, C, D, E, H;
            double xtop, ytop, ztop, topodist, l, m, n;
            DoubleRef xgeo = new DoubleRef();
            DoubleRef ygeo = new DoubleRef();
            DoubleRef zgeo = new DoubleRef();
    
            jd = jd + etcorr(jd) / SEC_IN_DAY;  /* might as well do it right .... */
            T = (jd - 2415020.) / 36525.;  /* 1900 --- this is an oldish theory*/
            Tsq = T * T;
            Tcb = T * Tsq;
            L = 279.69668 + 36000.76892 * T + 0.0003025 * Tsq;
            M = 358.47583 + 35999.04975 * T - 0.000150 * Tsq - 0.0000033 * Tcb;
            e = 0.01675104 - 0.0000418 * T - 0.000000126 * Tsq;
    
            L = circulo(L);
            M = circulo(M);
    /*      printf("raw L, M: %15.8f, %15.8f\n",L,M); */
    
            A = 153.23 + 22518.7541 * T;  /* A, B due to Venus */
            B = 216.57 + 45037.5082 * T;
            C = 312.69 + 32964.3577 * T;  /* C due to Jupiter */
            /* D -- rough correction from earth-moon
                barycenter to center of earth. */
            D = 350.74 + 445267.1142 * T - 0.00144 * Tsq;
            E = 231.19 + 20.20 * T;    /* "inequality of long period .. */
            H = 353.40 + 65928.7155 * T;  /* Jupiter. */
    
            A = circulo(A) / DEG_IN_RADIAN;
            B = circulo(B) / DEG_IN_RADIAN;
            C = circulo(C) / DEG_IN_RADIAN;
            D = circulo(D) / DEG_IN_RADIAN;
            E = circulo(E) / DEG_IN_RADIAN;
            H = circulo(H) / DEG_IN_RADIAN;
    
            L = L + 0.00134 * Math.cos(A)
                    + 0.00154 * Math.cos(B)
                    + 0.00200 * Math.cos(C)
                    + 0.00179 * Math.sin(D)
                    + 0.00178 * Math.sin(E);
    
            Mrad = M / DEG_IN_RADIAN;
    
            Cent = (1.919460 - 0.004789 * T - 0.000014 * Tsq) * Math.sin(Mrad)
                    + (0.020094 - 0.000100 * T) * Math.sin(2.0 * Mrad)
                    + 0.000293 * Math.sin(3.0 * Mrad);
            sunlong = L + Cent;
    
    
            nu = M + Cent;
            nurad = nu / DEG_IN_RADIAN;
    
            R = (1.0000002 * (1 - e * e)) / (1. + e * Math.cos(nurad));
            R = R + 0.00000543 * Math.sin(A)
                    + 0.00001575 * Math.sin(B)
                    + 0.00001627 * Math.sin(C)
                    + 0.00003076 * Math.cos(D)
                    + 0.00000927 * Math.sin(H);
    
            sunlong = sunlong / DEG_IN_RADIAN;
    
            dist.d = R;
            x.d = Math.cos(sunlong);  /* geocentric */
            y.d = Math.sin(sunlong);
            z.d = 0.;
            eclrot(jd, y, z);
    
            /*      --- code to include topocentric correction for sun .... */
    
            geocent(lst, geolat, 0., xgeo, ygeo, zgeo);
    
            xtop = x.d - xgeo.d * EQUAT_RAD / ASTRO_UNIT;
            ytop = y.d - ygeo.d * EQUAT_RAD / ASTRO_UNIT;
            ztop = z.d - zgeo.d * EQUAT_RAD / ASTRO_UNIT;
    
            topodist = Math.sqrt(xtop * xtop + ytop * ytop + ztop * ztop);
    
            l = xtop / (topodist);
            m = ytop / (topodist);
            n = ztop / (topodist);
    
            topora.d = atan_circ(l, m) * HRS_IN_RADIAN;
            topodec.d = Math.asin(n) * DEG_IN_RADIAN;
    
            ra.d = atan_circ(x.d, y.d) * HRS_IN_RADIAN;
            dec.d = Math.asin(z.d) * DEG_IN_RADIAN;
    
            x.d = x.d * R * -1;  /* heliocentric */
            y.d = y.d * R * -1;
            z.d = z.d * R * -1;
        }

    /**
     * Given a julian date in 1900-2100, returns the correction
     * delta t which is:
     * TDT - UT (after 1983 and before 1998)
     * ET - UT (before 1983)
     * an extrapolated guess  (after 1998).
     * <p/>
     * For dates in the past (<= 1998 and after 1900) the value is linearly
     * interpolated on 5-year intervals; for dates after the present,
     * an extrapolation is used, because the true value of delta t
     * cannot be predicted precisely.  Note that TDT is essentially the
     * modern version of ephemeris time with a slightly cleaner
     * definition.
     * <p/>
     * Where the algorithm shifts there is an approximately 0.1 second
     * discontinuity.  Also, the 5-year linear interpolation scheme can
     * lead to errors as large as 0.5 seconds in some cases, though
     * usually rather smaller.
     */
    protected static double etcorr(double jd) {
    
        double[] dates = new double[22];
        double year, delt = 0.;
        int i;
    
        for (i = 0; i <= 19; i++) {
            dates[i] = 1900 + i * 5.;
        }
        dates[20] = 1998.;  /* the last accurately tabulated one in the
                                2000 Almanac ... */
    
        year = 1900. + (jd - 2415019.5) / 365.25;
    
        if (year < 1998. && year >= 1900.) {
            i = (int) (year - 1900) / 5;
            delt = DELTS[i] +
                    ((DELTS[i + 1] - DELTS[i]) / (dates[i + 1] - dates[i])) *
                    (year - dates[i]);
        } else if (year >= 1998. && year < 2100.) {
            delt = 33.15 + (2.164e-3) * (jd - 2436935.4);  /* rough extrapolation */
        } else if (year < 1900) {
            System.out.println("etcorr ... no ephemeris time data for < 1900.\n");
            delt = 0.;
        } else if (year >= 2100.) {
            System.out.println(
                    "etcorr .. very long extrapolation in delta T - inaccurate.\n");
            delt = 180.; /* who knows? */
        }
    
        return (delt);
    }

    /**
     * assuming x is an angle in degrees, returns
     * modulo 360 degrees.
     */
    protected static double circulo(double x) {
        int n = (int) (x / 360.);
        return (x - 360. * n);
    }

    /**
     * rotates ecliptic rectangular coords x, y, z to
     * equatorial (all assumed of date.)
     */
    protected static void eclrot(double jd, DoubleRef y, DoubleRef z) {
            double incl;
    //        double xpr;
            double ypr;
            double zpr;
            double T;
    
            T = (jd - J2000) / 36525;  /* centuries since J2000 */
    
            incl = (23.439291 + T * (-0.0130042 - 0.00000016 * T)) / DEG_IN_RADIAN;
            /* 1992 Astron Almanac, p. B18, dropping the
               cubic term, which is 2 milli-arcsec! */
            ypr = Math.cos(incl) * y.d - Math.sin(incl) * z.d;
            zpr = Math.sin(incl) * y.d + Math.cos(incl) * z.d;
            y.d = ypr;
            z.d = zpr;
            /* x remains the same. */
        }

    protected static void eclrot(double jd, @SuppressWarnings("unused") DoubleRef x, DoubleRef y, DoubleRef z) {
        double incl;
        double /*xpr, */ypr,zpr;
        double T;
    
        T = (jd - J2000) / 36525;  /* centuries since J2000 */
    
        incl = (23.439291 + T * (-0.0130042 - 0.00000016 * T))/DEG_IN_RADIAN;
            /* 1992 Astron Almanac, p. B18, dropping the
               cubic term, which is 2 milli-arcsec! */
        ypr = cos(incl) * y.d - sin(incl) * z.d;
        zpr = sin(incl) * y.d + cos(incl) * z.d;
        y.d = ypr;
        z.d = zpr;
        /* x remains the same. */
    }

    /**
     * computes the geocentric coordinates from the geodetic
     * (standard map-type) longitude, latitude, and height.
     * These are assumed to be in decimal hours, decimal degrees, and
     * meters respectively.  Notation generally follows 1992 Astr Almanac,
     * p. K11
     */
    protected static void geocent(double geolong, double geolat, double height, DoubleRef x_geo, DoubleRef y_geo, DoubleRef z_geo) {
    
        double denom, C_geo, S_geo;
    
        geolat = geolat / DEG_IN_RADIAN;
        geolong = geolong / HRS_IN_RADIAN;
        denom = (1. - FLATTEN) * Math.sin(geolat);
        denom = Math.cos(geolat) * Math.cos(geolat) + denom * denom;
        C_geo = 1. / Math.sqrt(denom);
        S_geo = (1. - FLATTEN) * (1. - FLATTEN) * C_geo;
        C_geo = C_geo + height / EQUAT_RAD;  /* deviation from almanac
                   notation -- include height here. */
        S_geo = S_geo + height / EQUAT_RAD;
        x_geo.d = C_geo * Math.cos(geolat) * Math.cos(geolong);
        y_geo.d = C_geo * Math.cos(geolat) * Math.sin(geolong);
        z_geo.d = S_geo * Math.sin(geolat);
    }

    /**
     * adjusts a time (decimal hours) to be between -12 and 12,
     * generally used for hour angles.
     */
    protected static double adj_time(double x) {
        if (Math.abs(x) < 100000.) {  /* too inefficient for this! */
            while (x > 12.) {
                x = x - 24.;
            }
            while (x < -12.) {
                x = x + 24.;
            }
        } else {
            System.out.println("Out of bounds in adj_time!\n");
        }
        return (x);
    }

    /**
     * returns altitude(degr) for dec, ha, lat (decimal degr, hr, degr);
     * also computes and returns azimuth through pointer argument,
     * and as an extra added bonus returns parallactic angle (decimal degr)
     * through another pointer argument.
     *
     * @param dec target declination in degrees
     * @param ha  the hour angle in hours
     * @param lat the observer's latitude in radians
     * @return the parallactic angle in degrees
     */
    protected static double altit(double dec, double ha, double lat, DoubleRef az, DoubleRef parang) {
    
        double x, y, z;
        double sinp, cosp;  /* sin and cos of parallactic angle */
        double cosdec, sindec, cosha, sinha, coslat, sinlat;
        /* time-savers ... */
    
        dec = dec / DEG_IN_RADIAN;
        ha = ha / HRS_IN_RADIAN;
        lat = lat / DEG_IN_RADIAN;  /* thank heavens for pass-by-value */
        cosdec = Math.cos(dec);
        sindec = Math.sin(dec);
        cosha = Math.cos(ha);
        sinha = Math.sin(ha);
        coslat = Math.cos(lat);
        sinlat = Math.sin(lat);
        x = DEG_IN_RADIAN * Math.asin(cosdec * cosha * coslat + sindec * sinlat);
        y = sindec * coslat - cosdec * cosha * sinlat; /* due N comp. */
        z = -1. * cosdec * sinha; /* due east comp. */
        az.d = Math.atan2(z, y);
    
        /* as it turns out, having knowledge of the altitude and
               azimuth makes the spherical trig of the parallactic angle
               less ambiguous ... so do it here!  Method uses the
           "astronomical triangle" connecting celestial pole, object,
               and zenith ... now know all the other sides and angles,
               so we can crush it ... */
    
        if (cosdec != 0.) { /* protect divide by zero ... */
            sinp = -1. * Math.sin(az.d) * coslat / cosdec;
            /* spherical law of sines .. note cosdec = sin of codec,
                coslat = sin of colat .... */
            cosp = -1. * Math.cos(az.d) * cosha - Math.sin(az.d) * sinha * sinlat;
            /* spherical law of cosines ... also transformed to local
                          available variables. */
            parang.d = Math.atan2(sinp, cosp) * DEG_IN_RADIAN;
            /* let the library function find the quadrant ... */
        } else { /* you're on the pole */
            if (lat >= 0.) {
                parang.d = 180.;
            } else {
                parang.d = 0.;
            }
        }
    
        az.d *= DEG_IN_RADIAN;  /* done with taking trig functions of it ... */
        while (az.d < 0.) {
            az.d += 360.;  /* force 0 -> 360 */
        }
        while (az.d >= 360.) {
            az.d -= 360.;
        }
    
        return (x);
    }

    /**
     * Computes the secant of z, assuming the object is not
     * too low to the horizon; returns 100. if the object is
     * low but above the horizon, -100. if the object is just
     * below the horizon.
     */
    protected static double secant_z(double alt) {
    
        double secz;
        if (alt != 0) {
            secz = 1. / Math.sin(alt / DEG_IN_RADIAN);
        } else {
            secz = 100.;
        }
        if (secz > 100.) {
            secz = 100.;
        }
        if (secz < -100.) {
            secz = -100.;
        }
        return (secz);
    }

    /**
     * returns the true airmass for a given secant z.
     * The expression used is based on a tabulation of the mean KPNO
     * atmosphere given by C. M. Snell & A. M. Heiser, 1968,
     * PASP, 80, 336.  They tabulated the airmass at 5 degr
     * intervals from z = 60 to 85 degrees; I fit the data with
     * a fourth order poly for (secz - airmass) as a function of
     * (secz - 1) using the IRAF curfit routine, then adjusted the
     * zeroth order term to force (secz - airmass) to zero at
     * z = 0.  The poly fit is very close to the tabulated points
     * (largest difference is 3.2e-4) and appears smooth.
     * This 85-degree point is at secz = 11.47, so for secz > 12
     * I just return secz - 1.5 ... about the largest offset
     * properly determined.
     */
    protected static double true_airmass(double secz) {
    
        double seczmin1;
        int i, ord = 4;
        double[] coef = new double[5];
        double result = 0;
    
        coef[1] = 2.879465E-3;  /* sun compilers do not allow automatic
                initializations of arrays. */
        coef[2] = 3.033104E-3;
        coef[3] = 1.351167E-3;
        coef[4] = -4.716679E-5;
        if (secz < 0.) {
            return (-1.);  /* out of range. */
        }
        if (secz > 12) {
            return (secz - 1.5);  /* shouldn't happen .... */
        }
        seczmin1 = secz - 1.;
        /* evaluate polynomial ... */
        for (i = ord; i > 0; i--) {
            result = (result + coef[i]) * seczmin1;
        }
        /* no zeroth order term. */
        result = secz - result;
        return (result);
    
    }

    protected static double date_to_jd(DateTime date) {
        short yr1=0, mo1=1;
        long jdzpt = 1720982, jdint, inter;
        double jd,jdfrac;
    
    
        if((date.y <= 1900) | (date.y >= 2100)) {
    //        printf("Date out of range.  1900 - 2100 only.\n");
    //        return(0.);
            throw new IllegalArgumentException("Date out of range.  1900 - 2100 only.");
        }
    
        if(date.mo <= 2) {
            yr1 = -1;
            mo1 = 13;
        }
    
        jdint = (long) (365.25*(date.y+yr1));  /* truncates */
        inter = (long) (30.6001*(date.mo+mo1));
        jdint = jdint+inter+date.d+jdzpt;
        jd = jdint;
        jdfrac=date.h/24.+date.mn/1440.+date.s/SEC_IN_DAY;
        if(jdfrac < 0.5) {
            jdint--;
            jdfrac=jdfrac+0.5;
        }
        else jdfrac=jdfrac-0.5;
        jd=jdint+jdfrac;
        return(jd);
    }

    /**
     * returns the local MEAN sidereal time (dec hrs) at julian date jd
     * at west longitude long (decimal hours).  Follows
     * definitions in 1992 Astronomical Almanac, pp. B7 and L2.
     * Expression for GMST at 0h ut referenced to Aoki et al, A&A 105,
     * p.359, 1982.  On workstations, accuracy (numerical only!)
     * is about a millisecond in the 1990s.
     */
    protected static double lst(double jd, double longit) {
    
            double t, ut, jdmid, jdint, jdfrac, sid_g;
            long jdin, sid_int;
    
            jdin = (long) jd;         /* fossil code from earlier package which
                                       split jd into integer and fractional parts ... */
            jdint = jdin;
            jdfrac = jd - jdint;
            if (jdfrac < 0.5) {
                jdmid = jdint - 0.5;
                ut = jdfrac + 0.5;
            } else {
                jdmid = jdint + 0.5;
                ut = jdfrac - 0.5;
            }
            t = (jdmid - J2000) / 36525;
            sid_g = (24110.54841 + 8640184.812866 * t + 0.093104 * t * t -
                    6.2e-6 * t * t * t) / SEC_IN_DAY;
            sid_int = (long) sid_g;
            sid_g = sid_g - sid_int;
            sid_g = sid_g + 1.0027379093 * ut - longit / 24.;
            sid_int = (long) sid_g;
            sid_g = (sid_g - sid_int) * 24.;
    //        if (sid_g < 0.) {
    //            sid_g = sid_g + 24.;
    //        }
            return (sid_g);
        }

    /**
     * @param mpa moon phase angle in degrees
     * @param mdist moon/object distance in degreee
     * @param mZD moon zenith distance [deg]
     * @param ZD object zenith distance [deg]
     * @param sZD Sun zenith distance [deg]
     * @param moondist Earth-Moon distance
     */
    protected static double sb(double mpa, double mdist, double mZD, double ZD, double sZD, double moondist) {

        final double degrad  =   57.2957795130823d; 
        final double k=0.172d; // ; mag/airmass for Hale Pohaku
        final double a=2.51189d;
        final double Q=27.78151d;
    
        final double saltit = 90.0d - sZD; // ; Sun's altitude
    
        //    ; Dark sky zenith V surface brightness 
        //    Vzen = dblarr(n_elements(ZD))
        //    Vzen[*] = 21.587d
        double Vzen = 21.587d;

        //    ; Correct for brightening due to twilight
        //    ii = where(saltit gt -18.5)
        //    if (ii[0] ne -1) then Vzen[ii] = Vzen[ii] - ztwilight(saltit[ii])
        if (saltit > -18.5) {
            Vzen -= ztwilight(saltit);
        }

        // Sky contribution
        double Bzen = 0.263d * Math.pow(a, Q-Vzen);   // zenith sky brightness
        double Bsky= Bzen*xair(ZD)* Math.pow(10, (-0.4d*k*(xair(ZD)-1.0d)));

        // Moon contribution
        double istar=0.0d,  fp=0.0d,  Bmoon=0.0d, frho=0.0d;
        if (mZD <= 90.8) {
            moondist /= 60.27;    // divide by the mean Earth-Moon distance
            istar = Math.pow(10, (-0.4d*(3.84d + 0.026d*abs(mpa) + (4.e-9)* Math.pow(mpa, 4)))) / (moondist * moondist);
            if(abs(mpa) < 7.) {  // crude accounting for opposition effect
                /* 35 per cent brighter at full, effect tapering linearly to
                zero at 7 degrees away from full. mentioned peripherally in
                Krisciunas and Scheafer, p. 1035. */
                istar *= (1.35 - 0.05 * abs(mpa));
            }
            frho = 229087.0 * (1.06 + cos(mdist/degrad) * cos(mdist/degrad));
            if (abs(mdist) >= 10.) {
                fp = frho + Math.pow(10, (6.15d - mdist/40.0d));
            } else if (abs(mdist) > 0.25) {
                fp = frho + 6.2e7/Math.pow(mdist, 2);
            } else {
            	fp = frho + 9.9e8;
            }
            Bmoon = fp * istar * Math.pow(10, -0.4d * k * xair(mZD)) * (1.0d - Math.pow(10, -0.4d * k * xair(ZD)));
        }

        // sky brightness in Vmag/arcsec^2
        double ret = Q - Math.log10((Bmoon + Bsky) / 0.263) / Math.log10(a);
        // System.out.printf("sb(%1.2f, %1.2f, %1.2f, %1.2f, %1.2f) => %1.3f\n", mpa, mdist, mZD, ZD, sZD, ret);
        return ret;
        }

    protected static double xair(double z) {
        
    //    ;degrad=180.0d/!PI
        final double degrad  =   57.2957795130823d;
    //  return,1.0d/sqrt(1.0d - 0.96d*         sin(z/degrad)^ 2)
        return 1.0d/sqrt(1.0d - 0.96d*Math.pow(sin(z/degrad), 2));
        
        }

    protected static double lunskybright(double alpha, double rho, double kzen, double altmoon, double alt, double moondist) {
    
        double istar,Xzm,Xo,Z,Zmoon,Bmoon,fofrho,rho_rad; //,test;
    
        rho_rad = rho/DEG_IN_RADIAN;
        alpha = (180. - alpha);
        Zmoon = (90. - altmoon)/DEG_IN_RADIAN;
        Z = (90. - alt)/DEG_IN_RADIAN;
        moondist = moondist/(60.27);  /* divide by mean distance */
    
        istar = -0.4*(3.84 + 0.026*abs(alpha) + 4.0e-9*pow(alpha,4.)); /*eqn 20*/
        istar =  pow(10.,istar)/(moondist * moondist);
        if(abs(alpha) < 7.) {  // crude accounting for opposition effect
            /* 35 per cent brighter at full, effect tapering linearly to
            zero at 7 degrees away from full. mentioned peripherally in
            Krisciunas and Scheafer, p. 1035. */
            istar *= (1.35 - 0.05 * abs(alpha));
        }
        fofrho = 229087. * (1.06 + cos(rho_rad)*cos(rho_rad));
        if(abs(rho) > 10.)
           fofrho=fofrho+pow(10.,(6.15 - rho/40.));            /* eqn 21 */
        else if (abs(rho) > 0.25)
           fofrho= fofrho+ 6.2e7 / (rho*rho);   /* eqn 19 */
        else fofrho = fofrho+9.9e8;  /*for 1/4 degree -- radius of moon! */
        Xzm = sqrt(1.0 - 0.96*sin(Zmoon)*sin(Zmoon));
        if(Xzm != 0.) Xzm = 1./Xzm;
          else Xzm = 10000.;
        Xo = sqrt(1.0 - 0.96*sin(Z)*sin(Z));
        if(Xo != 0.) Xo = 1./Xo;
          else Xo = 10000.;
        Bmoon = fofrho * istar * pow(10.,(-0.4*kzen*Xzm))
          * (1. - pow(10.,(-0.4*kzen*Xo)));   /* nanoLamberts */
        if(Bmoon > 0.001)
          return(22.50 - 1.08574 * log(Bmoon/34.08)); /* V mag per sq arcs-eqn 1 */
        else return(99.);
    }

    protected static void accumoon(double jd, final double geolat, final double lst, final double elevsea, final DoubleRef geora, final DoubleRef geodec, final DoubleRef geodist, final DoubleRef topora, final DoubleRef topodec, final DoubleRef topodist) {
            /*      double *eclatit,*eclongit, *pie,*ra,*dec,*dist; geocent quantities,
                    formerly handed out but not in this version */
            
                final double pie;
                final double dist;  /* horiz parallax */
                
                double Lpr;
                double M;
                double Mpr;
                double D;
                double F;
                double Om;
                
                final double T;
                final double Tsq;
                final double Tcb;
                final double e;
                
                double lambda;
                double B;
                double beta;
                final double om1;
                final double om2;
                double sinx;
                double x;
                double y;
                double z;
                
                final DoubleRef x_geo = new DoubleRef();
                final DoubleRef y_geo = new DoubleRef();
                final DoubleRef z_geo = new DoubleRef();
                
    //            double x_geo, y_geo, z_geo;  /* geocentric position of *observer* */
    
                jd = jd + etcorr(jd)/SEC_IN_DAY;   /* approximate correction to ephemeris time */
                T = (jd - 2415020.) / 36525.;   /* this based around 1900 ... */
                Tsq = T * T;
                Tcb = Tsq * T;
    
                Lpr = 270.434164 + 481267.8831 * T - 0.001133 * Tsq
                        + 0.0000019 * Tcb;
                M = 358.475833 + 35999.0498*T - 0.000150*Tsq
                        - 0.0000033*Tcb;
                Mpr = 296.104608 + 477198.8491*T + 0.009192*Tsq
                        + 0.0000144*Tcb;
                D = 350.737486 + 445267.1142*T - 0.001436 * Tsq
                        + 0.0000019*Tcb;
                F = 11.250889 + 483202.0251*T -0.003211 * Tsq
                        - 0.0000003*Tcb;
                Om = 259.183275 - 1934.1420*T + 0.002078*Tsq
                        + 0.0000022*Tcb;
    
                Lpr = circulo(Lpr);
                Mpr = circulo(Mpr);
                M = circulo(M);
                D = circulo(D);
                F = circulo(F);
                Om = circulo(Om);
    
    
                sinx =  sin((51.2 + 20.2 * T)/DEG_IN_RADIAN);
                Lpr = Lpr + 0.000233 * sinx;
                M = M - 0.001778 * sinx;
                Mpr = Mpr + 0.000817 * sinx;
                D = D + 0.002011 * sinx;
    
                sinx = 0.003964 * sin((346.560+132.870*T -0.0091731*Tsq)/DEG_IN_RADIAN);
    
                Lpr = Lpr + sinx;
                Mpr = Mpr + sinx;
                D = D + sinx;
                F = F + sinx;
    
                sinx = sin(Om/DEG_IN_RADIAN);
                Lpr = Lpr + 0.001964 * sinx;
                Mpr = Mpr + 0.002541 * sinx;
                D = D + 0.001964 * sinx;
                F = F - 0.024691 * sinx;
                F = F - 0.004328 * sin((Om + 275.05 -2.30*T)/DEG_IN_RADIAN);
    
                e = 1 - 0.002495 * T - 0.00000752 * Tsq;
    
                M = M / DEG_IN_RADIAN;   /* these will all be arguments ... */
                Mpr = Mpr / DEG_IN_RADIAN;
                D = D / DEG_IN_RADIAN;
                F = F / DEG_IN_RADIAN;
    
                lambda = Lpr + 6.288750 * sin(Mpr)
                    + 1.274018 * sin(2*D - Mpr)
                    + 0.658309 * sin(2*D)
                    + 0.213616 * sin(2*Mpr)
                    - e * 0.185596 * sin(M)
                    - 0.114336 * sin(2*F)
                    + 0.058793 * sin(2*D - 2*Mpr)
                    + e * 0.057212 * sin(2*D - M - Mpr)
                    + 0.053320 * sin(2*D + Mpr)
                    + e * 0.045874 * sin(2*D - M)
                    + e * 0.041024 * sin(Mpr - M)
                    - 0.034718 * sin(D)
                    - e * 0.030465 * sin(M+Mpr)
                    + 0.015326 * sin(2*D - 2*F)
                    - 0.012528 * sin(2*F + Mpr)
                    - 0.010980 * sin(2*F - Mpr)
                    + 0.010674 * sin(4*D - Mpr)
                    + 0.010034 * sin(3*Mpr)
                    + 0.008548 * sin(4*D - 2*Mpr)
                    - e * 0.007910 * sin(M - Mpr + 2*D)
                    - e * 0.006783 * sin(2*D + M)
                    + 0.005162 * sin(Mpr - D);
    
                    /* And furthermore.....*/
    
                lambda = lambda + e * 0.005000 * sin(M + D)
                    + e * 0.004049 * sin(Mpr - M + 2*D)
                    + 0.003996 * sin(2*Mpr + 2*D)
                    + 0.003862 * sin(4*D)
                    + 0.003665 * sin(2*D - 3*Mpr)
                    + e * 0.002695 * sin(2*Mpr - M)
                    + 0.002602 * sin(Mpr - 2*F - 2*D)
                    + e * 0.002396 * sin(2*D - M - 2*Mpr)
                    - 0.002349 * sin(Mpr + D)
                    + e * e * 0.002249 * sin(2*D - 2*M)
                    - e * 0.002125 * sin(2*Mpr + M)
                    - e * e * 0.002079 * sin(2*M)
                    + e * e * 0.002059 * sin(2*D - Mpr - 2*M)
                    - 0.001773 * sin(Mpr + 2*D - 2*F)
                    - 0.001595 * sin(2*F + 2*D)
                    + e * 0.001220 * sin(4*D - M - Mpr)
                    - 0.001110 * sin(2*Mpr + 2*F)
                    + 0.000892 * sin(Mpr - 3*D)
                    - e * 0.000811 * sin(M + Mpr + 2*D)
                    + e * 0.000761 * sin(4*D - M - 2*Mpr)
                    + e * e * 0.000717 * sin(Mpr - 2*M)
                    + e * e * 0.000704 * sin(Mpr - 2 * M - 2*D)
                    + e * 0.000693 * sin(M - 2*Mpr + 2*D)
                    + e * 0.000598 * sin(2*D - M - 2*F)
                    + 0.000550 * sin(Mpr + 4*D)
                    + 0.000538 * sin(4*Mpr)
                    + e * 0.000521 * sin(4*D - M)
                    + 0.000486 * sin(2*Mpr - D);
    
            /*              *eclongit = lambda;  */
    
                B = 5.128189 * sin(F)
                    + 0.280606 * sin(Mpr + F)
                    + 0.277693 * sin(Mpr - F)
                    + 0.173238 * sin(2*D - F)
                    + 0.055413 * sin(2*D + F - Mpr)
                    + 0.046272 * sin(2*D - F - Mpr)
                    + 0.032573 * sin(2*D + F)
                    + 0.017198 * sin(2*Mpr + F)
                    + 0.009267 * sin(2*D + Mpr - F)
                    + 0.008823 * sin(2*Mpr - F)
                    + e * 0.008247 * sin(2*D - M - F)
                    + 0.004323 * sin(2*D - F - 2*Mpr)
                    + 0.004200 * sin(2*D + F + Mpr)
                    + e * 0.003372 * sin(F - M - 2*D)
                    + 0.002472 * sin(2*D + F - M - Mpr)
                    + e * 0.002222 * sin(2*D + F - M)
                    + e * 0.002072 * sin(2*D - F - M - Mpr)
                    + e * 0.001877 * sin(F - M + Mpr)
                    + 0.001828 * sin(4*D - F - Mpr)
                    - e * 0.001803 * sin(F + M)
                    - 0.001750 * sin(3*F)
                    + e * 0.001570 * sin(Mpr - M - F)
                    - 0.001487 * sin(F + D)
                    - e * 0.001481 * sin(F + M + Mpr)
                    + e * 0.001417 * sin(F - M - Mpr)
                    + e * 0.001350 * sin(F - M)
                    + 0.001330 * sin(F - D)
                    + 0.001106 * sin(F + 3*Mpr)
                    + 0.001020 * sin(4*D - F)
                    + 0.000833 * sin(F + 4*D - Mpr);
                 /* not only that, but */
                B = B + 0.000781 * sin(Mpr - 3*F)
                    + 0.000670 * sin(F + 4*D - 2*Mpr)
                    + 0.000606 * sin(2*D - 3*F)
                    + 0.000597 * sin(2*D + 2*Mpr - F)
                    + e * 0.000492 * sin(2*D + Mpr - M - F)
                    + 0.000450 * sin(2*Mpr - F - 2*D)
                    + 0.000439 * sin(3*Mpr - F)
                    + 0.000423 * sin(F + 2*D + 2*Mpr)
                    + 0.000422 * sin(2*D - F - 3*Mpr)
                    - e * 0.000367 * sin(M + F + 2*D - Mpr)
                    - e * 0.000353 * sin(M + F + 2*D)
                    + 0.000331 * sin(F + 4*D)
                    + e * 0.000317 * sin(2*D + F - M + Mpr)
                    + e * e * 0.000306 * sin(2*D - 2*M - F)
                    - 0.000283 * sin(Mpr + 3*F);
    
                om1 = 0.0004664 * cos(Om/DEG_IN_RADIAN);
                om2 = 0.0000754 * cos((Om + 275.05 - 2.30*T)/DEG_IN_RADIAN);
    
                beta = B * (1. - om1 - om2);
            /*      *eclatit = beta; */
    
                pie = 0.950724
                    + 0.051818 * cos(Mpr)
                    + 0.009531 * cos(2*D - Mpr)
                    + 0.007843 * cos(2*D)
                    + 0.002824 * cos(2*Mpr)
                    + 0.000857 * cos(2*D + Mpr)
                    + e * 0.000533 * cos(2*D - M)
                    + e * 0.000401 * cos(2*D - M - Mpr)
                    + e * 0.000320 * cos(Mpr - M)
                    - 0.000271 * cos(D)
                    - e * 0.000264 * cos(M + Mpr)
                    - 0.000198 * cos(2*F - Mpr)
                    + 0.000173 * cos(3*Mpr)
                    + 0.000167 * cos(4*D - Mpr)
                    - e * 0.000111 * cos(M)
                    + 0.000103 * cos(4*D - 2*Mpr)
                    - 0.000084 * cos(2*Mpr - 2*D)
                    - e * 0.000083 * cos(2*D + M)
                    + 0.000079 * cos(2*D + 2*Mpr)
                    + 0.000072 * cos(4*D)
                    + e * 0.000064 * cos(2*D - M + Mpr)
                    - e * 0.000063 * cos(2*D + M - Mpr)
                    + e * 0.000041 * cos(M + D)
                    + e * 0.000035 * cos(2*Mpr - M)
                    - 0.000033 * cos(3*Mpr - 2*D)
                    - 0.000030 * cos(Mpr + D)
                    - 0.000029 * cos(2*F - 2*D)
                    - e * 0.000029 * cos(2*Mpr + M)
                    + e * e * 0.000026 * cos(2*D - 2*M)
                    - 0.000023 * cos(2*F - 2*D + Mpr)
                    + e * 0.000019 * cos(4*D - M - Mpr);
    
                beta = beta/DEG_IN_RADIAN;
                lambda = lambda/DEG_IN_RADIAN;
                DoubleRef l = new DoubleRef(cos(lambda) * cos(beta));
                DoubleRef m = new DoubleRef(sin(lambda) * cos(beta));
                DoubleRef n = new DoubleRef(sin(beta));
                eclrot(jd, l, m, n);
    
                dist = 1/sin((pie)/DEG_IN_RADIAN);
                x = l.d * dist;
                y = m.d * dist;
                z = n.d * dist;
    
                geora.d = atan_circ(l.d,m.d) * HRS_IN_RADIAN;
                geodec.d = asin(n.d) * DEG_IN_RADIAN;
                geodist.d = dist;
    
                geocent(lst,geolat,elevsea, x_geo, y_geo, z_geo);
    
                x = x - x_geo.d;  /* topocentric correction using elliptical earth fig. */
                y = y - y_geo.d;
                z = z - z_geo.d;
    
                topodist.d = sqrt(x*x + y*y + z*z);
    
                l.d = x / (topodist.d);
                m.d = y / (topodist.d);
                n.d = z / (topodist.d);
    
                topora.d = atan_circ(l.d,m.d) * HRS_IN_RADIAN;
                topodec.d = asin(n.d) * DEG_IN_RADIAN;
    
            }

    protected static double ztwilight(double alt) {
    
        /*
         * evaluates a polynomial expansion for the approximate brightening in
         * magnitudes of the zenith in twilight compared to its value at full
         * night, as function of altitude of the sun (in degrees). To get this
         * expression I looked in Meinel, A., & Meinel, M., "Sunsets, Twilight, &
         * Evening Skies", Cambridge U. Press, 1983; there's a graph on p. 38
         * showing the decline of zenith twilight. I read points off this graph
         * and fit them with a polynomial; I don't even know what band there
         * data are for!
         */
        /*
         * Comparison with Ashburn, E. V. 1952, JGR, v.57, p.85 shows that this
         * is a good fit to his B-band measurements.
         */
    
        double y, val;
    
        y = (-1. * alt - 9.0) / 9.0; /* my polynomial's argument... */
        val = ((2.0635175 * y + 1.246602) * y - 9.4084495) * y + 6.132725;
        return (val);
    }

    protected static double subtend(double ra1, double dec1, double ra2, double dec2) {
    
        /*
         * angle subtended by two positions in the sky -- return value is in
         * radians. Hybrid algorithm works down to zero separation except very
         * near the poles.
         */
    
        double x1, y1, z1, x2, y2, z2;
        double theta;
    
        ra1 = ra1 / HRS_IN_RADIAN;
        dec1 = dec1 / DEG_IN_RADIAN;
        ra2 = ra2 / HRS_IN_RADIAN;
        dec2 = dec2 / DEG_IN_RADIAN;
        x1 = cos(ra1) * cos(dec1);
        y1 = sin(ra1) * cos(dec1);
        z1 = sin(dec1);
        x2 = cos(ra2) * cos(dec2);
        y2 = sin(ra2) * cos(dec2);
        z2 = sin(dec2);
        theta = acos(x1 * x2 + y1 * y2 + z1 * z2);
        /*
         * use flat Pythagorean approximation if the angle is very small and*
         * you're not close to the pole; avoids roundoff in arccos.
         */
        if (theta < 1.0e-5) { /* seldom the case, so don't combine test */
            if (abs(dec1) < (PI / 2. - 0.001) && abs(dec2) < (PI / 2. - 0.001)) {
                /* recycled variables here... */
                x1 = (ra2 - ra1) * cos((dec1 + dec2) / 2.);
                x2 = dec2 - dec1;
                theta = sqrt(x1 * x1 + x2 * x2);
            }
        }
        return (theta);
    }
}
