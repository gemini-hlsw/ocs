package edu.gemini.horizons.server.backend;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jsky.coords.DMS;
import jsky.coords.HMS;
import jsky.coords.WorldCoords;
import edu.gemini.horizons.api.EphemerisEntry;
import edu.gemini.horizons.api.HorizonsException;
import edu.gemini.horizons.api.HorizonsReply;
import edu.gemini.horizons.api.OrbitalElements;
import edu.gemini.horizons.api.ResultsTable;

//$Id: CgiReplyBuilder.java 895 2007-07-24 20:18:09Z anunez $
/**
 * This class builds a {@link HorizonsReply} based on the results of a
 * CGI query done to the JPL Horizons service
 */
public class CgiReplyBuilder {

	private static final Logger LOGGER = Logger.getLogger(CgiReplyBuilder.class.getName());

	/**
	 * Format a date and time, with hours in format HH:mm:ss.SSS (has milliseconds)
	 */
    private static final DateFormat DATE_FORMATTER_WITH_MS = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss.SSS");

    /**
     * Format a date and time, with hours in format HH:mm:ss (has seconds)
     */
    private static final DateFormat DATE_FORMATTER_WITH_SECONDS = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss");

    /**
     * Format a date and time, with hours in format HH:mm (no seconds)
     */
    private static final DateFormat DATE_FORMATTER_NO_SECONDS = new SimpleDateFormat("yyyy-MMM-dd HH:mm");

    static {
        final TimeZone tz = TimeZone.getTimeZone("UTC");
        DATE_FORMATTER_WITH_MS.setTimeZone(tz);
        DATE_FORMATTER_WITH_SECONDS.setTimeZone(tz);
        DATE_FORMATTER_NO_SECONDS.setTimeZone(tz);
    }

    private static DateFormat[] FORMATS = {
            DATE_FORMATTER_WITH_MS,
            DATE_FORMATTER_WITH_SECONDS,
            DATE_FORMATTER_NO_SECONDS
    };

    private static synchronized Date parseDate(String strDate) throws HorizonsException {
        if (strDate == null) {
            throw new HorizonsException(HorizonsException.Type.UNEXPECTED_RESULT, "Unable to parse null date.");
        }
        for (DateFormat df : FORMATS) {
            try {
                return df.parse(strDate);
            } catch (ParseException ex) {
                // ignore
            }
        }
        throw new HorizonsException(HorizonsException.Type.UNEXPECTED_RESULT, "Unable to parse date " + strDate);
    }

    /**
     * Used to get the ephemeris block. The ephemeris block is delimited by
     * the strings <code>$SOE</code> and <code>$EOE</code>
     */
    private static Pattern ephemerisBlockPattern = Pattern.compile("\\$\\$SOE(.+)\\$\\$EOE", Pattern.DOTALL);
    /**
     * Gets a row in the ephemeris data. A row contains the date where the
     * ephemeris is valid, plus the ra, dec, ra and dec track, airmass and visual magnitud values
     */
    // RCN: I cheated a little and made the time portion be any combination of numbers, colons, and dots.
    // Since we're passing it to a dateformat and catching the parseexception later, I think this is good enough.
    private static Pattern ephemerisRowPattern = Pattern.compile("(\\d{4}-\\w{3}-\\d{2}\\s+\\.*?[\\d\\.:]+).*?(\\d.*)");
//  private static Pattern ephemerisRowPattern = Pattern.compile("(\\d{4}-\\w{3}-\\d{2}\\s+\\.*?\\d{1,}:\\d{2}:{0,1}\\d{0,2}).*?(\\d.*)");

    /**
     * Pattern used to get name-value pairs of the form NAME= VALUE
     */
    private static Pattern nameValuePattern = Pattern.compile("(\\w+)\\s*=\\s+(-{0,1}\\d*\\.\\d*(E(-|\\+)\\d+)?)", Pattern.DOTALL);

