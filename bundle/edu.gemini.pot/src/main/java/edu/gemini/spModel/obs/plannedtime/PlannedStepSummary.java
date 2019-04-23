package edu.gemini.spModel.obs.plannedtime;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Planned steps contain a coarse-grained representation of time accounting
 * information.  In particular, it does not distinguish overheads of changing
 * filters, offset positions, etc. vs. science exposure.  This value is cached
 * in the observation so it needs to be kept small as possible.  It is used
 * repeatedly by the Queue Planning Tool.
 */
public class PlannedStepSummary implements Serializable {

	private static final long serialVersionUID = 2L;

	public static final PlannedStepSummary ZERO_PLANNED_STEPS =
        new PlannedStepSummary(SetupTime.ZERO, new long[0], new boolean[0], new String[0]);

	private final SetupTime setupTime;
	private final long[] stepTimes;
	private final boolean[] executed;
	private final String[] obsTypes;

	public PlannedStepSummary(SetupTime setupTime, long[] stepTimes, boolean[] executed, String[] obsTypes) {
		assert stepTimes.length == executed.length;
		this.setupTime = setupTime;
		this.stepTimes = stepTimes;
		this.executed  = executed;
		this.obsTypes  = obsTypes;
	}

	public int size() {
		return stepTimes.length;
	}

	public boolean isStepExecuted(int step) {
		return executed[step];
	}

	public SetupTime getSetupTime() {
		return setupTime;
    }

    public long getStepTime(int step) {
		return stepTimes[step];
	}

	public String getObsType(int step) {
		return obsTypes[step];
	}

	@Override
	public String toString() {
		return setupTime + " + " + Arrays.toString(stepTimes);
	}

}
