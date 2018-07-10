package edu.gemini.qpt.ui.view.problem;

import static edu.gemini.qpt.ui.view.problem.ProblemAttribute.Description;
import static edu.gemini.qpt.ui.view.problem.ProblemAttribute.Resource;
import static edu.gemini.qpt.ui.view.problem.ProblemAttribute.Severity;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JScrollPane;

import edu.gemini.qpt.core.Marker;
import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.ui.util.ScrollPanes;
import edu.gemini.ui.gface.GTableViewer;
import edu.gemini.ui.workspace.IShell;
import edu.gemini.ui.workspace.IViewAdvisor;
import edu.gemini.ui.workspace.IViewContext;
import edu.gemini.ui.workspace.util.Factory;

public class ProblemViewAdvisor implements IViewAdvisor, PropertyChangeListener {

    // The table viewer
    GTableViewer<Schedule, Marker, ProblemAttribute> viewer = 
        new GTableViewer<Schedule, Marker, ProblemAttribute>(new ProblemController());
    
    // The scroll bar
    JScrollPane scroll = Factory.createStrippedScrollPane(viewer.getTable());
    
    public ProblemViewAdvisor() {
        
        // Set up the viewer
        viewer.setColumns(Severity, Description, Resource);
        viewer.setColumnSize(Severity, 19);
        viewer.setColumnSize(Description, 250, Integer.MAX_VALUE);
        viewer.setColumnSize(Resource, 110, Integer.MAX_VALUE);
        viewer.setDecorator(new ProblemDecorator());
        viewer.setTranslator(new ProblemTranslator());
        viewer.setFilter(new ProblemFilter());
        
        // And the scroll pane
        ScrollPanes.setViewportHeight(scroll, 5);
        
    }

    public void close(IViewContext context) {
        context.getShell().removePropertyChangeListener(IShell.PROP_MODEL, this);
    }

    public void open(IViewContext context) {        
        context.setTitle("Problems");
        context.setContent(scroll);
        context.setSelectionBroker(viewer);        
        context.getShell().addPropertyChangeListener(IShell.PROP_MODEL, this);
    }

    public void setFocus() {
        viewer.getControl().requestFocus();
    }

    public void propertyChange(PropertyChangeEvent evt) {
        viewer.setModel((Schedule) evt.getNewValue());
    }
    
}
