//
// $Id: RecordIteratorTest.java 37 2005-08-20 17:46:18Z shane $
//

package edu.gemini.fits;

import junit.framework.TestCase;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 *
 */
public final class RecordIteratorTest extends TestCase {

    private File _f;
    private RandomAccessFile _raf;
    private FileChannel _channel;

    public void setUp() throws Exception {
        super.setUp();
        _f = File.createTempFile("test", ".fits");
        _raf = new RandomAccessFile(_f, "rw");
        _channel = _raf.getChannel();
    }

    public void tearDown() throws Exception {
        _raf.close();
        _f.delete();
    }

    // Initializes the tmp file with nrecs 2880 byte records and extraBytes
    // extra bytes.  Each record is filled with bytes for the number of its
    // sequence.  For example, record 0 is filled with 0s, record 1 with 1s,
    // etc.
    private void _initFile(int nrecs, int extraBytes) throws Exception {
        if (nrecs > 0) {
            byte[] buf = new byte[FitsConstants.RECORD_SIZE];
            for (int i=0; i<nrecs; ++i) {
                Arrays.fill(buf, (byte) i);
                _writeBytes(i, buf);
            }
        }
        if (extraBytes > 0) {
            byte[] buf = new byte[extraBytes];
            Arrays.fill(buf, (byte) nrecs);
            _writeBytes(nrecs, buf);
        }
        _channel.position(0);
    }

    private void _writeBytes(int recNumber, byte[] bytes)
            throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(bytes.length);
        buf.put(bytes);
        buf.flip();
        FileUtil.writeBuf(_channel, buf, recNumber*FitsConstants.RECORD_SIZE);
    }

    private void _test(int nrecs) throws Exception {
        _test(nrecs, 0, 0);
    }

    private void _test(int nrecs, int extraBytes) throws Exception {
        _test(nrecs, extraBytes, 0);
    }

    private void _test(int nrecs, int extraBytes, long filePos) throws Exception {
        _initFile(nrecs, extraBytes);
        if (filePos != 0) _channel.position(filePos);

        byte[] expBytes = new byte[FitsConstants.RECORD_SIZE];
        byte[] actBytes = new byte[FitsConstants.RECORD_SIZE];

        RecordIterator rit = RecordIterator.iterateFile(_channel);

        // expect to start on a block boundary
        int count = (int) (filePos / FitsConstants.RECORD_SIZE);

        while (count < nrecs) {
            assertTrue(rit.hasNext());
            Record rec = rit.next();
            ByteBuffer buf = rec.getBuffer();
            assertEquals(FitsConstants.RECORD_SIZE, buf.remaining());
            buf.get(actBytes);

            Arrays.fill(expBytes, (byte) count);
            Arrays.equals(expBytes, actBytes);

            ++count;
        }

        // extra bytes are not returned by the iterator
        assertFalse(rit.hasNext());
    }

    public void testEmpty() throws Exception {
        _test(0);
    }

    public void testOne() throws Exception {
        _test(1);
    }

    public void testTwo() throws Exception {
        _test(2);
    }

    public void testThree() throws Exception {
        _test(3);
    }

    public void testPartialBlock() throws Exception {
        _test(2, 80);
    }

    public void testResetFilePos0() throws Exception {
        _test(2, 0, 200);
    }

    public void testResetFilePos1() throws Exception {
        _test(2, 0, 2880 + 200);
    }
}
