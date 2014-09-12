//
// $
//

package edu.gemini.spModel.pio;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The version associated with a Container.
 */
public final class Version implements Comparable, Serializable {
    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d\\d\\d\\d)([AB])-(\\d)");

    public enum Semester {
        A,
        B,
    }

    public static Version match(String str) {
        Matcher m = VERSION_PATTERN.matcher(str);
        if (!m.matches()) return null;

        String yearStr     = m.group(1);
        String semesterStr = m.group(2);
        String countStr    = m.group(3);

        int year = Integer.parseInt(yearStr);
        Semester semester = Semester.valueOf(semesterStr);
        int count = Integer.parseInt(countStr);

        return new Version(year, semester, count);
    }

    private final int year;
    private final Semester semester;
    private final int count;

    private Version(int year, Semester semester, int count) {
        this.year     = year;
        this.semester = semester;
        this.count    = count;
    }

    public int getYear() {
        return year;
    }

    public Semester getSemester() {
        return semester;
    }

    public int getCount() {
        return count;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Version version = (Version) o;

        if (count != version.count) return false;
        if (year != version.year) return false;
        return semester == version.semester;

    }

    public int hashCode() {
        int result;
        result = year;
        result = 31 * result + (semester != null ? semester.hashCode() : 0);
        result = 31 * result + count;
        return result;
    }

    public int compareTo(Object other) {
        Version that = (Version) other;

        int res = year - that.year;
        if (res != 0) return res;
        res = semester.compareTo(that.semester);
        if (res != 0) return res;
        res = count - that.count;
        return res;
    }

    public String toString() {
        return String.format("%d%s-%d", year, semester.name(), count);
    }
}