    /**
     * First of two possible patterns for the object id, which will be a parseable Long in group(2).
     * I was unable to OR these together successfully. Probably something dumb.
     */
    // Revised: Jun 27, 2007   Pioneer 6 Spacecraft (interplanetary) / (Sun)      -6
    private static Pattern uidPattern1 = Pattern.compile("^\\s*(Revised:.*?(-?\\d+)\\s*$)", Pattern.MULTILINE);

    /**
     * Second of two possible patterns for the object id, which will be a parseable Long in group(2).
     * I was unable to OR these together successfully. Probably something dumb.
     */
    // Rec #:900033 (+COV)   Soln.date: 2001-Aug-02_13:51:39   # obs: 7428 (1835-1994)
    private static Pattern uidPattern2 = Pattern.compile("^\\s*(Rec #:\\s*(-?\\d+))", Pattern.MULTILINE);

    /**
     * Used to get text from blocks surrounded with ********* as in
     * <code>
     * **************************************************************<br>
     * Some text here<br>
     * **************************************************************<br>
     * </code>
     * The text inside the stars is in the group 1 in the regular expression
     */
    private static Pattern blockPattern = Pattern.compile("\\*+$(.+?)\\*+$", Pattern.DOTALL | Pattern.MULTILINE);

    /**
     * Builds an <code>HorizonsReply</code> based on the results of a CGI query streamed
     * from the <code>stream</code> argument. The <code>charset</code> defines the
     * Charset the <code>stream</code> is using.
     *
     * @param stream  The <code>InputStream</code> from where the CGI reply is being read from
     * @param charset The <code>Charset</code> used by the <code>stream</code>
     * @return The <code>HorizonsReply</code> object containing the results from the CGI query
     * @throws HorizonsException if a problem is detected while processing the CGI result
     */
    public static HorizonsReply buildResponse(InputStream stream, String charset) throws HorizonsException {
        HorizonsReply reply = new HorizonsReply();
        try {
            StringBuffer responseBuffer = _readFully(new InputStreamReader(stream, charset));
            //parse the buffer to get the real data...
            _processHeader(responseBuffer, reply);
            switch (reply.getReplyType()) {
                case COMET:
                case MINOR_OBJECT:
                case MAJOR_PLANET:
                case SPACECRAFT:
                    _buildEphemeris(responseBuffer, reply);
                default:
                    break;
            }
        } catch (IOException e) {
            throw HorizonsException.create("Exception reading data from Server", e);
        }
        return reply;
    }

    /**
     * Builds a <code>HorizonsReply</code> of the given type, assuming the
     * stream contains valid ephemeris data.  This method is like buildResponse
     * but it skips the header parsing and uses the given reply type directly.
     *
     * @return results from the CGI query
     */
    public static HorizonsReply readEphemeris(InputStream stream, HorizonsReply.ReplyType replyType, String charset) throws HorizonsException {
        final HorizonsReply reply = new HorizonsReply();
        reply.setReplyType(replyType);

        try {
            _buildEphemeris(_readFully(new InputStreamReader(stream, charset)), reply);
        } catch (IOException ex) {
            throw HorizonsException.create("Exception reading data from Server", ex);
        }

        return reply;
    }

