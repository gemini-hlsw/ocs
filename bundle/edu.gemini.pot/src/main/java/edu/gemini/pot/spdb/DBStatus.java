package edu.gemini.pot.spdb;

import java.io.Serializable;
import java.util.*;

/**
 * User: anunez
 * Date: Mar 31, 2004
 * Time: 4:32:29 PM
 * This class contains information related to the Status of the ODB.
 * $Id: DBStatus.java 46832 2012-07-19 00:28:38Z rnorris $
 */
public class DBStatus implements Serializable {
    protected static DBStatus dbStatus = new DBStatus();
    protected int totalThreads;

    // This Map contains as key a thread Group Name. As value,
    // there is a list with all the threads name associated
    // to that group
    protected Map<String, List<String>> threadMap = null;

    protected long freeMemory = 0;

    protected long totalMemory = 0;
    protected long storageInterval = 0;

    public static DBStatus getStatus(IDBAdmin admin) {
        dbStatus.updateStatus(admin);
        return dbStatus;
    }

    //fill all the information into the status object
    protected void updateStatus(IDBAdmin admin) {
        updateThreadUsage();
        updateMemoryUsage();
        updateDatabaseMetrics(admin);
    }

    protected void updateMemoryUsage() {
        Runtime rt = Runtime.getRuntime();
        freeMemory = rt.freeMemory();
        totalMemory = rt.totalMemory();
    }

    protected void updateThreadUsage() {
        ThreadGroup tg = Thread.currentThread().getThreadGroup();
        ThreadGroup parent = tg.getParent();
        while (parent != null) {
            tg = parent;
            parent = tg.getParent();
        }
        totalThreads = tg.activeCount();
        Thread[] th = new Thread[totalThreads];
        int sz = tg.enumerate(th);
        threadMap = new HashMap<>();
        String groupName = null;

        for (int i = 0; i < sz; ++i) {
            Thread t = th[i];
            String thisGroupName = t.getThreadGroup().getName();
            if (!thisGroupName.equals(groupName)) {
                List<String> l = new ArrayList<>();
                threadMap.put(thisGroupName, l);
                groupName = thisGroupName;
            }
            List<String> l = threadMap.get(thisGroupName);
            l.add(t.getName());
        }
    }

    protected void updateDatabaseMetrics(IDBAdmin admin) {
        if (admin != null) {
            storageInterval = admin.getStorageInterval();
        } else {
            storageInterval = 0;
        }
    }


    public long getTotalMemory() {
        return totalMemory;
    }

    public int getTotalThreads() {
        return totalThreads;
    }

    public long getFreeMemory() {
        return freeMemory;
    }

    public long getStorageInterval() {
        return storageInterval;
    }

    public Map<String, List<String>> getThreadMap() {
        return threadMap;
    }

    public static void main(String[] args) {
        DBStatus status = DBStatus.getStatus(null);
        System.out.println("Free Memory: " + status.getFreeMemory());
        System.out.println("Total Memory: " + status.getTotalMemory());
        System.out.println("Total Threads: " + status.getTotalThreads());
        int sz = status.getTotalThreads();
        StringBuilder buf = new StringBuilder();
        buf.append("\n-----------------------------");
        buf.append("\nTotal Active Threads = ").append(sz);
        buf.append("\n");

        Map<String, List<String>> map = status.getThreadMap();
        Set<String> set = map.keySet();
        for (String groupName : set) {
            List<String> l = map.get(groupName);
            buf.append("\t").append(groupName).append("\n");
            for (String threadName : l) {
                buf.append("\t\t").append(threadName).append("\n");
            }
        }
        System.out.println(buf.toString());
    }
}
