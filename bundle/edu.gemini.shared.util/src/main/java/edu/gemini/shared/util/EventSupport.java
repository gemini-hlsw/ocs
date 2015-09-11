// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: EventSupport.java 4392 2004-01-30 06:40:18Z gillies $
//

package edu.gemini.shared.util;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Support for event handling.  This class handles the details of keeping
 * track of listeners and firing events for clients.  It is thread safe.
 */
public class EventSupport {

    private static final Logger LOG = Logger.getLogger(EventSupport.class.getName());

    private Class _listenerClass;

    private Class _eventClass;

    private List _listeners;

    private Map _methodTable;   // Caches Methods by name so reflection
    // is not required each time.

    /**
     * Construct with the class of the listeners that will be added and the
     * event that will be fired.
     */
    public EventSupport(Class listenerClass, Class eventClass) {
        _listenerClass = listenerClass;
        _eventClass = eventClass;
        _listeners = new ArrayList();
        _methodTable = new HashMap();
    }

    /**
     * Add a listener to the set of listeners.
     */
    public synchronized void addListener(EventListener el) {
        if (!_listeners.contains(el)) {
            _listeners.add(el);
        }
    }

    /**
     * Remove a listener from the set of listeners.
     */
    public synchronized void removeListener(EventListener el) {
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
        return (EventListener[]) _listeners.toArray(evA);
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
     * Get the named method of the listener class.  Uses the cached value if
     * available, or else reflection to lookup the method.
     */
    protected Method getMethod(String methodName) {
        // See if there is a cached value.
        Method meth = (Method) _methodTable.get(methodName);
        if (meth != null) {
            return meth;
        }

        // Use reflection to get the method.
        try {
            meth = _listenerClass.getMethod(methodName, new Class[]{_eventClass});
        }
        catch (Exception ex) {
            System.out.println("Couldn't find the method: " + methodName);
        }

        // Cache the method for the next lookup.
        if (meth != null) {
            _methodTable.put(methodName, meth);
        }
        return meth;
    }

    /**
     * Fire the event to the listeners.
     * @param eo The event
     * @param methodName The name of the event on the listeners to call
     */
    public void fireEvent(EventObject eo, String methodName) {
        // Make a copy of the listeners so that the event can be fired without
        // holding a lock.
        List v;
        synchronized (this) {
            if (_listeners.size() == 0) {
                return;  // No listeners, so nothing to do.
            }
            v = (List) ((ArrayList) _listeners).clone();
        }

        // Lookup the listener method that will be called.
        Method meth = getMethod(methodName);
        if (meth == null) {
            return;
        }
        Object[] params = new Object[]{eo};

        // Fire the event to each listener.
        Iterator it = v.iterator();
        while (it.hasNext()) {
            Object target = it.next();
            try {
                meth.invoke(target, params);

                //} catch (InvocationTargetException ite) {
                //    System.err.println("Couldn't invoke method " + methodName +
                //                       "() on " + target);
                //    //ite.getTargetException().printStackTrace();
                //    //System.err.println("target exception: " + ite.getTargetException().getMessage());
                //    //System.err.println("actual exception: ");
                //    //ite.printStackTrace();
            }
            catch (Exception ex) {
                //System.err.println("Couldn't invoke method " + methodName +
                //                   "() on " + target + ": " + ex);
                LOG.log(Level.WARNING, "Couldn't invoke method " + methodName + "() on " + target, ex);
            }
        }
    }
}

