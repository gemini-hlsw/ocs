package edu.gemini.qpt.ui.view.program;

import java.util.ArrayList;
import java.util.Collection;

import edu.gemini.qpt.shared.sp.Group;
import edu.gemini.qpt.shared.sp.Prog;
import edu.gemini.ui.gface.GTreeController;
import edu.gemini.ui.gface.GViewer;

public class ScienceProgramController implements GTreeController<Prog, Object> {

    private Prog root;

    public Object getRoot() {
        return root;
    }

    public Collection<Object> getChildren(Object parent) {        
        ArrayList<Object> ret = new ArrayList<Object>();    
        
        if (parent instanceof Prog) {
            
            // Program has three kinds of children
            Prog prog = (Prog) parent;
            ret.addAll(prog.getNoteList());
            ret.addAll(prog.getObsList());
            ret.addAll(prog.getGroupList());
            
        } else if (parent instanceof Group) {
            
            // Group has two kinds
            Group group = (Group) parent;
            ret.addAll(group.getNoteList());
            ret.addAll(group.getObservations());
            
        } // others have none.
        
        return ret;
        
    }

    public void modelChanged(GViewer<Prog, Object> viewer, Prog oldModel, Prog newModel) {
        root = newModel;
    }

}
