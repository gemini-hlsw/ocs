package edu.gemini.qpt.core.listeners;

import java.beans.PropertyChangeEvent;

import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.core.Marker.Severity;
import edu.gemini.qpt.core.util.MarkerManager;

/**
 * Generates Schedule markers if there are no blocks.
 * @author rnorris
 */
public class EmptyScheduleListener extends MarkerModelListener<Schedule> {

    public void propertyChange(PropertyChangeEvent evt) {
        Schedule s = (Schedule) evt.getSource();
        MarkerManager mm = s.getMarkerManager();

        mm.clearMarkers(this, s);
        
        if (s.getBlocks().size() == 0) {
            mm.addMarker(true, this, Severity.Info, "Schedule contains no blocks.", s);
        }
        
        if (s.getVariants().size() == 0) {
            mm.addMarker(true, this, Severity.Info, "Schedule contains no variants.", s);
        }

    }

    protected MarkerManager getMarkerManager(Schedule t) {
        return t.getMarkerManager();
    }
    
}

                    
