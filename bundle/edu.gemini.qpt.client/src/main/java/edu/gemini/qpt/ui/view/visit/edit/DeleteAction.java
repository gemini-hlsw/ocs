package edu.gemini.qpt.ui.view.visit.edit;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;

import edu.gemini.qpt.core.Alloc;
import edu.gemini.qpt.core.Variant;
import edu.gemini.qpt.ui.util.CancelledException;
import edu.gemini.qpt.ui.util.VariantEditHelper;
import edu.gemini.ui.gface.GSelection;
import edu.gemini.ui.gface.GViewer;
import edu.gemini.ui.workspace.IShell;

@SuppressWarnings("serial")
public class DeleteAction extends AbstractAction implements PropertyChangeListener {
    
    private final GViewer<Variant, Alloc> viewer;
    private final IShell shell;
    
    public DeleteAction(final IShell shell, final GViewer<Variant, Alloc> viewer) {
        this.shell = shell;
        this.viewer = viewer;
        setEnabled(false);
        viewer.addPropertyChangeListener(GViewer.PROP_SELECTION, this);
    }
    
    public void actionPerformed(ActionEvent e) {
        GSelection<Alloc> sel = viewer.getSelection();
        VariantEditHelper veh = new VariantEditHelper(shell.getPeer());
        try {
            veh.cut(viewer.getModel(), sel);
        } catch (CancelledException e1) {
            // user hit cancel; this is ok
        }
    }

    public void propertyChange(PropertyChangeEvent evt) {
        GSelection<?> sel = (GSelection<?>) evt.getNewValue();
        setEnabled(sel.isSelectionOf(Alloc.class));
    }    
    
}
