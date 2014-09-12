package edu.gemini.spModel.smartgcal.repository;

import edu.gemini.spModel.gemini.calunit.smartgcal.*;
import edu.gemini.spModel.smartgcal.CalibrationMapReader;
import edu.gemini.spModel.smartgcal.UpdatableCalibrationRepository;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A repository implementation for smart gemini calibrations that uses the file system to cache
 * calbration files downloaded through the service. It uses a backup repository in case there
 * are no downloaded files in the cache available yet.
 */
public class CalibrationFileCache implements UpdatableCalibrationRepository {
    private static final Logger LOG = Logger.getLogger(CalibrationFileCache.class.getName());

    private final CalibrationRepository initialDataRepository;
    private final File fileCachePath;

    /**
     * Creates a new file based cache for calibration data allowing to make updated calibration
     * information persistent.
     * @param path
     */
    public CalibrationFileCache(File path) {
        this(path, new CalibrationResourceRepository());
    }

    /**
     * Creates a new file based cache for calibration data allowing to make updated calibration
     * information persistent.
     * @param path
     */
    public CalibrationFileCache(File path, CalibrationRepository initialDataRepository) {
        LOG.log(Level.INFO, "initializing smart calibration file cache :" + path.getAbsolutePath());
        this.fileCachePath = path;
        this.initialDataRepository = initialDataRepository;
        createDirectories();

//        LOG.warning("RCN: CLEARING THE GCAL CACHE");
//        clear();
        // As a precaution to possible changes in data formats or files that have been
        // damaged or manually edited we delete any cached files that can not be read
        // and hope that the problem goes away with the next update.
        deleteInvalidFiles();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Version getVersion(Calibration.Type type, String instrument) throws IOException {
        // Note: we're reading the whole file just to get the version number, this includes some overhead
        // but for now this is fast enough and simpler than having a special implementation just for this
        CalibrationFile file = getCalibrationFile(type, instrument);
        return file.getVersion();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CalibrationFile getCalibrationFile(Calibration.Type type, String instrument) throws IOException {
        File file = getCalibrationFileName(type, instrument);
        if (file.exists()) {
            LOG.log(Level.FINE,  String.format("accessing calibration file from file cache: %s", file.getAbsolutePath()));
            return CalibrationFile.fromFile(file);
        } else {
            return initialDataRepository.getCalibrationFile(type, instrument);
        }
    }

    /**
     * Clears the cache by deleting all files from the file system.
     */
    @Override
    public void clear() {
        LOG.log(Level.INFO,  "clearing cache");
        for (File f : fileCachePath.listFiles()) {
            f.delete();
        }
    }

    @Override
    public void writeUpdateTimestamp() throws IOException {
        PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(getTimestampFileName())));
        try {
            writer.println((new SimpleDateFormat()).format(new Date()));
        } finally {
            writer.close();
        }
    }

    @Override
    public Date getLastUpdateTimestamp() {
        Date timestamp = new Date((new Date()).getTime() - 365l*24l*60l*60l*1000l); // default return timestamp is one year in the past

        if (!getTimestampFileName().exists()) {
            return timestamp;
        }

        try {
            BufferedReader reader = new BufferedReader(new FileReader(getTimestampFileName()));
            try {
                String line = reader.readLine();
                timestamp = (new SimpleDateFormat()).parse(line);
            } catch (Exception e) {
                // intentionally ignore any error, there is not much we can do if we
                // can't read/parse this file so we just continue and return the default
                // timestamp (file will be overwritten with new timestamp which hopefully
                // solves this issue)
            } finally {
                reader.close();
            }
        } catch (IOException e) {
            // intentionally ignore, not much we can do, worst case we will force an update
            // and rewrite the timestamp
        }

        return timestamp;
    }

    private void createDirectories() {
        LOG.log(Level.FINE, "creating cache directories");
        if (!fileCachePath.exists()) {
            if (!fileCachePath.mkdirs()) {
                // there's nothing we can do here. fail.
                String message = "could not create directories for file cache " + fileCachePath.getAbsolutePath();
                LOG.log(Level.WARNING,  message);
                throw new RuntimeException(message);
            }
        }
    }

