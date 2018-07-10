package edu.gemini.qpt.core.util;

import java.util.Date;
import java.util.function.Function;

import edu.gemini.qpt.shared.util.TimeUtils;
import edu.gemini.spModel.core.Site;
import jsky.coords.WorldCoords;

public class SkyBackgroundSolver extends Solver {

    private final ImprovedSkyCalc calc;
    private final Function <Long, WorldCoords> coords;
    private final double mag;
    
    /**
     * Creates a new SkyBackgroundSolver that finds intervals in which the sky background
     * at a given point in the sky is dimmer than a given magnitude.
     * @param site
     * @param mag
     */
    public SkyBackgroundSolver(Site site, Function<Long, WorldCoords> coords, double mag) {
        super(TimeUtils.MS_PER_HOUR / 4, TimeUtils.MS_PER_MINUTE);
        this.coords = coords;
        this.calc = new ImprovedSkyCalc(site);
        this.mag = mag;
    }
    
    @Override
    protected boolean f(long t) {
        calc.calculate(coords.apply(t), new Date(t), true);
        return calc.getTotalSkyBrightness() >= mag;
    }

    @Override
    public Union<Interval> solve(Interval interval) {
        return super.solve(interval);
    }
    
}

