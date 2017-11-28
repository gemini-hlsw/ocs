package edu.gemini.skycalc;

import edu.gemini.shared.util.DateTimeUtils;
import edu.gemini.spModel.core.Site;
import jsky.coords.WorldCoords;

import java.util.Date;
import java.util.logging.Logger;

public abstract class ElevationConstraintSolver extends Solver {

	@SuppressWarnings({"unused", "UnusedDeclaration"})
    private static final Logger LOGGER = Logger.getLogger(ElevationConstraintSolver.class.getName());

    public static ElevationConstraintSolver forAirmass(Site site, WorldCoords coords) {
        return new AirmassSolver(site, coords, 1.0, 2.0);
    }

    public static ElevationConstraintSolver forAirmass(Site site, WorldCoords coords, double min, double max) {
        return new AirmassSolver(site, coords, min, max);
    }

    public static ElevationConstraintSolver forHourAngle(Site site, WorldCoords coords, double min, double max) {
        return new HourAngleSolver(site, coords, min, max);
    }


    protected final ImprovedSkyCalc calc;
	protected final WorldCoords coords;
	protected final double min, max;

	protected ElevationConstraintSolver(Site site, WorldCoords coords, double min, double max) {
		super(DateTimeUtils.MillisecondsPerHour() / 4, DateTimeUtils.MillisecondsPerMinute());
		this.coords = coords;
		this.calc = new ImprovedSkyCalc(site);
		this.max = max;
		this.min = min;
	}

	static class AirmassSolver extends ElevationConstraintSolver {
		protected AirmassSolver(Site site, WorldCoords coords, double min, double max) {
			super(site, coords, min, max);
		}

		@Override
		protected boolean f(long t) {
			calc.calculate(coords, new Date(t), false);
			double airmass = calc.getAirmass();
			return (min <= airmass) && (airmass <= max);
		}

	}

	static class HourAngleSolver extends ElevationConstraintSolver {
        protected HourAngleSolver(Site site, WorldCoords coords, double min, double max) {
			super(site, coords, min, max);
		}

		@Override
		protected boolean f(long t) {
			calc.calculate(coords, new Date(t), false);
			double ha = calc.getHourAngle();
			return (min <= ha) && (ha <= max);
		}

	}

}