    /**
     * Process the header in the CGI reply. The header defines the content type of the
     * reply, mostly
     *
     * @param buf   Buffer with the reply
     * @param reply the HorizonsReply object being built
     */
    private static void _processHeader(StringBuffer buf, HorizonsReply reply) throws HorizonsException {

        Matcher matcher = blockPattern.matcher(buf);
        if (matcher.find()) {
            //we have a block, identify the type, and based on that, continue the processing
            String block = matcher.group(1);

            // RCN: Try to pull out the object id. This may not be the right place to do it.
            Matcher m;
            if ((m = uidPattern1.matcher(block)).find() || (m = uidPattern2.matcher(block)).find()) {
            	try {
            		reply.setObjectId(Long.valueOf(m.group(2)));
            	} catch (NumberFormatException nfe) {
            		// will never happen unless someone messes with the regex above
            		LOGGER.warning(m.group(2) + " is not a number. Someone broke the regex :-(");
            	}
            }

            if (block.contains(CgiHorizonsConstants.ORBITAL_ELEMENTS_KEYWORD)) {
                //we have orbital elements in the first block, parse them
                _getOrbitalElements(block, reply);
            } else if (block.contains(CgiHorizonsConstants.SPACECRAFT_KEYWORD)) {
                //This is a spacecraft, try to get ephemeris
                reply.setReplyType(HorizonsReply.ReplyType.SPACECRAFT);
            } else if (block.contains(CgiHorizonsConstants.MAJOR_PLANET_KEYWORD) ||
                    block.contains(CgiHorizonsConstants.MAJOR_PLANET_KEYWORD_2)) {
                //This is a Major Planet, won't have orbital elements
                //Mark the reply type, and continue (will try to get ephemeris here)
                reply.setReplyType(HorizonsReply.ReplyType.MAJOR_PLANET);
            } else if (block.contains(CgiHorizonsConstants.MULTIPLE_MAJOR_BODIES_KEYWORD) ||
                    block.contains(CgiHorizonsConstants.MULTIPLE_MINOR_BODIES_KEYWORD)) {
                //This is a multiple answer reply, for major bodies
                if (block.contains(CgiHorizonsConstants.NO_RESULTS_KEYWORD)) {
                    reply.setReplyType(HorizonsReply.ReplyType.NO_RESULTS);
                } else {
                    reply.setReplyType(HorizonsReply.ReplyType.MUTLIPLE_ANSWER);
                    _getMultipleAnswer(block, reply);
                }
            } else if (block.contains(CgiHorizonsConstants.SPK_EPHEMERIS)) {
                 //SPK-Ephemeris received. Assuming only comets produce that.
                 reply.setReplyType(HorizonsReply.ReplyType.COMET);
            } else {
                throw new HorizonsException(HorizonsException.Type.UNEXPECTED_RESULT,
                        "Can't identify the result output");
            }
        } else {
            //no blocks were found, this is an invalid query sent to the server
            reply.setReplyType(HorizonsReply.ReplyType.INVALID_QUERY);
        }
    }

    /**
     * Process the multiple answers results from the CGI query
     *
     * @param buf   String with the multiple answer block
     * @param reply the <code>HorizonsReply</code> being built
     */
    private static void _getMultipleAnswer(String buf, HorizonsReply reply) throws HorizonsException {
        BufferedReader reader = new BufferedReader(new StringReader(buf));
        ResultsTable table = new ResultsTable();
        String line, previous = null;
        try {
            String header = null;
            line = reader.readLine();
            Vector<Integer> sizes = new Vector<Integer>();
            Vector<Integer> positions = new Vector<Integer>();
            while (line != null) {
                if (line.contains("-----")) {
                    //previous line contained the header
                    header = previous;
                    String[] elements = line.trim().split("\\s+");
                    int spaces;
                    int index = 0;
                    int pos = 0;

                    //really complicated way to figure out the positions in
                    //the strings where the headers Id's are located
                    //The problem is that headers and data aren't separated by tabs, but
                    //rather by spaces; so the only chance to get reliable data is from the
                    //actual positions where the "-----" signs are located. The weak assumption
                    //here is that at least the data will be located under or above the "----"
                    //lines
                    for (String e : elements) {
                        sizes.add(e.length());
                        spaces = line.indexOf(e) - index;
                        index = line.indexOf(e);
                        line = line.replaceFirst(e, "");
                        pos += spaces;
                        positions.add(pos);
                        pos += e.length();
                    }
                    break;
                }
                previous = line;
                line = reader.readLine();
            }
            //build the header row
            if (header != null) {
                Vector<String> headerVector = new Vector<String>();
                for (int i = 0; i < positions.size(); i++) {
                    int start = positions.get(i);
                    int end = start + sizes.get(i) >= header.length() ? header.length() : start + sizes.get(i);
                    headerVector.add(header.substring(start, end).trim());
                }
                table.setHeader(headerVector);
//            } else {
                //damn, no header
            }
            //Now, get the actual data
            line = reader.readLine();
            while (line != null && !"".equals(line.trim())) {
                Vector<String> row = new Vector<String>();
                for (int i = 0; i < positions.size(); i++) {
                    int start = positions.get(i);
                    int end = start + sizes.get(i) >= line.length() ? line.length() : start + sizes.get(i);
                    row.add(line.substring(start, end).trim());
                }
                table.addResult(row);
                line = reader.readLine();
            }
        } catch (IOException e) {
            //shouldn't happen
            throw HorizonsException.create(HorizonsException.Type.UNEXPECTED_RESULT, e);
        }
        reply.setResultsTable(table);
    }

