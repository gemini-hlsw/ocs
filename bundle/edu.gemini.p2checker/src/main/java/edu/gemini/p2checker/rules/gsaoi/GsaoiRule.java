package edu.gemini.p2checker.rules.gsaoi;

import edu.gemini.p2checker.api.*;
import edu.gemini.p2checker.util.SequenceRule;
import edu.gemini.p2checker.rules.gems.GemsGuideStarRule;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.gemini.gsaoi.Gsaoi;


import java.util.ArrayList;
import java.util.Collection;

/**
 *  GSAOI Rule set
 */
public class GsaoiRule implements IRule {
    private static final String PREFIX = "GsaoiRule_";
    private static final Collection<IConfigRule> GSAOI_RULES = new ArrayList<>();

    private static IConfigRule SHORT_EXPOSURE_TIME_RULE = new IConfigRule() {

        private static final String MESSAGE =
                "Exposure time (%.1f sec) is shorter than the minimum (%.1f sec) for read mode '%s'";

        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            Double expTime = SequenceRule.getExposureTime(config);
            if (expTime == null) return null;

            Gsaoi.ReadMode readMode = (Gsaoi.ReadMode)SequenceRule.getInstrumentItem(config, Gsaoi.READ_MODE_PROP);
            if (readMode == null) return null;

            Double minTime = Gsaoi.getMinimumExposureTimeSecs(readMode);

            if (expTime < minTime) {
                String msg = String.format(MESSAGE, expTime, minTime, readMode.displayValue());
                return new Problem(ERROR, PREFIX + "SHORT_EXPOSURE_TIME_RULE", msg, SequenceRule.getInstrumentOrSequenceNode(step, elems, config));
            }

            return null;
        }

        public IConfigMatcher getMatcher() {
            return SequenceRule.SCIENCE_MATCHER;
        }
    };

    private static IConfigRule LONG_EXPOSURE_TIME_RULE = new IConfigRule() {

        private static final String MESSAGE =
                "Exposure time (%.1f sec) may result in detector wells more than 50%% full (max for %s is %.0f sec)";

        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            Double expTime = SequenceRule.getExposureTime(config);
            if (expTime == null) return null;

            Gsaoi.Filter filter = (Gsaoi.Filter) SequenceRule.getInstrumentItem(config, Gsaoi.FILTER_PROP);
            if (filter == null) return null;

            double maxTime = filter.exposureTimeHalfWellSecs();
            if (maxTime <= 0) return null;

            if (expTime > maxTime) {
                String msg = String.format(MESSAGE, expTime, filter.logValue(), maxTime);
                return new Problem(WARNING, PREFIX + "LONG_EXPOSURE_TIME_RULE", msg, SequenceRule.getInstrumentOrSequenceNode(step, elems, config));
            }
            return null;
        }

        public IConfigMatcher getMatcher() {
            return SequenceRule.SCIENCE_MATCHER;
        }
    };

    /*
     * Register all the GSAOI rules to apply
     */
    static {
        GSAOI_RULES.add(SHORT_EXPOSURE_TIME_RULE);
        GSAOI_RULES.add(LONG_EXPOSURE_TIME_RULE);
    }

    public IP2Problems check(ObservationElements elems)  {
        return (new CompositeRule(
            new IRule[] {
                new GemsGuideStarRule(),
                new SequenceRule(GSAOI_RULES, null),
            },
            CompositeRule.Type.all
        )).check(elems);
    }
}
