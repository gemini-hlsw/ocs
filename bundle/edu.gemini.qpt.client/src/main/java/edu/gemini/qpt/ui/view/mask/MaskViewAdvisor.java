package edu.gemini.qpt.ui.view.mask;

import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.ui.util.ScrollPanes;
import static edu.gemini.qpt.ui.view.mask.MaskAttribute.*;

import edu.gemini.spModel.ictd.CustomMaskKey;
import edu.gemini.spModel.ictd.Availability;

import edu.gemini.ui.gface.GTableViewer;
import edu.gemini.ui.workspace.IShell;
import edu.gemini.ui.workspace.IViewAdvisor;
import edu.gemini.ui.workspace.IViewContext;
import edu.gemini.ui.workspace.util.Factory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.util.Map;

import javax.swing.JScrollPane;



public final class MaskViewAdvisor implements IViewAdvisor, PropertyChangeListener {

    // The table viewer
    final GTableViewer<Schedule, Map.Entry<CustomMaskKey, Availability>, MaskAttribute> viewer =
        new GTableViewer<>(new MaskController());

    // The scroll bar
    final JScrollPane scroll = Factory.createStrippedScrollPane(viewer.getTable());

    public MaskViewAdvisor() {

        // Set up the viewer
        viewer.setColumns(Name, Availability);
        viewer.setColumnSize(Name,         110, Integer.MAX_VALUE);
        viewer.setColumnSize(Availability, 240, Integer.MAX_VALUE);
        viewer.setDecorator(new MaskDecorator());

        // Add the scroll pane.  Height is ignored so just use 0.
        ScrollPanes.setViewportHeight(scroll, 0);
    }

    public void close(IViewContext context) {
        context.getShell().removePropertyChangeListener(IShell.PROP_MODEL, this);
    }

    public void open(IViewContext context) {
        context.setTitle("Masks");
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