    /**
     * Get the orbital elements from the CGI reply
     *
     * @param buf   String with the block that contains the orbital elements
     * @param reply the <code>HorizonsReply</code> being built
     */
    private static void _getOrbitalElements(String buf, HorizonsReply reply) throws HorizonsException {

        Matcher matcher = nameValuePattern.matcher(buf);
        OrbitalElements orbElements = new OrbitalElements();
        while (matcher.find()) {
            double val = Double.parseDouble(matcher.group(2));
            orbElements.addElement(matcher.group(1).toUpperCase(), val);
        }
        if (orbElements.getKeys().size() > 0) {

            if (buf.contains(CgiHorizonsConstants.COMET_KEYWORD)) {
                reply.setReplyType(HorizonsReply.ReplyType.COMET);
            } else {
                reply.setReplyType(HorizonsReply.ReplyType.MINOR_OBJECT);
            }
            reply.setOrbitalElements(orbElements);
        } else {
            //we couldn't get any orbital elements here.. shouldn't happen
            throw new HorizonsException(HorizonsException.Type.UNEXPECTED_RESULT, "Couldn't get any orbital element");
        }
    }

    /**
     * Auxiliary method to read the whole content of a <code>Reader</code> object
     * into a <code>StringBuffer</code>
     *
     * @param input The <code>Reader</code> from where to get the data
     * @return a <code>StringBuffer</code> with the data gotten from the <code>Reader</code>
     * @throws IOException if a problem happens reading from the <code>Reader</code>
     */
    private static StringBuffer _readFully(Reader input) throws IOException {
        BufferedReader br = input instanceof BufferedReader
                ? (BufferedReader) input
                : new BufferedReader(input);
        StringBuffer result = new StringBuffer();
        char[] buffer = new char[4 * 1024];
        int charsRead;
        while ((charsRead = br.read(buffer)) != -1) {
            result.append(buffer, 0, charsRead);
        }
        return result;
    }

