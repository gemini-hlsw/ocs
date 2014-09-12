//
// $
//

package edu.gemini.file.util;

import static edu.gemini.file.util.LockedFileChannel.Mode;
import static org.junit.Assert.*;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.nio.channels.Channel;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;

/**
 * Test cases for {@link LockedFileChannel}.
 */
public final class LockedFileChannelTest {

    private LockedFileChannel create(Mode mode) throws Exception {
        URL url = this.getClass().getResource("LockedFileChannelTestData.txt");
        File  f = new File(url.getPath());
        return new LockedFileChannel(f, mode);
    }

    // Task that simulates locking and reading a file.
    private class FileReader implements Runnable {
        private final LockedFileChannel lfc;

        // Used for conditions/signalling
        boolean finishedReading = false;
        boolean locked   = false;
        boolean unlocked = false;

        long startTime;
        long lockTime;
        long unlockTime;

        int queuedLength; // queue length just before unlocking

        Exception lockException;
        Exception unlockException;

        FileReader(LockedFileChannel lfc) {
            this.lfc      = lfc;
        }

        public void run() {
            // Record when the thread started running.
            startTime = System.currentTimeMillis();
            try {
                // Grab the lock as soon as available.
                lfc.lock();

                // Record when the lock was obtained and let anybody waiting
                // know that we arrived to this point.
                lockTime = System.currentTimeMillis();
                markLocked();

                // Wait until the client decides to indicate that reading has
                // stopped
                waitUntilFinishedReading();

            } catch (Exception ex) {
                lockException = ex;

            } finally {
                //
                queuedLength = lfc.getQueueLength();
                try {
                    lfc.unlock();
                } catch (Exception ex) {
                    unlockException = ex;
                }
                unlockTime = System.currentTimeMillis();
                markUnlocked();
            }
        }

        synchronized void stopReading() {
            finishedReading = true;
            notifyAll();
        }

        private synchronized void waitUntilFinishedReading() {
            while (!finishedReading) {
                try {
                    wait();
                } catch (InterruptedException ex) {
                    // ignore
                }
            }
        }

        private synchronized void markLocked() {
            locked = true;
            notifyAll();
        }

        synchronized void waitUntilLocked() {
            while (!locked) {
                try {
                    wait();
                } catch (InterruptedException ex) {
                    // ignore
                }
            }
        }

        private synchronized void markUnlocked() {
            unlocked = true;
            notifyAll();
        }

        synchronized void waitUntilUnlocked() {
            while (!unlocked) {
                try {
                    wait();
                } catch (InterruptedException ex) {
                    // ignore
                }
            }
        }

        void assertNoExceptions() {
            assertTrue(lockException == null);
            assertTrue(unlockException == null);
        }
    }

    /**
     * Tests that closing the LockedFileChannel closes the embedded channel.
     */
    @Test
    public void testClose() throws Exception {
        LockedFileChannel lfc = create(Mode.r);
        Channel c = lfc.getChannel();
        assertTrue(c.isOpen());
        lfc.close();
        assertFalse(c.isOpen());
    }

    /**
     * Tests that two threads don't have simultaneous access to the file.
     * @throws Exception
     */
    @Test
    public void testMutualExclusion() throws Exception {
        LockedFileChannel lfc1 = create(Mode.r);
        LockedFileChannel lfc2 = create(Mode.r);

        try {
            FileReader fr1 = new FileReader(lfc1);
            FileReader fr2 = new FileReader(lfc2);

            Thread t1 = new Thread(fr1, "LockedFileChannelTest.testMutualExclusion.t1");
            Thread t2 = new Thread(fr2, "LockedFileChannelTest.testMutualExclusion.t2");

            t1.start();
            fr1.waitUntilLocked();

            t2.start();

            // We don't know when t2 will actually get scheduled and try to grab
            // the lock.  We want to still be reading until that happens though.
            // Wait for a bit and it will surely happen....
            int count = 0;
            while (!lfc1.hasQueuedThread(t2) && count < 10) {
                Thread.sleep(100);
                ++count;
            }
            assertTrue(lfc1.hasQueuedThread(t2));

            fr1.stopReading();
            fr1.waitUntilUnlocked();

            fr2.waitUntilLocked();

            // Nobody else is waiting.
            assertFalse(lfc2.hasQueuedThreds());

            fr2.stopReading();
            fr2.waitUntilUnlocked();

            assertEquals(1, fr1.queuedLength);
            assertEquals(0, fr2.queuedLength);

            assertTrue(fr2.startTime >= fr1.lockTime);
            assertTrue(fr2.startTime <= fr1.unlockTime);
            assertTrue(fr2.lockTime >= fr1.unlockTime);
            fr1.assertNoExceptions();
            fr2.assertNoExceptions();
        } finally {
            lfc1.close();
            lfc2.close();
        }
    }

    /**
     * Tests that a single thread can call lock two or more successive times.
     */
    @Test
    public void testReentrant() throws Exception {
        LockedFileChannel lfc = create(Mode.r);

        try {
            assertEquals(0, lfc.getHoldCount());
            lfc.lock();
            assertEquals(1, lfc.getHoldCount());
            lfc.lock();
            assertEquals(2, lfc.getHoldCount());
            lfc.unlock();
            assertEquals(1, lfc.getHoldCount());
            lfc.unlock();
            assertEquals(0, lfc.getHoldCount());

            // Calling without the lock held throws an exception.
            try {
                lfc.unlock();
                fail("Expected an IllegalMonitorStateException");
            } catch (IllegalMonitorStateException ex) {
                // okay
            }
        } finally {
            lfc.close();
        }
    }

    /**
     * Tests that the FileLock is created.
     */
    @Test
    public void testFileLock() throws Exception {
        LockedFileChannel lfc1 = create(Mode.r);
        LockedFileChannel lfc2 = create(Mode.rw);

        FileReader fr = new FileReader(lfc1);
        Thread      t = new Thread(fr, "LockedFileChannelTest.testFileLock");
        t.start();
        fr.waitUntilLocked();

        FileChannel c = lfc2.getChannel();

        // Try to lock the channel, which should generate an exception since
        // it should be being read by thread t.
        try {
            c.lock();
            fail("Excepted an OverlappingFileLockException");
        } catch (OverlappingFileLockException ex) {
            // okay
        }

        fr.stopReading();
        fr.waitUntilUnlocked();

        // Now it should work.
        FileLock flock = c.lock();
        flock.release();

        lfc1.close();
        lfc2.close();

        fr.assertNoExceptions();
    }

    /**
     * Tests that you can't close with a lock held.
     */
    @Test
    public synchronized void testCloseWhileLocked() throws Exception {
        LockedFileChannel lfc1 = create(Mode.r);
        lfc1.lock();
        try {
            lfc1.close(); // throw IllegalMonitorStateException
        } catch (IllegalMonitorStateException ex) {
            // expected
        } finally {
            lfc1.unlock();
            lfc1.close();
        }
    }
}
