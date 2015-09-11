// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: HHMMSS.java 23468 2010-01-18 19:39:50Z swalker $
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
 *
 */

package edu.gemini.skycalc;

import static edu.gemini.skycalc.Angle.Unit.DEGREES;
import edu.gemini.shared.util.immutable.Option;

import java.text.ParseException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.math.MathContext;

/**
 * Support for converting angles in hours:minutes:seconds format over the
 * circular range 0, 24 hours.
 */
public final class HHMMSS {

    /**
     * Parses the given string into an Angle representing the corresponding RA.
     * It is assumed to be in the format HH:MM:SS (hours, minutes, seconds) with
     * an optional leading sign, and an optional trailing fractional seconds.
     * The separator must be the : character. Hours, minutes and seconds may be
     * two digits or one with a leading 0.
     *
     * @param ra string to parse into an RA angle
     *
     * @return {@link Angle} representing the right ascension
     *
     * @throws ParseException if the string cannot be parsed into an RA
     */
    public static Angle parse(String ra) throws ParseException {
        return parse(ra, RaDecParser.STANDARD_PARSER);
    }

    /**
     * Parses the given string into an Angle representing the corresponding
     * right ascension. It is assumed to be in the format
     * HH{separator}MM{separator}SS (hours, minutes, seconds) with an optional
     * leading sign, and an optional trailing fractional second amount.  The
     * separator may be any non-null, non-empty string that does not start or
     * end with a digit character. Hours, minutes and seconds may be two
     * digits or one with a leading 0.
     *
     * @param ra string to parse into an RA angle
     *
     * @return {@link Angle} representing the right ascension
     *
     * @throws ParseException if the string cannot be parsed into an RA
     * @throws IllegalArgumentException if the separator string is invalid
     */
    public static Angle parse(String ra, String separator) throws ParseException {
        return parse(ra, new RaDecParser(separator));
    }

    // Used to obtain degrees from a value expressed as seconds of time.
    // Note, every arcsec is 15 seconds of time.
    private static final BigDecimal DIV = new BigDecimal(240); // 3600/15

    private static Angle parse(String ra, RaDecParser parser) throws ParseException {
        // Parse the Ra into its constituent parts
        RaDecParser.Result res = parser.parse(ra);

        // Reject any value that is out of range.
        int hours = res.part1();
        int mins  = res.part2();
        int secs  = res.part3();

        if ((hours >= 24) || (hours <= -24)) {
            throw new ParseException("Hour of valid range: " + hours, 0);
        }

        // Compute the integral number of secs.  The goal is to avoid
        // dividing until the end.
        int totalsecs = hours*3600 + mins*60 + secs;
        BigDecimal bd = new BigDecimal(totalsecs);

        // Add in the amount of fractional seconds.
        Option<BigDecimal> fracSecOpt = res.part4();
        if (!fracSecOpt.isEmpty()) bd = bd.add(fracSecOpt.getValue());

        // Negate if necessary.
        if (res.signum() < 0) bd = bd.negate(MathContext.UNLIMITED);

        // Now divide by DIV to get degrees.  Since the value is expressed in
        // seconds at this point, we should divide by 3600, but we never
        // multiplied by 15 to convert to arcsecs so only divide by 3600/15=240.
        //
        // It's a bit unclear to what scale this division should be done.  We
        // want it to be at least enough precision to hold whatever fraction of
        // seconds is indicated in the input string when divided by 240.
        // Arbitrarily chosing a minimum scale of 10.
        bd = bd.divide(DIV, Math.max(10, bd.scale()+5), RoundingMode.HALF_UP);

        // Here we convert to a double -- need to update Angle to store a
        // BigDecimal when possible.  By convention, express the result as a
        // positive value.
        return new Angle(bd.doubleValue(), DEGREES).toPositive();
    }

    /**
     * Convert from an RA in degrees to a String in HH:MM:SS format.
     */
    public static String valStr(double degrees, int prec) {
        // Make sure the angle is between 0 (inclusive) and 360 (exclusive)
        degrees = new Angle(degrees, Angle.Unit.DEGREES).getMagnitude();
        double tmp = degrees / 15.0;
        int hh = (int) tmp;
        tmp = (tmp - (double) hh) * 60.0;
        int mm = (int) tmp;
        double ss = (tmp - (double) mm) * 60.0;

        //System.out.println("--------------> " + hh + ", " + mm + ", " + ss);

        // correct for formating errors caused by rounding
        if (ss > 59.99999) {
            ss = 0;
            mm += 1;
            if (mm >= 60) {
                mm = 0;
                hh += 1;
                if (hh >= 24)
                    hh -= 24.0;
            }
        }
        StringBuilder out = new StringBuilder();
        out.append(hh);
        if (prec == -2)
            return out.toString();
        out.append(':');
        if (mm < 10)
            out.append('0');
        out.append(mm);
        if (prec == -1)
            return out.toString();
        out.append(':');

        // Ignoring prec for now.
        ss = ((double) Math.round(ss * 1000.0)) / 1000.0;
        if (ss < 10)
            out.append('0');
        out.append(ss);

        //if (prec < -2) {
        //    if (ss < 0.000099)
        //	out.append("0.0000");
        //    else
        //	out.append(ss);
        //} else {
        //    // specific precision requested; NOT YET SUPPORTED
        //    out.append( ss );
        //}

        return out.toString();
    }

    /**
     * Convert from an RA in degrees to a String in HH:MM:SS format.
     */
    public static String valStr(double degrees) {
        return valStr(degrees, -3);
    }
}

