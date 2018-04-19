package edu.gemini.qpt.core.listeners;

import java.beans.PropertyChangeEvent;

import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.core.Marker.Severity;
import edu.gemini.qpt.core.util.MarkerManager;

/**
 * Generates Schedule markers if there is no ICTD information.
 */
public final class EmptyIctdListener extends MarkerModelListener<Schedule> {

    public void propertyChange(final PropertyChangeEvent evt) {

        final Schedule       s = (Schedule) evt.getSource();
        final MarkerManager mm = s.getMarkerManager();

        mm.clearMarkers(this, s);

        if (s.getIctd().isEmpty()) {
            mm.addMarker(true, this, Severity.Warning, "ICTD information unavailable.", s);
        }
    }

    @Override
    protected MarkerManager getMarkerManager(final Schedule s) {
        return s.getMarkerManager();
    }

}
