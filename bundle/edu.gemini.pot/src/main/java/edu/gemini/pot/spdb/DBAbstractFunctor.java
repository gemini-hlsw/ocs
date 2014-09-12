//
// $Id: DBAbstractFunctor.java 27833 2010-11-08 12:29:48Z swalker $
//

package edu.gemini.pot.spdb;


/**
 * An abstract implementation of the <code>IDBFunctor</code> interface that
 * takes care of setting and getting any exception that might be thrown.  A
 * concrete subclass must implement the <code>execute</code> method.
 */
public abstract class DBAbstractFunctor implements IDBFunctor {

    private Exception _ex;

    /**
     * Defaults the priority to {@link edu.gemini.pot.spdb.IDBFunctor.Priority#medium medium}.
     * Subclasses should override if a different priority level is appropriate.
     */
    public Priority getPriority() { return Priority.medium; }

    /**
     * Sets the exception (thereby notifying the functor implementation that
     * one of its method failed unexpectedly).
     */
    public void setException(Exception ex) {
        _ex = ex;
    }

    /**
     * Gets the exception, if one was thrown.  Returns <code>null</code>
     * otherwise.
     */
    public Exception getException() {
        return _ex;
    }
}

