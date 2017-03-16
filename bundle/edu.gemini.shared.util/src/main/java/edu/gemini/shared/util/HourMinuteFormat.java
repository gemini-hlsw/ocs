package edu.gemini.shared.util;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HourMinuteFormat extends Format {
    private static final long MS_PER_MINUTE = 1000 * 60;
    private static final long MS_PER_HOUR = MS_PER_MINUTE * 60;
    private static final Pattern HH_MM_PAT = Pattern.compile("^(\\d+):(\\d{2})$");
    private static final Pattern HH_MM_SS_PAT = Pattern.compile("^(\\d+):(\\d{2}):(\\d{2})$");

    public static final HourMinuteFormat HH_MM    = new HourMinuteFormat(false);
    public static final HourMinuteFormat HH_MM_SS = new HourMinuteFormat(true);

    private final boolean showSeconds;

    private HourMinuteFormat(final boolean showSeconds) {
        this.showSeconds = showSeconds;
    }

    @Override
    public StringBuffer format(final Object obj, final StringBuffer toAppendTo, final FieldPosition pos) {

        final long ms = (Long) obj;
        final long hh = ms / MS_PER_HOUR;
        final long mm = (ms % MS_PER_HOUR) / MS_PER_MINUTE;

        final String s;
        if (showSeconds) {
            final long ss = (ms % MS_PER_MINUTE) / 1000;
            s = String.format("%02d:%02d:%02d", hh, mm, ss);
        } else {
            s = String.format("%02d:%02d", hh, mm);
        }

        return toAppendTo.append(s);
    }

    @Override
    public Object parseObject(final String source, final ParsePosition pos) {
        final Pattern p = showSeconds ? HH_MM_SS_PAT : HH_MM_PAT;
        final Matcher m = p.matcher(source);
        if (!m.matches()) {
            pos.setIndex(0);
            pos.setErrorIndex(0);
            return null;
        }

        final long hh = Long.parseLong(m.group(1));
        final long mm = Long.parseLong(m.group(2));
        final long ss = showSeconds ? Long.parseLong(m.group(3)) : 0;
        pos.setIndex(m.end());
        return hh * MS_PER_HOUR + mm * MS_PER_MINUTE + ss * 1000;
    }
}
