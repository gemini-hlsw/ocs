package edu.gemini.qpt.ui.view.variant.edit;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import edu.gemini.qpt.core.Variant;
import edu.gemini.ui.gface.GSelection;
import edu.gemini.ui.gface.GViewer;
import edu.gemini.ui.workspace.IShell;

@SuppressWarnings("serial")
public class DeleteAction extends AbstractAction implements PropertyChangeListener {
    
    private final IShell shell;
    private final GViewer<?, Variant> viewer;
    
    public DeleteAction(final IShell shell, final GViewer<?, Variant> viewer) {
        this.shell = shell;
        this.viewer = viewer;
        setEnabled(false);
        viewer.addPropertyChangeListener(GViewer.PROP_SELECTION, this);
    }
    
    public void actionPerformed(ActionEvent e) {
        GSelection<Variant> sel = viewer.getSelection();
        for (Variant v: sel) {
            
            // Delete the selected variant. Selection will change.
            if (JOptionPane.NO_OPTION == JOptionPane.showConfirmDialog(shell.getPeer(), 
                        "Do you really want to delete this variant?",
                        "Confirm Delete", JOptionPane.YES_NO_OPTION))
                    continue;
            
            v.getSchedule().removeVariant(v);
        }
    }

    public void propertyChange(PropertyChangeEvent evt) {
        GSelection<?> sel = (GSelection<?>) evt.getNewValue();
        setEnabled(sel.isSelectionOf(Variant.class));
    }    
    
}
