//=== File Prolog===========================================================
//    This code was developed by NASA, Goddard Space Flight Center, Code 588
//    for the Scientist's Expert Assistant (SEA) project for Next Generation
//    Space Telescope (NGST).
//
//--- Development History---------------------------------------------------
//    Date              Author          Reference
//    11/17/97          S Grosvenor
//          Initial packaging of class into science package
//    5/25/00           S. Grosvenor
//          Moved/generalized for jsky.science - only formating stuff in here
//
//    5/14/02  J. Doggett, The Space Telescope Science Institute
//         Fixed bad assumption about Double.toString method in
//         fromatDouble(double,int,int,boolean):String.  In the case
//         where scientific notation should not be used, if the input double is
//         less than or equal to 10^inDecs, then scientific notation will be
//         used because that is the behavior of Double.toString(double):String.
//         The method was modified to explict using the DecimalFormat class to
//         to formatting for the case when scientific notation is not to be
//         used.
//--- DISCLAIMER---------------------------------------------------------------
//
//	This software is provided "as is" without any warranty of any kind, either
//	express, implied, or statutory, including, but not limited to, any
//	warranty that the software will conform to specification, any implied
//	warranties of merchantability, fitness for a particular purpose, and
//	freedom from infringement, and any warranty that the documentation will
//	conform to the program, or any warranty that the software will be error
//	free.
//
//	In no event shall NASA be liable for any damages, including, but not
//	limited to direct, indirect, special or consequential damages, arising out
//	of, resulting from, or in any way connected with this software, whether or
//	not based upon warranty, contract, tort or otherwise, whether or not
//	injury was sustained by persons or property or otherwise, and whether or
//	not loss was sustained from or arose out of the results of, or use of,
//	their software or services provided hereunder.
//=== End File Prolog=======================================================


package jsky.science.util;

import jsky.science.MathUtilities;

import java.text.DecimalFormat;

/**
 * MathUtilities class is a static class that contains methods
 * are generic in value and related to format objects.
 *
 * <P>This code was developed by NASA, Goddard Space Flight Center, Code 588
 *    for the Scientist's Expert Assistant (SEA) project for Next Generation
 *    Space Telescope (NGST).
 *
 * @version	5.25.00
 * @author	Sandy Grosvenor
 *
**/
public final class FormatUtilities
{
    /**
     * very simple error message
     */
    public static void writeError( Object source, Object message)
    {
        System.err.println( "[ERROR] " + source + ": " + message);
    }

   /**
     * Formats a double value to specified number of decimal places
     * handles need (or not) for scientific notation
     * assumes default max lenth of 10, for no particular reason
     */
    public static String formatDouble( double inVal, int inDecs)
    {
        return formatDouble( inVal, inDecs, 14, false);
    }

    /**
     * Formats a double value to specified number of decimal places
     * handles need (or not) for scientific notation
     * @param inVal double number to be formatted
     * @param inDecs integer of number of decimal places
     * @param inLeftOfDec integer of max number of places to left of decimal
     */
    public static String formatDouble( double inVal, int inDecs, int inLeftOfDec)
    {
        return formatDouble( inVal, inDecs, inLeftOfDec, false);
    }

    /**
     * Formats a double value to specified number of decimal places
     * handles need (or not) for scientific notation
     * @param inVal double number to be formatted
     * @param inDecs integer of number of decimal places
     * @param inLeftOfDec integer of max number of places to left of decimal
     */
    public static String formatDouble( double inVal, int inDecs, int inLeftOfDec, boolean recursing)
    {
        String returnVal;

        if ( Double.isInfinite(inVal))
        {
            returnVal = Double.toString( Double.POSITIVE_INFINITY);
        }
        else if ( Double.isNaN( inVal))
        {
            returnVal = Double.toString( Double.NaN);
        }
        else if ( inVal == 0)
        {
            StringBuilder sb = new StringBuilder("0");
            if (inDecs > 0) sb.append( ".");
            for (int i = 0; i < inDecs; i++) sb.append( "0");
            returnVal = sb.toString();
        }
        else
        {
            // dont let digits to left of decimal be less than 1
            inLeftOfDec = Math.max( inLeftOfDec, 1);

            int maxExp = inLeftOfDec - inDecs;  // max 10 power before going to sci notat
            if (inDecs == 0) maxExp++;
            if (inVal < 0) maxExp--;

            int minExp = -inDecs;           // min 10 power before going to sci notat

            boolean doSN = (Math.abs(inVal) > Math.pow(10, maxExp)) || (Math.abs(inVal) < Math.pow(10, minExp));

            if (!doSN)
            {
                String ret;
                // not doing scientific notation
                if (inDecs == 0)
                {
                    ret = Long.toString( Math.round( inVal));
                }
                else
                {
                    String formatPattern = "0.";
                    for (int i = 1; i <= inDecs; i++)
                        formatPattern += "0";
                    ret = (new DecimalFormat(formatPattern)).
                            format(Math.round( inVal * Math.pow( 10, inDecs)) / Math.pow( 10, inDecs));

                    int dotLoc = ret.indexOf(".");
                    if (inDecs > 0 && dotLoc > 0)
                    {
                        ret = ret.substring( 0, Math.min( ret.length(), dotLoc + inDecs+1));
                    }
                }
                return ret;
            }
            else
            {
                // dont let digits to left of decimal be less than 1
                inLeftOfDec = Math.max( inLeftOfDec, 1);

                // is scientific notation, still use recommended decs
                int sign = 1;
                if (inVal < 0)
                {
                    sign = -1;
                    inVal = -inVal;
                }

                int pow10 = 0;
                if (inVal != 0) pow10 = (int) Math.floor( MathUtilities.log10( inVal));

                double adjVal = sign * inVal / Math.pow( 10, pow10);

                if (Double.isNaN( adjVal))
                {
                    returnVal = "NaN";
                }
                else if (recursing)
                {
                    // to avoid an endless loop
                    returnVal = Double.toString( inVal) + "e" + Integer.toString( pow10);
                }
                else
                    returnVal = formatDouble( adjVal, inDecs, inLeftOfDec, true) + "e" + Integer.toString( pow10);
            }
        }

        //writeDebug( "formatDouble", Double.toString( inVal) + " to " + returnVal);
        return returnVal;
    }

}

