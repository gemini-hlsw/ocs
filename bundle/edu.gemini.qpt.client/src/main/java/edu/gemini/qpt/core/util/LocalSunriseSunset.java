package edu.gemini.qpt.core.util;

import edu.gemini.skycalc.TwilightBoundType;
import edu.gemini.spModel.core.Site;

public class LocalSunriseSunset {

	public static final TwilightBoundType forSite(Site site) {
		return new TwilightBoundType("Local Sunrise/Sunset Twilight Bound", 0.83 + getHorizon(site));
	}

	private static double getHorizon(Site site) {
		final double DEG_IN_RADIAN = 57.2957795130823;
		final double elev = site.altitude;
		return Math.sqrt(2 * elev / 6378140.) * DEG_IN_RADIAN;
	}

}
