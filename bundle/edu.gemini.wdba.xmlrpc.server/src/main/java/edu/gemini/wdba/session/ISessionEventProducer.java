//
// $Id: ISessionEventProducer.java 756 2007-01-08 18:01:24Z gillies $
//

package edu.gemini.wdba.session;

import edu.gemini.spModel.event.ExecEvent;


/**
 * An interface that defines methods that can be used by an
 * <tt>{@link ISessionEventListener}</tt> to register an interest in
 * receiving events.
 */
public interface ISessionEventProducer {

    /**
     * Register interest in reciving session events.
     */
    void addSessionEventListener(ISessionEventListener listener);

    /**
     * Remove a listener currently receiving events.
     */
    void removeSessionEventListener(ISessionEventListener listener);

    /**
     * Send an event to the listeners.
     * @param evt an <code>ExecEvent</code>
     */
    void fireEvent(ExecEvent evt);
}

