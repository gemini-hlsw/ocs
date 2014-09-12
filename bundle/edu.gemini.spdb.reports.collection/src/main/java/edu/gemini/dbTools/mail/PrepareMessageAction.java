//
// $Id: PrepareMessageAction.java 5617 2004-12-03 20:34:32Z shane $
//
package edu.gemini.dbTools.mail;

import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.shared.mail.MailTemplate;
import edu.gemini.spModel.core.SPProgramID;

import javax.mail.Message;
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

    private InternetAddress[] getPiAddresses(final String action, final boolean logError) {
        final InternetAddress[] adrs = _mail.getInternetAddresses().getPiAddresses();
        if ((adrs == null) || (adrs.length == 0)) {
            if (logError) {
                _log.warning(action + ": no PI addresses. Prog id = " +
                        _mail.getProgramId());
            }
            return null;
        }
        return adrs;
    }

    private InternetAddress[] getNgoAddresses(final String action, final boolean logError) {
        final InternetAddress[] adrs = _mail.getInternetAddresses().getNgoAddresses();
        if ((adrs == null) || (adrs.length == 0)) {
            if (logError) {
                _log.warning(action + ": no NGO addresses. Prog id = " +
                        _mail.getProgramId());
            }
            return null;
        }
        return adrs;
    }

    private InternetAddress[] getContactAddresses(final String action, final boolean logError) {
        final InternetAddress[] adrs = _mail.getInternetAddresses().getContactAddresses();
        if ((adrs == null) || (adrs.length == 0)) {
            if (logError) {
                _log.warning(action + ": no Gemini contact addresses. Prog id = " +
                        _mail.getProgramId());
            }
            return null;
        }
        return adrs;
    }

    /* Proposed change.
    private boolean _addRecipients(InternetAddress[] toAddrs,
                                   InternetAddress[] ccAddrs,
                                   InternetAddress[] initiatorAddrs)
            throws MessagingException {

        boolean recipientExists = false;

        // Trys to set the "to" recipients with toAddrs.  If there are
        // no toAddrs, then make the ccAddrs the "to" recipients.
        boolean toPresent = (toAddrs != null) && (toAddrs.length > 0);
        Message.RecipientType ccType = Message.RecipientType.TO;
        if (toPresent) {
            _msg.addRecipients(Message.RecipientType.TO, toAddrs);
            ccType = Message.RecipientType.CC;
            recipientExists = true;
        }
        if ((ccAddrs != null) && (ccAddrs.length > 0)) {
            _msg.addRecipients(ccType, ccAddrs);
            recipientExists = true;
        }

        // Also "cc" the initiators, unless there are no toAddrs or
        // ccAddrs, in which case make the initiators the "to"
        // recipients.
        if ((initiatorAddrs != null) && (initiatorAddrs.length > 0)) {
            ccType = Message.RecipientType.TO;
            if (recipientExists) ccType = Message.RecipientType.CC;
            _msg.addRecipients(ccType, initiatorAddrs);
            recipientExists = true;
        }

        // Return false if there was nobody to send the mail to.
        return recipientExists;
    }
    */

    public void down_ForReview() {
        final String action = "Down to ForReview";
        final Properties props = getProperties();
        final String body = OdbMailTemplate.DOWN_FOR_REVIEW.subsitute(props);

        try {
            /*
            InternetAddress[] ngoAdrs = getNgoAddresses(action, true);
            InternetAddress[] piAdrs  = getPiAddresses(action, false);
            InternetAddress[] iniAdrs = getContactAddresses(action, false);
            _addRecipients(ngoAdrs, piAdrs, iniAdrs);
            */

            final InternetAddress[] ngoAdrs = getNgoAddresses(action, true);
            final InternetAddress[] piAdrs = getPiAddresses(action, false);

            if (ngoAdrs == null) {
                if (piAdrs == null) return; // nobody to email
                // Use the pi contact address as the TO address.
                _msg.addRecipients(Message.RecipientType.TO, piAdrs);
            } else {
                _msg.addRecipients(Message.RecipientType.TO, ngoAdrs);
                if (piAdrs != null) {
                    _msg.addRecipients(Message.RecipientType.CC, piAdrs);
                }
            }

            final String subj = getSubject_Down_ForReview(_mail.getProgramId());
            _msg.setSubject(subj);
            _msg.setText(body);

            _readyToSend = true;
        } catch (MessagingException e) {
            _log.log(Level.SEVERE, "Problem configuring message", e);
        }
    }

    public void up_Ready() {
        final String action = "Up to Ready";
        final Properties props = getProperties();
        final String body = OdbMailTemplate.UP_READY.subsitute(props);

        try {
            InternetAddress[] adrs = getPiAddresses(action, true);
            if (adrs == null) return;
            _msg.addRecipients(Message.RecipientType.TO, adrs);

            adrs = getNgoAddresses(action, false);
            if (adrs != null) {
                _msg.addRecipients(Message.RecipientType.CC, adrs);
            }

            final String subj = getSubject_Up_Ready(_mail.getProgramId());
            _msg.setSubject(subj);
            _msg.setText(body);

            _readyToSend = true;
        } catch (MessagingException e) {
            _log.log(Level.SEVERE, "Problem configuring message", e);
        }
    }

    public void up_ForActivation() {
        final String action = "Up to ForActivation";
        final Properties props = getProperties();
        final String body = OdbMailTemplate.UP_FOR_ACTIVATION.subsitute(props);

        try {
            InternetAddress[] adrs = getContactAddresses(action, true);
            if (adrs == null) return;
            _msg.addRecipients(Message.RecipientType.TO, adrs);

            adrs = getPiAddresses(action, false);
            if (adrs != null) {
                _msg.addRecipients(Message.RecipientType.CC, adrs);
            }

            final String subj = getSubject_Up_ForActivation(_mail.getProgramId());
            _msg.setSubject(subj);
            _msg.setText(body);

            _readyToSend = true;
        } catch (MessagingException e) {
            _log.log(Level.SEVERE, "Problem configuring message", e);
        }
    }

    public void down_Phase2() {
        final String action = "Down to Phase 2";
        final Properties props = getProperties();
        final String body = OdbMailTemplate.DOWN_PHASE2.subsitute(props);

        try {
            InternetAddress[] adrs = getPiAddresses(action, true);
            if (adrs == null) return;
            _msg.addRecipients(Message.RecipientType.TO, adrs);

            adrs = getContactAddresses(action, false);
            if (adrs != null) {
                _msg.addRecipients(Message.RecipientType.CC, adrs);
            }

            final String subj = getSubject_Down_Phase2(_mail.getProgramId());
            _msg.setSubject(subj);
            _msg.setText(body);

            _readyToSend = true;
        } catch (MessagingException e) {
            _log.log(Level.SEVERE, "Problem configuring message", e);
        }
    }

    public void observed() {
//        String action = "Observed";
//        Properties props = getProperties();
//        DATE_FORMAT.format();
//        String body = OdbEventTemplate.OBSERVED.subsitute(props);
//
//        try {
//            InternetAddress[] adrs = getNgoAddresses(action, true);
//            if (adrs == null) return;
//            _msg.addRecipients(Message.RecipientType.TO, adrs);
//
//            adrs = getContactAddresses(action, false);
//            if (adrs != null) {
//                _msg.addRecipients(Message.RecipientType.CC, adrs);
//            }
//
//            _msg.setSubject(_mail.getProgramId().toString() +
//                            " Observation(s) Set to \"For Review\"");
//            _msg.setText(body);
//
//            _readyToSend = true;
//        } catch (MessagingException e) {
//            _log.error("Problem configuring message", e);
//        }
    }

    public void up_ForReview() {
        final String action = "Up to ForReview";
        final Properties props = getProperties();
        final String body = OdbMailTemplate.UP_FOR_REVIEW.subsitute(props);

        try {
            final InternetAddress[] ngoAdrs = getNgoAddresses(action, true);
            final InternetAddress[] gemAdrs = getContactAddresses(action, false);

            if (ngoAdrs == null) {
                if (gemAdrs == null) return; // nobody to email
                // Use the Gemini contact address as the TO address.
                _msg.addRecipients(Message.RecipientType.TO, gemAdrs);
            } else {
                _msg.addRecipients(Message.RecipientType.TO, ngoAdrs);
                if (gemAdrs != null) {
                    _msg.addRecipients(Message.RecipientType.CC, gemAdrs);
                }
            }

            final String subj = getSubject_Up_ForReview(_mail.getProgramId());
            _msg.setSubject(subj);
            _msg.setText(body);

            _readyToSend = true;
        } catch (MessagingException e) {
            _log.log(Level.SEVERE, "Problem configuring message", e);
        }
    }
}
