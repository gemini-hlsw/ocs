package edu.gemini.qpt.ui.html;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import edu.gemini.spModel.core.Site;
import jsky.coords.WorldCoords;
import edu.gemini.qpt.core.util.ImprovedSkyCalc;
import edu.gemini.qpt.core.util.Interval;
import edu.gemini.qpt.core.util.Solver;

/**
 * Value type that calculates the rise, transit, and set times for the moon.
 * @author rnorris
 */
public class MoonRiseTransitSet {

	private final ImprovedSkyCalc calc;
	private final WorldCoords coords = new WorldCoords();
	private final long rise, transit, set;
	
	private static final long MARGIN = TimeUnit.SECONDS.toMillis(15);
	
	public MoonRiseTransitSet(Site site, long start) {

		this.calc = new ImprovedSkyCalc(site);		
		final double DEG_IN_RADIAN = 57.2957795130823;
		final double elev = site.altitude;
		final double horiz = -(0.83 + Math.sqrt(2 * elev / 6378140.) * DEG_IN_RADIAN);

		Solver solver = new Solver(TimeUnit.HOURS.toMillis(1), MARGIN) {
			@Override
			protected boolean f(long t) {
				return elevation(t) >= horiz;
			}
		};
		
		Interval domain = solver.solve(new Interval(start - TimeUnit.DAYS.toMillis(1), start + TimeUnit.DAYS.toMillis(1)), start);
		
		rise = domain.getStart();
		set = domain.getEnd();
		transit = (rise + set) / 2;

	}
	
	private double elevation(long time) {
		calc.calculate(coords, new Date(time), true);
		return calc.getLunarElevation();
	}

	public long getRise() {
		return rise;
	}

	public long getSet() {
		return set;
	}

	public long getTransit() {
		return transit;
	}
	
}
