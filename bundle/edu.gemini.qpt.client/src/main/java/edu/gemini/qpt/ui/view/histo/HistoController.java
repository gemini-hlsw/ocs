package edu.gemini.qpt.ui.view.histo;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.core.Variant;
import edu.gemini.ui.gface.GViewer;
import edu.gemini.ui.gface.GViewerPlugin;

public class HistoController implements GViewerPlugin<Schedule, Variant>, PropertyChangeListener {

    GViewer<Schedule, Variant> viewer;
    Variant variant;
    
    public void modelChanged(GViewer<Schedule, Variant> viewer, Schedule oldModel, Schedule newModel) {

        // Keep the viewer reference
        this.viewer = viewer;
        
        // Remove old listeners, if any.
        if (oldModel != null) {
            oldModel.removePropertyChangeListener(Schedule.PROP_CURRENT_VARIANT, this);
            oldModel.getMarkerManager().removePropertyChangeListener(this);
        }
        if (variant != null) variant.removePropertyChangeListener(Variant.PROP_FLAGS, this);
        
        // Get the current variant, if any
        variant = newModel == null ? null : newModel.getCurrentVariant();

        // Add new listeners
        if (newModel != null) {
            newModel.addPropertyChangeListener(Schedule.PROP_CURRENT_VARIANT, this);
            newModel.getMarkerManager().addPropertyChangeListener(this);
        }
        if (variant != null) variant.addPropertyChangeListener(Variant.PROP_FLAGS, this);
        
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (Schedule.PROP_CURRENT_VARIANT.equals(evt.getPropertyName())) {
            variant = (Variant) evt.getNewValue();
        }
        viewer.refresh();
    }

    public Variant getVariant() {
        return variant;
    }
    
}
