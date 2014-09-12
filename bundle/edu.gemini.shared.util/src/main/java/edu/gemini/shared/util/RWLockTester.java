// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: RWLockTester.java 4392 2004-01-30 06:40:18Z gillies $
//

package edu.gemini.shared.util;

/**
 * Test code for the <code>{@link RWLock}</code> class.
 */
final class RWLockTester {

    private boolean _error;

    private int _state;

    private boolean _inWrite;

    private boolean _inRead;

    private boolean _inRead2;

    private void _printTestName(String testName) {
        System.out.println("\n*** " + testName + " Test  ***");
    }

    private void _clearError() {
        _error = false;
    }

    private synchronized void _errorTest(boolean condition) {
        if (!condition)
            _error = true;
    }

    /**
     * Test whether a reader is blocked while a writer has permission.
     */
    boolean testWriteBlock() {
        _printTestName("Write Block");
        final RWLock rwLock = new RWLock();
        _inWrite = false;
        _inRead = false;
        _clearError();
        Thread reader = new Thread(new Runnable() {
            public void run() {
                String threadName = Thread.currentThread().getName();
                System.out.println("Before Read.....: " + threadName);
                rwLock.getReadPermission();
                System.out.println("Reading.........: " + threadName);
                _errorTest(!_inWrite);
                rwLock.returnReadPermission();
                System.out.println("After Read......: " + threadName);
            }
        });
        String threadName = Thread.currentThread().getName();
        System.out.println("Before Write....: " + threadName);
        rwLock.getWritePermission();
        _inWrite = true;
        reader.start();
        try {
            Thread.sleep(500);
        }
        catch (Exception ex) {
        }
        System.out.println("Writing.........: " + threadName);
        _inWrite = false;
        rwLock.returnWritePermission();
        System.out.println("After Write.....: " + threadName);
        try {
            reader.join();
        }
        catch (Exception ex) {
        }
        return !_error;
    }

    /**
     * Test whether a writer is blocked while a reader has permission.
     */
    boolean testReadBlock() {
        _printTestName("Read Block");
        final RWLock rwLock = new RWLock();
        _inWrite = false;
        _inRead = false;
        _clearError();
        Thread writer = new Thread(new Runnable() {
            public void run() {
                String threadName = Thread.currentThread().getName();
                System.out.println("Before Write....: " + threadName);
                rwLock.getWritePermission();
                System.out.println("Writing.........: " + threadName);
                _errorTest(!_inRead);
                rwLock.returnWritePermission();
                System.out.println("After Write.....: " + threadName);
            }
        });
        String threadName = Thread.currentThread().getName();
        System.out.println("Before Read.....: " + threadName);
        rwLock.getReadPermission();
        _inRead = true;
        writer.start();
        try {
            Thread.sleep(500);
        }
        catch (Exception ex) {
        }
        System.out.println("Reading.........: " + threadName);
        _inRead = false;
        rwLock.returnReadPermission();
        System.out.println("After Read......: " + threadName);
        try {
            writer.join();
        }
        catch (Exception ex) {
        }
        return !_error;
    }

    /**
     * Test whether two readers may proceed simultaneously.
     */
    boolean testSimultaneousRead() {
        _printTestName("Simultaneous Read");
        final RWLock rwLock = new RWLock();
        _inRead = false;
        _inRead2 = false;
        _clearError();
        Thread reader = new Thread(new Runnable() {
            public void run() {
                String threadName = Thread.currentThread().getName();
                System.out.println("Before Read.....: " + threadName);
                rwLock.getReadPermission();
                System.out.println("Reading.........: " + threadName);
                _errorTest(_inRead);
                _inRead2 = true;
                try {
                    Thread.sleep(500);
                }
                catch (Exception ex) {
                }
                _inRead2 = false;
                rwLock.returnReadPermission();
                System.out.println("After Read......: " + threadName);
            }
        });
        String threadName = Thread.currentThread().getName();
        System.out.println("Before Read.....: " + threadName);
        rwLock.getReadPermission();
        _inRead = true;
        reader.start();
        try {
            Thread.sleep(500);
        }
        catch (Exception ex) {
        }
        System.out.println("Reading.........: " + threadName);
        _errorTest(_inRead2);
        _inRead = false;
        rwLock.returnReadPermission();
        System.out.println("After Read......: " + threadName);
        try {
            reader.join();
        }
        catch (Exception ex) {
        }
        return !_error;
    }

