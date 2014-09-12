// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: DDMMSS.java 23468 2010-01-18 19:39:50Z swalker $
//
/*
 * NCSA Horizon Image Browser
 * Project Horizon
 * National Center for Supercomputing Applications
 * University of Illinois at Urbana-Champaign
 * 605 E. Springfield, Champaign IL 61820
 * horizon@ncsa.uiuc.edu
 *
 * Copyright (C) 1996, Board of Trustees of the University of Illinois
 *
 * NCSA Horizon software, both binary and source (hereafter, Software) is
 * copyrighted by The Board of Trustees of the University of Illinois
 * (UI), and ownership remains with the UI.
 *
 * You should have received a full statement of copyright and
 * conditions for use with this package; if not, a copy may be
 * obtained from the above address.  Please see this statement
 * for more details.
 */

package edu.gemini.skycalc;

import edu.gemini.shared.util.immutable.Option;
import static edu.gemini.skycalc.Angle.Unit.DEGREES;

import java.text.ParseException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.math.MathContext;

/**
 * Support for converting between angles in string
 * and double representations.
 */
public class DDMMSS {

    /**
     * Parses the given string into an Angle representing the corresponding
     * declination. It is assumed to be in the format DD:MM:SS (degrees,
     * arcmins, arcsecs) with an optional leading sign, and an optional
     * trailing fractional arcsecond amount.  The separator must be the :
     * character. Degrees, arcmins and arcsecs may be two digits or one with
     * a leading 0.
     *
     * @param dec string to parse into an declination
     *
     * @return {@link Angle} representing the declination
     *
     * @throws ParseException if the string cannot be parsed into a declination
     */
    public static Angle parse(String dec) throws ParseException {
        return parse(dec, RaDecParser.STANDARD_PARSER);
    }

    /**
     * Parses the given string into an Angle representing the corresponding
     * declination. It is assumed to be in the format
     * DD{separator}MM{separator}SS (degrees, arcmins, arcsecs) with an optional
     * leading sign, and an optional trailing fractional arcsecond amount.  The
     * separator may be any non-null, non-empty string that does not start or
     * end with a digit character. Degrees, arcmins and arcsecs may be two
     * digits or one with a leading 0.
     *
     * @param dec string to parse into an declination
     *
     * @return {@link Angle} representing the declination
     *
     * @throws ParseException if the string cannot be parsed into a declination
     * @throws IllegalArgumentException if the separator string is invalid
     */
    public static Angle parse(String dec, String separator) throws ParseException {
        return parse(dec, new RaDecParser(separator));
    }

    private static final BigDecimal MIN_DEGREES = new BigDecimal(-90);
    private static final BigDecimal MAX_DEGREES = new BigDecimal( 90);

    private static Angle parse(String dec, RaDecParser parser) throws ParseException {
        // Parse the Ra into its constituent parts
        RaDecParser.Result res = parser.parse(dec);

        // Reject any value that is out of range.
        int deg  = res.part1();
        int mins = res.part2();
        int secs = res.part3();
        Option<BigDecimal> fracSecOpt = res.part4();

        // Compute the integral number of arcsecs.  The goal is to avoid
        // dividing until the end.
        int totalsecs = deg*3600 + mins*60 + secs;
        BigDecimal bd = new BigDecimal(totalsecs);

        // Add in the amount of fractional seconds.
        if (!fracSecOpt.isEmpty()) bd = bd.add(fracSecOpt.getValue());

        // Negate if necessary.
        if (res.signum() < 0) bd = bd.negate(MathContext.UNLIMITED);

        // Now divide by 3600 to get degrees.  It's a bit unclear to what
        // scale this division should be done.  We want it to be at least
        // enough precision to hold whatever fraction of seconds is indicated
        // in the input string when divided by 3600.  Arbitrarily chosing a
        // minimum scale of 10.
        bd = bd.divide(new BigDecimal(3600), Math.max(10, bd.scale()+5), RoundingMode.HALF_UP);

        // Check that the value is in the -90, 90 range.
        if ((bd.compareTo(MIN_DEGREES) < 0) || (bd.compareTo(MAX_DEGREES) > 0)) {
            throw new ParseException("Value out of range: " + dec, 0);
        }

        // Here we convert to a double -- need to update Angle to store a
        // BigDecimal when possible.
        return new Angle(bd.doubleValue(), DEGREES);
    }

    /**
     * Covert from a Dec in degrees to a DD:MM:SS String representation.
     */
    public static String valStr(double degrees, int prec) {
        int sign = (degrees < 0) ? -1 : 1;
        degrees = Math.abs(degrees);

        //System.out.println("--------------> " + MathUtil.doubleToString(degrees));
        int dd = (int) degrees;
        double tmp = (degrees - (double) dd) * 60.0;
        int mm = (int) tmp;
        double ss = (tmp - (double) mm) * 60.0;
        //System.out.println("--------------> " + dd + ", " + mm + ", " + ss);

        // correct for formating errors caused by rounding
        if (ss > 59.99999) {
            ss = 0;
            mm += 1;
            if (mm >= 60) {
                mm = 0;
                dd += 1;
            }
        }
        StringBuilder out = new StringBuilder();
        if (sign < 0)
            out.append('-');
        out.append(dd);
        if (prec == -2)
            return out.toString();
        out.append(':');
        if (mm < 10)
            out.append('0');
        out.append(mm);
        if (prec == -1)
            return out.toString();
        out.append(':');

        // Ignoring prec for now
        ss = ((double) Math.round(ss * 100.0)) / 100.0;
        if (ss < 10)
            out.append('0');
        out.append(ss);


        //if (prec < -2) {
        //   if (ss < 0.000099)
        //      out.append("0.0000");
        //   else
        //      out.append(ss);
        //} else {
        //   // specific precision requested; NOT YET SUPPORTED
        //   out.append( ss );
        //}
        return out.toString();
    }

    /**
     * Covert from a Dec in degrees to a DD:MM:SS String representation.
     */
    public static String valStr(double degrees) {
        return valStr(degrees, -3);
    }
}

