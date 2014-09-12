//
// $Id: ObsPercentage.java 8519 2008-05-04 22:09:28Z swalker $
//
package edu.gemini.dbTools.semesterStatus;

import edu.gemini.dbTools.odbState.ObservationState;
import edu.gemini.dbTools.odbState.ProgramState;
import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.spModel.obs.ObservationStatus;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * An ObsPercentage is a grouping of a percentage number with a Paint object.
 * The percentage represents the number of observations in a program of a
 * particular status relative to the total number of observations.  The status
 * itself isn't important for plotting though, so only the corresponding color
 * is kept.
 */
final class ObsPercentage {

    private static final ObsPercentage[] EMPTY_ARRAY = new ObsPercentage[0];

    private static final Integer ONE = 1;

    /**
     * Gets the percentages of observations in the various status for the
     * given program.  These are sorted from PHASE2 to OBSERVED.
     */
    static ObsPercentage[] getPercentages(final ProgramState pstate) {
        final Map<SPObservationID, ObservationState> obsMap = pstate.getObservations();
        final Collection<ObservationState> obsList = obsMap.values();

        final int total = obsList.size();
        final Map<ObservationStatus, Integer> counts;
        counts = new HashMap<ObservationStatus, Integer>();
        for (final ObservationState os : obsList) {
            final ObservationStatus status = os.getStatus();
            final Integer count = counts.get(status);
            if (count == null) {
                counts.put(status, ONE);
            } else {
                counts.put(status, count + 1);
            }
        }

        final ObsStatusData[] statusA = ObsStatusData.STATUS_ARRAY;

        final ObservationStatus observed;
        observed = ObservationStatus.OBSERVED;

        int runningCount = 0;
        final List<ObsPercentage> res = new ArrayList<ObsPercentage>();

        // Add everything but "OBSERVED".
        for (final ObsStatusData osd : statusA) {
            if (osd.includesStatus(observed)) continue;

            final ObservationStatus[] statuses = osd.getStatuses();
            int count = 0;
            for (final ObservationStatus statuse : statuses) {
                final Integer wrap = counts.get(statuse);
                if (wrap != null) count += wrap;
            }

            runningCount += count;
            final double percentage = ((double) count) / ((double) total);
            res.add(new ObsPercentage(percentage, osd.getPaint()));
        }

        final int other = total - runningCount;
        if (other > 0) {
            // Get the "OBSERVED" status data.
            final ObsStatusData osd = ObsStatusData.lookup(observed);
            final double percentage = ((double) other) / ((double) total);
            res.add(new ObsPercentage(percentage, osd.getPaint()));
        }

        return res.toArray(EMPTY_ARRAY);
    }

    private final double _percentage;
    private final Paint  _paint;

    private ObsPercentage(final double percentage, final Paint paint) {
        _percentage = percentage;
        _paint      = paint;
    }

    public double getPercentage() {
        return _percentage;
    }

    public Paint getPaint() {
        return _paint;
    }
}
