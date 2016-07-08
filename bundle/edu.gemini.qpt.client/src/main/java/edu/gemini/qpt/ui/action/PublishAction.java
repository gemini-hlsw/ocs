package edu.gemini.qpt.ui.action;

import edu.gemini.util.ssh.*;
import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.core.ScheduleIO;
import edu.gemini.qpt.core.Variant;
import edu.gemini.qpt.ui.html.ScheduleHTML;
import edu.gemini.qpt.ui.util.*;
import edu.gemini.spModel.core.Site;
import edu.gemini.ui.workspace.IShell;
import edu.gemini.util.security.auth.keychain.KeyChain;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import scala.util.Try;

/**
 * Saves the current model as a PDF file (work in progress). This action is enabled if the
 * current model is non-null.
 *
 * @author rnorris
 */
public class PublishAction extends AbstractAsyncAction implements PropertyChangeListener {

    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(PublishAction.class.getName());

//	private static final String[] MONTHS = { "jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec" };

    private static final long serialVersionUID = 1L;

    private final IShell shell;

    private final Destination INTERNAL, PACHON;

    private PublishDialog.PublishOptions opts = PublishDialog.defaultOptions();

    public PublishAction(IShell shell, KeyChain authClient, Destination internal, Destination pachon) {
        super("Publish...", authClient);
        this.INTERNAL = internal;
        this.PACHON = pachon;
        this.shell = shell;
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_P, Platform.MENU_ACTION_MASK));
        shell.addPropertyChangeListener(this);
    }

    public void propertyChange(PropertyChangeEvent evt) {
        setEnabled(shell.getModel() != null);
    }

    @Override
    protected void asyncActionPerformed(ActionEvent e) {

        shell.getPeer().getGlassPane().setVisible(true);

        Schedule sched = (Schedule) shell.getModel();

        int nonEmptyVariants = 0;
        for (Variant v : sched.getVariants()) {
            if (!v.isEmpty())
                ++nonEmptyVariants;
        }

        ProgressModel pm = new ProgressModel("Preparing...", nonEmptyVariants + 3);

        ProgressDialog pid = new ProgressDialog(shell.getPeer(), getName(), false, pm);

        try {

            final scala.Option<PublishDialog.PublishOptions> res = PublishDialog.prompt(shell.getPeer(), opts);
            if (res.isEmpty()) return; // cancelled

            opts = res.get();

            final boolean web       = opts.publishType() == PublishDialog.PublishWeb$.MODULE$;
            final boolean qcMarkers = opts.publishType() == PublishDialog.PublishPreviewWithMarkers$.MODULE$;
            final String password   = opts.password();
            final boolean utc       = opts.time() == PublishDialog.Utc$.MODULE$;

            final Destination[] destinations;
            if (web) {

                // Get the destination(s)
                destinations =
                        sched.getSite() == Site.GS ?
                                new Destination[]{INTERNAL, PACHON} :
                                new Destination[]{INTERNAL};

                // Show progress dialog
                pm.setMax(nonEmptyVariants * (destinations.length + 1) + 7);

            } else {
                destinations = new Destination[0];
            }

            pid.setVisible(true);

            // Get yy mmm dd
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            cal.setTimeInMillis(sched.getEnd());

            // Directory chain
            String[] DIRECTORY_CHAIN = {
                    sched.getSite() == Site.GS ? "GSqueue" : "GNqueue",
                    "qpt-plans",
                    Integer.toString(cal.get(Calendar.YEAR)),
                    String.format("%02d", cal.get(Calendar.MONTH) + 1),
                    String.format("%02d", cal.get(Calendar.DAY_OF_MONTH)),
            };


            // Create our local directories
            File dir = new File(System.getProperty("java.io.tmpdir"));
            for (String name : DIRECTORY_CHAIN) {
                dir = new File(dir, name);
            }
            dir.mkdirs();

            DateFormat df = new SimpleDateFormat("yyyyMMdd");
            df.setTimeZone(TimeZone.getTimeZone("UTC"));
            String prefix = df.format(cal.getTime());

            // Create the html stuff in temp
            File htmlFile = ScheduleHTML.writeHTML(dir, sched, pm, prefix, qcMarkers, utc);

            pm.work();
            pm.setMessage("Writing QPT file...");
            File qptFile = new File(htmlFile.getParentFile(), prefix + ".qpt");
            ScheduleIO.write(sched, qptFile);
            LOG.fine("Wrote QPT file to " + qptFile.getAbsolutePath());

            for (Destination dest : destinations) {
                // Connect FTP client and login
                pm.work();
                pm.setMessage("Connecting to " + dest.config.getHost());

                SftpSession session;
                final Try<SftpSession> result = SftpSession$.MODULE$.connect(dest.withPassword(password).config);
                if (result.isFailure()) {
                    final Throwable t = ((scala.util.Failure<SftpSession>) result).exception();
                    final String destString = dest.config.toString();
                    LOG.log(Level.WARNING, String.format("Could not sftp %s", destString), t);
                    throw new RuntimeException(String.format("Couldn't connect to %s: %s",  destString, t.getMessage()));
                } else {
                    session = result.get();

                    // Chdir if needed.
                    if (dest.root != null) session.remoteCd(dest.root);

                    // Create dirs as needed
                    pm.work();
                    pm.setMessage("Traversing...");
                    for (String name : DIRECTORY_CHAIN) {
                        session.remoteMkDir(name, false);
                        session.remoteCd(name);
                    }

                    // Copy all files.
                    for (File file : dir.listFiles()) {
                        pm.work();
                        pm.setMessage("Uploading " + file.getName());
                        session.copyLocalToRemote(file, ".", true);
                    }

                    pm.work();
                    pm.setMessage("Done.");

                    session.disconnect();
                }
            }


            // Preview
            pm.work();
            pm.setMessage("Opening plan...");
            if (web) {

                for (Destination dest : destinations) {
                    if (dest.httpRoot == null) continue;
                    StringBuilder url = new StringBuilder(dest.httpRoot);
                    for (String name : DIRECTORY_CHAIN)
                        url.append(name).append("/");
                    url.append(htmlFile.getName());
                    Platform.displayURL(url.toString());
                }

            } else {
                Platform.displayURL(htmlFile.toURL());
            }
            pm.work();

        } catch (CancelledException ce) {

            LOG.info("Operation was cancelled by user.");

        } catch (Exception ex) {

            LOG.log(Level.WARNING, "Trouble publishing schedule.", ex);
            pid.setVisible(false);
            JOptionPane.showMessageDialog(shell.getPeer(),
                    "There was a problem publishing the schedule:\n" + ex.getMessage(),
                    getName(), JOptionPane.ERROR_MESSAGE);

        } finally {

            // Hide and dispose of progress dialog
            pid.setVisible(false);
            pid.dispose();

            shell.getPeer().getGlassPane().setVisible(false);

        }

    }

    public static final class Destination {
        final SshConfig config;
        final String root;
        final String httpRoot;

        public Destination(String host, String user, String root, String httpRoot) {
            this(new DefaultSshConfig(host, user, "", SshConfig$.MODULE$.DEFAULT_TIMEOUT()), root, httpRoot);
        }

        private Destination(SshConfig config, String root, String httpRoot) {
            this.config   = config;
            this.root     = root;
            this.httpRoot = httpRoot;
        }

        public Destination withPassword(String password) {
            final SshConfig c = new DefaultSshConfig(config.getHost(), config.getUser(), password, config.getTimeout());
            return new Destination(c, root, httpRoot);
        }
    }
}
