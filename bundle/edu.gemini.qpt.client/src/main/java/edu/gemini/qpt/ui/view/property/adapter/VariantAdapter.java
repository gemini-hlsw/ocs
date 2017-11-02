package edu.gemini.qpt.ui.view.property.adapter;

import edu.gemini.qpt.core.Variant;
import edu.gemini.qpt.ui.view.property.PropertyTable;
import edu.gemini.qpt.ui.view.property.PropertyTable.Adapter;
import edu.gemini.skycalc.TwilightBoundType;
import edu.gemini.skycalc.TwilightBoundedNight;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class VariantAdapter implements Adapter<Variant> {

	public void setProperties(Variant variant, Variant target, PropertyTable table) {

		DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MMM-dd HH:mm")
				.withZone(target.getSchedule().getSite().timezone().toZoneId());

		TwilightBoundedNight night = new TwilightBoundedNight(
				TwilightBoundType.OFFICIAL,
				target.getSchedule().getStart(),
				target.getSchedule().getSite()
		);

		table.put(PROP_TYPE, "Plan Variant");
		table.put(PROP_TITLE, target.getName());
		table.put(PROP_SUNSET, dateFormat.format(Instant.ofEpochMilli(night.getStartTime())));
		table.put(PROP_SUNRISE, dateFormat.format(Instant.ofEpochMilli(night.getEndTime())));
		table.put(PROP_CONSTRAINTS, target.getConditions());
		if (target.getWindConstraint() != null)
			table.put(PROP_WIND, target.getWindConstraint());
		else
			table.put(PROP_WIND, "\u00ABnone\u00BB");
	}

}
