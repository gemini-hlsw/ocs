//
// $Id: QaCheckFunctor.java 724 2007-01-03 15:06:49Z shane $
//

package edu.gemini.dataman.qacheck;

import edu.gemini.pot.sp.*;
import edu.gemini.pot.spdb.DBAbstractQueryFunctor;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.dataset.Dataset;
import edu.gemini.spModel.dataset.DatasetQaState;
import edu.gemini.spModel.dataset.DatasetRecord;
import edu.gemini.spModel.gemini.obscomp.SPProgram;
import edu.gemini.spModel.obslog.ObsLog;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import java.security.Principal;
import java.util.*;
import java.util.logging.Logger;

/**
 * The GsaStateFunctor collects datasets with QA state
 * {@link edu.gemini.spModel.dataset.DatasetQaState#CHECK}.
 */
final class QaCheckFunctor extends DBAbstractQueryFunctor {
    private static final Logger LOG = Logger.getLogger(QaCheckFunctor.class.getName());

    private Map<InternetAddress, Collection<Dataset>> _checkMap;

    public Map<InternetAddress, Collection<Dataset>> getCheckMap() {
        if (_checkMap == null) return Collections.emptyMap();
        return Collections.unmodifiableMap(_checkMap);
    }

    private Map<InternetAddress, Collection<Dataset>> _getCheckCollection() {
        if (_checkMap == null) {
            _checkMap = new HashMap<InternetAddress, Collection<Dataset>>();
        }
        return _checkMap;
    }

    public void execute(IDBDatabaseService db, ISPNode node, Set<Principal> principals) {
        ISPProgram prog = (ISPProgram) node;
        for (ISPObservation obs : prog.getAllObservations()) {
            _scanObservation(db, obs);
        }
    }

    @SuppressWarnings("unchecked")
    private void _scanObservation(IDBDatabaseService db, ISPObservation obs) {

        final Map<InternetAddress, Collection<Dataset>> map = _getCheckCollection();
        Collection<InternetAddress> addrs = null;

        final ObsLog log = ObsLog.getIfExists(obs);
        final List<DatasetRecord> records = (log == null) ? Collections.<DatasetRecord>emptyList() : log.getAllDatasetRecords();
        for (DatasetRecord rec : records) {
            DatasetQaState qaState = rec.qa.qaState;
            if (DatasetQaState.CHECK.equals(qaState)) {
                if (addrs == null) {
                    addrs = _getEmailAddress(db, obs);
                    if ((addrs == null) || (addrs.size() == 0)) return;
                }

                for (InternetAddress addr : addrs) {
                    Collection<Dataset> dsetCol = map.get(addr);
                    if (dsetCol == null) {
                        dsetCol = new ArrayList<Dataset>();
                        map.put(addr, dsetCol);
                    }
                    dsetCol.add(rec.exec.dataset);
                }
            }
        }
    }



    private static Collection<InternetAddress> _getEmailAddress(IDBDatabaseService db, ISPObservation obs) {
        SPNodeKey key = obs.getProgramKey();
        if (key == null) return null;
        ISPProgram prog = db.lookupProgram(key);
        if (prog == null) return null;

        SPProgram dataObj = (SPProgram) prog.getDataObject();
        String email = dataObj.getContactPerson();
        return _parseAddresses(email);
    }

    private static Collection<InternetAddress> EMPTY_ADDRESSES =
                                                       Collections.emptyList();
    /**
     * Parses a string of comma, semicolon, space, or tab separated email
     * addresses into an array of valid InternetAddresses.  Any invalid
     * addresses are left out of the return array.
     */
    public static Collection<InternetAddress> _parseAddresses(String addresses) {
        if (addresses == null) return EMPTY_ADDRESSES;

        StringTokenizer st = new StringTokenizer(addresses, " \t,;", false);
        List<InternetAddress> lst = new ArrayList<InternetAddress>();
        while (st.hasMoreTokens()) {
            String addrStr = st.nextToken();
            InternetAddress addr;
            try {
                addr = new InternetAddress(addrStr);
                addr.validate();
            } catch (AddressException e) {
                LOG.warning("illegal email address: " + addrStr);
                continue;
            }
            lst.add(addr);
        }
        return lst;
    }


    /**
     * Obtains a mapping from InternetAddress to Collection of DatabaseLabel
     * for each dataset whose QA status is CHECK.
     */
    public static Map<InternetAddress, Collection<Dataset>> getCheckDatasets(IDBDatabaseService db, Set<Principal> user)
             {
        QaCheckFunctor func = new QaCheckFunctor();

        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        ClassLoader cl = func.getClass().getClassLoader();
        try {
            t.setContextClassLoader(cl);
            func = db.getQueryRunner(user).queryPrograms(func);
        } finally {
            t.setContextClassLoader(prev);
        }

        return func.getCheckMap();
    }
}
