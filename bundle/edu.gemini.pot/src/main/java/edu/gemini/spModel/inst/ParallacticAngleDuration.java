package edu.gemini.spModel.inst;

import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.spModel.obs.ObsTimesService;
import edu.gemini.spModel.obs.plannedtime.PlannedTime;
import edu.gemini.spModel.obs.plannedtime.PlannedTimeCalculator;
import edu.gemini.spModel.obs.plannedtime.PlannedTimeSummaryService;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;

import java.io.Serializable;

public final class ParallacticAngleDuration implements Serializable {
    // Static default instance for most uses.
    private final static ParallacticAngleDuration DEFAULT = new ParallacticAngleDuration(ParallacticAngleDurationMode.REMAINING_TIME, 0);

    // PIO parameter strings.
    private final static String PARAM_SET_PARALLACTIC_ANGLE_DURATION = "parallacticAngleDuration";
    private final static String PARAM_PARALLACTIC_ANGLE_DURATION_MODE = "parallacticAngleDurationMode";
    private final static String PARAM_EXPLICIT_DURATION = "explicitDuration";

    public final static ParallacticAngleDuration getInstance() {
        return DEFAULT;
    }

    public final static ParallacticAngleDuration getInstance(ParallacticAngleDurationMode mode, long explicitDuration) {
        if (mode == null || (mode == ParallacticAngleDurationMode.REMAINING_TIME && explicitDuration == 0)) {
            return DEFAULT;
        }
        return new ParallacticAngleDuration(mode, explicitDuration);
    }

    private final ParallacticAngleDurationMode mode;
    private final long explicitDuration;

    private ParallacticAngleDuration(ParallacticAngleDurationMode pmode, long pexplicitDuration) {
        mode = pmode;
        explicitDuration = pexplicitDuration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ParallacticAngleDuration that = (ParallacticAngleDuration) o;

        if (explicitDuration != that.explicitDuration) return false;
        if (mode != that.mode) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = mode != null ? mode.hashCode() : 0;
        result = 31 * result + (int) (explicitDuration ^ (explicitDuration >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "ParallacticAngleDuration{" +
                "mode=" + mode +
                ", explicitDuration=" + explicitDuration +
                '}';
    }

    public ParallacticAngleDurationMode getParallacticAngleDurationMode() {
        return mode;
    }

    public long getExplicitDuration() {
        return explicitDuration;
    }

    public static ParamSet toParamSet(PioFactory factory, ParallacticAngleDuration parallacticAngleDuration) {
        final ParamSet ps = factory.createParamSet(PARAM_SET_PARALLACTIC_ANGLE_DURATION);
        Pio.addParam(factory, ps, PARAM_PARALLACTIC_ANGLE_DURATION_MODE, parallacticAngleDuration.getParallacticAngleDurationMode().name());
        Pio.addLongParam(factory, ps, PARAM_EXPLICIT_DURATION, parallacticAngleDuration.getExplicitDuration());
        return ps;
    }

    public static ParallacticAngleDuration fromParamSet(ParamSet ps) {
        final ParallacticAngleDurationMode mode = Pio.getEnumValue(ps, PARAM_PARALLACTIC_ANGLE_DURATION_MODE, ParallacticAngleDurationMode.REMAINING_TIME);
        final long explicitDuration = Pio.getLongValue(ps, PARAM_EXPLICIT_DURATION, 0);
        return ParallacticAngleDuration.getInstance(mode, explicitDuration);
    }

    public static long calculateRemainingTime(ISPObservation ispObservation) {
        PlannedTime pt = PlannedTimeCalculator.instance.calc(ispObservation);

        long totalStepTime = 0;
        long executedStepTime = 0;
        for (PlannedTime.Step step : pt.steps) {
            totalStepTime += step.totalTime();
            if (step.executed)
                executedStepTime += step.totalTime();
        }
        long calcDuration = totalStepTime - executedStepTime;

        // The old calculation, which was inaccurate.
        //long totalExecTime   = ObsTimesService.getRawObsTimes(ispObservation).getTotalTime();
        //long plannedExecTime = PlannedTimeSummaryService.getTotalTime(ispObservation).getExecTime();
        //long calcDuration    = Math.max(0, plannedExecTime - totalExecTime);

        return calcDuration;
    }
}