package edu.gemini.qpt.ui.view.candidate;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.shared.sp.MiniModel;
import edu.gemini.qpt.shared.sp.Obs;
import edu.gemini.ui.gface.GTableController;
import edu.gemini.ui.gface.GViewer;

public class CandidateObsController implements GTableController<Schedule, Obs, CandidateObsAttribute>, PropertyChangeListener {

    private GViewer<Schedule, Obs> viewer;
    private Obs[] obs = {};
    
    public synchronized Object getSubElement(Obs obs, CandidateObsAttribute subElement) {
        switch (subElement) {
        case Dur: return obs.getRemainingTime();
        case Inst: return obs.getInstrumentString();
        case Observation: return obs;
        case P: return obs.getPriority();
        case RA: return obs.getRa(viewer.getModel().getMiddlePoint());
        case SB: return obs.getProg().getBand();
        case Target: return obs.getTargetName();

        // The decorator will take care of this one,
        // otherwise we have to track the variant.
        case Score: return null; 

        }
        return null;
    }

    public synchronized Obs getElementAt(int row) {
        return obs[row];
    }

    public synchronized int getElementCount() {
        return obs.length;
    }

    public synchronized void modelChanged(GViewer<Schedule, Obs> viewer, Schedule oldModel, Schedule newModel) {

        // Set the viewer (probably will be the same one)
        this.viewer = viewer;

        // Unhook previous variant and model, if any
        if (oldModel != null) {            
            oldModel.removePropertyChangeListener(Schedule.PROP_MINI_MODEL, this);
        }

        // Hook up new model and variant, if any
        if (newModel != null) {
            newModel.addPropertyChangeListener(Schedule.PROP_MINI_MODEL, this);
        }
              
        // Get the obs
        fetchObs(newModel != null ? newModel.getMiniModel() : null);
        
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (Schedule.PROP_MINI_MODEL.equals(evt.getPropertyName())) {
            fetchObs((MiniModel) evt.getNewValue());
            viewer.refresh();
        }
    }

    private synchronized void fetchObs(MiniModel model) {
        obs = model == null ? new Obs[0] : model.getAllObservations().toArray(new Obs[0]);
    }
    
}
