package edu.gemini.qpt.ui.view.visit.edit;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.FlavorEvent;
import java.awt.datatransfer.FlavorListener;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;

import javax.swing.AbstractAction;

import edu.gemini.qpt.core.Alloc;
import edu.gemini.qpt.core.Variant;
import edu.gemini.qpt.ui.util.CancelledException;
import edu.gemini.qpt.ui.util.VariantEditHelper;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.ui.gface.GSelection;
import edu.gemini.ui.gface.GViewer;
import edu.gemini.ui.workspace.IShell;

// not threadsafe, but does'nt really matter
@SuppressWarnings("serial")
public class PasteAction extends AbstractAction implements FlavorListener, PropertyChangeListener {

    enum Resolutions {
        Skip,
        Replace,
        Cancel
    }

    private static final DataFlavor SELECTION_OF_ALLOCS =
        GSelection.flavorForSelectionOf(Alloc.class);

    private final IShell shell;
    private final GViewer<Variant, Alloc> viewer;

    private boolean modelAvailable, clipAvailable;

    public PasteAction(final IShell shell, final GViewer<Variant, Alloc> viewer) {
        this.shell = shell;
        this.viewer = viewer;
        shell.getWorkspaceClipboard().addFlavorListener(this);
        viewer.addPropertyChangeListener(GViewer.PROP_MODEL, this);
        updateEnabledState();
    }

    @SuppressWarnings("unchecked")
    public void actionPerformed(ActionEvent e) {
        ImOption.apply(viewer.getModel()).foreach(target -> {
            try {
                GSelection<Alloc> paste = (GSelection<Alloc>) shell.getWorkspaceClipboard().getData(SELECTION_OF_ALLOCS);
                VariantEditHelper veh = new VariantEditHelper(shell.getPeer());
                GSelection<Alloc> pasted = veh.paste(target, paste);
                viewer.setSelection(pasted);

            } catch (UnsupportedFlavorException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (CancelledException ce) {
                // ok
            }
        });
    }

    public void flavorsChanged(FlavorEvent e) {
        Clipboard clip = shell.getWorkspaceClipboard();
        clipAvailable = clip.isDataFlavorAvailable(SELECTION_OF_ALLOCS);
        updateEnabledState();
    }

    private void updateEnabledState() {
        setEnabled(modelAvailable && clipAvailable);
    }

    public void propertyChange(PropertyChangeEvent evt) {
        modelAvailable = viewer.getModel() != null;
        updateEnabledState();
    }

}
