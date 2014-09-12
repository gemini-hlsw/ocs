package edu.gemini.spModel.obs.plannedtime;

import edu.gemini.skycalc.Angle;
import edu.gemini.skycalc.Offset;
import edu.gemini.shared.util.immutable.MapOp;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.obs.plannedtime.PlannedTime.CategorizedTime;
import edu.gemini.spModel.obs.plannedtime.PlannedTime.Category;
import edu.gemini.spModel.target.offset.OffsetPosBase;

/**
 * StepCalculator for telescope offset positions.
 */
public enum OffsetOverheadCalculator {
    instance;

    public static final String DETAIL = "Telescope Offset";


    public Offset extract(Option<Config> configOpt) {
        return configOpt.flatMap(new MapOp<Config, Option<Offset>>() {
            @Override
            public Option<Offset> apply(Config config) {
                return OffsetPosBase.extractSkycalcOffset(config);
            }
        }).getOrElse(Offset.ZERO_OFFSET);
    }

    public Offset extract(Config config) {
        return OffsetPosBase.extractSkycalcOffset(config).getOrElse(Offset.ZERO_OFFSET);
    }

    public double calc(Config curConfig, Option<Config> prevConfig) {
        // Fish out the telescope config info, if any.
        Option<Offset> opt = OffsetPosBase.extractSkycalcOffset(curConfig);
        if (opt.isEmpty()) return 0;
        Offset cur = opt.getValue();

        // If there was no change, there should be no time required to
        // offset.
        Offset prev = extract(prevConfig);
        if (cur.equals(prev)) return 0;

        // Calculate the time to do the offset.
        return calc(cur, prev);
    }

    /**
     * Computes the overhead for the specified offset.  This is an estimate for
     * how long it takes to configure the telescope to observe at this offset
     * position.
     *
     * @return time in seconds
     */
    public double calc(Offset cur, Offset prev) {
        if (prev.equals(cur)) return 0;
        Angle d = cur.distance(prev);
        return 7.0 + d.toArcsecs().getMagnitude()/160.0;
    }

    public CategorizedTime categorize(double secs) {
        return CategorizedTime.fromSeconds(Category.CONFIG_CHANGE, secs, DETAIL);
    }
}
