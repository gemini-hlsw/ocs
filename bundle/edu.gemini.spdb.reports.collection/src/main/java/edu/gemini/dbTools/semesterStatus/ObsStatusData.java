//
// $Id: ObsStatusData.java 8519 2008-05-04 22:09:28Z swalker $
//

package edu.gemini.dbTools.semesterStatus;

import edu.gemini.spModel.obs.ObservationStatus;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Chart information associated with each
 * {@link edu.gemini.spModel.obs.ObservationStatus}.
 */
final class ObsStatusData {

    // Creates an array of ObsStatusData elements.
    // The ObsStatusData elements could have more than one ObservationStatus
    // associated, but for every ObsStatus, the ObservationStatus must be different.
    // In other words, the ObsStatusData object has a set of ObservationStatus element,
    // and you must be carefull that all the sets you create for every single ObsStatusData
    // are disjoints.
    static final ObsStatusData[] STATUS_ARRAY = new ObsStatusData[]{
            new ObsStatusData(ObservationStatus.PHASE2,
                    Color.blue, "Phase2"),

            // Pale yellow.
            new ObsStatusData(ObservationStatus.FOR_REVIEW,
                    new Color(255, 255, 50), "For Rev"),

            // Purple
            new ObsStatusData(ObservationStatus.IN_REVIEW,
                    new Color(102, 51, 204), "In Rev"),

            // Reddish orange.
            new ObsStatusData(ObservationStatus.FOR_ACTIVATION,
                    new Color(255, 128, 0), "For Activ"),

            new ObsStatusData(ObservationStatus.ON_HOLD,
                    Color.lightGray, "On Hold"),

            new ObsStatusData(ObservationStatus.READY,
                    Color.green, "Ready"),

            new ObsStatusData(ObservationStatus.ONGOING,
                    new Color(255, 83, 221), "Ongoing"),

            new ObsStatusData(ObservationStatus.OBSERVED,
                    Color.black, "Exec'd"),

            new ObsStatusData(ObservationStatus.INACTIVE,
                    Color.white, "Inactive"),
    };

    private static final Map<ObservationStatus, ObsStatusData> _statusMap = new HashMap<ObservationStatus, ObsStatusData>();

    static {
        for (final ObsStatusData osd : STATUS_ARRAY) {
            final ObservationStatus[] statuses = osd.getStatuses();
            for (final ObservationStatus statuse : statuses) {
                _statusMap.put(statuse, osd);
            }
        }
    }

    static ObsStatusData lookup(final ObservationStatus status) {
        ObsStatusData res = _statusMap.get(status);
        if (res == null) {
            res = _statusMap.get(ObservationStatus.OBSERVED);
        }
        return res;
    }

    // The status(es) which share this information.
    private final ObservationStatus[] _statuses;

    // Paint that represents the status.
    private final Paint _paint;

    // Legend title for the status.
    private final String _legend;

    private ObsStatusData(final ObservationStatus status,
                          final Paint paint, final String legend) {
        this(new ObservationStatus[]{status}, paint, legend);
    }

    private ObsStatusData(final ObservationStatus[] statuses,
                          final Paint paint, final String legend) {
        _statuses = statuses;
        _paint = paint;
        _legend = legend;
    }

    public ObservationStatus[] getStatuses() {
        // not going to worry about copying this array since this class is
        // only used internally
        return _statuses;
    }

    public boolean includesStatus(final ObservationStatus status) {
        for (final ObservationStatus _statuse : _statuses) {
            if (_statuse == status) return true;
        }
        return false;
    }

    public Paint getPaint() {
        return _paint;
    }

    public String getLegend() {
        return _legend;
    }
}
