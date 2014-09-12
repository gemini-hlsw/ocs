package jsky.app.ot.gemini.editor.horizons;

import edu.gemini.horizons.api.HorizonsException;
import edu.gemini.horizons.api.HorizonsQuery;
import edu.gemini.horizons.api.HorizonsReply;
import edu.gemini.horizons.api.IQueryExecutor;
import edu.gemini.spModel.core.Peer;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.core.Site;
import edu.gemini.util.trpc.client.TrpcClient$;
import jsky.app.ot.OT;
import jsky.app.ot.userprefs.observer.ObservingPeer;
import jsky.util.gui.ProgressPanel;

import java.util.Date;
import java.util.logging.Logger;
import java.util.logging.Level;


/**
 * This class provides methods to interact with the Horizons Service
 * through an HorizonsClient object. This is just a wrapper for the
 * service, providing a singleton XML_RPC client for the OT.
 */
public final class HorizonsService {

    private static final Logger LOG = Logger.getLogger(HorizonsService.class.getName());
    private ProgressPanel panel;
    private static HorizonsService _instance;

    private final IQueryExecutor _client;
    private final HorizonsQuery _query;
    private HorizonsReply _reply;
    private boolean _changed = true;

    HorizonsMultipleChoiceHandler handler = new HorizonsMultipleChoiceHandler();


    private HorizonsService(Peer p) {
        _client = TrpcClient$.MODULE$.apply(p.host, p.port).withKeyChain(OT.getKeyChain()).proxy(IQueryExecutor.class);
        _query = new HorizonsQuery(p.site);

    }

    /**
     * Get the unique Horizon Service instance
     *
     * @return the <code>HorizonsService</code> singleton
     */
    public static HorizonsService getInstance() {
        if (_instance == null) {
            final Peer p = ObservingPeer.anyWithSiteOrNull();
            _instance = (p == null) ? null : new HorizonsService(p);
        }
        return _instance;
    }

    /**
     * Configures the service with the given object Id
     *
     * @param id a new object id. <code>null</code> values are ignored.
     */
    public void setObjectId(String id) {
        if (id != null) {
            if (!id.equals(_query.getObjectId())) {
                _query.setObjectId(id);
                _dirty();
            }
        }
    }

    /**
     * Retrieve the current object Id configured for the service
     *
     * @return the object id the service is configured to use
     */
    public String getObjectId() {
        return _query.getObjectId();
    }

    /**
     * Set the initial date that will be used to perform a query
     * and get an ephemeris. The date is interpreted by Horizons as UTC
     *
     * @param date the initial date to get an ephemeris
     */
    public void setInitialDate(Date date) {
        _query.setStartDate(date);
    }

    /**
     * Set the final date that will be used to perform a query
     * and get an ephemeris. The date is interpreted by Horizons as UTC
     *
     * @param date the final date to get an ephemeris
     */

    public void setFinalDate(Date date) {
        _query.setEndDate(date);
    }

    /**
     * Set the object type to be used to narrow the query if possible.
     * <code>null</code> means to look into every possible option
     *
     * @param type The object type or <code>null</code> if no
     *             type wants to be used.
     */
    public void setObjectType(HorizonsQuery.ObjectType type) {
        _query.setObjectType(type);
    }

    /**
     * Retrieves the object type this query is configured to use.
     *
     * @return The object type set for this query. <code>null</code>
     *         means the query shouldn't attempt to restrict the types
     *         of objects that can be retrieved.
     */
    public HorizonsQuery.ObjectType getObjectType() {
        return _query.getObjectType();
    }

    /**
     * Execute the current configured query on the server.
     * The only required argument is the object id. If
     * no start date or end date, defaults are used. The
     * default initial date is the current time. The default
     * final date is 5 hours later than the current time.
     *
     * @return a HorizonsReply object upon successful execution, null
     *         otherwise
     */
    public HorizonsReply execute() {
        if (_changed) {
            getProgressPanel().start();
            try {

                Date startDate = _query.getStartDate();
                if (startDate == null) {
                    startDate = new Date();
                }
                _query.setStartDate(startDate);

                Date endDate = _query.getEndDate();
                if (endDate == null) {
                    endDate = new Date(startDate.getTime() + 1000 * 60 * 60 * 5);
                }
                _query.setEndDate(endDate);
                _reply = _client.execute(_query);
                //_changed = false;
            } catch (HorizonsException e) {
                getProgressPanel().stop();
                _resetQuery();
                //As there are many possible reasons to get an exception, we should just log it and let the client deal with
                // it, not raise an error dialog window.
                //DialogUtil.error("Problem contacting the Horizons Service. Please try again later");
                LOG.log(Level.INFO, e.getMessage(), e);
                return null;
            }
        }
        getProgressPanel().stop();
        //for our concerns, if a query is "invalid" it means we don't have results,
        //so let's change the type.
        if (_reply.getReplyType() == HorizonsReply.ReplyType.INVALID_QUERY) {
            _reply.setReplyType(HorizonsReply.ReplyType.NO_RESULTS);
        }

        if (_reply.getReplyType() == HorizonsReply.ReplyType.MUTLIPLE_ANSWER) {
            handler.updateTable(_reply.getResultsTable());
            handler.show();
            String newObjectId = handler.getObjectId();
            //try again if we have a choice
            if (newObjectId != null) {
                setObjectId(newObjectId);
                _reply = execute();
            }
        }
        _resetQuery();
        return _reply;
    }


    /**
     * Set the current site based on the program Id. This is
     * only used by PI. If the program Id  is <code>null</code>
     * then the query will use the last site it had.
     *
     * @param programId program Id to use
     */
    public void setSite(SPProgramID programId) {
        if (programId == null) return; // use default
        final Site s = programId.site();
        if (s != null) _query.setSite(s);
    }

    /**
     * Reset the current query
     */
    private void _resetQuery() {
        _query.setStartDate(null);
        _query.setEndDate(null);
    }


    private void _dirty() {
        _changed = true;
    }

    /**
     * Retrieves the last <code>HorizonsReply</code> result gotten by
     * the service
     *
     * @return the last <code>HorizonsReply</code> this service knows about
     */
    public HorizonsReply getLastResult() {
        return _reply;
    }

    /**
     * Removes the last <code>HorizonsReply</code> stored in this object, if any
     *
     */
    public static void resetCache() {
        if (_instance != null) {
            _instance._reply = null;
        }
    }

    private ProgressPanel getProgressPanel() {
        if (panel == null) {
            panel = ProgressPanel.makeProgressPanel("Performing Horizons Query...");
        }
        return panel;
    }

}
