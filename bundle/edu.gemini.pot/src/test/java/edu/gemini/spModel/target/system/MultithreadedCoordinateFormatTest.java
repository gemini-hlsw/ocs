//
// $
//

package edu.gemini.spModel.target.system;

import junit.framework.TestCase;

/**
 * Test case that demonstrated a problem with sharing
 * [@link CoordinateFormat} instances among multiple threads and verifies that
 * it is solved.
 */
public class MultithreadedCoordinateFormatTest extends TestCase {

    public void testSimpleFormat() throws Exception {
        DMSFormat dms = new DMSFormat();
        assertEquals("45:30:00.00", dms.format(45.5));

        HMSFormat hms = new HMSFormat();
        assertEquals("12:00:00.000", hms.format(180.0));
    }

    private static class FormatTest implements Runnable {
        Throwable ex;

        private boolean done = false;

        synchronized void waitUntilDone() {
            while (!done) {
                try {
                    wait();
                } catch (InterruptedException ex) {
                    break;
                }
            }
        }

        private synchronized void setDone() {
            done = true;
            notifyAll();
        }

        public void run() {
            for (int i=0; i<100; ++i) {
//                System.out.println(i);
                DMSFormat dms = DMS.DEFAULT_FORMAT;
                HMSFormat hms = HMS.DEFAULT_FORMAT;

                try {
                    assertEquals("45:30:00.00",  dms.format( 45.5));
                    assertEquals("12:00:00.000", hms.format(180.0));
                } catch (Throwable ex) {
                    System.out.println(ex);
                    this.ex = ex;
                    break;
                }
            }
            setDone();
        }
    }

    public void testMultithreadFormat() throws Exception {
        FormatTest test1 = new FormatTest();
        FormatTest test2 = new FormatTest();

        Thread t1 = new Thread(test1);
        Thread t2 = new Thread(test2);

        t1.start(); t2.start();

        test1.waitUntilDone(); test2.waitUntilDone();

        assertNull(test1.ex); assertNull(test2.ex);
    }
}
