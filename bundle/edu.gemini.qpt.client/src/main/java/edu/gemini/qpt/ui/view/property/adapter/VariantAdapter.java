package edu.gemini.qpt.ui.view.property.adapter;

import edu.gemini.qpt.core.Variant;
import edu.gemini.qpt.ui.view.property.PropertyTable;
import edu.gemini.qpt.ui.view.property.PropertyTable.Adapter;
import edu.gemini.skycalc.TwilightBoundType;
import edu.gemini.skycalc.TwilightBoundedNight;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

public class VariantAdapter implements Adapter<Variant> {

    public void setProperties(Variant variant, Variant target, PropertyTable table) {

        final TimeZone zone = target.getSchedule().getSite().timezone();

        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MMM-dd HH:mm")
                .withZone(zone.toZoneId());

        TwilightBoundedNight night = new TwilightBoundedNight(
                TwilightBoundType.OFFICIAL,
                target.getSchedule().getStart(),
                target.getSchedule().getSite()
        );

        TwilightBoundedNight nautical = new TwilightBoundedNight(
                TwilightBoundType.NAUTICAL,
                target.getSchedule().getStart(),
                target.getSchedule().getSite()
        );

        table.put(PROP_TYPE, "Plan Variant");
        table.put(PROP_TITLE, target.getName());
        table.put(PROP_SUNSET, dateFormat.format(Instant.ofEpochMilli(night.getStartTimeRounded(zone))));
        table.put(PROP_DUSK, dateFormat.format(Instant.ofEpochMilli(nautical.getStartTimeRounded(zone))));
        table.put(PROP_DAWN, dateFormat.format(Instant.ofEpochMilli(nautical.getEndTimeRounded(zone))));
        table.put(PROP_SUNRISE, dateFormat.format(Instant.ofEpochMilli(night.getEndTimeRounded(zone))));
        table.put(PROP_CONSTRAINTS, target.getConditions());
        if (target.getWindConstraint() != null)
            table.put(PROP_WIND, target.getWindConstraint());
        else
            table.put(PROP_WIND, "\u00ABnone\u00BB");
    }

}
