package edu.gemini.qpt.ui.action;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.core.ScheduleIO;
import edu.gemini.qpt.ui.util.AbstractAsyncAction;
import edu.gemini.qpt.ui.util.DefaultDirectory;
import edu.gemini.qpt.ui.util.ProgressDialog;
import edu.gemini.qpt.ui.util.ProgressModel;
import edu.gemini.ui.workspace.IShell;
import edu.gemini.util.security.auth.keychain.KeyChain;

/**
 * Prompts the user for a file and saves the current model, associating the file with the
 * model for future SaveAction invocations. This method is enabled if the current model is
 * non-null.
 * @author rnorris
 */
public class SaveAsAction extends AbstractAsyncAction implements PropertyChangeListener {

    private static final long serialVersionUID = 1L;

    protected final IShell shell;

    public SaveAsAction(IShell shell, KeyChain authClient) {
        super("Save As...", authClient);
        this.shell = shell;
        shell.addPropertyChangeListener(this);
    }

    public void asyncActionPerformed(ActionEvent e) {

        shell.getPeer().getGlassPane().setVisible(true);

        ProgressModel pm = new ProgressModel("Saving...", 0);
        pm.setIndeterminate(true);
        ProgressDialog pd = new ProgressDialog(shell.getPeer(), getName(), false, pm);

        try {

            JFileChooser chooser = new JFileChooser(DefaultDirectory.get()); // TODO: remember preference
            chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {

                @Override
                public String getDescription() {
                    return "QPT Files";
                }

                @Override
                public boolean accept(File file) {
                    return file.getName().endsWith(".qpt") || file.isDirectory();
                }

            });
            if (JFileChooser.APPROVE_OPTION == chooser.showSaveDialog(shell.getPeer())) {
                File file = chooser.getSelectedFile();
                if (file.exists() && JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(shell.getPeer(),
                            "The file " + file.getName() + " already exists.\nAre you sure you want to overwrite it?",
                            "Confirm Save", JOptionPane.WARNING_MESSAGE, JOptionPane.YES_NO_OPTION))
                        return;

                pd.setVisible(true);
                Schedule sched = (Schedule) shell.getModel();
                sched.setFile(file);
                ScheduleIO.write(sched, file);
                sched.setDirty(false);

                DefaultDirectory.set(file);

            }
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

    public void propertyChange(PropertyChangeEvent evt) {
        setEnabled(shell.getModel() != null);
    }

}
