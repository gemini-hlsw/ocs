package edu.gemini.spModel.obs.plannedtime;

import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.ISPObservationContainer;
import edu.gemini.spModel.obs.ObsClassService;
import edu.gemini.spModel.obs.ObsPhase2Status;
import edu.gemini.spModel.obs.SPObsCache;
import edu.gemini.spModel.obs.SPObservation;
import edu.gemini.spModel.obsclass.ObsClass;

import java.util.Collection;


/**
 * A utility class used to calculate the total planned time for a set of
 * observations in a science program.
 *
 * @author Shane (but only slight modifications on Allan's original code)
 */
public final class PlannedTimeSummaryService {

    private PlannedTimeSummaryService() { }

    /**
     * Return the total planned observing time for the given program or group,
     * omitting inactive observations.
     *
     * @param node a program or group node
     */
    public static PlannedTimeSummary getTotalTime(final ISPObservationContainer node) {
        return getTotalTime(node, false);
    }

    /**
     * Return the total planned observing time for the given program or group.
     *
     * @param node a program or group node
     * @param includeInactive flag to indicate whether inactive observations should be
     *                        included in total time calculation
     *
     * @return the total planned observing time
     */
    public static PlannedTimeSummary getTotalTime(final ISPObservationContainer node, final boolean includeInactive) {
        PlannedTimeSummary totalTime = PlannedTimeSummary.ZERO_PLANNED_TIME;

        //noinspection unchecked
        final Collection<ISPObservation> obsList = node.getAllObservations();

        for (final ISPObservation obs : obsList) {
            // If we are inactive and the includeInactive flag is false, we ignore this observation.
            final boolean isInactive = ((SPObservation) obs.getDataObject()).getPhase2Status() == ObsPhase2Status.INACTIVE;
            if (includeInactive || !isInactive) {
                totalTime = totalTime.sum(getTotalTime(obs));
            }
        }
        return totalTime;
    }

    public static PlannedStepSummary getPlannedSteps(final ISPObservation obs)  {

    	// First check the cache.
    	PlannedStepSummary steps = SPObsCache.getPlannedSteps(obs);
    	if (steps == null) {
       		getTotalTime(obs);
       		steps = SPObsCache.getPlannedSteps(obs);
       		assert steps != null;
    	}
   		return steps;

    }

    /**
     * Return the total observing time for the given observation.
     *
     * @param obs the observation to examine
     *
     * @return the total planned observing time for the observation
     */
    public static PlannedTimeSummary getTotalTime(final ISPObservation obs) {

        // First check the cache.
        final PlannedTimeSummary cachedTime = SPObsCache.getPlannedTime(obs);
        if (cachedTime != null) {
            return cachedTime;
        }

        // Set steps and time to zero for Acq observations.
        // Having zero steps will automatically exclude them from showing up in QPT.
        if (!shouldCountPlannedExecTime(obs)) {
            final PlannedTimeSummary res = PlannedTimeSummary.ZERO_PLANNED_TIME;
            final PlannedStepSummary steps = PlannedStepSummary.ZERO_PLANNED_STEPS;
            SPObsCache.setPlannedTime(obs, res);
            SPObsCache.setPlannedSteps(obs, steps);
            return res;
        }

        final PlannedTime pta = PlannedTimeCalculator.instance.calc(obs);

        // Cache the values.
        final PlannedTimeSummary res = pta.toPlannedTimeSummary();
        SPObsCache.setPlannedTime(obs, res);
        SPObsCache.setPlannedSteps(obs, pta.toPlannedStepSummary());
        return res;
    }

    private static boolean shouldCountPlannedExecTime(final ISPObservation obs) {
        final ObsClass obsClass = ObsClassService.lookupObsClass(obs);
        return !((obsClass == ObsClass.ACQ) || (obsClass == ObsClass.ACQ_CAL));
    }

}

