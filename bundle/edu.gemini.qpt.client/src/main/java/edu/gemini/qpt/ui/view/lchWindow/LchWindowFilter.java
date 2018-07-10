package edu.gemini.qpt.ui.view.lchWindow;

import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.core.Variant;
import edu.gemini.ui.gface.GFilter;
import edu.gemini.ui.gface.GViewer;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class LchWindowFilter implements GFilter<Schedule, LchWindow>, PropertyChangeListener {

    private Variant variant;
    private GViewer<Schedule, LchWindow> viewer;

    public boolean accept(LchWindow element) {
        return true;
    }

    public void modelChanged(GViewer<Schedule, LchWindow> viewer, Schedule oldModel, Schedule newModel) {
        this.viewer = viewer;
        if (oldModel != null) oldModel.removePropertyChangeListener(Schedule.PROP_CURRENT_VARIANT, this);
        if (newModel != null) newModel.addPropertyChangeListener(Schedule.PROP_CURRENT_VARIANT, this);
        this.variant = newModel != null ? newModel.getCurrentVariant() : null;
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (viewer != null) {
            variant = (Variant) evt.getNewValue();
            viewer.refresh();
        }
    }

}
