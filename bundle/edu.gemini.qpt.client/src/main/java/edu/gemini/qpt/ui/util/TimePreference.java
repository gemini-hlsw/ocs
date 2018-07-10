package edu.gemini.qpt.ui.util;

import edu.gemini.qpt.core.util.ImprovedSkyCalc;
import edu.gemini.spModel.core.Site;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public enum TimePreference {

    LOCAL,
    UNIVERSAL,
    SIDEREAL
    ;

    public static EnumBox<TimePreference> BOX = new EnumBox<TimePreference>(LOCAL);

    public String format(Site site, Long timestamp, String pattern) {
        final String timeString;
        final DateTimeFormatter f = DateTimeFormatter.ofPattern(pattern);
        switch (this) {
            case LOCAL:
                timeString = f.withZone(site.timezone().toZoneId()).format(Instant.ofEpochMilli(timestamp));
                break;
            case UNIVERSAL:
                timeString = f.withZone(ZoneId.of("UTC")).format(Instant.ofEpochMilli(timestamp));
                break;
            // SIDEREAL
            default:
                final ImprovedSkyCalc calc = new ImprovedSkyCalc(site);
                final Long timestamp2 = calc.getLst(new Date(timestamp)).getTime();
                timeString = f.withZone(ZoneId.of("UTC")).format(Instant.ofEpochMilli(timestamp2));
                break;
        }
        return timeString;
    }

}
