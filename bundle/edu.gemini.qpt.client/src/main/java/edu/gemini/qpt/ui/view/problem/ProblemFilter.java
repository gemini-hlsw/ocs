package edu.gemini.qpt.ui.view.problem;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import edu.gemini.qpt.core.Marker;
import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.core.Variant;
import edu.gemini.ui.gface.GFilter;
import edu.gemini.ui.gface.GViewer;

public class ProblemFilter implements GFilter<Schedule, Marker>, PropertyChangeListener {

    private Variant variant;
    private GViewer<Schedule, Marker> viewer;

    public boolean accept(Marker element) {
        if (element != null) {
            for (Object o: element.getPath())
                if ((o == variant) || (o instanceof Schedule))
                    return true;
        }
        return false;
    }

    public void modelChanged(GViewer<Schedule, Marker> viewer, Schedule oldModel, Schedule newModel) {
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
