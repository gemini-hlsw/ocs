//
// $Id: TooEmailSender.java 46768 2012-07-16 18:58:53Z rnorris $
//

package edu.gemini.too.email;

import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.shared.mail.MailSender;
import edu.gemini.too.event.api.TooEvent;
import edu.gemini.too.event.api.TooSubscriber;

import javax.mail.Message;
import javax.mail.MessagingException;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sends emails to interested parties when a TOO trigger event is received.
 */
public class TooEmailSender implements TooSubscriber {
    private static final Logger LOG = Logger.getLogger(TooEmailSender.class.getName());

    private final TooEmail _mail;
    private final IDBDatabaseService _db;

    public TooEmailSender(TooEmailConfig conf, IDBDatabaseService db) {
        _mail = new TooEmail(conf);
        _db   = db;
    }

    public void tooObservationReady(TooEvent evt) {
        final SPObservationID obsId = evt.report().getObservationId();
        if (obsId == null) {
            LOG.warning("Received a ToO event without an obs id");
            return;

        }

        LOG.info("TOO observation ready: " + obsId);
        final ISPObservation obs = _db.lookupObservationByID(obsId);
        if (obs == null) {
            LOG.warning("Received ToO event for observation not in the database: " + obsId);
            return;
        }

        try {
            final Message msg = _mail.createMessage(obs);
            if (msg == null) {
                LOG.warning("Could not create mail message.");
            } else {
                LOG.info("Sending TOO email:\n" + msg);
                MailSender.DEFAULT_SENDER.send(msg);
                LOG.info("Sent TOO email.");
            }
        } catch (MessagingException ex) {
            LOG.log(Level.WARNING, "Could not send email for too event.", ex);
        }
    }
}
