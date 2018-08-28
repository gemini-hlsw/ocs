package edu.gemini.qpt.ui.action;

import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.shared.util.Ictd;
import edu.gemini.qpt.ui.util.AbstractAsyncAction;
import edu.gemini.qpt.ui.util.ProgressDialog;
import edu.gemini.qpt.ui.util.ProgressModel;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.spModel.core.Peer;
import edu.gemini.spModel.core.Site;
import edu.gemini.ui.gface.GSelection;
import edu.gemini.ui.workspace.IShell;
import edu.gemini.util.security.auth.keychain.KeyChain;


import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JOptionPane;


/**
 * Queries the ICTD for the latest instrument and mask configuration data,
 * updating the Schedule as necessary.
 */
public final class IctdAction extends AbstractAsyncAction implements PropertyChangeListener {

    private static final long serialVersionUID = 1L;

    private final IShell shell;
    private final KeyChain authClient;

    public IctdAction(IShell shell, final KeyChain authClient) {
        super("Update from ICTD", authClient);

        this.shell      = shell;
        this.authClient = authClient;

        shell.addPropertyChangeListener(this);
        setEnabled(false);
        authClient.asJava().addListener(this::updateEnabled);
    }

    public void asyncActionPerformed(ActionEvent e) {

        final ProgressModel pm = new ProgressModel("Creating...", 0);
        pm.setIndeterminate(true);

        final ProgressDialog pd = new ProgressDialog(shell.getPeer(), getName(), false, pm);
        shell.getPeer().getGlassPane().setVisible(true);
        pd.setVisible(true);

        try {

            final Schedule sched = (Schedule) shell.getModel();
            final Site      site = sched.getSite();
            final Peer      peer = authClient.asJava().peer(site);
            if (peer == null) {
                JOptionPane.showMessageDialog(
                    shell.getPeer(),
                    "Cannot figure out which ODB to query, sorry..",
                    "ICTD Error",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            pm.setMessage("Querying ICTD database ...");

            Ictd.query(authClient, peer, site).biForEach(
                msg -> {
                    pd.setVisible(false);
                    JOptionPane.showMessageDialog(
                        shell.getPeer(),
                        "There was a problem communicating with the ICTD, sorry..",
                        "ICTD Error",
                        JOptionPane.ERROR_MESSAGE);
                },
                ictd -> {
                    pm.setMessage("Updating model...");
                    sched.setIctdSummary(ImOption.apply(ictd));

                    shell.setModel(null);
                    shell.setModel(sched);
                }
            );

        } finally {

            pd.setVisible(false);
            pd.dispose();
            shell.getPeer().getGlassPane().setVisible(false);

        }
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (IShell.PROP_MODEL.equals(evt.getPropertyName())) {
            updateEnabled();
        }
    }

    private void updateEnabled() {
        setEnabled(shell.getModel() != null && !authClient.asJava().isLocked());
    }

}
