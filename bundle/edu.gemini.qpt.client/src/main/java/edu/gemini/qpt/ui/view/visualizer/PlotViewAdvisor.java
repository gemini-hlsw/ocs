package edu.gemini.qpt.ui.view.visualizer;

import static edu.gemini.qpt.ui.util.BooleanViewPreference.SHOW_IN_VISUALIZER;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.core.Variant;
import edu.gemini.qpt.shared.sp.Obs;
import edu.gemini.qpt.ui.CommonActions;
import edu.gemini.qpt.ui.util.ElevationPreference;
import edu.gemini.qpt.ui.util.EnumBox;
import edu.gemini.qpt.ui.util.PreferenceManager;
import edu.gemini.qpt.ui.view.visit.VisitController;
import edu.gemini.qpt.ui.view.visit.VisitTranslator;
import edu.gemini.qpt.ui.view.visit.edit.CopyAction;
import edu.gemini.qpt.ui.view.visit.edit.CutAction;
import edu.gemini.qpt.ui.view.visit.edit.DeleteAction;
import edu.gemini.qpt.ui.view.visit.edit.PasteAction;
import edu.gemini.qpt.ui.view.visit.edit.SelectAllAction;
import edu.gemini.ui.gface.GSelection;
import edu.gemini.ui.workspace.IShell;
import edu.gemini.ui.workspace.IViewAdvisor;
import edu.gemini.ui.workspace.IViewContext;

public class PlotViewAdvisor implements IViewAdvisor, PropertyChangeListener {

    private final PlotViewer viewer = new PlotViewer(new VisitController());
    
    public PlotViewAdvisor() {
        viewer.setTranslator(new VisitTranslator());
    }
    
    public void close(IViewContext context) {
    }

    @SuppressWarnings("serial")
    public void open(final IViewContext context) {
        
        // Configure the context
        context.setContent(viewer.getControl());
        context.setSelectionBroker(viewer);
        context.setTitle("Scheduled Visit Visualizer");
        context.getShell().addPropertyChangeListener(this);        

        context.addRetargetAction(CommonActions.CUT, new CutAction(context.getShell(), viewer));
        context.addRetargetAction(CommonActions.COPY, new CopyAction(context.getShell(), viewer));
        context.addRetargetAction(CommonActions.PASTE, new PasteAction(context.getShell(), viewer));
        context.addRetargetAction(CommonActions.DELETE, new DeleteAction(context.getShell(), viewer));
        context.addRetargetAction(CommonActions.SELECT_ALL, new SelectAllAction(viewer));
                
        ElevationPreference.BOX.addPropertyChangeListener(this);
        PreferenceManager.addPropertyChangeListener(SHOW_IN_VISUALIZER.name(), this);
        
    }

    public void setFocus() {
        viewer.getControl().requestFocusInWindow();
    }

    @SuppressWarnings("unchecked")
    public void propertyChange(PropertyChangeEvent evt) {
        
        if (IShell.PROP_MODEL.equals(evt.getPropertyName())) {
            
            // Move the listeners
            Schedule prev = (Schedule) evt.getOldValue();
            Schedule next = (Schedule) evt.getNewValue();
            if (prev != null) prev.removePropertyChangeListener(Schedule.PROP_CURRENT_VARIANT, this);
            if (next != null) next.addPropertyChangeListener(Schedule.PROP_CURRENT_VARIANT, this);
            
            // Initialize the viewer
            viewer.setModel(next == null ? null : next.getCurrentVariant());
            
        } else if (Schedule.PROP_CURRENT_VARIANT.equals(evt.getPropertyName())) {
            
            // New variant is current
            viewer.setModel((Variant) evt.getNewValue());
            
        } else if (EnumBox.PROP_VALUE.equals(evt.getPropertyName())) {
            
            viewer.refresh();
            
        } else if (SHOW_IN_VISUALIZER.name().equals(evt.getPropertyName())) {
            
            viewer.refresh();
            
        } else if (IShell.PROP_SELECTION.equals(evt.getPropertyName())) {
            
            GSelection<Object> sel = (GSelection<Object>) evt.getNewValue();
            if (sel.size() == 1 && sel.first() instanceof Obs) {
                viewer.getControl().setPreview((Obs) sel.first());                
            } else {
                viewer.getControl().setPreview(null);
            }
            
            
        }
    }

}
