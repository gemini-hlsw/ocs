package edu.gemini.qpt.ui.view.problem;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import edu.gemini.qpt.core.Alloc;
import edu.gemini.qpt.core.Marker;
import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.core.Variant;
import edu.gemini.qpt.shared.sp.Obs;
import edu.gemini.ui.gface.GTranslator;
import edu.gemini.ui.gface.GViewer;

public class ProblemTranslator implements GTranslator<Schedule, Marker> {

    private Schedule schedule;

    public Set<Marker> translate(Object o) {    
        try {
            
            if (o instanceof Variant) {
                
                // For now let's not select all markers when you click on a 
                // variant. The marker list is filtered anyway, so it doesn't
                // really accomplish anything.
                return Collections.<Marker>emptySet();
                
            } else if (o instanceof Marker) {
                return Collections.singleton((Marker) o);
            } else if (o instanceof Obs) {
                Set<Marker> accum = new HashSet<Marker>();
                for (Alloc a: schedule.getCurrentVariant().getAllocs((Obs) o))
                    accum.addAll(translate(a));
                return accum;            
            } else if (schedule != null) {
                return schedule.getMarkerManager().getMarkers(o, true);
            } else {
                return Collections.<Marker>emptySet();
            }
        } catch (NullPointerException npe) {
            // This is ok; it can happen in some shutdown race conditions
            return Collections.<Marker>emptySet();
        }
    }

    public void modelChanged(GViewer<Schedule, Marker> viewer, Schedule oldModel, Schedule newModel) {
        schedule = newModel;
    }
    
    
}
