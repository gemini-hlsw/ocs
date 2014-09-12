// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: IDBQueryFunctor.java 4336 2004-01-20 07:57:42Z gillies $
//

package edu.gemini.pot.spdb;


/**
 * An interface made for use with the <code>{@link IDBQueryRunner}</code>.
 * It defines an operation that a client wishes to have performed by the
 * database server on all the programs or observations in the database.
 */
public interface IDBQueryFunctor extends IDBFunctor {
    /**
     * Called once before the first call to <code>isDone()</code> or
     * <code>execute</code>.  Provides an opportunity for the functor
     * to initialize itself on the server.
     */
    void init();

    /**
     * Checks whether the operation should be executed on the remaining
     * program nodes.  Called once per node before calling
     * <code>execute()</code>.  If and when <code>true</code> is returned,
     * execution stops.
     *
     * @return <code>true</code> if the execution should continue;
     * <code>false</code> otherwise
     */
    boolean isDone();

    /**
     * Called once when all nodes have been operated upon.
     */
    void finished();
}

