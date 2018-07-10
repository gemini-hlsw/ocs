package edu.gemini.qpt.ui.view.visit;

import static edu.gemini.qpt.ui.view.visit.VisitAttribute.BG;
import static edu.gemini.qpt.ui.view.visit.VisitAttribute.Config;
import static edu.gemini.qpt.ui.view.visit.VisitAttribute.Dur;
import static edu.gemini.qpt.ui.view.visit.VisitAttribute.Group;
import static edu.gemini.qpt.ui.view.visit.VisitAttribute.Inst;
import static edu.gemini.qpt.ui.view.visit.VisitAttribute.Observation;
import static edu.gemini.qpt.ui.view.visit.VisitAttribute.Start;
import static edu.gemini.qpt.ui.view.visit.VisitAttribute.Steps;
import static edu.gemini.qpt.ui.view.visit.VisitAttribute.Target;
import static edu.gemini.qpt.ui.view.visit.VisitAttribute.WFS;

import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import edu.gemini.qpt.core.Alloc;
import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.core.Variant;
import edu.gemini.qpt.ui.CommonActions;
import edu.gemini.qpt.ui.util.ScrollPanes;
import edu.gemini.qpt.ui.view.visit.edit.CopyAction;
import edu.gemini.qpt.ui.view.visit.edit.CutAction;
import edu.gemini.qpt.ui.view.visit.edit.DeleteAction;
import edu.gemini.qpt.ui.view.visit.edit.PasteAction;
import edu.gemini.qpt.ui.view.visit.edit.SelectAllAction;
import edu.gemini.ui.gface.GTableViewer;
import edu.gemini.ui.workspace.IShell;
import edu.gemini.ui.workspace.IViewAdvisor;
import edu.gemini.ui.workspace.IViewContext;
import edu.gemini.ui.workspace.util.Factory;

public class VisitViewAdvisor implements IViewAdvisor, PropertyChangeListener {

    // The viewer, which is the main thing here.
    GTableViewer<Variant, Alloc, VisitAttribute> viewer = 
        new GTableViewer<Variant, Alloc, VisitAttribute>(new VisitController());
    
    // More UI stuff
    JScrollPane scroll = Factory.createStrippedScrollPane(viewer.getTable());
    
    public VisitViewAdvisor() {
        
        // Set up the viewer
        viewer.setColumns(Group, Start, Dur, BG, Observation, Steps, Inst, Config, WFS, Target);
        viewer.setColumnSize(Group, 10);        
        viewer.setColumnSize(Start, 35);
        viewer.setColumnSize(Dur, 35);
        viewer.setColumnSize(BG, 30);
        viewer.setColumnSize(Observation, 110);
        viewer.setColumnSize(Steps, 45);
        viewer.setColumnSize(Inst, 50);
        viewer.setColumnSize(Config, 50, Integer.MAX_VALUE);
        viewer.setColumnSize(WFS, 50);
        viewer.setDecorator(new VisitDecorator());
        viewer.setTranslator(new VisitTranslator());
        viewer.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        
        viewer.getTable().setShowGrid(false); // ?
        viewer.getTable().setIntercellSpacing(new Dimension(0, 0));
        
        // Set up the scroll bar
        ScrollPanes.setViewportHeight(scroll, 5);
        
    }
    
    
    @SuppressWarnings("serial")
    public void open(IViewContext context) {

        // Set up the context
        context.setTitle("Scheduled Visits");
        context.setSelectionBroker(viewer);
        context.setContent(scroll);
        context.getShell().addPropertyChangeListener(IShell.PROP_MODEL, this);
        
        context.addRetargetAction(CommonActions.CUT, new CutAction(context.getShell(), viewer));
        context.addRetargetAction(CommonActions.COPY, new CopyAction(context.getShell(), viewer));
        context.addRetargetAction(CommonActions.PASTE, new PasteAction(context.getShell(), viewer));
        context.addRetargetAction(CommonActions.DELETE, new DeleteAction(context.getShell(), viewer));
        context.addRetargetAction(CommonActions.SELECT_ALL, new SelectAllAction(viewer));
        
    }

    public void close(IViewContext context) {
        // Nothing to do
    }

    public void setFocus() {
        viewer.getTable().requestFocus();
    }

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
            
        }        
    }

}
