package edu.gemini.qpt.ui.html;

import java.util.Date;
import java.util.function.Function;

import edu.gemini.spModel.core.Site;
import jsky.coords.WorldCoords;
import edu.gemini.qpt.core.util.ImprovedSkyCalc;
import edu.gemini.qpt.core.util.Interval;
import edu.gemini.qpt.core.util.Solver;
import edu.gemini.qpt.shared.util.TimeUtils;

/**
 * Value type that calculates the rise, transit, and set times for a given object, given a reference
 * time. If the object is above airmass 2.0 at the reference time, the current transit is calculated. 
 * Otherwise the next one will be calculated. If the target doesn't rise/set with respect to the
 * airmass 2 boundary within +/- 24 hrs of the reference time, rise and set will be null. If it 
 * doesn't rise/set with respect to the horizon, transit will be null. 
 * @author rnorris
 */
public class RiseTransitSet {

	private final ImprovedSkyCalc calc;
	private final Function<Long, WorldCoords> coords;
	private final Long rise, transit, set;
	
	private static final long MARGIN = TimeUtils.MS_PER_MINUTE / 4; // 15 sec
	
	public RiseTransitSet(Site site, Function<Long, WorldCoords> coords, long start) {
		this.coords = coords;
		this.calc = new ImprovedSkyCalc(site);
		
		Solver solver = new Solver(TimeUtils.MS_PER_HOUR, MARGIN) {
			@Override
			protected boolean f(long t) {
				return airmass(t) < 2.0;
			}
		};

		Interval domain = solver.solve(new Interval(start - TimeUtils.MS_PER_DAY, start + TimeUtils.MS_PER_DAY), start);

		if (domain != null) {

			// Target rises and sets normally.
			rise = domain.getStart();
			set = domain.getEnd();
			transit = (rise + set) / 2;
			
		} else {

			// [QPT-184] Target never gets above 2.0, so rise and set are undefined.			
			rise = null;
			set = null;

			// We can still find the apex and call it transit, though. Do the calculation again
			// using the horizon instead of airmass 2.
			solver = new Solver(TimeUtils.MS_PER_HOUR, MARGIN) {
				@Override
				protected boolean f(long t) {
					return airmass(t) < Double.MAX_VALUE;  // see airmass() below
				}
			};
			domain = solver.solve(new Interval(start - TimeUtils.MS_PER_DAY, start + TimeUtils.MS_PER_DAY), start);

			// If there is a solution we can define transit. Otheriwse it's null too.
			transit = (domain == null) ? null : ((domain.getStart() + domain.getEnd()) / 2);
											
		} 
		
	}
	
	private double airmass(long time) {
		calc.calculate(coords.apply(time), new Date(time), false);
		double ret = calc.getAirmass();
		// WACK: skycalc returns 0 for targets below the horizon		
		return (ret < 1.0) ? Double.MAX_VALUE : ret;
	}

	public Long getRise() {
		return rise;
	}

	public Long getSet() {
		return set;
	}

	public Long getTransit() {
		return transit;
	}
		
}
