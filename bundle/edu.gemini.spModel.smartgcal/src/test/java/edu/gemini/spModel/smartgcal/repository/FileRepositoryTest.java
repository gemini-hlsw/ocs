// Copyright 2011 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: OT.java 36881 2011-08-26 20:02:12Z fnussber $

package edu.gemini.spModel.smartgcal.repository;

import edu.gemini.spModel.gemini.calunit.smartgcal.*;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 */
public class FileRepositoryTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void canUpdate() throws IOException {

        CalibrationRepository update = new FakeUpdateRepository();
        CalibrationFileCache repository = new CalibrationFileCache(folder.getRoot());

        CalibrationUpdateEvent event = CalibrationUpdater.instance.update(repository, update);

        // check if timestamp was set
        Date timestamp = repository.getLastUpdateTimestamp();
        Assert.assertNotNull(timestamp);
        Assert.assertTrue((new Date()).getTime() - timestamp.getTime() < 60l*60l*1000l);

        // check if update was successful
        // -> for an empty cache that means that for all instruments and types the data has been read from the
        // service and written to the file system
        for (String instrument : SmartGcalService.getInstrumentNames()) {
            for (Calibration.Type type : SmartGcalService.getAvailableTypes(instrument)) {
                String fileName = instrument + "_" + type + ".csv";
                String updateName = instrument + " " + type + "s";
                File cachedFile = new File(folder.getRoot().getAbsolutePath()+File.separator+fileName);

                // each file should have been updated
                Assert.assertTrue(event.getUpdatedFiles().contains(updateName));

                // the cache should have a file for each instrument and type
                Assert.assertTrue(cachedFile.exists());
            }
        }

    }

    @Test
    public void canGetData() throws IOException {

        CalibrationFileCache repository = new CalibrationFileCache(folder.getRoot());

        Version currentVersion = repository.getVersion(Calibration.Type.FLAT, SmartGcalService.getInstrumentNames().get(0));
        CalibrationFile file = repository.getCalibrationFile(Calibration.Type.FLAT, SmartGcalService.getInstrumentNames().get(0));

        // the two versions should be identical...
        Assert.assertEquals(currentVersion, file.getVersion());

    }

    private class FakeUpdateRepository implements CalibrationRepository {
        @Override
        public Version getVersion(Calibration.Type type, String instrument) throws IOException {
            return new Version(Integer.MAX_VALUE, new Date());  // force update!
        }

        @Override
        public CalibrationFile getCalibrationFile(Calibration.Type type, String instrument) throws IOException {
            return new CalibrationFile(new Version(0, new Date()), "blah blah blah!");
        }
    }
}
