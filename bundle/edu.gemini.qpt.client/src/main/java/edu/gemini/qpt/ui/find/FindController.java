package edu.gemini.qpt.ui.find;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Map.Entry;

import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.shared.sp.Obs;
import edu.gemini.qpt.shared.sp.Prog;
import edu.gemini.qpt.shared.sp.ServerExclusion;
import edu.gemini.qpt.ui.view.candidate.ClientExclusion;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.ui.gface.GTableController;
import edu.gemini.ui.gface.GViewer;

class FindController implements GTableController<Schedule, FindElement, FindColumns> {

    private final List<FindElement> list = new ArrayList<FindElement>();
    
    public Object getSubElement(FindElement element, FindColumns subElement) {
        if (element == null) return null;
        switch (subElement) {
        case TARGET: return element.getTarget();
        case ERROR: return element.getError();
        default:
            throw new Error("Impossible.");
        }
    }
    
    public FindElement getElementAt(int row) {
        try {
            return list.get(row);
        } catch (NoSuchElementException nsee) {
            return null;
        }
    }
    
    public int getElementCount() {
        return list.size();
    }
    
    public void modelChanged(GViewer<Schedule, FindElement> viewer, Schedule oldModel, Schedule newModel) {
        list.clear();
        
        if (newModel != null) {
            
            // Excluded Programs
            for (Entry<SPProgramID, ServerExclusion.ProgramExclusion> e: newModel.getMiniModel().getProgramExclusions().entrySet()) {
                try {
                    list.add(new FindElement(new Prog(e.getKey()), e.getValue()));
                } catch (IllegalArgumentException iae) {
                    // nonstandard program id, just skip it.
                }
            }
            
            // Excluded Observations
            for (Entry<SPObservationID, ServerExclusion.ObsExclusion> e: newModel.getMiniModel().getObsExclusions().entrySet()) {
                SPObservationID id = e.getKey();
                list.add(new FindElement(new Obs(new Prog(id.getProgramID()), id), e.getValue()));
            }
            
            // Client-filtered Observations
            for (Prog p: newModel.getMiniModel().getPrograms()) {
                for (Obs o: p.getFullObsSet()) {
                    ClientExclusion ex = ClientExclusion.forObs(newModel.getCurrentVariant(), o);
                    list.add(new FindElement(o, ex));
                }
            }
            
            Collections.sort(list);
        }
        
    }

}



