//
// $
//

package edu.gemini.dataman.context;

import junit.framework.Assert;

import java.io.File;
import java.io.FileWriter;

/**
 * Test configuration for GSA transfer.
 */
public final class TestGsaXferConfig extends TestXferConfig implements GsaXferConfig {
    private final File mdIngest;

    public TestGsaXferConfig(File root) throws Exception {
        super(root);
        mdIngest = writeMdIngestScript(getRoot());
    }

    private File writeMdIngestScript(File gsaDir) throws Exception {
        File mdIngest = new File(gsaDir, "mdIngest.sh");

        // Build the dummy mdIngest script.
        StringBuilder buf = new StringBuilder();
        buf.append("#! /bin/sh\n");
        buf.append("echo \"IS ready for ingestion\"\n");
        String script = buf.toString();

        // Write it out.
        FileWriter w = new FileWriter(mdIngest);
        try {
            w.write(script);
            w.flush();
        } finally {
            w.close();
        }

        // Make it executable.
        String cmd = "chmod 755 " + mdIngest.getPath();
        Process proc = Runtime.getRuntime().exec(cmd);
        Assert.assertEquals(0, proc.waitFor());

        return mdIngest;
    }

    public String getMdIngestScript() {
        return mdIngest.getPath();
    }

    public String getCadcRoot() {
        return null;
    }

    public String getCadcGroup() {
        return "staff"; // assuming the tester belongs to "staff" group...
    }
}
