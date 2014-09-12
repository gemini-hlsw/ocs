//
// $Id: HeditTest.java 37 2005-08-20 17:46:18Z shane $
//

package edu.gemini.fits;

import junit.framework.TestCase;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

/**
 *
 */
public class HeditTest extends TestCase {
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
        _f.delete();
    }

    private static List<HeaderItem> _createHeaderItems(int nkeys) {
        List<HeaderItem> lst = new ArrayList<HeaderItem>();

        // Create the fake header keys.
        // KEY0    = 'Value 0 '           / Comment 0
        // KEY1    = 'Value 1 '           / Comment 1
        HeaderItem hi;
        for (int i = 0; i < nkeys; ++i) {
            //noinspection StringContatenationInLoop
            hi = DefaultHeaderItem.create("KEY" + i, "Value " + i, "Comment " + i);
            lst.add(hi);
        }

        return lst;
    }

    private void _initFile(int nkeys, int drecs) throws Exception {
        int recsize = FitsConstants.RECORD_SIZE;

        int keybytes = (nkeys + 1) * FitsConstants.HEADER_ITEM_SIZE;  // + 1 for END
        int keyrecs = (keybytes / recsize) + (((keybytes % recsize) > 0) ? 1 : 0);
        int totalrecs = keyrecs + drecs;

        ByteBuffer buf = ByteBuffer.allocate(totalrecs * recsize);

        // Write out the fake header keys.
        List<HeaderItem> lst = _createHeaderItems(nkeys);
        for (HeaderItem hi : lst) {
            buf.put(HeaderItemFormat.toBytes(hi));
        }

        // Write out END
        String end = String.format("%-80s", "END");
        buf.put(end.getBytes(FitsConstants.CHARSET_NAME));

        // Figure out how many blanks to write.
        byte[] blanks = new byte[(keyrecs * 2880) - keybytes];
        Arrays.fill(blanks, (byte) ' ');
        buf.put(blanks);

        // Write out the fake data.
        for (int i = 0; i < drecs; ++i) {
            byte[] fakedata = new byte[recsize];
            Arrays.fill(fakedata, (byte) i);
            buf.put(fakedata);
        }

        buf.flip();
        FileUtil.writeBuf(_channel, buf, 0);
        _channel.position(0);
        _raf.close();
    }

    public void _testRead(int nkeywords) throws Exception {
        _initFile(nkeywords, 1);

        Hedit hedit = new Hedit(_f);
        List<HeaderItem> res = hedit.readPrimary();
        assertEquals(nkeywords, res.size());

        int i = 0;
        for (HeaderItem hi : res) {
            //noinspection StringContatenationInLoop
            assertEquals("KEY" + i, hi.getKeyword());
            //noinspection StringContatenationInLoop
            assertEquals("Value " + i, hi.getValue());
            //noinspection StringContatenationInLoop
            assertEquals("Comment " + i, hi.getComment());
            ++i;
        }
    }

    public void testRead0() throws Exception {
        _testRead(0);
    }

    public void testRead1() throws Exception {
        _testRead(1);
    }

    public void testRead2() throws Exception {
        _testRead(2);
    }

    // completely fills one header record without going over (since END takes
    // the last 80 byes
    public void testRead35() throws Exception {
        _testRead(35);
    }

    // One record full of keywords, one extra record for "END"
    public void testRead36() throws Exception {
        _testRead(36);
    }

    // One record full of keywords, one extra record for the last keyword and
    // "END"
    public void testRead37() throws Exception {
        _testRead(37);
    }

    public void testReadSubset() throws Exception {
        _initFile(10, 1);

        Set<String> keys = new HashSet<String>();
        keys.add("KEY0");
        keys.add("KEY7");
        keys.add("NOEXIST");

        Hedit hedit = new Hedit(_f);
        List<HeaderItem> res = hedit.readPrimary(keys);
        assertEquals(2, res.size());
        assertEquals("KEY0", res.get(0).getKeyword());
        assertEquals("KEY7", res.get(1).getKeyword());
    }

    private void _testUpdate(int existingKeys, Set<String> updateKeys, int newKeys) throws Exception {
        // Create the file.
        _initFile(existingKeys, 1);

        // Get the list of header items as they will appear after the update
        // in a list called "expected".
        // Get the list of just the updates to apply in a list called "updates".
        List<HeaderItem> expected = _createHeaderItems(existingKeys + newKeys);
        List<HeaderItem> updates = new ArrayList<HeaderItem>();
        for (ListIterator<HeaderItem> it = expected.listIterator(); it.hasNext();) {
            HeaderItem hi = it.next();
            String keyword = hi.getKeyword();
            if (updateKeys.contains(keyword)) {
                hi = DefaultHeaderItem.create(keyword, "Updated", hi.getComment());
                it.set(hi);
                updates.add(hi);
            }
        }
        updates.addAll(expected.subList(existingKeys, existingKeys + newKeys));

        // Do the updates.
        Hedit hedit = new Hedit(_f);
        hedit.updatePrimary(updates);

        // Check that it equals what we expected.  Calculate the file size.
        int nonblankHeaderBytes = FitsConstants.HEADER_ITEM_SIZE *
                (existingKeys + newKeys + 1);
        int overflow = nonblankHeaderBytes % FitsConstants.RECORD_SIZE;
        int blankHeaderBytes = (overflow == 0 ? 0 :
                FitsConstants.RECORD_SIZE -
                        nonblankHeaderBytes % FitsConstants.RECORD_SIZE);
        int headerBytes = nonblankHeaderBytes + blankHeaderBytes;

        int totalBytes = headerBytes + FitsConstants.RECORD_SIZE;

        assertEquals(totalBytes, _f.length());

        // Create a byte array with the expected content.
        ByteBuffer expect = ByteBuffer.allocate(totalBytes);
        for (HeaderItem hi : expected) {
            expect.put(HeaderItemFormat.toBytes(hi));
        }
        String end = String.format("%-80s", "END");
        expect.put(end.getBytes(FitsConstants.CHARSET_NAME));
        byte[] blanks = new byte[blankHeaderBytes];
        Arrays.fill(blanks, (byte) ' ');
        expect.put(blanks);
        byte[] data = new byte[FitsConstants.RECORD_SIZE];
        Arrays.fill(data, (byte) 0);
        expect.put(data);
        expect.flip();

        // Read the entire file into a ByteBuffer.
        RandomAccessFile raf = new RandomAccessFile(_f, "r");
        FileChannel channel = raf.getChannel();
        ByteBuffer actual = ByteBuffer.allocate(totalBytes);
        FileUtil.readBuf(channel, actual, 0);
        actual.flip();

        // Compare the two byte arrays.
        assertEquals(expect, actual);
    }

    public void testNoUpdate() throws Exception {
        Set<String> updateKeys = new HashSet<String>(0);
        _testUpdate(10, updateKeys, 0);
    }

    public void testOneUpdate() throws Exception {
        Set<String> updateKeys = new HashSet<String>(0);
        updateKeys.add("KEY4");
        _testUpdate(10, updateKeys, 0);
    }

    public void testMultiUpdate() throws Exception {
        Set<String> updateKeys = new HashSet<String>(0);
        updateKeys.add("KEY0");
        updateKeys.add("KEY9");
        _testUpdate(10, updateKeys, 0);
    }

    public void testAddOne() throws Exception {
        Set<String> updateKeys = new HashSet<String>(0);
        _testUpdate(10, updateKeys, 1);
    }

    public void testAddOneUpdateOne() throws Exception {
        Set<String> updateKeys = new HashSet<String>(0);
        updateKeys.add("KEY4");
        _testUpdate(10, updateKeys, 1);
    }

    // fill a header record, but don't go over one record
    public void testFillRecord() throws Exception {
        Set<String> updateKeys = new HashSet<String>(0);
        updateKeys.add("KEY4");
        // 10 existing + 25 new + 1 for END = 36 == # header items in a record
        _testUpdate(10, updateKeys, 25);
    }

    // fill a header record with keywords, requiring the END to go in a record
    // of its own.
    public void testEndSpillsOver() throws Exception {
        Set<String> updateKeys = new HashSet<String>(0);
        updateKeys.add("KEY4");
        // 10 existing + 26 new + 1 for END = 37 == (1 + # header items in a record)
        _testUpdate(10, updateKeys, 26);
    }

    // fill a header record with keywords, and spill over with a keyword in the
    // next record.
    public void testKeywordSpillsOver() throws Exception {
        Set<String> updateKeys = new HashSet<String>(0);
        updateKeys.add("KEY4");
        // 10 existing + 27 new + 1 for END = 37 == (2 + # header items in a record)
        _testUpdate(10, updateKeys, 27);
    }

    public void testReadMultipleExtensions() throws URISyntaxException, IOException, FitsParseException, InterruptedException {
        File fitsFile = new File(HeditTest.class.getResource("FITS_WITH_EXTENSIONS.fits").toURI());
        Hedit hedit = new Hedit(fitsFile);
        List<Header> headers = hedit.readAllHeaders();
        assertEquals(2, headers.size());
        assertEquals(0, headers.get(0).getIndex());
        assertEquals(1, headers.get(1).getIndex());
        assertEquals(212, headers.get(0).getKeywords().size());
        assertEquals(109, headers.get(1).getKeywords().size());
    }

    public void testNewKeywordToExtension() throws Exception, IOException, FitsParseException, InterruptedException {
        File fitsFile = new File(HeditTest.class.getResource("FITS_WITH_EXTENSIONS.fits").toURI());
        copyFile(fitsFile, _f);

        Hedit hedit = new Hedit(_f);

        Set<HeaderItem> updateKeys = new HashSet<HeaderItem>(1);
        updateKeys.add(new DefaultHeaderItem("KEY4", "VALUE", "Comment", true));

        // Create the file.
        hedit.updateHeader(updateKeys, 1);

        hedit = new Hedit(_f);
        List<Header> headers = hedit.readAllHeaders();
        assertEquals(2, headers.size());

        Header extensionHeader = headers.get(1);
        assertEquals(110, extensionHeader.getKeywords().size());
        assertEquals("VALUE", extensionHeader.get("KEY4").getValue());
        assertEquals("Comment", extensionHeader.get("KEY4").getComment());
    }

    public void testUpdateKeywordInExtension() throws Exception, IOException, FitsParseException, InterruptedException {
        File fitsFile = new File(HeditTest.class.getResource("FITS_WITH_EXTENSIONS.fits").toURI());
        copyFile(fitsFile, _f);

        Hedit hedit = new Hedit(_f);

        Set<HeaderItem> updateKeys = new HashSet<HeaderItem>(1);
        updateKeys.add(new DefaultHeaderItem("TFORM13", "NEWVALUE", "New Comment", true));

        // Create the file.
        hedit.updateHeader(updateKeys, 1);

        hedit = new Hedit(_f);
        List<Header> headers = hedit.readAllHeaders();
        assertEquals(2, headers.size());

        Header extensionHeader = headers.get(1);
        assertEquals(109, extensionHeader.getKeywords().size());
        assertEquals("NEWVALUE", extensionHeader.get("TFORM13").getValue());
        assertEquals("New Comment", extensionHeader.get("TFORM13").getComment());
    }

    private static void copyFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.exists()) {
            destFile.createNewFile();
        }

        FileChannel source = null;
        FileChannel destination = null;
        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }
}
