package edu.gemini.qpt.ui.view.candidate;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.core.Variant;
import edu.gemini.qpt.shared.sp.Obs;
import edu.gemini.qpt.ui.util.PreferenceManager;
import edu.gemini.ui.gface.GFilter;
import edu.gemini.ui.gface.GViewer;

public class CandidateObsFilter implements GFilter<Schedule, Obs>, PropertyChangeListener {

//    private static final Logger LOGGER = Logger.getLogger(CandidateObsFilter.class.getName());
    private GViewer<Schedule, Obs> viewer;
    private Variant variant;

    public boolean accept(Obs obs) {
        return ClientExclusion.forObs(variant, obs) == null;
    }

    public void propertyChange(PropertyChangeEvent evt) {
        
        // If the current variant changed, we need to swap our listeners.
        if (Schedule.PROP_CURRENT_VARIANT.equals(evt.getPropertyName())) {
            if (variant != null) variant.removePropertyChangeListener(Variant.PROP_FLAGS, this);
            variant = (Variant) evt.getNewValue();
            if (variant != null) variant.addPropertyChangeListener(Variant.PROP_FLAGS, this);            
        }
        
        // And refresh the viewer.
        viewer.refresh();
        
    }

    public void modelChanged(GViewer<Schedule, Obs> viewer, Schedule oldModel, Schedule newModel) {
        
        // First time through we should hook up with the prefs manager
        if (this.viewer == null)
            PreferenceManager.addPropertyChangeListener(this);
        
        // Ok, we need to track the current variant because its flags will determine how we want
        // to decorate stuff. So we want to unhook the old listeners, if any, then hook the new.
        if (variant != null) variant.removePropertyChangeListener(Variant.PROP_FLAGS, this);
        if (oldModel != null) oldModel.removePropertyChangeListener(Schedule.PROP_CURRENT_VARIANT, this);        
        variant = newModel == null ? null : newModel.getCurrentVariant();                
        if (newModel != null) newModel.addPropertyChangeListener(Schedule.PROP_CURRENT_VARIANT, this);
        if (variant != null) variant.addPropertyChangeListener(Variant.PROP_FLAGS, this);

        // Don't need to refresh yet (the viewer will do it), but keep track of the viewer.
        this.viewer = viewer;

    }

}
