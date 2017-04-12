//
// $Id: PrepareMessageAction.java 5617 2004-12-03 20:34:32Z shane $
//
package edu.gemini.dbTools.mail;

import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.shared.mail.MailTemplate;
import edu.gemini.spModel.core.SPProgramID;

import javax.mail.Message;
import static javax.mail.Message.RecipientType.*;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PrepareMessageAction implements OdbMailEvent.Action {

    public static String getSubject_Down_ForReview(final SPProgramID progId) {
        final StringBuilder buf = new StringBuilder(progId.toString());
        buf.append(" Observation(s) Reset to \"For Review\"");
        return buf.toString();
    }

    public static String getSubject_Down_Phase2(final SPProgramID progId) {
        final StringBuilder buf = new StringBuilder(progId.toString());
        buf.append(" Observation(s) Reset to \"Phase II\"");
        return buf.toString();
    }

    public static String getSubject_Up_ForActivation(final SPProgramID progId) {
        final StringBuilder buf = new StringBuilder(progId.toString());
        buf.append(" Observation(s) Set to \"For Activation\"");
        return buf.toString();
    }

    public static String getSubject_Up_ForReview(final SPProgramID progId) {
        final StringBuilder buf = new StringBuilder(progId.toString());
        buf.append(" Observation(s) Set to \"For Review\"");
        return buf.toString();
    }

    public static String getSubject_Up_Ready(final SPProgramID progId) {
        final StringBuilder buf = new StringBuilder(progId.toString());
        buf.append(" Observation(s) Set to \"Ready\"");
        return buf.toString();
    }

    private final Logger _log;
    private final OdbMail _mail;
    private final Message _msg;
    private boolean _readyToSend;

    public PrepareMessageAction(final Logger log, final OdbMail mail, final Message msg, final OdbMailConfig mailConfig) {
        if (mail == null) throw new NullPointerException("mail is null");
        if (msg == null) throw new NullPointerException("msg is null");
        _mail = mail;
        _msg = msg;
        this._log = log;

        // Add the sender as a BCC recipient, in order to get a copy to verify
        // that the email went through.
        final InternetAddress addr = mailConfig.sender;
        try {
            _msg.addRecipient(Message.RecipientType.BCC, addr);
        } catch (MessagingException ex) {
            // this shouldn't happen, but if it does, it shouldn't be fatal
            _log.log(Level.SEVERE, "Could not add sender as BCC recipient.", ex);
        }
    }

    public boolean isReadyToSend() {
        return _readyToSend;
    }

    private Properties getProperties() {
        final Properties props = new Properties();
        props.setProperty(OdbMailTemplate.PROG_ID_VAR,
                          _mail.getProgramId().toString());

        final List<SPObservationID> obsIdList = _mail.getObsIds();
        final boolean plural = obsIdList.size() > 1;
        MailTemplate.addPluralProperties(props, plural);

        final StringBuilder buf = new StringBuilder();
        for (Iterator<SPObservationID> it = obsIdList.iterator(); it.hasNext();) {
            final String obsIdStr = it.next().toString();
            buf.append("\t").append(obsIdStr);
            if (it.hasNext()) buf.append("\n");
        }
        props.setProperty(OdbMailTemplate.OBS_ID_LIST_VAR, buf.toString());

        return props;
    }

    private void go(MailTemplate template,
                    InternetAddress[] to,
                    InternetAddress[] cc0,
                    InternetAddress[] cc1,
                    String subject) {

        try {
            final ProgramInternetAddresses pia = _mail.getInternetAddresses();
            _msg.addRecipients(TO, to);

            final Message.RecipientType ccType = (to.length > 0) ? CC : TO;
            _msg.addRecipients(ccType, cc0);
            _msg.addRecipients(ccType, cc1);

            _msg.setSubject(subject);
            _msg.setText(template.subsitute(getProperties()));

            _readyToSend = _msg.getRecipients(TO).length > 0;
        } catch (MessagingException e) {
            _log.log(Level.SEVERE, "Problem configuring message", e);
        }
    }

    public void down_ForReview() {
        final ProgramInternetAddresses pia = _mail.getInternetAddresses();

        go(
            OdbMailTemplate.DOWN_FOR_REVIEW,
            pia.getNgoAddresses(),
            pia.getContactAddresses(),
            pia.getPiAddresses(),
            getSubject_Down_ForReview(_mail.getProgramId())
        );
    }

    public void up_Ready() {
        final ProgramInternetAddresses pia = _mail.getInternetAddresses();

        go(
            OdbMailTemplate.UP_READY,
            pia.getPiAddresses(),
            pia.getContactAddresses(),
            pia.getNgoAddresses(),
            getSubject_Up_Ready(_mail.getProgramId())
        );
    }

    public void up_ForActivation() {
        final ProgramInternetAddresses pia = _mail.getInternetAddresses();

        go(
            OdbMailTemplate.UP_FOR_ACTIVATION,
            pia.getContactAddresses(),
            pia.getPiAddresses(),
            pia.getNgoAddresses(),
            getSubject_Up_ForActivation(_mail.getProgramId())
        );
    }

    public void down_Phase2() {
        final ProgramInternetAddresses pia = _mail.getInternetAddresses();

        go(
            OdbMailTemplate.DOWN_PHASE2,
            pia.getPiAddresses(),
            pia.getContactAddresses(),
            pia.getNgoAddresses(),
            getSubject_Down_Phase2(_mail.getProgramId())
        );
    }

    public void observed() {
        // Do nothing.
    }

    public void up_ForReview() {
        final ProgramInternetAddresses pia = _mail.getInternetAddresses();

        go(
            OdbMailTemplate.UP_FOR_REVIEW,
            pia.getNgoAddresses(),
            pia.getContactAddresses(),
            pia.getPiAddresses(),
            getSubject_Up_ForReview(_mail.getProgramId())
        );
    }
}
