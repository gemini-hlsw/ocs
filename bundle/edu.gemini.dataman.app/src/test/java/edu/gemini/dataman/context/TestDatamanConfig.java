//
// $
//

package edu.gemini.dataman.context;

import javax.mail.internet.InternetAddress;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Collection;
import java.net.URL;

/**
 * {@link DatamanConfig} used for testing.
 */
public class TestDatamanConfig implements DatamanConfig {
    public static final String MULTI_XFER_QUERY_FILE = "MultiXferQueryResults.txt";

    private final File tempDir;
    private final File rawDir;
    private final File workDir;

    private final File xferQueryDir;
    private final GsaUrl xferQueryUrl;
    private final URL xferQueryMultiUrl;

    private final File crcQueryDir;
    private final GsaUrl crcQueryUrl;

    private final XferConfig baseXferConfig;
    private final GsaXferConfig gsaXferConfig;

    public TestDatamanConfig() throws Exception {
        tempDir = File.createTempFile("dataman-test", "");
        if (!tempDir.delete()) throw new IOException();
        if (!tempDir.mkdir()) throw new IOException();

        rawDir = new File(tempDir, "raw");
        if (!rawDir.mkdir()) throw new IOException();

        workDir = new File(tempDir, "work");
        if (!workDir.mkdir()) throw new IOException();

        xferQueryDir = new File(tempDir, "xferQueryTest");
        if (!xferQueryDir.mkdir()) throw new IOException();
        xferQueryUrl = new GsaUrl("file://" + xferQueryDir.getPath() + "/%FILE%");
        xferQueryMultiUrl = new URL("file://" + xferQueryDir.getPath() + "/" + MULTI_XFER_QUERY_FILE);

        crcQueryDir = new File(tempDir, "crcQueryTest");
        if (!crcQueryDir.mkdir()) throw new IOException();
        crcQueryUrl = new GsaUrl("file://" + crcQueryDir.getPath() + "/%FILE%");

        baseXferConfig = new TestXferConfig(new File(tempDir, "base"));
        gsaXferConfig = new TestGsaXferConfig(new File(tempDir, "gsa"));
    }

    public File getTempDir() {
        return tempDir;
    }

    public File getRawDir() {
        return rawDir;
    }

    public File getWorkDir() {
        return workDir;
    }

    public FileFilter getFileFilter() {
        return null;
    }

    public XferConfig getBaseXferConfig() {
        return baseXferConfig;
    }

    public GsaXferConfig getGsaXferConfig() {
        return gsaXferConfig;
    }

    public File getCrcQueryDir() {
        return crcQueryDir;
    }

    public GsaUrl getGsaCrcUrl() {
        return crcQueryUrl;
    }

    public File getXferQueryDir() {
        return xferQueryDir;
    }

    public GsaUrl getGsaXferStatusUrl() {
        return xferQueryUrl;
    }

    public URL getGsaAllXferStatusUrl() {
        return xferQueryMultiUrl;
    }

    public String getSmtpHost() {
        return null;
    }

    public Collection<InternetAddress> getCheckMailCc() {
        return null;
    }

    public long getOdbScanTime() {
        return 5 * 60 * 1000;
    }

    public boolean cleanup() {
        return deleteDir(tempDir);
    }

    private boolean deleteDir(File dir) {
      File[] files = dir.listFiles(new FileFilter() {
          public boolean accept(File file) {
              return !file.isDirectory();
          }
      });
      boolean res = true;
      if (files != null) {
          for (File file : files) {
              if (!file.delete()) res = false;
          }
      }
      files = dir.listFiles(new FileFilter() {
          public boolean accept(File file) {
              return file.isDirectory();
          }
      });
      if (files != null) {
          for (File f : files) {
              if (!deleteDir(f)) res = false;
          }
      }
      if (!dir.delete()) res = false;
      return res;
  }
}
