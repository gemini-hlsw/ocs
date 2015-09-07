// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: DBAbstractQueryFunctor.java 4336 2004-01-20 07:57:42Z gillies $
//

package edu.gemini.pot.spdb;


/**
 * An abstract implementation of the <code>IDBQueryFunctor</code> interface
 * that leaves only <code>execute()</code> unimplemented.
 */
public abstract class DBAbstractQueryFunctor extends DBAbstractFunctor implements IDBQueryFunctor {

    /**
     * Empty implementation of the <code>{@link IDBQueryFunctor#init}</code>
     * method.
     */
    public void init() {
    }

    /**
     * Implements the <code>{@link IDBQueryFunctor#isDone}</code> method to
     * always return <code>false</code>.  In other words, the execution of
     * the functor is never finished until all nodes have been examined.
     *
     * @return <code>false</code>
     */
    public boolean isDone() {
        return false;
    }

    /**
     * Empty implementation of the <code>{@link IDBQueryFunctor#finished}</code>
     * method.
     */
    public void finished() {
    }
}

