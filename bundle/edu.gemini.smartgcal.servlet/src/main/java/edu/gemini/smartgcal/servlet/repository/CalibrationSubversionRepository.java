package edu.gemini.smartgcal.servlet.repository;

import edu.gemini.spModel.gemini.calunit.smartgcal.Calibration;
import edu.gemini.spModel.gemini.calunit.smartgcal.CalibrationFile;
import edu.gemini.spModel.gemini.calunit.smartgcal.CalibrationRepository;
import edu.gemini.spModel.gemini.calunit.smartgcal.Version;
import org.tigris.subversion.svnclientadapter.*;
import org.tigris.subversion.svnclientadapter.commandline.*;

import java.io.*;
import java.net.MalformedURLException;
import java.util.Date;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * A simple repository for calibration files that uses a subversion server as its backend
 * to store and versionize the calibration files.
 */
public class CalibrationSubversionRepository implements CalibrationRepository {

    private static final Logger LOG = Logger.getLogger(CalibrationSubversionRepository.class.getName());

    private static final String CALIBRATION_FILE_POSTFIX = ".csv";

    private final ISVNClientAdapter svnClient;
    private final String svnRootUrl;

    /**
     * Constructs an calibration repository that accesses a subversion repository.
     */
    public CalibrationSubversionRepository(String url, String user, String password) {
        if (url      == null) throw new IllegalArgumentException("url must not be null");
        if (user     == null) throw new IllegalArgumentException("user must not be null");
        if (password == null) throw new IllegalArgumentException("password must not be null");

        // If svn is not available CmdLineClientAdapterFactory.setup() will fail with an exception.
        // Unfortunately the original exception is swallowed and the one thrown by setup() does not have a
        // case and is generic and therefore gives no clues as to what went wrong. This additional code tries
        // to execute "svn --version" manually (which is what the setup() function is doing) and log the exception,
        // hopefully giving us some additional information.
        try {
            LOG.info("Trying to execute svn --version to validate svn availability");
            final CmdLineClientAdapter12 cmd = new CmdLineClientAdapter12(new CmdLineNotificationHandler());
            cmd.getVersion();
            LOG.info("svn --version: OK");
        } catch (final Exception e) {
            LOG.log(Level.WARNING, "svn --version: FAILED", e);
        }

        // Try to properly set things up.
        try {
            // NOTE: We are using the cmd line setup; svn cmd line client has to be installed locally to make this work.
            LOG.info("Trying to setup svn cmd line support");
            CmdLineClientAdapterFactory.setup();
            svnClient = SVNClientAdapterFactory.createSVNClient(SVNClientAdapterFactory.getPreferredSVNClientType());
            LOG.info("svn command line setup: OK");
        } catch (final Exception e) {
            LOG.log(Level.SEVERE, "svn command line setup: FAILED", e);
            throw new RuntimeException(e);
        }

        this.svnRootUrl = url;
        svnClient.setUsername(user);
        svnClient.setPassword(password);
    }

    /**
     * Updates a calibration file in the repository. We rely on the synchronization provided by subversion
     * in case multiple users try to update the same file at the same time.
     * @param type
     * @param instrument
     * @param data
     * @return
     */
    public Number updateCalibrationFile(Calibration.Type type, String instrument, byte[] data) {

        // create a temporary directory
        String tmpPath = System.getProperty("java.io.tmpdir");
        File targetPath = new File(tmpPath + File.separator + "subversion"+Long.toString(System.nanoTime()));
        targetPath.mkdir();

        try {

            System.out.println("Target temp directory: " + targetPath.getAbsolutePath());

            // check out the calibration file(s) to the temp directory
            SVNUrl pathUrl = getCalibrationPathUrl();
            svnClient.checkout(pathUrl, targetPath, SVNRevision.HEAD, false);

            // replace current file with new one ...
            FileOutputStream fis = new FileOutputStream(targetPath.getAbsolutePath() + File.separator + instrument + "_" + type + CALIBRATION_FILE_POSTFIX);
            fis.write(data);
            fis.close();

            // ... and commit changes. This will throw an SVNClientException in the unlikely case
            // that the file has been changed since we checked it out.
            File[] filesToCommit = new File[] {targetPath};
            return svnClient.commit(filesToCommit, "smart calibrations svn client update", false);

        } catch (SVNClientException svnEx) {
            // svn errors: e.g. concurrent check-ins
            throw new RuntimeException(svnEx);
        } catch (IOException ioEx) {
            // not much we can do about that
            throw new RuntimeException(ioEx);
        } finally {
            // cleanup the temp directory
            cleanup(targetPath);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Version getVersion(Calibration.Type type, String instrument) {
        try {
            SVNRevision revision = SVNRevision.HEAD;
            SVNUrl fileUrl = getCalibrationFileUrl(type, instrument);
            ISVNInfo fileInfo = svnClient.getInfo(fileUrl, revision, null);
            // we are using the last changed revision number as the "current" version for the files
            Number version = fileInfo.getLastChangedRevision().getNumber();
            Date timestamp = fileInfo.getLastChangedDate();
            return new Version(version, timestamp);
        } catch (Exception e) {
            // catch any low level exception and re-throw them wrapped
            // in a runtime exception
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CalibrationFile getCalibrationFile(Calibration.Type type, String instrument) {
        Version version = getVersion(type, instrument);
        String data = getCalibrationFile(type, instrument, version);
        return new CalibrationFile(version, data);
    }

    /**
     * Gets a specific version from the subversion repository. Currently not in use.
     * @param type
     * @param instrument
     * @param version
     * @return
     */
    private String getCalibrationFile(Calibration.Type type, String instrument, Version version) {
        try {
            // read the file
            SVNRevision revision = SVNRevision.getRevision(version.getRevision().toString());
            SVNUrl fileUrl = getCalibrationFileUrl(type, instrument);
            InputStream is = svnClient.getContent(fileUrl, revision);
            ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
            byte[] bytes = new byte[10000];
            int cnt;
            while ((cnt = is.read(bytes)) > 0) {
                bos.write(bytes, 0, cnt);
            }
            is.close();
            return new String(bos.toByteArray());
        } catch (Exception e) {
            // catch any low level exception and re-throw them wrapped
            // in a runtime exception
            throw new RuntimeException(e);
        }
    }

    private SVNUrl getCalibrationPathUrl() throws MalformedURLException {
        return new SVNUrl(svnRootUrl);
    }

    private SVNUrl getCalibrationFileUrl(Calibration.Type type, String instrument) throws MalformedURLException {
        return new SVNUrl(svnRootUrl + "/" + instrument + "_" + type + CALIBRATION_FILE_POSTFIX);
    }

    /**
     * Deletes recursively all files in a directory and the directory itself.
     * @param file
     */
    private void cleanup(File file) {
        if (file.isDirectory()) {
            for (File f : file.listFiles((FileFilter)null)) {
                cleanup(f);
            }
        }
        file.delete();
    }
}