    /**
     * Build the ephemeris data (if any) from the cgi reply
     *
     * @param buffer with the CGI reply
     * @param reply  <code>HorizonsReply</code> object being built
     * @throws HorizonsException if a problem is found while
     *                           trying to get the ephemeris
     */
    private static void _buildEphemeris(StringBuffer buffer, HorizonsReply reply) throws HorizonsException {
        Matcher matcher = ephemerisBlockPattern.matcher(buffer);
        if (matcher.find()) {
            //get the ephemeris block, and scan it, line by line
            Scanner scanner = new Scanner(matcher.group(1));
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (!"".equals(line.trim())) {
                    //analyzes the row, based on the row pattern
                    matcher = ephemerisRowPattern.matcher(line);
                    if (matcher.find()) {
                        _buildRow(matcher.group(1), matcher.group(2), reply);
                    }
                }
            }
//        } else {
            //no ephemeris found...
        }
    }

    /**
     * Builds an ephemeris row in the <code>HorizonsReply</code> object
     *
     * @param strDate String with the date of the ephemeris entry
     * @param row     String with the data for the ephemeris entry associated to the
     *                <code>strDate</code> date
     * @param reply   <code>HorizonsReply</code> object being built
     * @throws HorizonsException if there is a problem building the entry
     */
    private static void _buildRow(String strDate, String row, HorizonsReply reply) throws HorizonsException {
        final Date date = parseDate(strDate);

//        if (row.matches("^\\d{3} .*")) row = row.substring(8);
//        System.out.println(row);


        Scanner scanner = new Scanner(row);
        WorldCoords wc = new WorldCoords(
        		new HMS(scanner.nextDouble(), scanner.nextInt(), scanner.nextDouble()),
                new DMS(scanner.nextDouble(), scanner.nextInt(), scanner.nextDouble()));

        double raTrack = scanner.nextDouble();
        double decTrack = scanner.nextDouble();
        double airmass;
        String strAirmass;
        try {
            strAirmass = scanner.next();
        } catch (NoSuchElementException ex) {
            throw HorizonsException.create(HorizonsException.Type.UNEXPECTED_RESULT, "Uncomplete ephemeris row data", ex);
        }
        try {
            airmass = Double.parseDouble(strAirmass);
        } catch (NumberFormatException ex) {
            airmass = -1;
        }

        // Next quantity is extinction (which we get for free along with airmass) but we don't
        // care about it so we skip it.
        scanner.next();

        double magnitude;
        String strMag;
        try {
            strMag = scanner.next();
        } catch (NoSuchElementException ex) {
            throw HorizonsException.create(HorizonsException.Type.UNEXPECTED_RESULT, "Uncomplete ephemeris row data", ex);
        }
        try {
            magnitude = Double.parseDouble(strMag);
        } catch (NumberFormatException ex) {
            magnitude = -1;
        }
        EphemerisEntry entry = new EphemerisEntry(date, wc, raTrack, decTrack, airmass, magnitude);
        reply.addEphemerisEntry(entry);
    }

    public static void main(String[] args) {




//        //block extraction
//        StringBuffer sb = new StringBuffer("$$SOE");
//        sb.append("\n");
//        sb.append("This is just a test");
//        sb.append("\n");
//        sb.append("$$EOE");
//        sb.append("More crap");
//        Matcher matcher = ephemerisBlockPattern.matcher(sb);
//        if (matcher.find()) {
//            System.out.println("Match found");
//            System.out.println(matcher.group(1));
//        }
//        // row analysis
//        matcher = ephemerisRowPattern.matcher("   2006-Oct-19 16:16   21 18 58.04 -28 07 17.0    11.19     11.16   n.a.   8.73   7.00");
//        if (matcher.find()) {
//            System.out.println("Date Match found");
//            System.out.println(matcher.group(1));
//            System.out.println("Rest");
//            System.out.println(matcher.group(2));
//            Scanner scanner = new Scanner(matcher.group(2));
//            int hh = scanner.nextInt();
//            int mm = scanner.nextInt();
//            double ss = scanner.nextDouble();
//            System.out.println(hh + ":" + mm + ":" + ss);
//            int dd = scanner.nextInt();
//            mm = scanner.nextInt();
//            ss = scanner.nextDouble();
//            System.out.println(dd + ":" + mm + ":" + ss);
//        }
//
//        //orbital elements
//
//
//        sb = new StringBuffer("Some crap ").append("\n");
//        sb.append("EPOCH= 244242.5 ! 1928-22-11.00 CT Residual RMS= .3914").append("\n");
//        sb.append("EC= .0777373 QR= 2.22223234 TP= 223.1010").append("\n");
//        sb.append("PER= 4.334    MA= .213344    ANGMON= 33.883").append("\n");
//        sb.append("A= 3.33354 B= 222. CD= 153.222").append("\n");
//        sb.append("RES=12.2           TT= -33.03").append("\n").append("\n");
//        sb.append("Physical parameters and other crap").append("\n");
//        matcher = nameValuePattern.matcher(sb);
//        while (matcher.find()) {
//
//            double val = Double.parseDouble(matcher.group(2));
//            System.out.println("ID = " + matcher.group(1) + " Value = " + val);
//        }
//
//
//        sb = new StringBuffer("Some crap ").append("\n");
//        sb.append("**********************************").append("\n");
//        sb.append("Content of the crap").append("\n");
//        sb.append("more crap").append("\n");
//        sb.append("**********************************").append("\n");
//        matcher = blockPattern.matcher(sb);
//        if (matcher.find()) {
//            System.out.println("Content :" + matcher.group(1));
//        }

    }


}
