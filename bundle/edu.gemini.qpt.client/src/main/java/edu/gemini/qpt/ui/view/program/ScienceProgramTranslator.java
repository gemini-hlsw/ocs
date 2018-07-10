package edu.gemini.qpt.ui.view.program;

import java.util.Collections;
import java.util.Set;

import edu.gemini.qpt.core.Alloc;
import edu.gemini.qpt.core.Marker;
import edu.gemini.qpt.shared.sp.Prog;
import edu.gemini.ui.gface.GTranslator;
import edu.gemini.ui.gface.GViewer;

public class ScienceProgramTranslator implements GTranslator<Prog, Object> {

    public Set<Object> translate(Object o) {
        if (o instanceof Alloc) {
            o = ((Alloc) o).getObs();
        } else if (o instanceof Marker) {
            return translate(((Marker) o).getTarget());
        }
        return Collections.singleton(o);
    }

    public void modelChanged(GViewer<Prog, Object> viewer, Prog oldModel, Prog newModel) {
    }
    
}
