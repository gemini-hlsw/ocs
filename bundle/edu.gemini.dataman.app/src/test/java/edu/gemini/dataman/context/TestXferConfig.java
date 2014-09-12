//
// $
//

package edu.gemini.dataman.context;

import edu.gemini.util.ssh.SshConfig;
import edu.gemini.util.ssh.SshConfig$;

import java.io.File;
import java.io.IOException;

/**
 * Test configuration for GSA transfer.
 */
public class TestXferConfig implements XferConfig {
    private static final String USER_PROPERTY = "edu.gemini.dataman.context.user";
    private static final String PASS_PROPERTY = "edu.gemini.dataman.context.password";

    private final File rootDir;
    private final File tempDir;
    private final File destDir;

    private final String userProp;
    private final String passProp;

    public TestXferConfig(File root) throws IOException {
        if (!root.mkdir()) throw new IOException();
        rootDir = root;

        destDir = new File(root, "dest");
        if (!destDir.mkdir()) throw new IOException();

        tempDir = new File(root, "temp");
        if (!tempDir.mkdir()) throw new IOException();

        userProp = System.getProperty(USER_PROPERTY);
        if (userProp == null) {
            throw new RuntimeException("Missing property " + USER_PROPERTY);
        }

        passProp = System.getProperty(PASS_PROPERTY);
        if (passProp == null) {
            throw new RuntimeException("Missing property " + PASS_PROPERTY);
        }
    }

    protected File getRoot() {
        return rootDir;
    }

    public String getTempDir() {
        return tempDir.getPath();
    }

    public String getDestDir() {
        return destDir.getPath();
    }

    public String getUser() {
        return userProp;
    }

    public String getHost() {
        return "localhost";
    }

    public String getPassword() {
        return passProp;
    }

    public int getTimeout() {
        return SshConfig$.MODULE$.DEFAULT_TIMEOUT();
    }
}