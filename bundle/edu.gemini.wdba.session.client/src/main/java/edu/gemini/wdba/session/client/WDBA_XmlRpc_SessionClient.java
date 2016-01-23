package edu.gemini.wdba.session.client;

import edu.gemini.wdba.xmlrpc.ISessionXmlRpc;
import edu.gemini.wdba.xmlrpc.ServiceException;
import edu.gemini.wdba.shared.QueuedObservation;
import edu.gemini.wdba.shared.Helpers;
import edu.gemini.wdba.shared.WdbaHelperException;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gemini Observatory/AURA
 * $Id: WDBA_XmlRpc_SessionClient.java 756 2007-01-08 18:01:24Z gillies $
 */
public class WDBA_XmlRpc_SessionClient {
    private final Logger LOG = Logger.getLogger(WDBA_XmlRpc_SessionClient.class.getName());

    // Note that this must be the same as the value in WdbaConstants.APP_CONTEXT
    private final String APP_CONTEXT = "/wdba";

    private XmlRpcClient _client;

    public WDBA_XmlRpc_SessionClient(String host, String port) {
        if (host == null) throw new NullPointerException();
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();

        StringBuilder url = new StringBuilder("http://");
        url.append(host).append(':').append(port);
        url.append(APP_CONTEXT);

        try {
            config.setServerURL(new URL(url.toString()));
            config.setEnabledForExtensions(true);
        } catch (MalformedURLException ex) {
            LOG.log(Level.SEVERE, "Bad URL: " + url, ex);
            return;
        }

        XmlRpcClient client = new XmlRpcClient();
        client.setConfig(config);
        _client = client;
    }

    public int getSessionCount() throws ServiceException {
        List<String> args = getArgs();
        return (Integer) _execute("getSessionCount", args);
    }

    public String createSession() throws ServiceException {
        List<String> args = getArgs();
        return (String) _execute("createSession", args);
    }

    public String createSession(String sessionID) throws ServiceException {
        List<String> args = getArgs(sessionID);
        return (String) _execute("createSession", args);
    }

    public boolean removeSession(String sessionID) throws ServiceException {
        List<String> args = getArgs(sessionID);
        return (Boolean) _execute("removeSession", args);
    }

    public boolean removeAllSessions() throws ServiceException {
        List<String> args = getArgs();
        _execute("removeAllSessions", args);
        return true;
    }

    // Session activities
    public boolean addObservation(String sessionID, String observationID) throws ServiceException {
        List<String> args = getArgs(sessionID, observationID);
        return (Boolean) _execute("addObservation", args);
    }

    public int getObservationCount(String sessionID) throws ServiceException {
        List<String> args = getArgs(sessionID);
        return (Integer) _execute("getObservationCount", args);
    }

    public boolean removeObservation(String sessionID, String observationID) throws ServiceException {
        List<String> args = getArgs(sessionID, observationID);
        return (Boolean) _execute("removeObservation", args);
    }

    public boolean removeAllObservations(String sessionID) throws ServiceException {
        List<String> args = getArgs(sessionID);
        return (Boolean) _execute("removeAllObservations", args);
    }

    public String[] getObservations(String sessionID) throws ServiceException {
        List<String> args = getArgs(sessionID);
        Object[] o = (Object[]) _execute("getObservations", args);
        String[] result = new String[o.length];
        for (int i = 0; i < o.length; i++) {
            result[i] = (String) o[i];
        }
        return result;
    }

    @SuppressWarnings("unchecked")    
    public List<QueuedObservation> getObservationsAndTitles(String sessionID) throws ServiceException {
        List<String> args = getArgs(sessionID);
        Object[] hashArray = (Object[])_execute("getObservationsAndTitles", args);

        List<QueuedObservation> result = new ArrayList<>(hashArray.length);
        for (Object obj : hashArray) {
            try {
                result.add(Helpers.toQueuedObservation((Map <String, String>)obj));
            } catch (WdbaHelperException ex) {
                throw ServiceException.create(ex);
            }
        }
        return result;
    }

