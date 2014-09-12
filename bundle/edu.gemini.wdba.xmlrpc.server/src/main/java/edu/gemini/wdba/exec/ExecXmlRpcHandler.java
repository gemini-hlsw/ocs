//
// $Id: ExecXmlRpcHandler.java 842 2007-05-15 00:01:38Z gillies $
//
package edu.gemini.wdba.exec;

import edu.gemini.wdba.glue.api.WdbaContext;
import edu.gemini.wdba.xmlrpc.IExecXmlRpc;
import edu.gemini.wdba.xmlrpc.ServiceException;
import edu.gemini.wdba.glue.api.DatabaseUnavailableException;
import edu.gemini.wdba.glue.api.WdbaGlueException;

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * Implementation of the OCS 1/TCC SeqExec sequence fetch.
 *
 * @author K.Gillies
 */
public final class ExecXmlRpcHandler implements IExecXmlRpc {
    private static final Logger LOG = Logger.getLogger(ExecXmlRpcHandler.class.getName());

    // SW: Okay this class has to have an empty argument constructor (thank you
    // apache XML RPC).  Storing a WdbaContext to use :/
    private static final AtomicReference<WdbaContext> context = new AtomicReference<WdbaContext>();

    public static void setContext(WdbaContext context) {
        ExecXmlRpcHandler.context.set(context);
    }

    private ServiceException dbUnavailable() {
        String message = "Database unavailable: try again later.";
        LOG.severe(message);
        return new ServiceException(message);
    }

    /**
     * Returns the XML sequence file for a specific observation id.
     *
     * @param observationId the observation that should be returned
     * @return an XML document that contains the sequence
     * @throws WdbaGlueException if the observation Id is null
     * @throws DatabaseUnavailableException if the ODB is currently not available
     */
    public String getSequence(String observationId) throws ServiceException {
        final WdbaContext ctx = context.get();
        if (ctx == null) throw dbUnavailable();
        return new ExecHandler(ctx).getSequence(observationId);
    }

}
