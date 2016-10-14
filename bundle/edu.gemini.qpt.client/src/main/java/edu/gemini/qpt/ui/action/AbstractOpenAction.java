package edu.gemini.qpt.ui.action;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import edu.gemini.ags.api.AgsMagnitude;
import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.core.ScheduleIO;
import edu.gemini.qpt.core.util.LttsServicesClient;
import edu.gemini.qpt.ui.util.AbstractAsyncAction;
import edu.gemini.qpt.ui.util.DefaultDirectory;
import edu.gemini.qpt.ui.util.ProgressDialog;
import edu.gemini.qpt.ui.util.ProgressModel;
import edu.gemini.ui.workspace.IShell;
import edu.gemini.util.security.auth.keychain.KeyChain;

public abstract class AbstractOpenAction extends AbstractAsyncAction {

    private static final Logger LOGGER = Logger.getLogger(AbstractOpenAction.class.getName());

    private final IShell shell;
    protected final KeyChain authClient;
    protected final AgsMagnitude.MagnitudeTable magTable;

    protected AbstractOpenAction(String title, IShell shell, KeyChain authClient, AgsMagnitude.MagnitudeTable magTable) {
        super(title, authClient);
        this.shell = shell;
        this.authClient = authClient;
        this.magTable = magTable;
        authClient.asJava().addListener(this::updateEnabled);
    }

    protected IShell getShell() {
        return shell;
    }

    protected Schedule open() {

        JFileChooser chooser = new JFileChooser(DefaultDirectory.get());
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
        if (JFileChooser.APPROVE_OPTION == chooser.showOpenDialog(shell.getPeer())) {

            ProgressModel pm = new ProgressModel("Opening...", 0);
            pm.setIndeterminate(true);

            ProgressDialog pd = new ProgressDialog(shell.getPeer(), getName(), false, pm);

            try {

                pd.setVisible(true);


                pm.setMessage("Querying Database...");
                File file = chooser.getSelectedFile();
                Schedule sched = null;
                LttsServicesClient.clearInstance();

                for (int i = 0; sched == null ; i++) {
                    try {
                        sched = ScheduleIO.read(file, 1000, authClient, magTable);
                    } catch (TimeoutException te) {
                        pm.setMessage("Retrying (" + i + ") ...");
                        if (pm.isCancelled())
                            throw te;
                    }
                }

                if (pm.isCancelled()) return null;
                LttsServicesClient.getInstance().showStatus(shell.getPeer());
                sched.setFile(file);
                return sched;

            } catch (RemoteException re) {

                LOGGER.log(Level.SEVERE, "Remote Exception", re);

                pd.setVisible(false);
                JOptionPane.showMessageDialog(
                    shell.getPeer(),
                    "There was a problem communicating with the database, sorry.",
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);


            } catch (TimeoutException te) {

                pd.setVisible(false);
                JOptionPane.showMessageDialog(
                        shell.getPeer(),
                        "The database is not available right now, but I will continue searching for it.\n" +
                                "Try back in a few minutes.",
                        "Database Unavailable",
                        JOptionPane.ERROR_MESSAGE);

            } catch (IOException ex) {

                LOGGER.log(Level.SEVERE, "Trouble opening file.", ex);
                pd.setVisible(false);
                JOptionPane.showMessageDialog(shell.getPeer(),
                    "This schedule could not be opened. The error was:\n" + ex.getMessage(),
                    "Problem Opening Schedule", JOptionPane.ERROR_MESSAGE);

            } finally {
                pd.setVisible(false);
                pd.dispose();
            }

        }

        return null;

    }

    protected void updateEnabled() {
        setEnabled(!authClient.asJava().isLocked());
    }

}
