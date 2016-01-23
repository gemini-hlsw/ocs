package edu.gemini.wdba.session;

import edu.gemini.spModel.event.ExecEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Support for event handling.  This class handles the details of keeping
 * track of listeners and firing events for clients.  It is thread safe.
 */
class SessionEventSupport implements ISessionEventProducer {
    private static final Logger LOG = Logger.getLogger(edu.gemini.shared.util.EventSupport.class.getName());

    private List<ISessionEventListener> _listeners;

    /**
     * Construct with the class of the listeners that will be added and the
     * event that will be fired.
     */
    SessionEventSupport() {
        _listeners = new ArrayList<>();
    }

    /**
     * Add a listener to the set of listeners.
     */
    public synchronized void addSessionEventListener(ISessionEventListener el) {
        if (!_listeners.contains(el)) {
            _listeners.add(el);
        }
    }

    /**
     * Remove a listener from the set of listeners.
     */
    public synchronized void removeSessionEventListener(ISessionEventListener el) {
        _listeners.remove(el);
    }

    /**
     * Fire the event to the listeners.
     *
     * @param eo         The event
     */
    public void fireEvent(ExecEvent eo) {
        // Make a copy of the listeners so that the event can be fired without
        // holding a lock.
        List<ISessionEventListener> v;
        synchronized (this) {
            if (_listeners.size() == 0) {
                return;  // No listeners, so nothing to do.
            }
            v = new ArrayList<>(_listeners);
        }

        // Fire the event to each listener.
        for (ISessionEventListener target : v) {
            try {
                target.sessionUpdate(eo);
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, "Couldn't invoke method sessionUpdate() on " + target, ex);
            }
        }
    }
}
