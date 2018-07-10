package edu.gemini.qpt.ui.view.variant;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.core.Variant;
import edu.gemini.ui.gface.GTableController;
import edu.gemini.ui.gface.GViewer;

public class VariantController implements GTableController<Schedule, Variant, VariantAttribute>, PropertyChangeListener {

    private Schedule schedule;
    private GViewer<Schedule, Variant> viewer;

    public Object getSubElement(Variant variant, VariantAttribute subElement) {
        switch (subElement) {
            case CC:
                return variant.getConditions().getCC();
            case IQ:
                return variant.getConditions().getIQ();
            case Name:
                return variant.getName();
            case WV:
                return variant.getConditions().getWV();
            case Wind:
                return variant.getWindConstraint();
            case LGS:
                return variant.getLgsConstraint()?"yes":"no";
        }
        return null;
    }

    public synchronized Variant getElementAt(int row) {
        return schedule == null ? null : schedule.getVariants().get(row);
    }

    public synchronized int getElementCount() {
        return schedule == null ? 0 : schedule.getVariants().size();
    }

    public synchronized void modelChanged(GViewer<Schedule, Variant> viewer, Schedule oldModel, Schedule newModel) {
        this.viewer = viewer;
        
        // Disconnect old listeners, if any.
        if (schedule != null) {
            schedule.removePropertyChangeListener(Schedule.PROP_VARIANTS, this);
            schedule.getMarkerManager().removePropertyChangeListener(this);
            for (Variant v: schedule.getVariants()) {
                v.removePropertyChangeListener(Variant.PROP_SITE_CONDITIONS, this);
                v.removePropertyChangeListener(Variant.PROP_WIND_CONSTRAINT, this);
                v.removePropertyChangeListener(Variant.PROP_LGS_CONSTRAINT, this);
                v.removePropertyChangeListener(Variant.PROP_NAME, this);
            }
        }        

        // Move our pointer
        schedule = newModel;
        
        // And connect new listeners.
        if (schedule != null) {
            schedule.addPropertyChangeListener(Schedule.PROP_VARIANTS, this);
            schedule.getMarkerManager().addPropertyChangeListener(this);
            for (Variant v: schedule.getVariants()) {
                v.addPropertyChangeListener(Variant.PROP_SITE_CONDITIONS, this);
                v.addPropertyChangeListener(Variant.PROP_WIND_CONSTRAINT, this);
                v.addPropertyChangeListener(Variant.PROP_LGS_CONSTRAINT, this);
                v.addPropertyChangeListener(Variant.PROP_NAME, this);
            }
        }
        
    }

    public void propertyChange(PropertyChangeEvent evt) {
        viewer.refresh();
    }

}
