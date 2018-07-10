package edu.gemini.qpt.ui.view.program;

import edu.gemini.qpt.shared.sp.Obs;
import edu.gemini.qpt.shared.sp.Prog;
import edu.gemini.ui.gface.GSelection;
import edu.gemini.ui.gface.GSelectionInterloper;
import edu.gemini.ui.gface.GViewer;

public class ScienceProgramSelectionInterloper implements GSelectionInterloper<Prog, Object> {

    private GViewer<Prog, Object> viewer;
    
    public void beforeSetSelection(GSelection<?> newSelection) {
        if (!newSelection.isEmpty()) {
            
            // Translator will try to turn everything into an Obs. So we need to make sure the
            // program is visible.
            if (newSelection.first() instanceof Obs) {
                viewer.setModel(((Obs) newSelection.first()).getProg());
            }
            
        } else {
            
            viewer.setModel(null);
            
        }
    }

    public void modelChanged(GViewer<Prog, Object> viewer, Prog oldModel, Prog newModel) {
        this.viewer = viewer;
    }

}
