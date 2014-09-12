//
// $Id: SPProgramID.java 7517 2007-01-02 13:25:27Z shane $
//

package edu.gemini.spModel.core;

import java.io.Serializable;
import java.util.regex.Pattern;


/**
 * A class that represents a science program id.
 */
public final class SPProgramID implements Serializable, Comparable<SPProgramID> {
    private static final long serialVersionUID = 5760278176288957899L;

    private static final Pattern PAT = Pattern.compile("[a-zA-Z0-9\\-]*");

    /** Null-safe equals comparison */
    public static boolean same(SPProgramID id0, SPProgramID id1) {
        return (id0 == null) ? id1 == null : id0.equals(id1);
    }

    /**
     * Obtains the SPProgramID corresponding to the given id string.  The id
     * must consist of only alphanumeric and hyphen '-' characters.
     *
     * @throws edu.gemini.spModel.core.SPBadIDException if the program id is not valid
     */
    public static SPProgramID toProgramID(String idStr) throws SPBadIDException {
        if (idStr == null) throw new NullPointerException();
        if (!PAT.matcher(idStr).matches()) {
            throw new SPBadIDException("Program id contains an unsupported character: " + idStr);
        }
        return new SPProgramID(idStr);
    }

    private final String _programID;

    private SPProgramID(String programID) {
        _programID = programID;
    }

    /**
     * Gets the site associated with this program id based upon its site
     * prefix (GS or GN), if any.
     *
     * @return site for this program, or <code>null</code> if not known
     */
    public Site site() {
        if (_programID.startsWith("GN-")) return Site.GN;
        else if (_programID.startsWith("GS-")) return Site.GS;
        else return null;
    }

    /** Returns true if this is a classical program. */
    public Boolean isClassical() {
        return _programID.contains("-C-");
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof SPProgramID)) return false;
        SPProgramID that = (SPProgramID) obj;
        return this._programID.equals(that._programID);
    }

    public int hashCode() {
        return _programID.hashCode();
    }

    /**
     * Gets the science program id as a string.
     */
    public String stringValue() {
        return _programID;
    }

    /**
     * Returns the science program id as a string, or an empty string if
     * isDefault() returns true (since the default id should not normally
     * be displayed).
     */
    public String toString() {
        return _programID;
    }


    /***
     * A simple helper class for the compareTo() method.
     * Contains information about a part of an id.
     */
    private static class CompareInfo {
        int nextPartStart;
        String part;
        int partInt;
    }

    /**
     * Implement the Comparable interface.
     */
    public int compareTo(SPProgramID that) {
        String id1 = _programID;
        int id1Length = id1.length();
        String id2 = that._programID;
        int id2Length = id2.length();

        CompareInfo ci = new CompareInfo();
        int id1Start = 0;
        int id2Start = 0;

        int res;
        while ((id1Start >= 0) && (id2Start >= 0)) {
            // Get information about the next part of id1
            _getNextPart(id1, id1Length, id1Start, ci);
            id1Start = ci.nextPartStart;
            String part1 = ci.part;
            int part1Int = ci.partInt;

            // Get information about the next part of id2
            _getNextPart(id2, id2Length, id2Start, ci);
            id2Start = ci.nextPartStart;
            String part2 = ci.part;
            int part2Int = ci.partInt;

            // Compare the parts as integers.
            if ((part1Int >= 0) && (part2Int >= 0)) {
                res = part1Int - part2Int;
                if (res != 0) return res;
            }

            // Either they weren't integers or were the same integer.
            // Compare as strings.
            res = part1.compareTo(part2);
            if (res != 0) return res;
        }

        // Handle the case where one id has fewer parts than the other
        // but they match up till the end.  For example:
        // GN-2003B-Q-2 and GN-2003B-Q-2-copy
        return id1Start - id2Start;
    }


    private void _getNextPart(String id, int idLength, int start, CompareInfo compareInfo) {
        if (start >= idLength) {
            compareInfo.nextPartStart = -1;
            compareInfo.part = "";
            compareInfo.partInt = -1;
            return;
        }

        int dashPos = id.indexOf('-', start);

        String partStr;
        int partStrLength;
        if (dashPos == -1) {
            compareInfo.nextPartStart = -1;
            partStr = id.substring(start);
            partStrLength = idLength - start;
        } else {
            compareInfo.nextPartStart = dashPos + 1;
            partStr = id.substring(start, dashPos);
            partStrLength = dashPos - start;
        }

        compareInfo.part = partStr;
        compareInfo.partInt = -1;
        // Note: Tests showed that the NumberFormatException below was thrown often, leading to a
        // performance problem, so try to improve performance by avoiding it (allan).
        if (partStrLength > 0 && Character.isDigit(partStr.charAt(0))) {
            if (partStrLength == 1 || Character.isDigit(partStr.charAt(partStrLength - 1))) {
                try {
                    compareInfo.partInt = Integer.parseInt(partStr, 10);
                } catch (NumberFormatException ex) {
                    // ignore, this wasn't a number
                }
            }
        }
    }
}
