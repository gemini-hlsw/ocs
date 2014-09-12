//
// $Id: Mode.java 756 2007-01-08 18:01:24Z gillies $
//

package edu.gemini.wdba.session;

import edu.gemini.wdba.xmlrpc.ServiceException;

import java.io.Serializable;

/**
 * An enumerated type to express the state of a session event.
 */
public abstract class Mode extends EventMsg.AbstractAction implements Serializable {

    private String _modeStr;
    private Mode _nextMode;

    protected Mode(String modeStr) {
        _modeStr = modeStr;
    }

    public String getModeName() {
        return _modeStr;
    }

    public String toString() {
        return getModeName();
    }

    protected void _setNextMode(Mode mode) {
        _nextMode = mode;
    }

    // This method must be provided by the subclasses to indicate the mode that may be a product of the processing
    public Mode _getNextMode() {
        return _nextMode;
    }

    public Mode doModeAction(SessionEvent st) throws ServiceException {
        EventMsg msg = st.getMsg();
        msg.doAction(this, st);
        return _getNextMode();
    }

}

