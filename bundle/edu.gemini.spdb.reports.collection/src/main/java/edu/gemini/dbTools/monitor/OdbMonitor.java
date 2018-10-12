//
// $Id: OdbMonitor.java 46832 2012-07-19 00:28:38Z rnorris $
//
package edu.gemini.dbTools.monitor;

import edu.gemini.pot.client.SPDB;
import edu.gemini.pot.spdb.DBStatus;
import edu.gemini.pot.spdb.IDBAdmin;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spdb.cron.CronStorage;

import java.security.Principal;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/** Print out a summary of ODB state. */
@SuppressWarnings("rawtypes")
public class OdbMonitor {

    // curl -i -G http://localhost:8442/cron/monitor
    @SuppressWarnings("UnusedParameters")
    public static void monitor(final CronStorage store, final Logger log, final Map<String, String> env, Set<Principal> user) {
        final IDBDatabaseService db = SPDB.get();
        final IDBAdmin admin = db.getDBAdmin();
        final DBStatus status = admin.getStatus();
        dumpStatus(status,log);
    }

    private static void dumpStatus(final DBStatus status, final Logger log) {
        final int sz = status.getTotalThreads();
        final Map map = status.getThreadMap();
        final Set set = map.keySet();
        final Iterator it = set.iterator();
        final StringBuilder buf = new StringBuilder();
        buf.append("Free Memory: ").append(status.getFreeMemory()).append("\n");
        buf.append("Total Memory: ").append(status.getTotalMemory()).append("\n");
        buf.append("Storage Interval: ").append(status.getStorageInterval()).append("\n");
        buf.append("Total Threads: ").append(status.getTotalThreads());
        buf.append("\n-----------------------------");
        buf.append("\nTotal Active Threads = ").append(sz);
        buf.append("\nTotal Group Threads = ").append(set.size());
        buf.append("\n");
        while (it.hasNext()) {
            final String groupName = (String) it.next();
            final List l = (List) map.get(groupName);
            buf.append("\t").append(groupName).append("\n");
            for (final Object aL : l) {
                final String threadName = (String) aL;
                buf.append("\t\t").append(threadName).append("\n");
            }
        }
        log.info("status from odb monitor:\n" + buf);
    }

}
