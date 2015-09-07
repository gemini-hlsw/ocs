// Copyright 1997-2011
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id:$
//
package edu.gemini.spModel.smartgcal.repository;

import edu.gemini.spModel.gemini.calunit.smartgcal.*;
import edu.gemini.spModel.smartgcal.UpdatableCalibrationRepository;

import java.net.ConnectException;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class CalibrationUpdater {

    private static final Logger LOG = Logger.getLogger(CalibrationUpdater.class.getName());

    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss z");
    public static final CalibrationUpdater instance = new CalibrationUpdater();

    private List<ActionListener> listeners = new CopyOnWriteArrayList<ActionListener>();

    private boolean isRunning;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture scheduledUpdates;
    private UpdatableCalibrationRepository target;
    private CalibrationRepository source;
    private long interval;

    private CalibrationUpdater() {
        this.isRunning = false;
    }

    public static CalibrationUpdater getInstance() {
        return instance;
    }

    public synchronized void start(UpdatableCalibrationRepository target, CalibrationRepository source, long interval) {
        // start periodical updates; if lastUpdate was too long ago, an update is scheduled immediately
        if (!isRunning) {
            this.target = target;
            this.source = source;
            this.interval = interval;

            long lastUpdate = timeSinceLastUpdate();
            long nextUpdate = timeTilNextUpdate(lastUpdate, interval);
            LOG.log(Level.INFO, "starting smartgcal updater");
            LOG.log(Level.INFO, "  interval between updates:  " + interval/60000 + " minutes");
            LOG.log(Level.INFO, "  last update was:           " + lastUpdate/60000 + " minutes ago");
            LOG.log(Level.INFO, "  scheduling next update in: " + nextUpdate/60000 + " minutes");

            start(nextUpdate);

        } else {
            LOG.log(Level.WARNING, "updater can only be started once");
        }
    }

    public synchronized boolean stop() {
        if (isRunning) {
            isRunning = !scheduledUpdates.cancel(false);
        }
        return !isRunning;
    }

    public synchronized boolean updateNowInBackground() {
        if (!inProgress) {
            stop();
            start(0);
            return true;
        }
        return false;
    }

    public CalibrationUpdateEvent updateNow() {
        return update(target, source);
    }

    private void start(long nextUpdate) {
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.scheduledUpdates = scheduler.scheduleWithFixedDelay(new Update(target, source), nextUpdate, interval, TimeUnit.MILLISECONDS);
        this.isRunning = true;
    }

    public void addListener(ActionListener listener) {
        listeners.add(listener);
    }

    public void removeListener(ActionListener listener) {
        listeners.remove(listener);
    }

    public long nextUpdate() {
        long lastUpdate = timeSinceLastUpdate();
        long nextUpdate = timeTilNextUpdate(lastUpdate, interval);
        return nextUpdate;
    }


    //-- the actual update task
    private class Update implements Runnable {
        private final UpdatableCalibrationRepository target;
        private final CalibrationRepository source;

        protected Update(UpdatableCalibrationRepository target, CalibrationRepository source) {
            this.target = target;
            this.source = source;
        }

        @Override
        public void run() {
            try {
                update(target, source);
            } catch (Throwable t) {
                LOG.log(Level.WARNING, "update failed", t);
            }
        }
    }

    private Object lock = new Object();
    private boolean inProgress = false;
    public CalibrationUpdateEvent update(UpdatableCalibrationRepository target, CalibrationRepository source) {
        synchronized (lock) {
            inProgress = true;
            LOG.log(Level.INFO, "starting to update smart calibrations...");
            CalibrationUpdateEvent event = doUpdate(target, source);
            LOG.log(Level.INFO, "finished updating smart calibrations");

            if (event.getUpdatedFiles().size() > 0) {
                LOG.log(Level.INFO, "successfully updated the following smartgcal files: " + event.getUpdatedFiles());
            }
            if (event.getFailedFiles().size() > 0) {
                LOG.log(Level.INFO, "updating the following smartgcal files failed: " + event.getFailedFiles());
            }

            for (ActionListener listener : listeners) {
                try {
                    listener.actionPerformed(event);
                } catch (Exception e) {
                    // Don't let a problem in a listener keeping us from notifying the other ones.
                    LOG.log(Level.INFO, "update listener threw exception", e);
                }
            }


            inProgress = false;
            return event;
        }
    }

    public List<String> getVersionInfo() {
        try {
            List<String> versionInfos = new ArrayList<String>();
            for (String instrument : SmartGcalService.getInstrumentNames()) {
                for (Calibration.Type type : SmartGcalService.getAvailableTypes(instrument)) {
                    Version version = target.getVersion(type, instrument);
                    StringBuffer sb = new StringBuffer();
                    sb.append(instrument).append(" ").append(type).append("s:  ");
                    sb.append(sdf.format(version.getTimestamp()));
                    sb.append(" (rev=").append(version.getRevision()).append(")");
                    versionInfos.add(sb.toString());
                }
            }
            return versionInfos;
        } catch (Exception e) {
            throw new RuntimeException("Could not access smartgcal version information.", e);
        }
    }

    private CalibrationUpdateEvent doUpdate(UpdatableCalibrationRepository target, CalibrationRepository source) {
        try {
            target.writeUpdateTimestamp();
        } catch (Exception e) {
            LOG.log(Level.INFO, "could not write smartgcal timestamp: " + e.getMessage() == null ? "" : e.getMessage(), e);
        }
        // compare versions of files available with the versions that can be downloaded through the service
        // if a newer version is available use the service to get it and update the file in the file system
        List<String> updatedFiles = new ArrayList<String>();
        List<String> failedFiles = new ArrayList<String>();
        for (String instrument : SmartGcalService.getInstrumentNames()) {
            for (Calibration.Type type : SmartGcalService.getAvailableTypes(instrument)) {
                try {
                    Version currentVersion = target.getVersion(type, instrument);
                    Version newestVersion = source.getVersion(type, instrument);
                    if (newestVersion.compareTo(currentVersion) > 0) {
                        CalibrationFile file = source.getCalibrationFile(type, instrument);
                        target.updateCalibrationFile(type, instrument, newestVersion, file.getData().getBytes());
                        updatedFiles.add(instrument + " " + type + "s");
                    }
                } catch (Exception e) {
                    failedFiles.add(instrument + " " + type + "s");
                    if (e instanceof ConnectException) {
                        LOG.log(Level.INFO, "could not update smartgcal file: " + (e.getMessage() == null ? "" : e.getMessage()), e);
                    } else {
                        LOG.log(Level.INFO, "could not update smartgcal file: " + (e.getMessage() == null ? "" : e.getMessage()));
                    }
                }
            }
        }

        return new CalibrationUpdateEvent(instance, updatedFiles, failedFiles);
    }

    private long timeSinceLastUpdate() {
        return System.currentTimeMillis() - target.getLastUpdateTimestamp().getTime();
    }

    private long timeTilNextUpdate(long timeSinceLastUpdate, long interval) {
        long nextUpdate = interval - timeSinceLastUpdate;
        if (nextUpdate < 0) {
            nextUpdate = 0;
        }
        return nextUpdate;
    }

}
