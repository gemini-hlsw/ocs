//
// $Id: SessionQueue.java 46768 2012-07-16 18:58:53Z rnorris $
//

package jsky.app.ot.session;

import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.spModel.core.Peer;
import edu.gemini.spModel.core.Site;
import edu.gemini.wdba.xmlrpc.ServiceException;
import edu.gemini.wdba.shared.QueuedObservation;
import edu.gemini.wdba.session.client.WDBA_XmlRpc_SessionClient;
import jsky.app.ot.userprefs.model.PreferencesChangeEvent;
import jsky.app.ot.userprefs.model.PreferencesChangeListener;
import jsky.app.ot.userprefs.observer.ObserverPreferences;
import jsky.app.ot.userprefs.observer.ObservingPeer;
import jsky.util.gui.DialogUtil;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import java.util.Collections;
import java.util.logging.Logger;
import java.util.List;


/**
 * Manages a single static session queue containing science
 * program ids. The list of ids is stored on a network service
 * accessed via the WDBA ISession class.
 *
 * @version $Revision: 46768 $
 * @author Allan Brighton
 */
public final class SessionQueue {
    private static final Logger LOG = Logger.getLogger("jsky.app.ot.session.SessionQueue");

    // The session queue name
    public static final String SESSION_NAME = "sessionQueue";

    // Singleton instance of this class
    public static final SessionQueue INSTANCE = new SessionQueue();

    /** list of listeners for change events */
    private final EventListenerList _listenerList = new EventListenerList();

    // SessionManagement instance, used to access the session server
    private WDBA_XmlRpc_SessionClient _sm;

    // private constructor
    private SessionQueue() {
        // Update the session manager as the observing site changes.
        ObserverPreferences.addChangeListener(new PreferencesChangeListener<ObserverPreferences>() {
            @Override public void preferencesChanged(PreferencesChangeEvent<ObserverPreferences> evt) {
                final ObserverPreferences oldValue = evt.getOldValue().getOrNull();
                final Site oldSite = (oldValue == null) ? null : oldValue.observingSite();
                final Site newSite = evt.getNewValue().observingSite();
                if (oldSite != newSite) initClient();
            }
        });

        initClient();
    }

    private void initClient() {
        final Peer p = ObservingPeer.getOrPromptOrNull();
        try {
            if (p == null) {
                LOG.info("Cannot connect to WDBA because the Observing Site is not known.");
                _sm = null;
            } else {
                // OCSINF-159.  This is a bit of a problem.  Can't use the
                // peer's port because it is expecting to be contacted over trpc
                // with https and how to set up XMLRPC to create an https
                // connection w/o resorting to setting default socket factories
                // or something is unclear.
//                LOG.info("Connecting to wdba at: " + p.host + ':' + p.port);
//                _sm = new WDBA_XmlRpc_SessionClient(p.host, String.valueOf(p.port));
                LOG.info("Connecting to wdba at: " + p.host + ":8442");
                _sm = new WDBA_XmlRpc_SessionClient(p.host, "8442");
            }
        } catch (Exception ex) {
            DialogUtil.error(ex);
        }
    }

    /** Add the given observation to the session queue */
    public void addObservation(ISPObservation obs) throws ServiceException {
        SPObservationID spObsId = obs.getObservationID();
        if (spObsId == null) {
            DialogUtil.error("This observation does not have a valid observation id");
            return;
        }
        addObservation(spObsId.toString());
    }


    public void addObservation(SPObservationID obsId) throws ServiceException {
        addObservation(obsId.toString());
    }

    /**
     * Add the named observation to the session queue. The list is checked to
     * ensure that the obsId isn't already in the list in the server.
     */
    public void addObservation(String obsId) throws ServiceException {
        if (_sm == null) initClient();

        if (_sm == null) {
            LOG.warning("Not adding observation '" + obsId + "' to session queue because the observing site has not been established.");
        } else {
            _sm.addObservation(SESSION_NAME, obsId);
            _fireChange();
        }
    }

    /** Remove the given observation from the session queue */
    public void removeObservation(ISPObservation obs) throws ServiceException {
        SPObservationID spObsId = obs.getObservationID();
        if (spObsId == null) {
            DialogUtil.error("This observation does not have a valid observation id");
            return;
        }
        removeObservation(spObsId.toString());
    }

    /** Remove the named observation from the session queue */
    public void removeObservation(String obsId) throws ServiceException {
        if (_sm == null) initClient();

        if (_sm == null) {
            LOG.warning("Not removing observation '" + obsId + "' from session queue because the observing site has not been established.");
        } else {
            _sm.removeObservation(SESSION_NAME, obsId);
            _fireChange();
        }
    }

    public List<QueuedObservation> getObservationsAndTitles() throws ServiceException {
        if (_sm == null) initClient();

        if (_sm == null) {
            return Collections.emptyList();
        } else {
            return _sm.getObservationsAndTitles(SESSION_NAME);
        }
    }

    /**
     * register to receive change events from this object
     */
    public void addChangeListener(ChangeListener l) {
        _listenerList.add(ChangeListener.class, l);
    }

    /**
     * Stop receiving change events from this object.
     */
    public void removeChangeListener(ChangeListener l) {
        _listenerList.remove(ChangeListener.class, l);
    }


    // Notify any listeners of a change in this object.
    private void _fireChange() {
        ChangeEvent e = new ChangeEvent(this);
        Object[] listeners = _listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ChangeListener.class) {
                ((ChangeListener) listeners[i + 1]).stateChanged(e);
            }
        }
    }
}

