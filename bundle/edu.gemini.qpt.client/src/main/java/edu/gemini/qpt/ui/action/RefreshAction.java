package edu.gemini.qpt.ui.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import edu.gemini.ags.api.AgsMagnitude;
import edu.gemini.qpt.core.Alloc;
import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.shared.sp.MiniModel;
import edu.gemini.qpt.core.util.LttsServicesClient;
import edu.gemini.qpt.ui.util.AbstractAsyncAction;
import edu.gemini.qpt.ui.util.ProgressDialog;
import edu.gemini.qpt.ui.util.ProgressModel;
import edu.gemini.spModel.core.Peer;
import edu.gemini.ui.gface.GSelection;
import edu.gemini.ui.workspace.IShell;
import edu.gemini.util.security.auth.keychain.KeyChain;

@SuppressWarnings("serial")
public class RefreshAction extends AbstractAsyncAction implements PropertyChangeListener {

	private static final Logger LOGGER = Logger.getLogger(RefreshAction.class.getName());

	protected final IShell shell;
    protected final KeyChain authClient;
    protected final AgsMagnitude.MagnitudeTable magTable;

	public RefreshAction(IShell shell, KeyChain authClient, AgsMagnitude.MagnitudeTable magTable) {
		super("Refresh", authClient);
        this.authClient = authClient;
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0 /* no modifiers */));
		this.shell = shell;
        this.magTable = magTable;
		setEnabled(false);
		shell.addPropertyChangeListener(this);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void asyncActionPerformed(ActionEvent e) {

		ProgressModel pm = new ProgressModel("Preparing to refresh...", 0);
		pm.setIndeterminate(true);

		ProgressDialog pd = new ProgressDialog(shell.getPeer(), "Refresh", false, pm);
		shell.getPeer().getGlassPane().setVisible(true);
		pd.setVisible(true);

		try {

			Schedule sched = (Schedule) shell.getModel();

            final Peer peer = authClient.asJava().peer(sched.getSite());
            if (peer == null)
                throw new IOException("No peer found for " + sched.getSite());



            pm.setMessage("Querying database...");
			MiniModel miniModel = null;
			for (int i = 1; miniModel == null ; i++) {
				try {
					miniModel = MiniModel.newInstance(authClient, peer, sched.getEnd(), sched.getExtraSemesters(), magTable);
				} catch (TimeoutException te) {
					pm.setMessage("Retrying (" + i + ") ...");
					if (pm.isCancelled())
						throw te;
				}
			}

            LttsServicesClient.newInstance(sched.getStart(), peer);
            LttsServicesClient.getInstance().showStatus(shell.getPeer());
			pm.setMessage("Updating model...");

			// HACK: force viewers to refresh
			GSelection<?> sel = shell.getSelection();
			sched.setMiniModel(miniModel);
			shell.setModel(null);
			shell.setModel(sched);

			// HACK:
			// Ok, this is quite bad. We need to translate the old bogus allocs to
			// the new ones, if the selection contains allocs, otherwise the
			// selection will contain allocs pointing to the old miniModel.
			ArrayList accum = new ArrayList<Object>();
			for (Object o: sel) {
				if (o instanceof Alloc) {
					for (Alloc a: ((Alloc) o).getVariant().getAllocs()) {
						if (a.equals(o))
							accum.add(a);
					}
				} else {
					accum.add(o);
				}
			}
			shell.setSelection(new GSelection(accum.toArray()));


			pm.setMessage("Finishing up...");

		} catch (IOException ioe) {

			LOGGER.log(Level.SEVERE, "IO Exception", ioe);

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

		} finally {
			pd.setVisible(false);
			pd.dispose();
			shell.getPeer().getGlassPane().setVisible(false);
		}

//		// Warn the user if there are misconfigured observations.
//		if (shell.getModel() != null)
//			ConfigErrorDialog.show((Schedule) shell.getModel(), shell.getPeer());


	}

	public void propertyChange(PropertyChangeEvent evt) {
		if (IShell.PROP_MODEL.equals(evt.getPropertyName())) {
			setEnabled(shell.getModel() != null);
		}
	}

}
