//
// $
//

package edu.gemini.dataman.context;

import edu.gemini.datasetfile.DatasetFileService;
import edu.gemini.datasetfile.impl.DatasetFileServiceImpl;
import edu.gemini.datasetfile.impl.DatasetFileState;
import edu.gemini.datasetrecord.DatasetRecordService;
import edu.gemini.datasetrecord.impl.DsetRecordServiceImpl;
import edu.gemini.pot.spdb.DBLocalDatabase;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.util.security.principal.StaffPrincipal;

import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of {@link DatamanServices} for testing.  Comes loaded with a
 * test ODB and dataset record and file services.
 */
public class TestDatamanServices implements DatamanServices {

    private TestDatamanConfig config;
    private IDBDatabaseService odb;
    private DsetRecordServiceImpl datasetRecordService;
    private DatasetFileServiceImpl datasetFileService;

    // test as superuser
    private final Set<Principal> user = Collections.<Principal>singleton(StaffPrincipal.Gemini());

    public TestDatamanServices(TestDatamanConfig config) throws IOException {
        this.config = config;

//        setOdbReleaseProperties();
        odb = DBLocalDatabase.createTransient();

        datasetRecordService = new DsetRecordServiceImpl(user);
        datasetRecordService.addDatabase(odb);
        datasetRecordService.start();

        File stateFile = new File(config.getTempDir(), "state");

        datasetFileService = new DatasetFileServiceImpl(config.getWorkDir(),
                                             new DatasetFileState(stateFile));
    }

    public IDBDatabaseService getTestOdb() { return odb; }

    public void cleanup() {
        datasetRecordService.stop();
        datasetRecordService.removeDatabase(odb);
        config.cleanup();
        odb.getDBAdmin().shutdown();
    }

    public DatasetFileService getDatasetFileService() {
        return datasetFileService;
    }

    public DatasetRecordService getDatasetRecordService() {
        return datasetRecordService;
    }

    public Set<IDBDatabaseService> getDatabases() {
        Set<IDBDatabaseService> res = new HashSet<IDBDatabaseService>();
        res.add(odb);
        return res;
    }

    private void setOdbReleaseProperties() {
        // Set release properties needed to make and destroy the test database.
        System.setProperty("gemini.release.name",    "1969B.1.1.1");
        System.setProperty("gemini.release.version", "1.1.1");
        System.setProperty("gemini.release.build",   "1");
        System.setProperty("gemini.release.date",    "1969/08/15");
    }

    public DatamanConfig getConfig() {
        return config;
    }
}
