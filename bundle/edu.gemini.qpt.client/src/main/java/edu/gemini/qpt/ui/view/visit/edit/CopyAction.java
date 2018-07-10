package edu.gemini.qpt.ui.view.visit.edit;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;

import edu.gemini.qpt.core.Alloc;
import edu.gemini.ui.gface.GSelection;
import edu.gemini.ui.gface.GViewer;
import edu.gemini.ui.workspace.IShell;

@SuppressWarnings("serial")
public class CopyAction extends AbstractAction implements PropertyChangeListener {
    
    private final IShell shell;
    private final GViewer<?, Alloc> viewer;
    
    public CopyAction(final IShell shell, final GViewer<?, Alloc> viewer) {
        this.shell = shell;
        this.viewer = viewer;
        setEnabled(false);
        viewer.addPropertyChangeListener(GViewer.PROP_SELECTION, this);
    }
    
    public void actionPerformed(ActionEvent e) {
        GSelection<Alloc> sel = viewer.getSelection();
        shell.getWorkspaceClipboard().setContents(sel, null);
    }

    public void propertyChange(PropertyChangeEvent evt) {
        GSelection<?> sel = (GSelection<?>) evt.getNewValue();
        setEnabled(sel.isSelectionOf(Alloc.class));
    }    
    
}
