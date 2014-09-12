//
// $Id: QaCheckMailer.java 136 2005-09-15 19:37:59Z shane $
//

package edu.gemini.dataman.qacheck;

import edu.gemini.dataman.context.DatamanContext;

import java.security.Principal;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.*;

/**
 * The QaCheckMailer is charged with notifying (via email) contact scientists
 * who have one or more datasets with a
 * {@link edu.gemini.spModel.dataset.DatasetQaState#CHECK qa state}.
 */
public class QaCheckMailer {
    private static final Logger LOG = Logger.getLogger(QaCheckMailer.class.getName());

    private static final long PERIOD = 1000 * 60 * 60 * 24;  // every 24 hours
//    private static final long PERIOD = 1000 * 15;

    private static final int HOUR = 7;  // send at 7 AM

    private DatamanContext _ctx;
    private Timer _scanTimer;
    private Set<Principal> _user;

    public QaCheckMailer(DatamanContext ctx, Set<Principal> user) {
        _ctx = ctx;
        _user = user;
    }

    private static Date _getStartDate() {
        Calendar cal = GregorianCalendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, HOUR);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        Date appointedDate = cal.getTime();
        Date curDate = new Date();
        if (appointedDate.before(curDate)) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
            appointedDate = cal.getTime();
        }
        return appointedDate;
    }

    /**
     * Starts the mailer, turning on periodic scanning of the ODB and sending
     * of email messages.
     */
    public synchronized void start() {
        LOG.log(Level.FINE, "QaCheckMailer.start()");

        if (_scanTimer != null) return;
        _scanTimer = new Timer("QaCheckMailer", true);

        TimerTask task = new QaCheckMailerTask(_ctx, _user);
        _scanTimer.scheduleAtFixedRate(task, _getStartDate(), PERIOD);
    }

    /**
     * Stops the mailer, turning off period scanning of the ODB.
     */
    public synchronized void stop() {
        LOG.log(Level.FINE, "QaCheckMailer.stop()");

        if (_scanTimer == null) return;
        _scanTimer.cancel();
        _scanTimer = null;
    }
}
