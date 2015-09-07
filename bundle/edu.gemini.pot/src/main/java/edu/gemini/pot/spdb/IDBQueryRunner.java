// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: IDBQueryRunner.java 46768 2012-07-16 18:58:53Z rnorris $
//

package edu.gemini.pot.spdb;


import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.SPNodeNotLocalException;

/**
 * The query interface provided to clients.  Each method scans all available
 * nodes of the matching type, applying the functor to each node.  It is
 * anticipated that the functor implementation will collect its result and
 * make it available to the client.
 */
public interface IDBQueryRunner {
    /**
     * Queries the available observations, applying the given
     * <code>functor</code> on each.
     *
     * @return the query functor itself; if called remotely the return
     * value will (of course) be a distinct copy of the method argument
     */
    <T extends IDBQueryFunctor> T queryObservations(T functor) ;

    /**
     * Queries the available programs, applying the given
     * <code>functor</code> on each.
     *
     * @return the query functor itself; if called remotely the return
     * value will (of course) be a distinct copy of the method argument
     */
    <T extends IDBQueryFunctor> T queryPrograms(T functor) ;

    /**
     * Queries the available nightly plans, applying the given
     * <code>functor</code> on each.
     *
     * @return the query functor itself; if called remotely the return
     * value will (of course) be a distinct copy of the method argument
     */
    <T extends IDBQueryFunctor> T queryNightlyPlans(T functor) ;

    /**
     * Executes the given functor on the given node.  The remote reference is
     * first converted to a local reference by the database before it is passed
     * to the {@link IDBFunctor#execute} method.
     *
     * @param functor the functor to execute; its {@link IDBFunctor#execute}
     *                method is called
     * @param node    the (optional) program node to operate upon; may be
     *                <code>null</code> in which case the functor's execute method will be
     *                called with a <code>null</code> node; if not <code>null</code> it will
     *                be converted to a local reference before being passed to the functor's
     *                execute method
     * @return a reference to the functor that was passed to the database; if
     *         called remotely the return value will be a distinct copy of the
     *         <code>functor</code> argument
     * @throws edu.gemini.pot.sp.SPNodeNotLocalException if <code>node</code> is not known to
     *                                 the database (in other words, was created outside of the database
     *                                 service)
     */
    <T extends IDBFunctor> T execute(T functor, ISPNode node)
            throws SPNodeNotLocalException;

}
