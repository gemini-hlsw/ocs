//
// $Id: OdbMailTest.java 46733 2012-07-12 20:43:36Z rnorris $
//
package edu.gemini.dbTools.mail.test;

import edu.gemini.dbTools.mail.OdbMailConfig;
import edu.gemini.dbTools.odbState.OdbStateConfig;
import edu.gemini.pot.spdb.IDBDatabaseService;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.net.URL;

// ***** TODO: Check with Shane on this class d*****
// Do we even really need this class? OdbMailCase calls getDatabase, which throws an exception.
// This is the only method called outside of this class.

@Ignore
public class OdbMailTest {
//    public OdbMailTest(final String name) {
//        super(name);
//    }

    // Not sure if this is right.
    @Before
    public static TestSuite suite() {
        // Set the property that defines where the state file lives.
        final File testDir = getTestDir();
        final File mailStateFile = new File(testDir, "odbMailState.xml");
//        System.setProperty(OdbMailConfig.STATE_FILE_PROPERTY, mailStateFile.getPath());

        final File stateFile = new File(testDir, "odbState.xml");
//        System.setProperty(OdbStateConfig.STATE_FILE_PROPERTY, stateFile.getPath());

        final TestSuite suite = new TestSuite();
        //suite.addTestSuite(OdbMailCase.class);
        return suite;
    }

    public static IDBDatabaseService getDatabase() {
        throw new Error("Remote database not supported");
//        return DBAccess.getRemoteDatabase(5);
    }

    private static String getTestDirPath() {
        // Get the path to this class.
        final URL url = OdbMailTest.class.getResource("OdbMailTest.class");
        if (url == null) {
            throw new RuntimeException("Couldn't find own class!");
        }

        final String file = url.getFile();

        // Strip off the classname
        final int pos = file.lastIndexOf('/');
        return file.substring(0, pos);
    }

    private static File getTestDir() {
        return new File(getTestDirPath());
    }
}
