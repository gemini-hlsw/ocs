package edu.gemini.shared.util;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;

/**
 * A reader/writer locking pattern class.   Multiple simultaneous readers
 * are permitted, but only one writer at a time may proceed.  When a new
 * write request comes in, it is blocked until any current readers are
 * finished.  However, read requests that occur while a writer is blocked
 * are deferred until the writer is finished.
 *
 * <p>A thread that has acquired a writer lock may successfully request
 * subsequent reader and/or writer locks until it releases the original
 * writer lock.  A thread that has first acquired a reader lock may
 * <em>not</em> "upgrade" to a writer lock, but will be granted subsequent
 * (nested) reader locks.
 *
 * <p>This class requires the caller to release the locks it acquires.
 * Failure to do so will result in blocking other threads permanently.
 * It is recommended to follow this pattern:
 *
 * <pre>
 *    rwLock.getReadPermission();
 *    try {
 *       // do reading
 *    } finally {
 *       // unlock no matter what happens
 *       rwLock.returnReadPermission();
 *    }
 * </pre>
 *
 * A write operation would be performed similarly.
 */
final class RWLock {

    private class RWRecord {

        Thread thread;     // The thread that holds (or will hold) the lock

        int readCount;  // The number of reads being currently performed

        int writeCount; // The number of writes being currently performed

        // For debugging.
        public String toString() {
            StringBuilder sb = new StringBuilder(getClass().getName());
            sb.append(" [thread=");
            sb.append(thread.toString());
            sb.append(", readCount=");
            sb.append(readCount);
            sb.append(", writeCount=");
            sb.append(writeCount);
            sb.append("]");
            return sb.toString();
        }
    }

    // Manages a pool of record instances so that new records need not be
    // created all the time.
    private class RWRecordPool {

        private LinkedList<RWRecord> _recordPool = new LinkedList<>();

        synchronized RWRecord getRecord(Thread t, int readCount, int writeCount) {
            RWRecord record;
            if (_recordPool.size() == 0) {
                record = new RWRecord();
            }
            else {
                record = _recordPool.removeFirst();
            }
            record.thread = t;
            record.readCount = readCount;
            record.writeCount = writeCount;
            return record;
        }

        synchronized void putRecord(RWRecord record) {
            // keep using same record as often as possible
            _recordPool.addFirst(record);
            record.thread = null;  // why not release the thread reference
        }
    }

    private RWRecordPool _recordPool = new RWRecordPool();

    private LinkedList<RWRecord> _waitingReaders = new LinkedList<>();

    private LinkedList<RWRecord> _waitingWriters = new LinkedList<>();

    private Map<Thread, RWRecord> _activeReaders = new HashMap<>();

    private RWRecord _activeWriter;

    /**
     * Constructs a new reader/writer lock ready to be used by clients.
     */
    RWLock() {
    }

    /**
     * Checks the given <code>cond</code> and throws a
     * <code>RuntimeException</code> if not true.  This method is used to
     * help ensure that the implementation of the <code>RWLock</code> class is
     * not buggy.
     */
    private void _assertTrue(boolean cond) {
        if (!cond) {
            throw new RuntimeException("Bug in RWLock.");
        }
    }

    /**
     * Checks the given <code>cond</code> and throws an
     * <code>IllegalStateException</code> if not true.  This method is used to
     * help ensure that callers aren't using the <code>RWLock</code> incorrectly.
     */
    private void _assertState(boolean cond) {
        if (!cond) {
            throw new IllegalStateException("Bug in client of RWLock.");
        }
    }

    /**
     * Dumps the state of the RWLock for debugging.
     */
    public synchronized void dump() {
        boolean active = false;
        System.out.println("RWLock = " + this);
        if (_activeWriter != null) {
            System.out.println("\tActive Writer: " + _activeWriter);
            active = true;
        }
        for (Object o : _activeReaders.values()) {
            System.out.println("\tActive Reader: " + o);
            active = true;
        }
        if (!active) {
            System.out.println("\tNo active readers or writers.");
        }
    }

    /**
     * Notifies (unblocks) all waiting readers that they may begin reading.
     */
    private void _notifyReaders() {
        if (_waitingReaders.size() > 0) {
            ListIterator<RWRecord> lit = _waitingReaders.listIterator();
            while (lit.hasNext()) {
                RWRecord record = lit.next();
                record.readCount = 1;
                _activeReaders.put(record.thread, record);
                lit.remove();
            }
            notifyAll();  // unblock the waiting readers
        }
    }

    /**
     * Attempts to obtain read permission, but does not block if unable to
     * do so.  Returns true if read permission was granted, false otherwise.
     * If permission is granted, the state is updated accordingly.
     */
    private boolean _getReadPermission(Thread t) {
        // If there is an active writer, only allow a read by that same writer.
        if (_activeWriter != null) {
            if (_activeWriter.thread != t) {
                return false;  // somebody else is writing, cannot begin reading now
            }
            // This thread is writing, so let him read too.
            ++(_activeWriter.readCount);
            return true;
        }

        // No active writer.  If this is a new reader, only let him in if there
        // are no waiting writers.
        RWRecord record = _activeReaders.get(t);
        if (record == null) {
            // This is a new reader, are there any waiting writers?
            if (_waitingWriters.size() > 0) {
                // There are waiting writers, so block this guy.
                return false;
            }
            // No waiting writers, so let him in.
            _activeReaders.put(t, _recordPool.getRecord(t, 1, 0));
            return true;
        }

        // This is an existing reader, let him keep reading.
        _assertTrue(record.readCount > 0);  // must be reading if in _activeReaders
        ++(record.readCount);
        return true;
    }

