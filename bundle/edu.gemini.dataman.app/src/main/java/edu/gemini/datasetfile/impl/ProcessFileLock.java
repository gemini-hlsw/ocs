//
// $Id: ProcessFileLock.java 82 2005-09-05 18:35:21Z shane $
//

package edu.gemini.datasetfile.impl;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.HashMap;

/**
 * Manages a ReadWriteLock per filepath, creating it only when needed and
 * discarding it when no longer necessary.  Java NIO FileLock helps coordinate
 * file locking among distinct processes but within an application multiple
 * threads can still access the same file.  This class is provided to fill
 * that gap.
 */
public final class ProcessFileLock implements Lock {

    // Holds a ReadWriteLock and the number of outstanding references to it.
    private static class LockAndCount {
        int count; // reference count
        ReadWriteLock lock;
    }

    // Map of filepath to LockAndCount objects.
    private static final Map<String, LockAndCount> _lockMap =
                                new HashMap<String, LockAndCount>();

    private static ReadWriteLock _getRwLock(String filename) {
        synchronized (_lockMap) {
            LockAndCount lc = _lockMap.get(filename);
            if (lc == null) {
                lc = new LockAndCount();
                lc.lock  = new ReentrantReadWriteLock(true);
                _lockMap.put(filename, lc);
            }
            ++lc.count;
            return lc.lock;
        }
    }

    private static ReadWriteLock _ungetRwLock(String filename) {
        synchronized (_lockMap) {
            LockAndCount lc = _lockMap.get(filename);
            if (--lc.count == 0) _lockMap.remove(filename);
            return lc.lock;
        }
    }

    private String _filepath;
    private Lock _lock;

    /**
     * Creates with the path to the file that should be locked.  The filepath
     * is only used as a way of associating a lock with the File.  If two or
     * more paths refer to the same file, then distinct locks would be created
     * by creating this object with those paths.  In other words, be sure that
     * the filepath for files to be locked are unique within the application.
     *
     * @param filepath path to the file to lock
     * @param shared <code>true</code> for read locks, <code>false</code> for
     * write locks
     */
    public ProcessFileLock(String filepath, boolean shared) {
        _filepath = filepath;
        ReadWriteLock rwLock = _getRwLock(filepath);
        if (shared) {
            _lock = rwLock.readLock();
        } else {
            _lock = rwLock.writeLock();
        }
    }

    public void lock() {
        _lock.lock();
    }

    public void lockInterruptibly() throws InterruptedException {
        _lock.lockInterruptibly();
    }

    public boolean tryLock() {
        return _lock.tryLock();
    }

    public boolean tryLock(long l, TimeUnit timeUnit) throws InterruptedException {
        return _lock.tryLock(l, timeUnit);
    }

    public void unlock() {
        _lock.unlock();
        _ungetRwLock(_filepath);
    }

    public Condition newCondition() {
        return _lock.newCondition();
    }
}
