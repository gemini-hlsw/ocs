//
// $Id: SpdbBaseTestCase.java 47163 2012-08-01 23:09:47Z rnorris $
//
package edu.gemini.pot.spdb.test;

import edu.gemini.pot.sp.ISPProgram;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.pot.sp.SPNodeKey;
import edu.gemini.pot.spdb.DBLocalDatabase;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.gemini.obscomp.SPProgram;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@Ignore
public abstract class SpdbBaseTestCase {

    private IDBDatabaseService _database;
    private Set<SPNodeKey> _keys;

    @Before
    public void setUp() throws Exception {
        _database = DBLocalDatabase.createTransient();
        _keys     = new HashSet<SPNodeKey>();
    }

    @After
    public void tearDown() throws Exception {
        for (Iterator<SPNodeKey> it=_keys.iterator(); it.hasNext(); ) {
            SPNodeKey key = it.next();
            _database.removeProgram(key);
        }
        _keys.clear();
        _database.getDBAdmin().shutdown();
    }

    public IDBDatabaseService getDatabase() {
        return _database;
    }

    public ISPProgram createProgram() throws Exception {
        ISPProgram res = getDatabase().getFactory().createProgram(
                new EmptyNodeInitializer<ISPProgram, SPProgram>(),
                new SPNodeKey(),
                null);
        recordProgram(res);
        return res;
    }

    public SPNodeKey recordProgram(ISPProgram prog) throws Exception {
        _database.put(prog);
        SPNodeKey key = prog.getNodeKey();
        _keys.add(key);
        return key;
    }
}
