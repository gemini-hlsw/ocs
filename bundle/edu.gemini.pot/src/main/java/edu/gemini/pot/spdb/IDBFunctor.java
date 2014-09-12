//
// $Id: IDBFunctor.java 46733 2012-07-12 20:43:36Z rnorris $
//

package edu.gemini.pot.spdb;

import edu.gemini.pot.sp.ISPNode;

import java.io.Serializable;
import java.security.Principal;
import java.util.Set;


/**
 * An interface that defines an operation which should be performed by the
 * database server on behalf of the client.
 */
public interface IDBFunctor extends Serializable {

    /**
     * The priority at which the functor should execute.
     */
    enum Priority {
        /**
         * High priority runs at the same level as internal database threads
         * and should be used sparingly, if at all.
         */
        high,

        /**
         * Medium priority is appropriate for most functors including those
         * issued by a UI.
         */
        medium,

        /**
         * Low priority should be considered for use by batch processing tasks,
         * particularly long running or compute-intensive ones.
         */
        low,
        ;
    }

    /**
     * Gets the priority at which the functor should run.
     */
    Priority getPriority();

    /**
     * The function that should be executed by the database service on behalf
     * of the client.
     *  @param db the database in which the functor is running
     * @param node the node upon which the functor will be applied (if any)
     * @param principals the user for whom this functor is being executed;
     * any permission checks should be in terms of this set.
     */
    void execute(IDBDatabaseService db, ISPNode node, Set<Principal> principals);

    /**
     * Called if the functor generates an unexpected exception.
     * @param ex the exception that was generated
     */
    void setException(Exception ex);
}

