//
// $
//

package edu.gemini.skycalc;

import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A helper class used to parse an RA or Dec string into its constituent
 * parts, when expressed as HH:MM:SS (RA) or DD:MM:SS (Dec).  Fractional
 * seconds or arcsecs are supported as well as a leading + or - sign.
 */
final class RaDecParser {

    /**
     * The parts that make up an RA/Dec string.
     */
    public final class Result {
        private final int signum;
        private final int _1;
        private final int _2;
        private final int _3;
        private final Option<BigDecimal> _4;

        Result(Pattern pattern, String s) throws ParseException {
            Matcher mat = pattern.matcher(s);
            if (!mat.matches()) throw new ParseException(s, 0);

            String signStr  = mat.group(1);
            String str1     = mat.group(2);  // hours or degrees
            String str2     = mat.group(3);
            String str3     = mat.group(4);
            String str4     = mat.group(5);

            int tsignum = "-".equals(signStr) ? -1 : 1;

            _1 = Integer.parseInt(str1);
            _2 = Integer.parseInt(str2);
            _3 = Integer.parseInt(str3);

            if ((str4 != null) && !"".equals(str4)) {
                _4 = new Some<BigDecimal>(new BigDecimal(str4));
            } else {
                _4 = None.instance();
            }

            if ((_1 == 0) && (_2 == 0) && (_3 == 0)) {
                if (_4.isEmpty()) {
                    signum = 0;
                } else {
                    signum = BigDecimal.ZERO.compareTo(_4.getValue()) == 0 ?
                             0 : tsignum;
                }
            } else {
                signum = tsignum;
            }
        }

        /**
         * Gets the signum of the value, which is -1 if negative, 0 if zero, or
         * 1 if positive.
         */
        public int signum() { return signum; }

        /**
         * Gets the first part of the RA or dec, which is the hours (RA) or
         * degrees (Dec).
         */
        public int part1()  { return _1; }

        /**
         * Gets the second part of the RA or dec string, which will be minutes
         * of time (RA), or arcmins (dec).
         */
        public int part2()  { return _2; }

        /**
         * Gets the third part of the RA or dec string, which will be seconds
         * of time (RA), or arcsecs (dec).
         */
        public int part3()  { return _3; }

        /**
         * Gets the fractional part of seconds (RA) or arcsecs (dec), if any.
         *
         * @return fractional part of the RA/dec number, if any, wrapped in
         * a {@link Some} instance; {@link None} otherwise
         */
        public Option<BigDecimal> part4() { return _4; }
    }

    /**
     * A template pattern for parsing RA/dec strings.  The separator string
     * must be supplied to produce a template that may be used for parsing.
     * The separator string cannot be numeric, or it can be confused with the
     * RA/dec values.
     */
    public static final String PATTERN_TEMPLATE = "([+-])?(\\d+)%s(\\d\\d?)%s(\\d\\d?)(\\.\\d*)?";

    /**
     * The standard RA/dec parser where the fields are separated by a :
     * character.
     */
    public static final RaDecParser STANDARD_PARSER = new RaDecParser(":");

    private final Pattern pattern;

    /**
     * Constructs the parser with the separator string to be used.
     *
     * @param separator string used to separate fields of the RA/dec
     *
     * @throws IllegalArgumentException if the separator character is null or
     * empty or starts or ends with a digit
     */
    RaDecParser(String separator) throws IllegalArgumentException {
        if ((separator == null) || "".equals(separator)) {
            throw new IllegalArgumentException("separator character must be specified");
        }
        if (Character.isDigit(separator.charAt(0)) ||
            Character.isDigit(separator.charAt(separator.length()-1))) {
            throw new IllegalArgumentException("separator cannot start or end with a digit");
        }
        String quoted     = Pattern.quote(separator);
        String patternStr = String.format(PATTERN_TEMPLATE, quoted, quoted);
        pattern = Pattern.compile(patternStr);
    }

    /**
     * Parses the RA/dec string into its constituent parts.
     *
     * @param s RA or dec string to parse
     *
     * @return the constituent parts of the RA or dec
     *
     * @throws ParseException if the string cannot be parsed
     */
    public Result parse(String s) throws ParseException {
        return new Result(pattern, s);
    }
}
