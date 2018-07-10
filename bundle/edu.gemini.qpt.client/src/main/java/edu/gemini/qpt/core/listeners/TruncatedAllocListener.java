package edu.gemini.qpt.core.listeners;

import java.beans.PropertyChangeEvent;

import edu.gemini.qpt.core.Alloc;
import edu.gemini.qpt.core.Variant;
import edu.gemini.qpt.core.Marker.Severity;
import edu.gemini.qpt.core.util.MarkerManager;

/**
 * Generates Alloc markers for Allocs that overlap.
 * @author rnorris
 */
public class TruncatedAllocListener extends MarkerModelListener<Variant> {

    public void propertyChange(PropertyChangeEvent evt) {
        Variant v = (Variant) evt.getSource();
        MarkerManager mm = getMarkerManager(v);        
        mm.clearMarkers(this, v);
        
        for (Alloc a: v.getAllocs()) {
            if (a.getSuccessor() == null) {
                int lastScheduleStep = a.getLastStep();
                int lastSequenceStep = a.getObs().getSteps().size() - 1;
                if (lastScheduleStep < lastSequenceStep) {
                    int firstUnscheduledStep = lastScheduleStep + 1;
                    String msg = (firstUnscheduledStep == lastSequenceStep) ?
                            ("Step "  + (firstUnscheduledStep + 1) + " is unscheduled.") :
                            ("Steps " + (firstUnscheduledStep + 1) + "-" + (lastSequenceStep + 1) + " are unscheduled.");
                    
                    mm.addMarker(true, this, Severity.Warning, msg, v, a);        
                }
            }            
        }
        
    }

    @Override
    protected MarkerManager getMarkerManager(Variant t) {
        return t.getSchedule().getMarkerManager();
    }
    
}