    /**
     * Blocks until it is okay to read.  When this method returns, the
     * calling thread will have read access to the section of code guarded
     * by the lock.  Other simultaneous readers may be present.
     */
    synchronized void getReadPermission() {
        //System.out.println("*** ASKING FOR READ PERMISSION");
        Thread t = Thread.currentThread();
        if (!_getReadPermission(t)) {
            // Didn't get read permission, so record a waiting reader.
            _waitingReaders.add(_recordPool.getRecord(t, 0, 0));
            while (true) {
                try {
                    wait();
                }
                catch (InterruptedException ex) {
                }
                // Make sure not interrupted for some other reason
                if (_activeReaders.get(t) != null)
                    break;
            }
        }
    }

    /**
     * Relinquishes read permission, possibly allowing blocked writers to
     * proceed.
     */
    synchronized void returnReadPermission() {
        //System.out.println("*** RETURN READ PERMISSION");
        Thread t = Thread.currentThread();
        if (_activeWriter != null) {
            // A writer is returning read permission.
            _assertTrue(_activeReaders.size() == 0);

            // Make sure the reader also holds the write lock in this case.
            _assertState(_activeWriter.thread == t);
            --(_activeWriter.readCount);

            // Make sure the writer is still writing.
            _assertTrue(_activeWriter.writeCount > 0);
            return;
        }

        // A true reader is returning its read permission.
        RWRecord record = _activeReaders.get(t);
        _assertState(record != null);
        _assertTrue(record.writeCount == 0);
        --(record.readCount);
        if (record.readCount > 0) {
            // This reader is actually still reading.
            return;
        }

        // This reader is completely finished, so remove him.
        _assertTrue(record.readCount == 0);  // can't be negative
        _activeReaders.remove(t);
        _recordPool.putRecord(record);  // reuse the record later

        // If there are no more active readers, notify a writer that it
        // may now continue.
        if (_activeReaders.size() == 0) {
            _notifyWriter();
        }
    }

    /**
     * Notifies the next waiting writer (if any) that it may proceed.
     */
    private void _notifyWriter() {
        if (_waitingWriters.size() > 0) {
            _activeWriter = _waitingWriters.removeFirst();
            _activeWriter.writeCount = 1;
            synchronized (_activeWriter) {
                _activeWriter.notify();
            }
        }
    }

    /**
     * Attempts to obtain write permission, but does not block if unable to
     * do so.  Returns true if write permission was granted, false otherwise.
     * If permission is granted, the state is updated accordingly.
     */
    private boolean _getWritePermission(Thread t) {
        // If there is an active writer, only allow a write by that same writer.
        if (_activeWriter != null) {
            if (_activeWriter.thread != t) {
                return false;  // somebody else is writing
            }
            // This thread is writing, so let him write more.
            ++(_activeWriter.writeCount);
            return true;
        }

        // No active writer.

        // Are there waiting writers?
        if (_waitingWriters.size() > 0) {
            // yes, must wait for these guys to finish their writes first.
            return false;
        }

        // No waiting writers, are there active readers?
        if (_activeReaders.size() > 0) {
            // yes, somebody else is reading, so this writer must wait
            // Make sure the thread making this request isn't a pure reader.  If
            // so, throw an "upgrade" exception (otherwise the thread would block
            // forever).
            _assertState(_activeReaders.get(t) == null);
            return false;
        }

        // Okay, nobody is writing or waiting to write or reading.  Go ahead
        // and allow writing for this thread.
        _activeWriter = _recordPool.getRecord(t, 0, 1);
        return true;
    }

    /**
     * Blocks until it is okay to write.  When this method returns, the
     * calling thread will have exclusive write access to the section of code
     * guarded by the lock.  No other thread, reader or writer, will be able
     * to execute the code.
     */
    void getWritePermission() {
        //System.out.println("*** ASKING FOR WRITE PERMISSION: " + this);
        Thread t = Thread.currentThread();

        // Get a fresh record.  May end up putting it on the waiting writers
        // queue.
        RWRecord record = _recordPool.getRecord(t, 0, 0);
        synchronized (record) {
            synchronized (this) {
                if (_getWritePermission(t)) {
                    // didn't need to use the record ...
                    _recordPool.putRecord(record);
                    //System.out.println("*** GOT WRITE PERMISSION IMMEDIATELY: "+this);
                    return;
                }
                _waitingWriters.add(record);
            }
            while (true) {
                try {
                    record.wait();
                }
                catch (InterruptedException ex) {
                }
                // Make sure not interrupted for some other reason
                if (_activeWriter == record)
                    break;
            }
        }
        //System.out.println("*** GOT WRITE PERMISSION AFTER WAIT: " + this);
    }

    /**
     * Notifies any waiting readers or writers that the write is over.
     */
    synchronized void returnWritePermission() {
        //System.out.println("*** RETURN WRITE PERMISSION: " + this);
        Thread t = Thread.currentThread();

        // Make sure the proper thread is returning write permission
        _assertState((_activeWriter != null) && (_activeWriter.thread == t));
        --(_activeWriter.writeCount);
        if (_activeWriter.writeCount > 0) {
            // This writer is actually still writing.
            return;
        }

        // Finished writing, make sure not reading and write count isn't negative.
        _assertState(_activeWriter.readCount == 0);
        _assertTrue(_activeWriter.writeCount == 0);

        // The writer is completely finished.
        _recordPool.putRecord(_activeWriter);  // reuse later
        _activeWriter = null;

        // Notify any waiting readers, or if none, any waiting writers.
        if (_waitingReaders.size() > 0) {
            _notifyReaders();
        }
        else {
            _notifyWriter();
        }
    }

}
