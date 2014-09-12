//
// $
//

package edu.gemini.file.util;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.FileLockInterruptionException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

/**
 * Utility for locking access to a file and obtaining a FileChannel for reading
 * or updating. Prevents OverlappingFileLockExceptions by restricting access to
 * the file within the JVM to a single Thread.
 */
public final class LockedFileChannel implements Closeable {
    private static final Logger LOG = Logger.getLogger(LockedFileChannel.class.getName());

    /**
     * File access modes corresponding to RandomAccessFile modes.  See the
     * RandomAccessFile constructor documentation.
     */
    public enum Mode {
        r,
        rw,
        rws,
        rwd,
        ;
    }

    // A single lock per File must be shared across all threads. This class
    // groups the lock and a reference count.  The reference count is used to
    // know when a shared map of LockRef can be cleaned.
    private static final class LockRef {
        private final ReentrantLock lock;
        private int refCount;
        LockRef()  { this.lock  = new ReentrantLock(true); }
        void inc() { ++refCount; }
        void dec() { --refCount; }
        boolean isFree() { return refCount == 0; }
        ReentrantLock getLock()   { return lock; }
    }

    // Map of filepath to LockAndCount objects.
    private static final Map<File, LockRef> LOCK_MAP = new HashMap<File, LockRef>();

    // Gets the ReentrantLock associated with this file, creating it if
    // necessary.
    private static ReentrantLock getLock(File f) {
        synchronized (LOCK_MAP) {
            LockRef ref = LOCK_MAP.get(f);
            if (ref == null) {
                ref = new LockRef();
                LOCK_MAP.put(f, ref);
            }
            ref.inc();
            return ref.getLock();
        }
    }

    // Releases a reference to the shared ReentrantLock associated with this
    // File, releasing the memory required to keep up with it if possible.
    private static void ungetLock(File f) {
        synchronized (LOCK_MAP) {
            LockRef ref = LOCK_MAP.get(f);
            ref.dec();
            if (ref.isFree()) LOCK_MAP.remove(f);
        }
    }


    private final Mode mode;
    private final File file;
    private final RandomAccessFile raf;
    private final FileChannel channel;
    private final ReentrantLock lock;

    private FileLock flock;

    /**
     * Creates the LockedFileChannel with the associated File and an access
     * mode.
     *
     * @param file File to be locked/read
     * @param mode access mode; see the RandomAccessFile constructor
     * documentation
     *
     * @throws IOException if there is a problem obtaining the canonical file
     * path for the file, or if opening for reading but the file doesn't exist,
     * or if opening for writing but the file cannot be created or written
     */
    public LockedFileChannel(File file, Mode mode) throws IOException {
        this.mode    = mode;
        this.file    = file.getCanonicalFile();
        this.raf     = new RandomAccessFile(this.file, mode.name());
        this.channel = raf.getChannel();
        this.lock    = getLock(this.file);
    }

    /**
     * Gets the canonical file representation associated with this
     * LockedFileChannel.
     */
    public File getFile() {
        return file;
    }

    /**
     * Gets the FileChannel associated with the file.
     */
    public FileChannel getChannel() {
        return channel;
    }

    /**
     * Locks the file so that it will not be accessed by other threads in this
     * JVM that use LockedFileChannel or by other processes that respect
     * FileLocks.  Though multiple threads in the same JVM will not be able
     * to read the file simultaneously, the FileLock will be created in shared
     * mode if the File was opened in read mode ({@link Mode#r}).
     *
     * <p>This method may be called twice or more by the same thread. Each call
     * to lock must be accompanied by a matching call to {@link #unlock()}.
     *
     * <p>Be sure to always {@link #unlock()} locked files. Typically the
     * unlock call should be placed in a finally clause of the try block
     * surrounding the file access.
     *
     * @throws InterruptedException if interrupted while waiting to lock; if
     * interrupted the lock will not be held
     *
     * @throws IOException if there is a problem obtaining the file lock; if
     * interrupted the lock will not be held
     */
    public void lock() throws InterruptedException, IOException {
        // FileLocks are not applicable to controlling access to a file from
        // multiple threads in a single process.  So we protect the file lock
        // with a normal lock.

        // First grab the lock for this process.
        lock.lockInterruptibly();

        // Now lock the file itself.
        if (lock.getHoldCount() == 1) {
            try {
                flock = channel.lock(0, Long.MAX_VALUE, mode == Mode.r);
            } catch (FileLockInterruptionException flie) {
                lock.unlock();
                throw new InterruptedException("Interrupted while locking file " + file.getPath());
            } catch (IOException ex) {
                lock.unlock();
                throw ex;
            }
        }
    }

    /**
     * Unlocks the file allowing other threads and other processes to access
     * it.  Must be called with the lock held as a result of a call to
     * {@link #lock}.
     *
     * @throws IOException if there is a problem releasing the FileLock;
     * regardless, the file lock is released for other threads in the same JVM
     */
    public void unlock() throws IOException {
        try {
            if (lock.getHoldCount() == 1) {
                try {
                    flock.release();
                } catch (ClosedChannelException cce) {
                    LOG.warning("Unlocked a closed channel for file " + file.getName());
                    // eat the exception
                } finally {
                    flock = null;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Closes this LockedFileChannel, closing the embedded Channel it contains
     * as well.  Should not be called with the lock held.
     *
     * @throws IllegalMonitorStateException if called while still locked
     * @throws IOException if there is a problem closing the channel
     */
    public void close() throws IOException {
        if (lock.isHeldByCurrentThread()) {
            throw new IllegalMonitorStateException();
        }

        raf.close(); // closes channel as well
        ungetLock(file);
    }

    /**
     * Determines whether the current thread holds the lock on this file.
     *
     * @return <code>true</code> if the current thread has the file locked;
     * <code>false</code> otherwise
     */
    public boolean isHeldByCurrentThread() {
        return lock.isHeldByCurrentThread();
    }

    // Package-protected methods intended to be used only for test cases.

    int getQueueLength() {
        return lock.getQueueLength();
    }

    boolean hasQueuedThread(Thread thread) {
        return lock.hasQueuedThread(thread);
    }

    boolean hasQueuedThreds() {
        return lock.hasQueuedThreads();
    }

    int getHoldCount() {
        return lock.getHoldCount();
    }
}