    // Observation events

    public boolean observationStart(String sessionID, String observationID) throws ServiceException {
        List<String> args = getArgs(sessionID, observationID);
        return (Boolean) _execute("observationStart", args);
    }

    public boolean observationEnd(String sessionID, String observationID) throws ServiceException {
        List<String> args = getArgs(sessionID, observationID);
        return (Boolean) _execute("observationEnd", args);
    }

    public boolean sequenceStart(String sessionID, String observationID, String firstFileID) throws ServiceException {
        List<String> args = getArgs(sessionID, observationID, firstFileID);
        return (Boolean) _execute("sequenceStart", args);
    }

    public boolean sequenceEnd(String sessionID, String observationID) throws ServiceException {
        List<String> args = getArgs(sessionID, observationID);
        return (Boolean) _execute("sequenceEnd", args);
    }

    public boolean datasetStart(String sessionID, String observationID, String datasetID, String fileName) throws ServiceException {
        List<String> args = getArgs(sessionID, observationID, datasetID, fileName);
        return (Boolean) _execute("datasetStart", args);
    }

    public boolean datasetComplete(String sessionID, String observationID, String datasetID, String fileName) throws ServiceException {
        List<String> args = getArgs(sessionID, observationID, datasetID, fileName);
        return (Boolean) _execute("datasetComplete", args);
    }

    public boolean observationAbort(String sessionID, String observationID, String reason) throws ServiceException {
        List<String> args = getArgs(sessionID, observationID, reason);
        return (Boolean) _execute("observationAbort", args);
    }

    public boolean observationPause(String sessionID, String observationID, String reason) throws ServiceException {
        List<String> args = getArgs(sessionID, observationID, reason);
        return (Boolean) _execute("observationPause", args);
    }

    public boolean observationStop(String sessionID, String observationID, String reason) throws ServiceException {
        List<String> args = getArgs(sessionID, observationID, reason);
        return (Boolean) _execute("observationStop", args);
    }

    public boolean observationContinue(String sessionID, String observationID) throws ServiceException {
        List<String> args = getArgs(sessionID, observationID);
        return (Boolean) _execute("observationContinue", args);
    }

    public static final String CAT_TELESCOPE_SYSTEM = "telescope";
    public static final String CAT_INSTRUMENT_SYSTEM = "instrument";
    public static final String CAT_NETWORK_COMP_SYSTEM = "network";
    public static final String CAT_SOFTWARE_SYSTEM = "software";
    public static final String CAT_WEATHER = "weather";
    public static final String CAT_HUMAN_ERR = "humanError";
    public static final String CAT_INACTIVE = "inactive";

    /**
     * The sending application indciates that the session is now idle and includes
     * a category for why it is idle.  Categories are indicated with an optional
     * comment.
     * <p/>
     * Once in the idle state, the only way to leave is with a <tt>startObservation</tt>.
     * It is possible to send consequitive startIdle
     */
    public boolean setIdleCause(String sessionId, String category, String comment) throws ServiceException {
        return true;
    }

    private static String getMethodName(String name) {
        StringBuilder buf = new StringBuilder();
        buf.append(ISessionXmlRpc.NAME);
        buf.append('.');
        buf.append(name);
        return buf.toString();
    }

    @SuppressWarnings("unchecked")
    private static List<String> getArgs() {
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    private static List<String> getArgs(String... args) {
        List<String> argArray = new ArrayList<>();
        Collections.addAll(argArray, args);
        return argArray;
    }

    private Object _execute(String method, List<String> args) throws ServiceException {
        String fullMethod = getMethodName(method);
        try {
            Object res = _client.execute(fullMethod, args);

            if (res instanceof XmlRpcException) {
                // why the hell is it returning an exception?
                throw (XmlRpcException) res;
            }
            return res;
        } catch (Exception ex) {
            LOG.log(Level.WARNING, ex.getMessage(), ex.getCause());
            throw ServiceException.create(ex);
        }
    }
}
