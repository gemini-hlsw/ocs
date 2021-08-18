//
// $Id: ExecXmlRpcHandler.java 842 2007-05-15 00:01:38Z gillies $
//
package edu.gemini.wdba.exec;

import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.spModel.io.SequenceOutputService;
import edu.gemini.wdba.glue.api.WdbaContext;
import edu.gemini.wdba.glue.api.WdbaDatabaseAccessService;
import edu.gemini.wdba.glue.api.WdbaGlueException;
import edu.gemini.wdba.xmlrpc.IExecXmlRpc;
import edu.gemini.wdba.xmlrpc.ServiceException;

import java.io.StringWriter;
import java.util.logging.Logger;

/**
 * Implementation of the OCS 1/TCC SeqExec sequence fetch.
 *
 * @author K.Gillies
 */
public final class ExecHandler implements IExecXmlRpc {
    // Version printed on startup
    private static final String _VERSION = "20070513";

    private static final Logger LOG = Logger.getLogger(ExecHandler.class.getName());

    private final WdbaContext ctx;

    /**
     * The public constructor for the ExecXmlRpcHandler.
     */
    public ExecHandler(WdbaContext ctx) {
        LOG.info("ExecXmlRpcHandler Version: " + _VERSION);
        this.ctx = ctx;
    }

    /**
     * Display a text version of the observing sequence for return to the caller.
     * @param obs the <tt>ISPObservation</tt> instance that should have its sequence converted
     * @return the XML representation of the configuration
     * @throws edu.gemini.wdba.glue.api.WdbaGlueException if the database returns a remote exception
     */
    private String _getSequence(ISPObservation obs) throws ServiceException {
        if (obs == null) throw new NullPointerException();

        StringWriter strw = new StringWriter();
        if (!SequenceOutputService.printSequence(strw, obs, true)) {
            throw new ServiceException("Database access failed.");
        }
        return strw.toString();
    }

    /**
     * Returns the XML sequence file for a specific observation id.
     *
     * @param observationId the observation that should be returned
     * @return an XML document that contains the sequence
     * @throws edu.gemini.wdba.glue.api.WdbaGlueException if the observation Id is null
     * @throws edu.gemini.wdba.glue.api.DatabaseUnavailableException if the ODB is currently not available
     */
    public String getSequence(String observationId) throws ServiceException {
        if (observationId == null) throw new ServiceException("ObservationId was null.");

        LOG.info("Fetching exec: " + observationId);
        final WdbaDatabaseAccessService dbAccess = ctx.getWdbaDatabaseAccessService();
        try {
            ISPObservation spObs = dbAccess.getObservation(observationId);
            return _getSequence(spObs);
        } catch (WdbaGlueException ex) {
            throw ServiceException.create(ex);
        }
    }

}
