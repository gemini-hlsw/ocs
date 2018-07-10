package edu.gemini.qpt.ui.view.program;

import edu.gemini.qpt.shared.sp.Group;
import edu.gemini.qpt.shared.sp.Prog;
import edu.gemini.ui.gface.GFilter;
import edu.gemini.ui.gface.GViewer;

public class ScienceProgramFilter implements GFilter<Prog, Object> {

    public boolean accept(Object element) {
        
        // Throw out groups with no Obs in them.
        if (element instanceof Group) {
            Group g = (Group) element;
            if (g.getObservations().size() == 0) 
                return false;
        }
        
        return true;
        
    }

    public void modelChanged(GViewer<Prog, Object> viewer, Prog oldModel, Prog newModel) {
        // TODO Auto-generated method stub
        
    }
    
}
