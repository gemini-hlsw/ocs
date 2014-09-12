//
// $Id: ProgramGroupId.java 6709 2005-11-02 14:23:48Z shane $
//
package edu.gemini.dbTools.semesterStatus;

import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.core.Site;

import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


// TODO: probably something like this should be moved to the SPProgramID class
// TODO: itself.

/**
 * A ProgramGroup is used to group programs belonging to the same location
 * (GN or GS), semester (for example, 2004B), and kind (Q, C, DD, etc).
 */
final class ProgramGroupId implements Comparable<ProgramGroupId> {
    private static final Pattern PAT = Pattern.compile("^(G[NS])-(\\d\\d\\d\\d[AB])-([A-Z]+)-\\d+");

    /**
     * Parses a program id to create a ProgramGroup object.
     *
     * @throws ParseException if the program id is non-standard (and therefore
     * the location, semester, and kind cannot be determined)
     */
    public static ProgramGroupId parse(final SPProgramID progId)
            throws ParseException {
        return parse(progId.stringValue());
    }

    /**
     * Parses a program id to create a ProgramGroup object.
     *
     * @throws ParseException if the program id is non-standard (and therefore
     * the location, semester, and kind cannot be determined)
     */
    private static ProgramGroupId parse(final String progId)  throws ParseException {
        final Matcher m = PAT.matcher(progId);
        if (!m.matches()) {
            throw new ParseException("Nonstandard Id: " + progId, 0);
        }

        final Site location = Site.parse(m.group(1));
        final String semester = m.group(2);
        final String kind     = m.group(3);
        return new ProgramGroupId(location, semester, kind);
    }

    private final Site _site;
    private final String _semester;
    private final String _kind;

    /**
     * Constructs with all required information.
     *
     * @param location GN or GS
     * @param semester for example, 2004B
     * @param kind queue (Q), classical (C), etc.
     *
     * @throws NullPointerException if any parameter is <code>null</code>
     */
    private ProgramGroupId(final Site location, final String semester, final String kind) {
        if ((location == null) || (semester == null) || (kind == null)) {
            throw new NullPointerException();
        }

        _site = location;
        _semester = semester;
        _kind     = kind;
    }

    public Site getSite() {
        return _site;
    }

    public String getSemester() {
        return _semester;
    }

    public String getKind() {
        return _kind;
    }

    public boolean equals(final Object other) {
        if (other == null) return false;
        if (other.getClass() != this.getClass()) return false;

        final ProgramGroupId that = (ProgramGroupId) other;
        if (!_site.equals(that._site)) return false;
        if (!_semester.equals(that._semester)) return false;
        return _kind.equals(that._kind);

    }

    public int hashCode() {
        int res = _site.hashCode();
        res = 37*res + _semester.hashCode();
        res = 37*res + _kind.hashCode();
        return res;
    }

    public int compareTo(final ProgramGroupId that) {

        int res;

        res = _site.compareTo(that._site);
        if (res != 0) return res;

        res = _semester.compareTo(that._semester);
        if (res != 0) return res;

        res = _kind.compareTo(that._kind);
        if (res != 0) return res;

        return 0;
    }

    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append(_site).append("-");
        buf.append(_semester).append("-");
        buf.append(_kind);
        return buf.toString();
    }
}
