// Copyright 2011 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: OT.java 36881 2011-08-26 20:02:12Z fnussber $

package edu.gemini.spModel.smartgcal.repository;

import edu.gemini.spModel.gemini.calunit.smartgcal.*;
import edu.gemini.spModel.smartgcal.CalibrationMapReader;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

/**
 * This test checks the internal mechanics as well as the validity of the calibration data that is part
 * of the OT delivery in the form of resource files. If this test fails OT will not work properly!
 */
public class ResourceRepositoryTest {

    @Test
    /**
     * Checks that calibration files for all instruments can be read and are valid.
     */
    public void calibrationFilesAreValid() throws IOException {

        // NOTE: OT/OTR/ODB WILL NOT WORK PROPERLY IF ONE OF THE FILES
        // PROVIDED AS RESOURCES IS BROKEN!

        CalibrationRepository repository = new CalibrationResourceRepository();
        for (String instrument : SmartGcalService.getInstrumentNames()) {
            for (Calibration.Type type : SmartGcalService.getAvailableTypes(instrument)) {

                Version currentVersion = repository.getVersion(type, instrument);
                Assert.assertNotNull(currentVersion);
                Assert.assertNotNull(currentVersion.getTimestamp());
                Assert.assertNotNull(currentVersion.getRevision());

                CalibrationFile file = repository.getCalibrationFile(type, instrument);
                List<String> errors = CalibrationMapReader.validateData(instrument, file);
                Assert.assertEquals(0, errors.size());
            }
        }

    }
}
