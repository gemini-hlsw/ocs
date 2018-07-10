package edu.gemini.qpt.ui.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;

import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.core.ScheduleIO;
import edu.gemini.qpt.ui.util.AbstractAsyncAction;
import edu.gemini.qpt.ui.util.Platform;
import edu.gemini.qpt.ui.util.ProgressDialog;
import edu.gemini.qpt.ui.util.ProgressModel;
import edu.gemini.ui.workspace.IShell;
import edu.gemini.util.security.auth.keychain.KeyChain;

/**
 * Saves the current model and sets it to <code>dirty = false</code>. This action is enabled
 * only if the model has an associated file (it has been saved before) and the dirty flag is
 * set.
 * @author rnorris
 */
public class SaveAction extends AbstractAsyncAction implements PropertyChangeListener {

    private static final long serialVersionUID = 1L;

    protected final IShell shell;

    public SaveAction(IShell shell, KeyChain authClient) {
        this("Save", shell, authClient);
        setEnabled(false);
    }

    protected SaveAction(String name, IShell shell, KeyChain authClient) {
        super(name, authClient);
        this.shell = shell;
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, Platform.MENU_ACTION_MASK));
        shell.addPropertyChangeListener(this);
    }

    public void asyncActionPerformed(ActionEvent e) {

        Schedule sched = (Schedule) shell.getModel();

        if (sched.getFile() == null) {

            // [SCT-318] delegate to Save As in this case
            new SaveAsAction(shell, authClient).actionPerformed(e);

        } else {

            shell.getPeer().getGlassPane().setVisible(true);

            ProgressModel pm = new ProgressModel("Saving...", 0);
            pm.setIndeterminate(true);
            ProgressDialog pd = new ProgressDialog(shell.getPeer(), getName(), false, pm);
            pd.setVisible(true);

            try {
                ScheduleIO.write(sched, sched.getFile());
                sched.setDirty(false);
                updateEnabledState(sched);
            } catch (IOException ioe) {
                pd.setVisible(false);
                JOptionPane.showMessageDialog(shell.getPeer(),
                    "This schedule could not be saved. The error was:\n" + ioe.getMessage(),
                    "Problem Saving Schedule", JOptionPane.ERROR_MESSAGE);
            } finally {
                pd.setVisible(false);
                pd.dispose();
                shell.getPeer().getGlassPane().setVisible(false);
            }

        }
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(IShell.PROP_MODEL)) {
            Schedule prev = (Schedule) evt.getOldValue();
            Schedule next = (Schedule) evt.getNewValue();
            if (prev != null) prev.removePropertyChangeListener(this);
            if (next != null) next.addPropertyChangeListener(Schedule.PROP_DIRTY, this);
            updateEnabledState(next);
        } else if (evt.getPropertyName().equals(Schedule.PROP_DIRTY)) {
            Schedule s = (Schedule) evt.getSource();
            updateEnabledState(s);
        }
    }

    protected void updateEnabledState(Schedule s) {
        setEnabled(s != null && s.isDirty());
    }

}
