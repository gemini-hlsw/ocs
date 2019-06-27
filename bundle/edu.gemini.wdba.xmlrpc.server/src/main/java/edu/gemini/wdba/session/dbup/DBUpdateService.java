//
// $Id: DBUpdateService.java 887 2007-07-04 15:38:49Z gillies $
//

package edu.gemini.wdba.session.dbup;

import edu.gemini.pot.sp.*;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.event.*;
import edu.gemini.spModel.gemini.plan.NightlyRecord;
import edu.gemini.spModel.obs.ObsExecEventFunctor;
import edu.gemini.spModel.util.NightlyProgIdGenerator;
import edu.gemini.wdba.glue.api.WdbaContext;
import edu.gemini.wdba.glue.api.WdbaDatabaseAccessService;
import edu.gemini.wdba.glue.api.WdbaGlueException;
import edu.gemini.wdba.session.AbstractSessionEventConsumer;
import edu.gemini.wdba.session.ISessionEventProducer;
import edu.gemini.wdba.session.OneLineLogFormatter;


import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class responds to session events and updates the database as needed.
 */
public class DBUpdateService extends AbstractSessionEventConsumer {
    private static final Logger LOG = Logger.getLogger(DBUpdateService.class.getName());
    private WdbaContext _context;

    // Made this static since XMLRPC Creates a new instance  every time and the logger is static
    static {
        _initLogger();
    }

    public DBUpdateService(ISessionEventProducer ssp, WdbaContext context) {
        super(ssp);
        assert context != null : "Session Context is null";
        _context = context;
        Thread t = new Thread(this, "DBUpdate");
        t.start();
    }

    private static void _initLogger() {
        Formatter logFormatter = new OneLineLogFormatter();

        Logger realLogger = _findRealLogger(LOG);
        assert realLogger != null;
        Handler[] handlers = realLogger.getHandlers();
        LOG.info("Size of handlers: " + handlers.length);
        for (Handler h : handlers) {
            h.setFormatter(logFormatter);
        }
    }

    private static Logger _findRealLogger(Logger logger) {
        Handler[] handlers = logger.getHandlers();
        if (handlers.length != 0) return logger;
        return _findRealLogger(logger.getParent());
    }

    // Add the given obs id to nightly log, creating the log if necessary
    // for the current night.
    private void _addToNightlyRecord(SPObservationID obsId) {
        final Site site = _context.getSite();

        SPProgramID recordId = NightlyProgIdGenerator.getProgramID(NightlyProgIdGenerator.PLAN_ID_PREFIX, site);
        WdbaDatabaseAccessService dbAccess = _context.getWdbaDatabaseAccessService();

        ISPNightlyRecord nightlyRecordNode;
        try {
            nightlyRecordNode = dbAccess.getNightlyRecord(recordId);
        } catch (Exception ex) {
            // Messages are logged at lower level
            return;
        }

        NightlyRecord nightlyRecord = (NightlyRecord) nightlyRecordNode.getDataObject();
        nightlyRecord.addObservation(obsId);
        nightlyRecordNode.setDataObject(nightlyRecord);

        LOG.info("Added observation ID to nightly record: " + obsId.stringValue());
    }

    public String getName() {
        return "DBUpdateService";
    }

    public void doMsgUpdate(ExecEvent evt) throws WdbaGlueException {
        try {
            if (evt instanceof StartSequenceEvent) {
                _addToNightlyRecord(((ObsExecEvent) evt).getObsId());
            }
            if (evt instanceof ObsExecEvent) {
                final WdbaDatabaseAccessService dbAccess = _context.getWdbaDatabaseAccessService();
                ObsExecEventFunctor.handle((ObsExecEvent) evt, dbAccess.getDatabase(), _context.user);
            }
        } catch (Throwable ex) {
            LOG.log(Level.INFO, ex.getMessage(), ex);
        }
    }
}
