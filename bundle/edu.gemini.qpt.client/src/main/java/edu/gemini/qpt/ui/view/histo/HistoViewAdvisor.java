package edu.gemini.qpt.ui.view.histo;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import edu.gemini.qpt.core.Schedule;
import edu.gemini.ui.workspace.IShell;
import edu.gemini.ui.workspace.IViewAdvisor;
import edu.gemini.ui.workspace.IViewContext;

public class HistoViewAdvisor implements IViewAdvisor, PropertyChangeListener {

    HistoViewer viewer = new HistoViewer();
    
    public void open(IViewContext context) {        
        context.getShell().addPropertyChangeListener(this);
        context.setTitle("RA Distribution");
        context.setContent(viewer.getControl());        
    }

    public void close(IViewContext context) {
    }

    public void setFocus() {
        viewer.getControl().requestFocus();
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (IShell.PROP_MODEL.equals(evt.getPropertyName())) {
            viewer.setModel((Schedule) evt.getNewValue());
        }
    }

}