    /**
     * Check all cached files and deletes the ones that could not be read.
     */
    private void deleteInvalidFiles() {
        LOG.log(Level.FINE, "checking cached files");
        for (String instrument : SmartGcalService.getInstrumentNames()) {
            for (Calibration.Type type : SmartGcalService.getAvailableTypes(instrument)) {
                File file = getCalibrationFileName(type, instrument);
                if (!file.exists()) {
                    continue;
                }
                boolean valid = true;
                try {
                    CalibrationFile calibrationFile = CalibrationFile.fromFile(file);
                    List<String> errors = CalibrationMapReader.validateData(instrument, calibrationFile);
                    if (errors.size() != 0) {
                        LOG.log(Level.FINE, String.format("calibration file has errors, first error is: %s", errors.get(0)));
                        valid = false;
                    }
                } catch (Exception e) {
                    LOG.log(Level.FINE, String.format("could not read calibration file", e));
                    valid = false;
                }
                if (!valid) {
                    LOG.log(Level.WARNING, String.format("deleting invalid calibration file: %s", file.getAbsolutePath()));
                    file.delete(); // delete corrupted file
                    getTimestampFileName().delete(); //make sure update is run asap
                }
            }
        }
    }

    /**
     * Updates a single file in the file system with a newer version.
     * In order to keep track of the version the version number is added as a comment as first line of the new file.
     * Before trying to write a new / updated version of the file the existing one is renamed to xx.backup. In
     * case an error happens while writing the backup file is renamed back and will be kept as most current
     * version.
     * @param instrument
     * @param newestVersion
     * @param data
     * @throws IOException
     */
    @Override
    public synchronized void updateCalibrationFile(Calibration.Type type, String instrument,  Version newestVersion, byte[] data) throws IOException {
        File calibrationFile = getCalibrationFileName(type, instrument);
        File newFile = getNewFileName(type, instrument);

        // if backup file is still there, remove it
        if (newFile.exists() && !newFile.delete()) {
            String message = "removing 'new' calibration file failed " + newFile.getAbsolutePath();
            LOG.log(Level.SEVERE, message);
            throw new RuntimeException(message);
        }

        // now try to write the new calibration file
        BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(data)));
        PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(newFile)));
        try {
            // write revision and timestamp of this file as first line
            writer.println(newestVersion);
            // now write all the rest
            String line = null;
            while ((line = reader.readLine()) != null) {
                writer.println(line);
            }
        } finally {
            reader.close();
            writer.close();
        }

        // replace 'old' calibration file with new one
        // NOTE: unfortunately renameTo works different on UNIX and Windows systems: on UNIX it is atomic and
        // replaces an already existing file with the same name, on Windows it will fail if a file with
        // the target name already exists. Therefore we have to delete the 'old' file first (if it exists).
        if (calibrationFile.exists() && !calibrationFile.delete()) {
            LOG.log(Level.INFO,  "removing 'old' calibration file failed " + calibrationFile.getAbsolutePath());
        }
        if (!newFile.renameTo(calibrationFile)) {
            LOG.log(Level.SEVERE,  "renaming 'new' calibration file failed " + newFile.getAbsolutePath());
        }

        // NOTE: if for whatever weird reason these lines end up deleting the calibration file without replacing
        // it with a new version the next update cycle will download a new version.
    }

    /**
     * Creates a file representing the calibration file.
     * @param instrument
     * @return
     */
    private File getCalibrationFileName(Calibration.Type type, String instrument) {
        return new File(fileCachePath.getPath() + File.separator + instrument + "_" + type + ".csv");
    }

    /**
     * Creates a file representing the new calibration files.
     * @param instrument
     * @return
     */
    private File getNewFileName(Calibration.Type type, String instrument) {
        return new File(fileCachePath.getPath() + File.separator + instrument + "_" + type + ".new");
    }

    /**
     * Creates a file representing the timestamp file.
     * @return
     */
    private File getTimestampFileName() {
        return new File(fileCachePath.getPath() + File.separator + "timestamp");
    }

}
