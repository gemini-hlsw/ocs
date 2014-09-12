//
// $Id: DBSlaveSegregatedListFunctor.java 46768 2012-07-16 18:58:53Z rnorris $
//

package edu.gemini.pot.spdb;

import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.SPNodeKey;
import edu.gemini.spModel.core.SPProgramID;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.logging.Logger;

/**
 * A program or plan list functor that is master/slave database aware and that
 * returns its results segregated by slave.  This is useful for operations that
 * can be subsequently performed on every slave concurrently.
 */
public class DBSlaveSegregatedListFunctor extends DBAbstractQueryFunctor implements IDBParallelFunctor {
    private static final Logger LOG = Logger.getLogger(DBSlaveSegregatedListFunctor.class.getName());

    private Collection<Collection<DBProgramKeyAndId>> _allDatabaseLists;
    private Collection<DBProgramKeyAndId> _singleDatabaseList = new ArrayList<DBProgramKeyAndId>();

    public void execute(IDBDatabaseService db, ISPNode node, Set<Principal> principals) {
        SPNodeKey key = node.getProgramKey();
        SPProgramID id = node.getProgramID();
        _singleDatabaseList.add(new DBProgramKeyAndId(key, id));
    }

    public void mergeResults(Collection<IDBFunctor> functorCollection) {
        Collection<Collection<DBProgramKeyAndId>> res = new ArrayList<Collection<DBProgramKeyAndId>>();

        for (IDBFunctor fun : functorCollection) {
            DBSlaveSegregatedListFunctor plf = (DBSlaveSegregatedListFunctor) fun;
            res.add(plf._singleDatabaseList);
        }

        _singleDatabaseList = null;
        _allDatabaseLists = res;
    }

    /**
     * Obtains a slave segregated collection of collections of Science Program
     * keys and ids.  Works in both the master/slave and single database
     * environments.
     *
     * @return a collection of collections of {@link DBProgramKeyAndId} for science
     * programs in the database; each subcollection of KeyAndId corresponds
     * to the contents of a single slave database
     */
    public static Collection<Collection<DBProgramKeyAndId>> getProgramList(IDBDatabaseService db, Set<Principal> user)  {
        DBSlaveSegregatedListFunctor ssf = new DBSlaveSegregatedListFunctor();
        IDBQueryRunner runner = db.getQueryRunner(user);
        return extractResults(runner.queryPrograms(ssf));
    }

    /**
     * Obtains a slave segregated collection of collections of Nightly Plan
     * keys and ids.  Works in both the master/slave and single database
     * environments.
     *
     * @return a collection of collections of {@link DBProgramKeyAndId} for nightly
     * plans in the database; each subcollection of KeyAndId corresponds
     * to the contents of a single slave database
     */
    public static Collection<Collection<DBProgramKeyAndId>> getNightlyPlanList(IDBDatabaseService db, Set<Principal> user)  {
        DBSlaveSegregatedListFunctor ssf = new DBSlaveSegregatedListFunctor();
        IDBQueryRunner runner = db.getQueryRunner(user);
        return extractResults(runner.queryNightlyPlans(ssf));
    }

    private static Collection<Collection<DBProgramKeyAndId>> extractResults(DBSlaveSegregatedListFunctor fun) {
        if (fun._allDatabaseLists != null) return fun._allDatabaseLists;

        Collection<Collection<DBProgramKeyAndId>> res = new ArrayList<Collection<DBProgramKeyAndId>>();
        if (fun._singleDatabaseList != null) res.add(fun._singleDatabaseList);
        return res;
    }
}
