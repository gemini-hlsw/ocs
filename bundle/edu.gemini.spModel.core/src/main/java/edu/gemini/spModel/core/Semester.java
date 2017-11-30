package edu.gemini.spModel.core;

import java.io.Serializable;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An observing semester value object.  Contains a year and a letter, 'A' or
 * 'B' for the first and second semesters respectively.
 */
public final class Semester implements Comparable<Semester>, Serializable {

    /** Hour of the day we switch from one night/date to the next. */
    public static final int SWITCH_OVER_HOUR = 14;

    /**
     * Enumeration of the two semester halves for each year.
     * The first half of the year is the 'A' semester, while
     * the second half of the year is the 'B' semester.
     */
    public enum Half {
        A(Calendar.FEBRUARY) {
            public Half opposite() { return B; }
            public int prev(int year) { return year-1; }
            public int next(int year) { return year;   }
        },
        B(Calendar.AUGUST) {
            public Half opposite() { return A; }
            public int prev(int year) { return year;   }
            public int next(int year) { return year+1; }
        },
        ;

        private final int startMonth;

        Half(int startMonth) {
            this.startMonth = startMonth;
        }

        public int getStartMonth() { return startMonth; }

        public abstract Half opposite();
        public abstract int next(int year);
        public abstract int prev(int year);

        /**
         * Gets the Semester Half that corresponds to the given Java
         * calendar month (which is zero based).
         *
         * @param javaCalendarMonth zero-based month
         * (January = 0, December = 11); use Calendar.JANUARY, etc.
         *
         * @return Semester half that corresponds to the given month
         */
        public static Half forMonth(int javaCalendarMonth) {
            return (javaCalendarMonth >= A.startMonth) &&
                   (javaCalendarMonth  < B.startMonth) ? A : B;
        }
    }

    private static Calendar mkCalendar(Site site) {
        Calendar cal = new GregorianCalendar(site.timezone());
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    private static final Pattern SEMESTER_PATTERN = Pattern.compile("(\\d\\d\\d\\d)-?([AB])");

    /**
     * Parses a Semester from a string of the form \\d\\d\\d\\d[AB].  For
     * example, 2008B or 2009A.
     *
     * @param semesterString contains the string representation of the semester
     *
     * @return Semester object corresponding to the given string
     *
     * @throws ParseException if the <code>semesterString</code> cannot be
     * parsed
     */
    public static Semester parse(String semesterString) throws ParseException {
        Matcher m = SEMESTER_PATTERN.matcher(semesterString);
        if (!m.matches()) {
            throw new ParseException("Could not parse semester: " + semesterString, 0);
        }

        String yearStr = m.group(1);
        String nameStr = m.group(2);

        int year = Integer.parseInt(yearStr);
        Half half = Half.valueOf(nameStr);
        return new Semester(year, half);
    }

    private final int year;
    private final Half half;

    public Semester(int year, Half half) {
        if (year < 0) throw new IllegalArgumentException("negative year");
        if (half == null) throw new NullPointerException("half is null");

        this.year = year;
        this.half = half;
    }

    /**
     * Constructs a semster that corresponds to the current date.
     */
    public Semester(Site site) {
        this(site, new Date());
    }

    /**
     * Constructs the semester in which the given date falls.
     *
     * @param time an instant contained in the Semester to construct
     */
    public Semester(Site site, Date time) {
        this(site, time.getTime());
    }

    public Semester(Site site, long time) {
        Calendar cal = mkCalendar(site);
        cal.setTimeInMillis(time);
        // semester starts "early", i.e. at 14:00 (2pm) of "previous" day, add 10hrs to correct for this
        // example: 2009A starts on Jan-31-2009 at 14hrs; adding 10hrs brings us to Feb-1-2009.
        cal.add(Calendar.HOUR_OF_DAY, 24 - SWITCH_OVER_HOUR);
        int dateYear = cal.get(Calendar.YEAR);
        int dateMnth = cal.get(Calendar.MONTH);

        // everything up to the start of the A semester belongs to the previous year! (i.e. all dates in January)
        year = dateMnth >= Half.A.getStartMonth() ? dateYear : dateYear - 1;
        half = Half.forMonth(dateMnth);
    }


    public int getYear() {
        return year;
    }

    public Half getHalf() {
        return half;
    }

    public Date getStartDate(Site site) {
        Calendar cal = mkCalendar(site);
        cal.set(year, half.startMonth, 1, SWITCH_OVER_HOUR, 0, 0);
        cal.add(Calendar.DAY_OF_MONTH, -1);
        return cal.getTime();
    }

    public Date getEndDate(Site site) {
        Calendar cal = mkCalendar(site);
        cal.setTime(getStartDate(site));
        cal.add(Calendar.MONTH, 6);
        return cal.getTime();
    }

    public Semester prev() {
        return new Semester(half.prev(year), half.opposite());
    }

    public Semester next() {
        return new Semester(half.next(year), half.opposite());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Semester semester = (Semester) o;

        if (year != semester.year) return false;
        return (half == semester.half);
    }

    @Override
    public int hashCode() {
        int result;
        result = year;
        result = 31 * result + (half != null ? half.hashCode() : 0);
        return result;
    }

    public int compareTo(Semester that) {
        int res = year - that.year;
        if (res != 0) return res;
        return half.compareTo(that.half);
    }

    /**
     * Returns a formatted semester suitable for parsing with
     * the {@link #parse(String)} method.
     */
    @Override
    public String toString() {
        return String.format("%d%s", year, half.name());
    }

    /**
     * Returns the last two digits of the year plus the semester half.
     * For example, "14B" for 2014B.
     */
    public String toShortString() {
        final String full = toString();
        final int len = full.length();
        return (len <= 3) ? full : full.substring(len - 3, len);
    }
}
