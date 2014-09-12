package edu.gemini.wdba.session;

import edu.gemini.spModel.event.ExecEvent;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

//
// Gemini Observatory/AURA
// $Id: SessionEventSupport.java 756 2007-01-08 18:01:24Z gillies $
//

/**
 * Support for event handling.  This class handles the details of keeping
 * track of listeners and firing events for clients.  It is thread safe.
 */
public class SessionEventSupport implements ISessionEventProducer {
    private static final Logger LOG = Logger.getLogger(edu.gemini.shared.util.EventSupport.class.getName());

    private List<ISessionEventListener> _listeners;

    /**
     * Construct with the class of the listeners that will be added and the
     * event that will be fired.
     */
    public SessionEventSupport() {
        _listeners = new ArrayList<ISessionEventListener>();
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
     * Remove all listeners.
     */
    public synchronized void removeAllListeners() {
        _listeners.clear();
    }

    /**
     * Gets the listeners in a newly created array.  Clients may modify the
     * array without invalidating or changing the set of listeners.
     */
    public synchronized EventListener[] getListeners() {
        EventListener[] evA = new EventListener[_listeners.size()];
        return _listeners.toArray(evA);
    }

    /**
     * Returns whether the listener list is empty.
     */
    public synchronized boolean hasListeners() {
        return (_listeners != null && !_listeners.isEmpty());
    }

    /**
     * Print all the listeners to stdout.  This is just a debugging method.
     */
    public synchronized void showListeners(String title) {
        System.out.println("--- " + title + " ---");
        for (int i = 0; i < _listeners.size(); ++i) {
            System.out.println(i + ") " + _listeners.get(i));
        }
        System.out.println("------------------------------");
    }

    /**
     * Fire the event to the listeners.
     *
     * @param eo         The event
     */
    public void fireEvent(ExecEvent eo) {
        // Make a copy of the listeners so that the event can be fired without
        // holding a lock.
        List v;
        synchronized (this) {
            if (_listeners.size() == 0) {
                return;  // No listeners, so nothing to do.
            }
            v = (List) ((ArrayList) _listeners).clone();
        }

        // Fire the event to each listener.
        Iterator it = v.iterator();
        while (it.hasNext()) {
            ISessionEventListener target = (ISessionEventListener) it.next();
            try {
                target.sessionUpdate(eo);
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, "Couldn't invoke method sessionUpdate() on " + target, ex);
            }
        }
    }
}
