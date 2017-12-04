package edu.gemini.qpt.ui.action;

import edu.gemini.shared.util.DateTimeUtils;

import java.util.concurrent.TimeUnit;

public class DragLimit {

	private static int limit = 10; // hours, by default

	public static synchronized String caption() {
		return "Drag Limit " + DateTimeUtils.msToHMMSS(value());
	}

	public static synchronized void lower() {
		if (limit > 1)
			--limit;
	}
	
	public static synchronized void higher() {
		limit++;
	}
	
	public static synchronized long value() {
		return TimeUnit.HOURS.toMillis(limit);
	}
	
}
