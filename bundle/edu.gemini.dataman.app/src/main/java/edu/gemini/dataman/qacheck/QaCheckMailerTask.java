//
// $Id: QaCheckMailerTask.java 724 2007-01-03 15:06:49Z shane $
//

package edu.gemini.dataman.qacheck;

import edu.gemini.dataman.context.DatamanContext;
import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.dataset.Dataset;
import edu.gemini.spModel.dataset.DatasetLabel;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import java.security.Principal;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is a TimerTask that polls the database looking for datasets with a
 * {@link edu.gemini.spModel.dataset.DatasetQaState#CHECK CHECK} QA state.
 * For each of these, it finds the associated contact scientist and sends an
 * email (grouping all datasets for a particular contact scientist into one
 * email).
 */
final class QaCheckMailerTask extends TimerTask {
    private static final Logger LOG = Logger.getLogger(QaCheckMailerTask.class.getName());

    private static InternetAddress SENDER;

    static {
        try {
            SENDER = new InternetAddress("noreply@gemini.edu");
        } catch (AddressException ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
            throw new RuntimeException("cannot parse address noreplay@gemini.edu");
        }
    }

    private DatamanContext _ctx;
    private Collection<InternetAddress> _ccList;
    private final Set<Principal> _user;

    QaCheckMailerTask(DatamanContext ctx, Set<Principal> user) {
        _ctx    = ctx;
        _ccList = ctx.getConfig().getCheckMailCc();
        _user   = user;
    }

    private Map<InternetAddress, Collection<Dataset>> _getMap() {
        Set<IDBDatabaseService> dbs = _ctx.getDatabases();
        if (dbs == null) return null;

        Map<InternetAddress, Collection<Dataset>> map = null;
//        RemoteException rex = null;
        for (IDBDatabaseService db : dbs) {
//            try {
                map = QaCheckFunctor.getCheckDatasets(db, _user);
//            } catch (RemoteException ex) {
//                rex = ex;
//            }
        }

//        if ((map == null) && (rex != null)) {
//            LOG.log(Level.WARNING, "Problem get CHECK QA states", rex);
//        }
        return map;
    }

    private void _sendMail(Map<InternetAddress, Collection<Dataset>> map)
            throws MessagingException {

        Properties sessionProps = new Properties();
        sessionProps.put("mail.transport.protocol", "smtp");
        sessionProps.put("mail.smtp.host", _ctx.getConfig().getSmtpHost());
        Session session = Session.getInstance(sessionProps, null);
        for (InternetAddress ia : map.keySet()) {
            _sendMail(session, ia, map.get(ia));
        }
    }

    private void _sendMail(Session session, InternetAddress to,
                           Collection<Dataset> dsets)
            throws MessagingException {

        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(SENDER);
        msg.setRecipient(Message.RecipientType.TO, to);
        msg.setSubject(_formatSubject(dsets));
        msg.setText(_formatBody(dsets));

        if (_ccList.size() > 0) {
            for (InternetAddress cc : _ccList) {
                msg.addRecipient(Message.RecipientType.CC, cc);
            }
        }

        Transport.send(msg);
    }

    private static String _formatSubject(Collection<Dataset> dsets) {
        StringBuilder buf = new StringBuilder();

        int sz = dsets.size();
        buf.append(sz).append(" dataset");
        if (sz > 1) buf.append("s");
        buf.append(" require");
        if (sz == 1) buf.append("s");
        buf.append(" your attention");

        return buf.toString();
    }

    private static String _formatBody(Collection<Dataset> dsets) {
        StringBuilder buf = new StringBuilder();

        int sz = dsets.size();
        buf.append("The following dataset");
        if (sz > 1) buf.append("s");
        buf.append(", for which you are the contact scientist, ");
        if (sz == 1) {
            buf.append("has");
        } else {
            buf.append("have");
        }
        buf.append(" a \"Check\" QA state:\n\n");

        List<Dataset> sortedDsets = new ArrayList<Dataset>(dsets);
        Collections.sort(sortedDsets);

        SPProgramID progId = null;
        for (Dataset dset : dsets) {
            SPObservationID curObsId = dset.getObservationId();
            SPProgramID curProgramID = curObsId.getProgramID();

            if (!curProgramID.equals(progId)) {
                progId = curProgramID;
                buf.append(progId).append("\n");
            }

            DatasetLabel lab = dset.getLabel();
            String   dhsName = dset.getDhsFilename();
            int idx = dhsName.lastIndexOf(".fits");
            if (idx >= 0) dhsName = dhsName.substring(0, idx);

            buf.append(String.format("\t\t%s (%s)\n", lab, dhsName));
        }

        return buf.toString();
    }

    public void run() {
        Map<InternetAddress, Collection<Dataset>> map = _getMap();
        if (map == null) return;
        try {
            _sendMail(map);
        } catch (MessagingException ex) {
            LOG.log(Level.WARNING, "Could not send contact scientist emails", ex);
        }
    }
}
