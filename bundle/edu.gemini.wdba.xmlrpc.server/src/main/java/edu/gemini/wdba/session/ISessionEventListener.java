//
// $Id: ISessionEventListener.java 756 2007-01-08 18:01:24Z gillies $
//

package edu.gemini.wdba.session;

import edu.gemini.spModel.event.ExecEvent;

import java.util.EventListener;

/**
 * An interface for session state listeners.
 */
public interface ISessionEventListener extends EventListener {

    /**
     * Receive a state update event.
     */
    void sessionUpdate(ExecEvent event) throws InterruptedException;

}

