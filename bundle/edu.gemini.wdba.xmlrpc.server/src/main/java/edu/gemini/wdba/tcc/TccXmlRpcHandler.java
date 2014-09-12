//
// $Id: TccXmlRpcHandler.java 887 2007-07-04 15:38:49Z gillies $
//
package edu.gemini.wdba.tcc;

import edu.gemini.wdba.glue.api.WdbaContext;
import edu.gemini.wdba.xmlrpc.ITccXmlRpc;
import edu.gemini.wdba.xmlrpc.ServiceException;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementation of the OCS 1/TCC Coordinate fetch functionality.
 *
 * @author K.Gillies
 */
public final class TccXmlRpcHandler implements ITccXmlRpc {

    // SW: Okay this class has to have an empty argument constructor (thank you
    // apache XML RPC).  Storing a WdbaContext to use :/
    private static final AtomicReference<WdbaContext> context = new AtomicReference<WdbaContext>();

    public static void setContext(WdbaContext context) {
        TccXmlRpcHandler.context.set(context);
    }

    /**
     * Returns the XML coordinate data for a specific observation id.
     *
     * @param observationId the observation that should be returned
     * @return an XML document that contains the coordinates.
     */
    public String getCoordinates(String observationId) throws ServiceException {
        final WdbaContext ctx = context.get();
        if (ctx == null) throw new ServiceException("Database unavailable");
        return new TccHandler(ctx).getCoordinates(observationId);
    }
}
