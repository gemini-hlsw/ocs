package edu.gemini.qpt.core.util;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Logger;

import edu.gemini.spModel.core.Site;
import jsky.coords.WorldCoords;
import edu.gemini.qpt.shared.sp.Obs;

public abstract class ElevationConstraintSolver extends Solver {

	@SuppressWarnings("unused")
	private static final Logger LOGGER = Logger.getLogger(ElevationConstraintSolver.class.getName());
	
	protected final ImprovedSkyCalc calc;
	protected final Function<Long, WorldCoords> coords;
	protected final double min, max;
	
	protected ElevationConstraintSolver(Site site, Function<Long, WorldCoords> coords, double min, double max) {
	    super(TimeUnit.MINUTES.toMillis(15), TimeUnit.MINUTES.toMillis(1));
		this.coords = coords;
		this.calc = new ImprovedSkyCalc(site);
		this.max = max;
		this.min = min;
	}

	public static ElevationConstraintSolver forObs(Site site, Obs obs) {
		switch (obs.getElevationConstraintType()) {
		case AIRMASS:    return new AirmassSolver(site, obs);
		case HOUR_ANGLE: return new HourAngleSolver(site, obs);
		case NONE:       return new AirmassSolver(site, obs, 1.0, 2.0);
		default:
			throw new Error("Unknown ElevationConstraintType: " + obs.getElevationConstraintType());
		}		
	}
	
	static class AirmassSolver extends ElevationConstraintSolver {

		protected AirmassSolver(Site site, Obs obs) {
			super(site, obs::getCoords, obs.getElevationConstraintMin(), obs.getElevationConstraintMax());
		}

		protected AirmassSolver(Site site, Obs obs, double min, double max) {
			super(site, obs::getCoords, min, max);
		}

		@Override
		protected boolean f(long t) {
			calc.calculate(coords.apply(t), new Date(t), false);
			double airmass = calc.getAirmass();
			return (min <= airmass) && (airmass <= max);
		}

	}

	static class HourAngleSolver extends ElevationConstraintSolver {
		
		protected HourAngleSolver(Site site, Obs obs) {
			super(site, obs::getCoords, obs.getElevationConstraintMin(), obs.getElevationConstraintMax());
		}

		@Override
		protected boolean f(long t) {
			calc.calculate(coords.apply(t), new Date(t), false);
			double ha = calc.getHourAngle();
			return (min <= ha) && (ha <= max);
		}

	}

}
