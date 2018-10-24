//
// $Id: OdbMailAgent.java 4410 2004-02-02 00:51:35Z shane $
//
package edu.gemini.dbTools.mail;

import edu.gemini.dbTools.odbState.OdbStateConfig;
import edu.gemini.dbTools.odbState.OdbStateIO;
import edu.gemini.dbTools.odbState.ProgramState;
import edu.gemini.shared.mail.MailSender;
import edu.gemini.shared.mail.MailUtil;
import edu.gemini.shared.util.FileUtil;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spdb.cron.CronStorage;

import javax.mail.Message;
import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class OdbMailAgent {

    private final OdbMailConfig mailConfig;
    private final OdbStateConfig stateConfig;

    public OdbMailAgent(final OdbMailConfig mailConfig, final OdbStateConfig stateConfig) {
        this.mailConfig = mailConfig;
        this.stateConfig = stateConfig;
    }

    private static MailSender _mailSender = MailSender.DEFAULT_SENDER;

    /**
     * Sets the mail sender to use, if the default one is not appropriate
     * for some reason (for example, if a test email sender which doesn't
     * actually send the emails is needed).
     */
    public static synchronized void setMailSender(final MailSender sender) {
        if (sender == null) throw new NullPointerException();

        _mailSender = sender;
    }

    /** Gets the mail sender in use. */
    private static synchronized MailSender getMailSender() {
        return _mailSender;
    }

    /**
     * Gets the state on disk as a Map keyed by program id.  Each
     * value is a {@link ProgramState} object.
     */
    private Map<SPProgramID, ProgramState> _readStoredState(final Logger log) throws IOException {
        final ProgramState[] state = OdbStateIO.readState(mailConfig.stateFile, log);
        return _hashState(state);
    }

    /**
     * Gets a current version of the state.
     * Each value is a {@link ProgramState} object.
     */
    private Map<SPProgramID, ProgramState> _getCurrentState(final Logger log) throws IOException {
        final File curState = stateConfig.stateFile;

        final File newMailState = _getTmpStateFile();
        FileUtil.copy(curState, newMailState, false);

        final ProgramState[] state = OdbStateIO.readState(newMailState, log);
        return _hashState(state);
    }

    private File _getTmpStateFile() {
        final File mailState = mailConfig.stateFile;
        final File parent = mailState.getParentFile();
        final String filename = mailState.getName() + ".TMP";
        return new File(parent, filename);
    }

    private static Map<SPProgramID, ProgramState> _hashState(final ProgramState[] state) {
        final Map<SPProgramID, ProgramState> m = new HashMap<>();
        for (final ProgramState ps : state) {
            m.put(ps.getProgramId(), ps);
        }
        return m;
    }

    /** Writes the current state to disk. */
    private void _updateState() {
        // Delete the existing state.
        final File oldMailState = mailConfig.stateFile;
        oldMailState.delete();

        // Move the tmp state to the old state.
        final File newMailState = _getTmpStateFile();
        newMailState.renameTo(oldMailState);
    }

    private void sendMail(final OdbMail[] mails, final Logger log) {
        for (final OdbMail mail : mails) {
            try {
                final OdbMailEvent evt = mail.getMailEvent();
                final Message msg = mailConfig.createMessage();
                if (msg != null) {
                    final PrepareMessageAction pma = new PrepareMessageAction(log, mail, msg, mailConfig);
                    evt.doAction(pma);
                    if (pma.isReadyToSend()) {
                        getMailSender().send(msg);
                        log.info("Sent message:\n" + MailUtil.toString(msg));
                    }
                }
            } catch (Exception e) {
                log.log(Level.SEVERE, "problem sending message", e);
            }
        }
    }

    /*
    private static void warnDatabaseIsDown() {
        InternetAddress[] recipients = OdbMailConfig.getWarningRecipients();
        if ((recipients == null) || (recipients.length == 0)) {
            LOG.warn("There are no warning recipients configured.");
            return;
        }

        MailTemplate tmpl = OdbMailTemplate.CANT_FIND_DATABASE;
        String siteHost = OdbMailConfig.getSiteHost();
        if (siteHost == null) siteHost = "unknown";
        Properties props = new Properties();
        props.setProperty("SITE_HOST", siteHost);
        String text = tmpl.substitute(props);

        Message msg = OdbMailConfig.createMessage();
        try {
            msg.setFrom(OdbMailConfig.getSender());
            msg.addRecipients(Message.RecipientType.TO, recipients);
            msg.setSubject("WARNING: ODB at '" + siteHost +
                           "' is not reachable");
            msg.setText(text);

            getMailSender().send(msg);
        } catch (MessagingException ex) {
            LOG.error("Problem sending warning email", ex);
        }
    }
    */

    /**
     * Executes one cycle of operation which includes: <ul>
     * <li>reads last observation state from disk
     * <li>fetches current observation state from the database
     * <li>sends an email for set of observations in each program whose state
     * has changed in such a way that an email event is generated
     * <li>writes the current observation state to disk for the next cycle
     * </ul>
     */
    public void executeOnce(final Logger log) throws IOException {
        // Get the last known state from disk
        final Map<SPProgramID, ProgramState> lastState = _readStoredState(log);

        // Get the current state from the database.
        final Map<SPProgramID, ProgramState> currentState;
        try {
            currentState = _getCurrentState(log);
        } catch (IOException ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            return;
        }

        // For each program, determine whether we need to send emails.
        // If so send them.
        for (final Object o : currentState.keySet()) {
            final SPProgramID progId = (SPProgramID) o;

            final ProgramState lastProg;
            lastProg = lastState.get(progId);
            if (lastProg == null) {
                // Nothing was known about this program before.  So no emails.
                // It is important to skip unknown programs.  Otherwise, if
                // the state file is ever lost, then we could end up with a
                // flood of emails about "new" observations.
                continue;
            }

            final ProgramState curProg;
            curProg = currentState.get(progId);

            final OdbMail[] mails = OdbMail.create(log, lastProg, curProg);
            if ((mails == null) || (mails.length == 0)) continue;
            sendMail(mails, log);
        }

        // Write the current state for comparison next time.
        _updateState();
    }

    public static void run(final CronStorage store, final Logger log, final Map<String, String> env, Set<Principal> user) throws IOException {
        new OdbMailAgent(new OdbMailConfig(store.tempDir(), env), new OdbStateConfig(store.tempDir())).executeOnce(log);
    }
}
