package edu.gemini.qpt.ui.util;

import java.util.Date;
import java.util.function.Function;

import edu.gemini.spModel.core.Site;
import jsky.coords.WorldCoords;
import edu.gemini.qpt.core.util.ApproximateAngle;
import edu.gemini.qpt.core.util.ImprovedSkyCalc;
import edu.gemini.qpt.core.util.Solver;
import edu.gemini.qpt.shared.util.TimeUtils;

public class AzimuthSolver extends Solver {

	private final Function <Long, WorldCoords> coords;
	private final ImprovedSkyCalc calc;
	private final ApproximateAngle angle;
	
	public AzimuthSolver(Site site, Function<Long, WorldCoords> coords, ApproximateAngle angle) {
		super(TimeUtils.MS_PER_HOUR / 4, TimeUtils.MS_PER_MINUTE);
		this.coords = coords;
		this.calc = new ImprovedSkyCalc(site);
		this.angle = angle;
	}

	@Override
	protected boolean f(long t) {
		calc.calculate(coords.apply(t), new Date(t), false);
		double az = calc.getAzimuth();
		return angle.contains((int) az);
	}

}
