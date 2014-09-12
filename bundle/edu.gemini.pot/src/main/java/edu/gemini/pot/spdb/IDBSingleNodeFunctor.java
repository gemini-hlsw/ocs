//
// $Id: IDBSingleNodeFunctor.java 7398 2006-10-17 18:47:05Z shane $
//

package edu.gemini.pot.spdb;

import edu.gemini.pot.sp.SPNodeKey;

/**
 * An interface that provides a hint that the functor works on a particular
 * program.  This hint is used in a master/slave database context.  The master
 * database uses the root node key to determine the proper database to which
 * the functor should be sent.
 */
public interface IDBSingleNodeFunctor extends IDBFunctor {

    /**
     * Returns the root (program or plan) node key upon which this functor
     * operates.
     */
    SPNodeKey getNodeKey();
}
