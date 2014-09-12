//
// $
//

package edu.gemini.dataman.util;

import static org.junit.Assert.*;
import org.junit.Test;

import java.io.File;
import java.net.URL;

/**
 *
 */
public final class DatamanFileUtilTest {
    private static final String MD5_RES = "0add241c7230a0eec1d1d516b0c52264";

    private File getTestFile() {
        URL url = this.getClass().getResource("TestFile.txt");
        return new File(url.getPath());
    }

    @Test
    public void testCopy() throws Exception {
        File dest = File.createTempFile("TestFileCopy", "txt");
        dest.deleteOnExit();

        DatamanFileUtil.copyFile(getTestFile(), dest);
        String md5 = DatamanFileUtil.md5HexString(dest);
        assertEquals(MD5_RES, md5);
    }

    @Test
    public void testMd5() throws Exception {
        assertEquals(MD5_RES, DatamanFileUtil.md5HexString(getTestFile()));
    }

    @Test
    public void testMd5_byte() throws Exception {
        byte[] ba = DatamanFileUtil.md5(getTestFile());
        int i = 0;
        for (byte b : ba) {
            String s = MD5_RES.substring(i, i+2);
            i+=2;
            assertEquals(s, String.format("%02x", b));
        }
    }

    @Test
    public void testCRC() throws Exception {
        long l = DatamanFileUtil.crc(getTestFile());
        assertEquals(3910581467l, l);
    }
}
