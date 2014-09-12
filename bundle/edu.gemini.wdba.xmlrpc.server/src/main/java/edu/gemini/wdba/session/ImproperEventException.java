//
// $Id: ImproperEventException.java 756 2007-01-08 18:01:24Z gillies $
//

package edu.gemini.wdba.session;

import edu.gemini.wdba.xmlrpc.ServiceException;

/**
 * An exception thrown when an imporoper state event is received.
 */
public class ImproperEventException extends ServiceException {

    /**
     * Use this constructor when only a message should be returned.
     */
    public ImproperEventException() {
        super("The current session state does not allow this event.");
    }

    /**
     * Use when a message must be propagated
     */
    public ImproperEventException(String message) {
        super(message);
    }
}

