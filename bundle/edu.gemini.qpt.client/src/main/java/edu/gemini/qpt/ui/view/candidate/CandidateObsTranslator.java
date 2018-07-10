package edu.gemini.qpt.ui.view.candidate;

import java.util.Collections;
import java.util.Set;

import edu.gemini.qpt.core.Alloc;
import edu.gemini.qpt.core.Marker;
import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.shared.sp.Obs;
import edu.gemini.ui.gface.GTranslator;
import edu.gemini.ui.gface.GViewer;

public class CandidateObsTranslator implements GTranslator<Schedule, Obs> {

    public Set<Obs> translate(Object o) {
        
        if (o instanceof Marker)
            o = ((Marker) o).getTarget();
        
        if (o instanceof Obs) {
            return Collections.singleton((Obs) o);
        } else if (o instanceof Alloc) {
            return Collections.singleton(((Alloc) o).getObs());
        } else {
            return Collections.emptySet();
        }
    }

    public void modelChanged(GViewer<Schedule, Obs> viewer, Schedule oldModel, Schedule newModel) {
    }

}
