package edu.gemini.qpt.ui.html;

import java.util.Date;

import edu.gemini.spModel.core.Site;
import jsky.coords.WorldCoords;
import edu.gemini.qpt.core.util.ImprovedSkyCalc;
import edu.gemini.qpt.core.util.Interval;
import edu.gemini.qpt.core.util.Solver;
import edu.gemini.qpt.shared.util.TimeUtils;

/**
 * Value type that calculates the rise, transit, and set times for the moon.
 * @author rnorris
 */
public class MoonRiseTransitSet {

    private final ImprovedSkyCalc calc;
    private final WorldCoords coords = new WorldCoords();
    private final long rise, transit, set;
    
    private static final long MARGIN = TimeUtils.MS_PER_MINUTE / 4; // 15 sec
    
    public MoonRiseTransitSet(Site site, long start) {

        this.calc = new ImprovedSkyCalc(site);        
        final double DEG_IN_RADIAN = 57.2957795130823;
        final double elev = site.altitude;
        final double horiz = -(0.83 + Math.sqrt(2 * elev / 6378140.) * DEG_IN_RADIAN);

        Solver solver = new Solver(TimeUtils.MS_PER_HOUR, MARGIN) {
            @Override
            protected boolean f(long t) {
                return elevation(t) >= horiz;
            }
        };
        
        Interval domain = solver.solve(new Interval(start - TimeUtils.MS_PER_DAY, start + TimeUtils.MS_PER_DAY), start);
        
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
