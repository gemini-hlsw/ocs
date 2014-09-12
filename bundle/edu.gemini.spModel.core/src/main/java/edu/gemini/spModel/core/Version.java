//
// $
//

package edu.gemini.spModel.core;

import java.io.Serializable;
import java.text.ParseException;
import java.util.Comparator;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The version of the SpModel as a whole.  The version is broken down into:
 * <pre>semester.xml_compatibility.serial_compatibility.minor_release</pre>
 * where the semester must be parse-able as a {@link Semester}, and the
 * remainder of the items are simple positive integers.  XML compatibility
 * means that external clients can fetch/store XML representations of
 * Science Programs if the semester and XML compatibility parts of two versions
 * are the same.  Serial compatibility of program files on disk requires, in
 * addition, that the serial_compatibility numbers match.  The final digit
 * may vary without breaking either XML or serial compatibility.
 */
public final class Version implements Comparable<Version>, Serializable {

    // *** IMPORTANT ... TRY TO KEEP THIS COMPATIBLE!  ***
    static final long serialVersionUID = 42L;

    private static final Logger LOG = Logger.getLogger(Version.class.getName());

    // The pattern for parsing an SpModel version number.
    private static final Pattern PAT = Pattern.compile("(\\d\\d\\d\\d[AB])(-test)?\\.(\\d+)\\.(\\d+)\\.(\\d+)");

    /** Current version. This is a hardcoded constant. */
    public static final Version current = CurrentVersion.get();
            // new Version(new Semester(2014, Semester.Half.B), true, 1, 6, 1);

    /**
     * An enumeration that lists the compatibility levels and provides
     * an easy way to {@link #check verify} that two versions are compatible.
     */
    public enum Compatibility implements Comparator<Version> {
        test() {
            public int compare(Version v1, Version v2) {
                int res = 0;
                if (v1.test != v2.test) {
                    res = v1.test ? 1 : 0;
                }
                return res;
            }
        },

        semester() {
            public int compare(Version v1, Version v2) {
                int res = test.compare(v1, v2);
                if (res != 0) return res;

                return v1.semester.compareTo(v2.semester);
            }
        },

        xml() {
            public int compare(Version v1, Version v2) {
                int res = semester.compare(v1, v2);
                if (res != 0) return res;

                return v1.xmlCompatibility - v2.xmlCompatibility;
            }
        },

        serial() {
            public int compare(Version v1, Version v2) {
                int res = xml.compare(v1, v2);
                if (res != 0) return res;

                return v1.serialCompatibility - v2.serialCompatibility;
            }
        },

        minor() {
            public int compare(Version v1, Version v2) {
                int res = serial.compare(v1, v2);
                if (res != 0) return res;

                return v1.minor - v2.minor;
            }
        },
        ;

        public boolean check(Version v1, Version v2) {
            return compare(v1, v2) == 0;
        }
    }


    /**
     * Parses a version string into a Version object, if possible.  Guaranteed
     * to work for the String returned by {@link #toString()}.
     *
     * @param versionString string representation of the Version
     *
     * @throws ParseException if the version string cannot be parsed
     */
    public static Version parse(String versionString) throws ParseException {

        Matcher m = PAT.matcher(versionString);
        if (!m.matches()) {
            throw new ParseException("Could not parse version: " + versionString, 0);
        }

        Semester semester = Semester.parse(m.group(1));
        String testStr = m.group(2);
        boolean test = "-test".equals(testStr);
        int xmlCompat = Integer.parseInt(m.group(3));
        int serCompat = Integer.parseInt(m.group(4));
        int minorCompat = Integer.parseInt(m.group(5));

        return new Version(semester, test, xmlCompat, serCompat, minorCompat);
    }

    private final Semester semester;
    private final boolean  test;
    private final int      xmlCompatibility;
    private final int      serialCompatibility;
    private final int      minor;

    /**
     * Constructs with all the required information (defaulting the test flag
     * to false). Two versions are typically compared for a particular
     * {@link Compatibility} level.  For example, to know whether two versions
     * are compatible at the XML externalization level, the client can check
     * using:
     *
     * <pre>Version.Compatibility.xml.check(v1, v2)</pre>
     *
     * or
     *
     * <pre>v1.isCompatible(v2, Version.Compatibility.xml)</pre>
     *
     * @param semester observing semester
     * @param xmlCompatibility XML compatibility version number
     * @param serialCompatibility serialization compatibility version number
     * @param minor minor compatibility version number
     */
    public Version(Semester semester, int xmlCompatibility, int serialCompatibility, int minor) {
        this(semester, false, xmlCompatibility, serialCompatibility, minor);
    }

    /**
     * Constructs with all the required information.  Two versions are typically
     * compared for a particular {@link Compatibility} level.  For example, to
     * know whether two versions are compatible at the XML externalization
     * level, the client can check using:
     *
     * <pre>Version.Compatibility.xml.check(v1, v2)</pre>
     *
     * or
     *
     * <pre>v1.isCompatible(v2, Version.Compatibility.xml)</pre>
     *
     * @param semester observing semester
     * @param test whether this is a test version
     * @param xmlCompatibility XML compatibility version number
     * @param serialCompatibility serialization compatibility version number
     * @param minor minor compatibility version number
     */
    public Version(Semester semester, boolean test, int xmlCompatibility, int serialCompatibility, int minor) {
        if (semester == null) throw new NullPointerException();
        if (xmlCompatibility < 0) throw new IllegalArgumentException("xmlCompatibility=" + xmlCompatibility);
        if (serialCompatibility < 0) throw new IllegalArgumentException("serialCompatibility=" + xmlCompatibility);
        if (minor < 0) throw new IllegalArgumentException("minor=" + minor);

        this.semester = semester;
        this.test = test;
        this.xmlCompatibility = xmlCompatibility;
        this.serialCompatibility = serialCompatibility;
        this.minor    = minor;
    }

    public Semester getSemester() {
        return semester;
    }

    public boolean isTest() {
        return test;
    }

    public int getXmlCompatibility() {
        return xmlCompatibility;
    }

    public int getSerialCompatibility() {
        return serialCompatibility;
    }

    public int getMinor() {
        return minor;
    }

    public boolean isCompatible(Version other, Compatibility level) {
        return level.check(this, other);
    }

    public int compareTo(Version o) {
        return Compatibility.minor.compare(this, o);
    }

    public Version setTest(boolean test) {
        return new Version(semester, test, xmlCompatibility, serialCompatibility, minor);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Version that = (Version) o;

        if (!this.semester.equals(that.semester)) return false;
        if (this.test != that.test) return false;
        if (this.xmlCompatibility != that.xmlCompatibility) return false;
        if (this.serialCompatibility != that.serialCompatibility) return false;
        return (this.minor == that.minor);
    }

    @Override
    public int hashCode() {
        int result = semester.hashCode();
        result = 31 * result + (test ? Boolean.TRUE.hashCode() : Boolean.FALSE.hashCode());
        result = 31 * result + xmlCompatibility;
        result = 31 * result + serialCompatibility;
        return 31 * result + minor;
    }

    @Override
    public String toString() {
        String testStr = test ? "-test" : "";
        return String.format("%s%s.%d.%d.%d", semester.toString(), testStr, xmlCompatibility, serialCompatibility, minor);
    }
}
