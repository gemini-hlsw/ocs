package edu.gemini.smartgcal.servlet.repository;

import edu.gemini.spModel.gemini.calunit.smartgcal.Calibration;
import edu.gemini.spModel.gemini.calunit.smartgcal.CalibrationFile;
import edu.gemini.spModel.gemini.gnirs.InstGNIRS;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.text.SimpleDateFormat;

/**
 * Test utility for checking the svn implementation.
 * Since we don't have a development svn repository that could be used for these tests it's marked as ignore.
 * Use this to test implementation changes to the svn stuff manually.
 */
@Ignore
public class SvnRepositoryTest {

    private static final String SVN_ROOT_URL = "http://source.gemini.edu/gcal/branches/development";
    private static final String SVN_USER = "software";
    private static final String SVN_PASSWORD = "S0ftware";

    @Test
    public void canReadHeadFromSvn() throws IOException {
        CalibrationSubversionRepository repo = new CalibrationSubversionRepository(SVN_ROOT_URL, SVN_USER, SVN_PASSWORD);
        CalibrationFile file = repo.getCalibrationFile(Calibration.Type.FLAT, InstGNIRS.SP_TYPE.readableStr);
        Assert.assertNotNull(file);
        Assert.assertNotNull(file.getVersion());
        Assert.assertNotNull(file.getData());
        Assert.assertEquals("Check if this works - updated\n", file.getData());
    }

    @Test
    public void canReadRevisionFromSvn() throws IOException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssz");
        CalibrationSubversionRepository repo = new CalibrationSubversionRepository(SVN_ROOT_URL, SVN_USER, SVN_PASSWORD);
        CalibrationFile file = repo.getCalibrationFile(Calibration.Type.FLAT, InstGNIRS.SP_TYPE.readableStr);
        Assert.assertEquals("Check if this works\n", file.getData());
    }

    @Test
    public void canWriteFileToSvn() {
        byte[] data = "Check if this works - updated2\n".getBytes();
        CalibrationSubversionRepository repo = new CalibrationSubversionRepository(SVN_ROOT_URL, SVN_USER, SVN_PASSWORD);
        repo.updateCalibrationFile(Calibration.Type.FLAT, InstGNIRS.SP_TYPE.readableStr, data);
    }
}
