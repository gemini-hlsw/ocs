//
// $Id: GeminiFileName.java,v 1.1 2005/12/11 15:54:15 gillies Exp $
//

package edu.gemini.obslog.obslog;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 *
 */
final class GeminiFileName {

    private static final Pattern PAT = Pattern.compile("(([NS])(\\d\\d\\d\\d)(\\d\\d)(\\d\\d)([A-Z]))(\\d+)");

    enum Site {
        N("north"),
        S("south"),
        ;

        private String displayName;

        Site(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private String _fileName;
    private String _prefix = "";

    private int _year  = -1;
    private int _month = -1;
    private int _day   = -1;
    private Site _site = null;
    private int _sequenceNumber = -1;

    public GeminiFileName(String fileName) {
        _fileName = fileName;

        Matcher m = PAT.matcher(fileName);
        if (m.matches()) {
            _prefix = m.group(1);
            _site   = Site.valueOf(m.group(2));
            _year   = Integer.parseInt(m.group(3));
            _month  = Integer.parseInt(m.group(4));
            _day    = Integer.parseInt(m.group(5));
            _sequenceNumber = Integer.parseInt(m.group(7));
        }
    }

    public String getFileName() {
        return _fileName;
    }

    public String getPrefix() {
        return _prefix;
    }

    public int getYear() {
        return _year;
    }

    public int getMonth() {
        return _month;
    }

    public int getDay() {
        return _day;
    }

    public Site getSite() {
        return _site;
    }

    public int getSequenceNumber() {
        return _sequenceNumber;
    }


    public static void main(String[] args) {
        GeminiFileName fn;
        fn = new GeminiFileName("S20050114S0187");
        StringBuilder buf = new StringBuilder();

        buf.append("File Name.: ").append(fn.getFileName()).append("\n");
        buf.append("Prefix....: ").append(fn.getPrefix()).append("\n");
        buf.append("Site......: ").append(fn.getSite()).append("\n");
        buf.append("Year......: ").append(fn.getYear()).append("\n");
        buf.append("Month.....: ").append(fn.getMonth()).append("\n");
        buf.append("Day.......: ").append(fn.getDay()).append("\n");
        buf.append("Sequence..: ").append(fn.getSequenceNumber()).append("\n");

        System.out.println(buf.toString());

//        assert fn.getYear() == 2005;
//        assert fn.getMonth() == 1;
//        assert fn.getDay() == 14;
//        assert !fn.isNorth();
//        assert fn.getSequenceNumber() == 187;
    }
}
