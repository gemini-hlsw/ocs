package edu.gemini.qpt.ui.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import edu.gemini.qpt.core.Alloc;
import edu.gemini.qpt.ui.util.Platform;
import edu.gemini.ui.gface.GSelection;
import edu.gemini.ui.workspace.IShell;

@SuppressWarnings("serial")
public class ToggleSetupRequiredAction extends AbstractAction implements PropertyChangeListener {

    private final IShell shell;
    
    public ToggleSetupRequiredAction(IShell shell) {
        super("Toggle Setup Time");
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_T, Platform.MENU_ACTION_MASK));
        shell.addPropertyChangeListener(IShell.PROP_SELECTION, this);
        this.shell = shell;
        setEnabled(false);
    }
    
    @SuppressWarnings("unchecked")
    public synchronized void actionPerformed(ActionEvent e) { // non re-entrant, important
        ArrayList<Alloc> updated = new ArrayList<Alloc>();
        for (Alloc a: (GSelection<Alloc>) shell.getSelection()) {
            a = a.toggleSetupTime();
            updated.add(a);
        }
        shell.setSelection(new GSelection<Alloc>(updated.toArray(new Alloc[updated.size()])));
    }

    public synchronized void propertyChange(PropertyChangeEvent evt) {
        GSelection<?> sel = (GSelection<?>) evt.getNewValue();
        setEnabled(false);
        for (Object o: sel) {
            if (o instanceof Alloc) { 
                setEnabled(true);
            } else {
                setEnabled(false);
                break;
            }
        }
    }

}