    /**
     * Test whether one writer blocks a reader and writer.
     */
    boolean testSimultaneousWrite() {
        _printTestName("Simultaneous Write");
        final RWLock rwLock = new RWLock();
        _inWrite = false;
        _clearError();
        Thread writer = new Thread(new Runnable() {
            public void run() {
                String threadName = Thread.currentThread().getName();
                System.out.println("Before Write 2..: " + threadName);
                rwLock.getWritePermission();
                System.out.println("Writing 2.......: " + threadName);
                _errorTest(!_inWrite);
                rwLock.returnWritePermission();
                System.out.println("After Write 2...: " + threadName);
            }
        });
        Thread reader = new Thread(new Runnable() {
            public void run() {
                String threadName = Thread.currentThread().getName();
                System.out.println("Before Read.....: " + threadName);
                rwLock.getReadPermission();
                System.out.println("Reading.........: " + threadName);
                _errorTest(!_inWrite);
                rwLock.returnReadPermission();
                System.out.println("After Read......: " + threadName);
            }
        });
        String threadName = Thread.currentThread().getName();
        System.out.println("Before Write....: " + threadName);
        rwLock.getWritePermission();
        _inWrite = true;
        writer.start();
        reader.start();
        try {
            Thread.sleep(500);
        }
        catch (Exception ex) {
        }
        System.out.println("Writing.........: " + threadName);
        _inWrite = false;
        rwLock.returnWritePermission();
        System.out.println("After Write.....: " + threadName);
        try {
            reader.join();
        }
        catch (Exception ex) {
        }
        try {
            writer.join();
        }
        catch (Exception ex) {
        }
        return !_error;
    }

    /**
     * Test whether a nested read may occur.
     */
    boolean testNestedRead() {
        _printTestName("Nested Read");
        final RWLock rwLock = new RWLock();
        _clearError();
        String threadName = Thread.currentThread().getName();
        System.out.println("Before Read 1...: " + threadName);
        rwLock.getReadPermission();
        System.out.println("Reading 1.......: " + threadName);
        System.out.println("Before Read 2...: " + threadName);
        rwLock.getReadPermission();
        System.out.println("Reading 2.......: " + threadName);
        rwLock.returnReadPermission();
        System.out.println("After Read 2....: " + threadName);
        rwLock.returnReadPermission();
        System.out.println("After Read 1....: " + threadName);
        return !_error;
    }

    /**
     * Test whether a nested write may occur.
     */
    boolean testNestedWrite() {
        _printTestName("Nested Write");
        final RWLock rwLock = new RWLock();
        _clearError();
        String threadName = Thread.currentThread().getName();
        System.out.println("Before Write 1..: " + threadName);
        rwLock.getWritePermission();
        System.out.println("Writing 1.......: " + threadName);
        System.out.println("Before Write 2..: " + threadName);
        rwLock.getWritePermission();
        System.out.println("Writing 2.......: " + threadName);
        rwLock.returnWritePermission();
        System.out.println("After Write 2...: " + threadName);
        rwLock.returnWritePermission();
        System.out.println("After Write 1...: " + threadName);
        return !_error;
    }

    /**
     * Test whether a read may be nested inside a write.
     */
    boolean testReadInWrite() {
        _printTestName("Read In Write");
        final RWLock rwLock = new RWLock();
        _clearError();
        String threadName = Thread.currentThread().getName();
        System.out.println("Before Write....: " + threadName);
        rwLock.getWritePermission();
        System.out.println("Writing.........: " + threadName);
        System.out.println("Before Read.....: " + threadName);
        rwLock.getReadPermission();
        System.out.println("Reading.........: " + threadName);
        rwLock.returnReadPermission();
        System.out.println("After Read......: " + threadName);
        rwLock.returnWritePermission();
        System.out.println("After Write.....: " + threadName);
        return !_error;
    }

    /**
     * Test an "upgrade" from reader to writer.
     */
    boolean testUpgrade() {
        _printTestName("Upgrade");
        final RWLock rwLock = new RWLock();
        _clearError();
        String threadName = Thread.currentThread().getName();
        System.out.println("Before Read.....: " + threadName);
        rwLock.getReadPermission();
        System.out.println("Reading.........: " + threadName);
        System.out.println("Before Write....: " + threadName);
        try {
            rwLock.getWritePermission();
        }
        catch (Exception ex) {
            System.out.println("Got expected exception: " + ex.getMessage());
            return true;
        }
        System.out.println("Writing.........: " + threadName);
        rwLock.returnWritePermission();
        System.out.println("After Write.....: " + threadName);
        rwLock.returnReadPermission();
        System.out.println("After Read......: " + threadName);
        return !_error;
    }

    static void _fail() {
        System.out.println("Failed.");
        System.exit(1);
    }

    static void main(String[] args) {
        RWLockTester tester = new RWLockTester();
        boolean result;
        if (!tester.testWriteBlock())
            _fail();
        if (!tester.testReadBlock())
            _fail();
        if (!tester.testSimultaneousRead())
            _fail();
        if (!tester.testSimultaneousWrite())
            _fail();
        if (!tester.testNestedRead())
            _fail();
        if (!tester.testNestedWrite())
            _fail();
        if (!tester.testReadInWrite())
            _fail();
        if (!tester.testUpgrade())
            _fail();
        System.out.println("\nAll Passed.");
    }

}
