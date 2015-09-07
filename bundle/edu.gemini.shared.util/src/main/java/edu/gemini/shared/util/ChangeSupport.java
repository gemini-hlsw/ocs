// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: ChangeSupport.java 4392 2004-01-30 06:40:18Z gillies $
//

package edu.gemini.shared.util;

import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * This is a utility class that can be used by objects publishing
 * <code>{@link ChangeEvent}</code>s.
 * <code>ChangeEvent</code> is a nice generic event to signify
 * some kind of change.  This class takes care of some of the
 * tedious work of dealing with listeners and firing events.
 *
 * You can use an instance of this class as a member field
 * of your class and delegate various work to it.
 *
 * A <code>ChangeEvent</code> has no data other than
 * a source.  Remember that when judging the suitability of this
 * event type for your application.
 *
 * Note that this class allows a null source.
 * There may be situations where clients want to know when change has
 * taken place but there is no single appropriate source.
 */
public class ChangeSupport {

    /** Defer some listener management to EventSupport */
    private transient EventSupport _eventSupport;

    /**
     * The object to be provided as the "source" for any generated events.
     * @serial
     */
    private Object _source;

    /**
     * Constructs a <code>ChangeSupport</code> object.
     *
     * @param source  The object to be given as the source for any events.
     */
    public ChangeSupport(Object source) {
        _source = source;
    }

    /**
     * Constructs a <code>ChangeSupport</code> object.
     * Events will have a null source until
     * <code>{@link #setSource}</code> is called.
     * This constructor is offered because there may be situations
     * where clients need to know when change has taken place but
     * there is no appropriate source for the change.
     */
    public ChangeSupport() {
    }

    /** Get the default source for events fired by this object. */
    public synchronized Object getSource() {
        return _source;
    }

    /** Set the default source for events fired by this object. */
    public synchronized void setSource(Object source) {
        _source = source;
    }

    /**
     * Add a ChangeListener to the listener list.
     *
     * @param listener  The ChangeListener to be added
     */
    public synchronized void addChangeListener(ChangeListener listener) {
        _getEventSupport().addListener(listener);
    }

    /**
     * Remove a ChangeListener from the listener list.
     *
     * @param listener  The ChangeListener to be removed
     */
    public synchronized void removeChangeListener(ChangeListener listener) {
        _getEventSupport().removeListener(listener);
    }

    /**
     * Fire <code>ChangeEvent</code> to any registered listeners.
     * The source of this event will be the default source.
     *
     * @see #setSource
     * @see #getSource
     */
    public void fireChangeEvent() {
        fireChangeEvent(_source);
    }

    /**
     * Fire <code>ChangeEvent</code> to any registered listeners.
     *
     * @param source  Fire the event with this source rather than
     * the default source.
     *
     * @see #setSource
     * @see #getSource
     */
    public void fireChangeEvent(Object source) {
        // The cost of acquiring a lock on EventSupport object is
        // greater than creating a ChangeEvent.  So don't creat it lazily,
        // just do it.
        EventSupport es = _getEventSupport();
        ChangeEvent evt = new ChangeEvent(source);
        es.fireEvent(evt, "stateChanged");
    }

    /**
     * Fire an existing ChangeEvent to any registered listeners.
     * @param evt  The ChangeEvent object.
     */
    public void fireChangeEvent(ChangeEvent evt) {
        EventSupport es = _getEventSupport();
        es.fireEvent(evt, "stateChanged");
    }

    /**
     * Check if there are any listeners.
     *
     * @return true if there are ore or more listeners
     */
    public synchronized boolean hasListeners() {
        return _getEventSupport().hasListeners();
    }

    // Returns the EventSupport lazily constructed
    private EventSupport _getEventSupport() {
        if (_eventSupport == null) {
            _eventSupport = new EventSupport(ChangeListener.class, ChangeEvent.class);
        }
        return _eventSupport;
    }
}
