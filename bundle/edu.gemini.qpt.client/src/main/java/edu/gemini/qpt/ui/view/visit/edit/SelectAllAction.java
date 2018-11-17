package edu.gemini.qpt.ui.view.visit.edit;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;

import javax.swing.AbstractAction;

import edu.gemini.qpt.core.Alloc;
import edu.gemini.qpt.core.Variant;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.ui.gface.GSelection;
import edu.gemini.ui.gface.GViewer;

@SuppressWarnings("serial")
public class SelectAllAction extends AbstractAction implements PropertyChangeListener {

    private final GViewer<Variant, Alloc> viewer;

    public SelectAllAction(final GViewer<Variant, Alloc> viewer) {
        this.viewer = viewer;
        setEnabled(false);
        viewer.addPropertyChangeListener(GViewer.PROP_MODEL, this);
    }

    public void actionPerformed(ActionEvent e) {
        ImOption.apply(viewer.getModel()).foreach(variant -> {
            Collection<Alloc> all = variant.getAllocs();
            viewer.setSelection(new GSelection<>(all.toArray(new Alloc[all.size()])));
        });
    }

    public void propertyChange(PropertyChangeEvent evt) {
        setEnabled(viewer.getModel() != null);
    }

}
