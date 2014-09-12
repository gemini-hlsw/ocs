package edu.gemini.smartgcal.odbinit;

import edu.gemini.spModel.gemini.calunit.smartgcal.CalibrationProvider;
import edu.gemini.spModel.gemini.calunit.smartgcal.CalibrationProviderHolder;
import edu.gemini.spModel.gemini.calunit.smartgcal.CalibrationRepository;
import edu.gemini.spModel.smartgcal.UpdatableCalibrationRepository;
import edu.gemini.spModel.smartgcal.provider.CalibrationProviderImpl;
import edu.gemini.spModel.smartgcal.repository.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class SmartGcalOdbInitializer {
    private static final Logger LOG = Logger.getLogger(SmartGcalOdbInitializer.class.getName());

    private final File dir;
    private final String host;
    private final int port;
    private final long intervalSeconds;

    private CalibrationUpdateListener listener;

    public SmartGcalOdbInitializer(File dir, String host, int port, long intervalSeconds) {
        validate(dir);
        this.dir = dir;
        this.host = host; // host running gcal servlet for udpates
        this.port = port; // gcal servlet port number
        this.intervalSeconds = intervalSeconds;

        setFailSafeMode();
    }

    public File getDirectory() { return dir; }

    private static void validate(File dir) {
        if (dir == null) direrror(dir, "'dir' not specified");
        assert dir != null;
        if (dir.exists() && !dir.isDirectory()) direrror(dir, "not a directory");
        if (!dir.exists() && !dir.mkdirs()) direrror(dir, "could not make directory");
        if (!dir.canRead() || !dir.canWrite()) direrror(dir, "permissions problem");
    }

    private static void direrror(File dir, String msg) {
        throw new IllegalArgumentException(msg + ": " + dir);
    }

    private void setFailSafeMode() {
        final CalibrationRepository rep = new CalibrationResourceRepository();
        CalibrationProviderHolder.setProvider(new CalibrationProviderImpl(rep));
    }

    public void start() {
        try {
            final UpdatableCalibrationRepository cachedRepository = new CalibrationFileCache(dir);
            listener = new CalibrationUpdateListener(cachedRepository);

            final CalibrationProviderImpl calibrationProvider = new CalibrationProviderImpl(cachedRepository);
            CalibrationProviderHolder.setProvider(calibrationProvider);

            final CalibrationRepository service = new CalibrationRemoteRepository(host, port);
            CalibrationUpdater.instance.addListener(listener);
            CalibrationUpdater.instance.start(cachedRepository, service, intervalSeconds * 1000);

            LOG.log(Level.INFO, "smartgcal provider and updater successfully initialised");

        } catch (Exception e) {
            // severe problem: could not initialise smartgcal properly, use a failsafe fallback instead
            LOG.log(Level.SEVERE, "could not initialise smartgcal, using failsafe fallback instead", e);
            setFailSafeMode();
        }
    }

    public void stop() {
        if (listener != null) CalibrationUpdater.instance.removeListener(listener);
        CalibrationUpdater.instance.stop();
        setFailSafeMode();
    }

    /**
     * A simple listener that will be called after updates and replace the current calibration provider with
     * an updated one after a successful update or log an error message in case of an error.
     */
    private static class CalibrationUpdateListener implements ActionListener {

        private final CalibrationRepository repository;

        public CalibrationUpdateListener(CalibrationRepository repository) {
            this.repository = repository;
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            if (actionEvent instanceof CalibrationUpdateEvent) {
                List<String> updatedCalibrations = ((CalibrationUpdateEvent)actionEvent).getUpdatedFiles();
                if (updatedCalibrations.size() > 0) {
                    CalibrationProvider updatedProvider = new CalibrationProviderImpl(repository);
                    CalibrationProviderHolder.setProvider(updatedProvider);

                    LOG.log(Level.INFO, "Updated CalibrationMappings: " + updatedCalibrations);
                }
            }
        }
    }

}
