package edu.gemini.spModel.core;

import java.io.Serializable;
import java.text.ParseException;
import java.time.Instant;
import java.time.Month;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.gemini.shared.util.DateTimeUtils;

/**
 * An observing semester value object.  Contains a year and a letter, 'A' or
 * 'B' for the first and second semesters respectively.
 */
public final class Semester implements Comparable<Semester>, Serializable {

    /**
     * Enumeration of the two semester halves for each year.
     * The first half of the year is the 'A' semester, while
     * the second half of the year is the 'B' semester.
     */
    public enum Half {
        A(Month.FEBRUARY) {
            public Half opposite() { return B; }
            public int prev(int year) { return year-1; }
            public int next(int year) { return year;   }
        },
        B(Month.AUGUST) {
            public Half opposite() { return A; }
            public int prev(int year) { return year;   }
            public int next(int year) { return year+1; }
        },
        ;

        private final Month startMonth;

        Half(final Month startMonth) {
            this.startMonth = startMonth;
        }

        public Month getStartMonth() { return startMonth; }

        public abstract Half opposite();
        public abstract int next(int year);
        public abstract int prev(int year);

        /**
         * Gets the Semester Half that corresponds to the given Java
         * Month (which is one-based).
         *
         * @return Semester half that corresponds to the given month
         */
        public static Half forMonth(final Month month) {
            return (month.compareTo(A.startMonth) >= 0 && month.compareTo(B.startMonth) < 0) ? A : B;
        }
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
        // semester starts "early", i.e. at 14:00 (2pm) of "previous" day, add 10hrs to correct for this
        // example: 2009A starts on Jan-31-2009 at 14hrs; adding 10hrs brings us to Feb-1-2009.
        final ZonedDateTime zdt = Instant.ofEpochMilli(time).atZone(site.timezone().toZoneId()).plus(24 - DateTimeUtils.StartOfDayHour(), ChronoUnit.HOURS);
        final int dateYear   = zdt.getYear();
        final Month dateMnth = zdt.getMonth();

        // everything up to the start of the A semester belongs to the previous year! (i.e. all dates in January)
        year = dateMnth.compareTo(Half.A.getStartMonth()) >= 0 ? dateYear : dateYear - 1;
        half = Half.forMonth(dateMnth);
    }


    public int getYear() {
        return year;
    }

    public Half getHalf() {
        return half;
    }

    public Date getStartDate(Site site) {
        final ZonedDateTime zdt = ZonedDateTime.of(year, half.startMonth.getValue(), 1, DateTimeUtils.StartOfDayHour(), 0, 0, 0, site.timezone().toZoneId());
        return Date.from(zdt.minusDays(1).toInstant());
    }

    public Date getEndDate(Site site) {
        final ZonedDateTime zdt = getStartDate(site).toInstant().atZone(site.timezone().toZoneId());
        return Date.from(zdt.plusMonths(6).toInstant());
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
