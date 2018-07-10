package edu.gemini.qpt.ui.view.variant.edit;

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
    private final GViewer<?, Variant> viewer;
    
    private boolean exactlyOneVariantSelected, clipAvailable, setSelectionOnShell = false;
    
    public PasteAction(final IShell shell, final GViewer<?, Variant> viewer) {
        this.shell = shell;
        this.viewer = viewer;
        shell.getWorkspaceClipboard().addFlavorListener(this);
        viewer.addPropertyChangeListener(GViewer.PROP_SELECTION, this);
        updateEnabledState();
    }

    public PasteAction(final IShell shell, final GViewer<?, Variant> viewer, boolean setSelectionOnShell) {
        this(shell, viewer);
        this.setSelectionOnShell = setSelectionOnShell;
    }
    
    @SuppressWarnings("unchecked")
    public void actionPerformed(ActionEvent e) {
        try {
            
            GSelection<Alloc> paste = (GSelection<Alloc>) shell.getWorkspaceClipboard().getData(SELECTION_OF_ALLOCS);
            Variant target = viewer.getSelection().first();
            VariantEditHelper veh = new VariantEditHelper(shell.getPeer());
            GSelection<Alloc> pasted = veh.paste(target, paste);
            (setSelectionOnShell ? shell : viewer).setSelection(pasted);
            
        } catch (UnsupportedFlavorException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (CancelledException ce) {

            // do nothing

        }
        
    }

    public void flavorsChanged(FlavorEvent e) {
//        System.out.println("Flavors changed!");
        Clipboard clip = shell.getWorkspaceClipboard();
        clipAvailable = clip.isDataFlavorAvailable(SELECTION_OF_ALLOCS);
        updateEnabledState();
    }

    private void updateEnabledState() {
//        System.out.println("modelAvailable == " + modelAvailable + ", clipAvailable == " + clipAvailable);
        setEnabled(exactlyOneVariantSelected && clipAvailable);
    }

    public void propertyChange(PropertyChangeEvent evt) {
        GSelection<?> sel = (GSelection<?>) evt.getNewValue();
        exactlyOneVariantSelected = sel.size() == 1;
        updateEnabledState();
    }
    
}
