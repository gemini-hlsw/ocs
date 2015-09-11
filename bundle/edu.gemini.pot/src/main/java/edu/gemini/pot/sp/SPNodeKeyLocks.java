package edu.gemini.pot.sp;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Provides a low-level API for getting read/write locks associated with
 * SPNodeKeys.  Locks are created on demand but never removed.
 */
public enum SPNodeKeyLocks {
    instance;

    private Map<SPNodeKey, ReentrantReadWriteLock> locks = new HashMap<SPNodeKey, ReentrantReadWriteLock>();

    private synchronized ReentrantReadWriteLock getLock(SPNodeKey key) {
        ReentrantReadWriteLock l = locks.get(key);
        if (l == null) {
            l = new ReentrantReadWriteLock();
            locks.put(key, l);
        }
        return l;
    }

    private Lock getReadLock(SPNodeKey key)  { return getLock(key).readLock();  }
    private Lock getWriteLock(SPNodeKey key) { return getLock(key).writeLock(); }

    public void readLock(SPNodeKey key)     { getReadLock(key).lock();   }
    public void readUnlock(SPNodeKey key)   { getReadLock(key).unlock(); }

    public void writeLock(SPNodeKey key)    { getWriteLock(key).lock();   }
    public void writeUnlock(SPNodeKey key)  { getWriteLock(key).unlock(); }

    /**
     * Returns <code>true</code> if the current thread has a write lock for the
     * indicated program key.
     */
    public boolean isWriteLockHeld(SPNodeKey key) {
        return getLock(key).isWriteLockedByCurrentThread();
    }
}
