package edu.gemini.spModel.smartgcal.provider;

import edu.gemini.spModel.gemini.calunit.smartgcal.*;
import edu.gemini.spModel.gemini.calunit.smartgcal.keys.CalibrationKeyImpl;
import edu.gemini.spModel.smartgcal.CalibrationMapFactory;
import edu.gemini.spModel.smartgcal.repository.CalibrationUpdater;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class will be used to create a singleton to provide access to the smart calibration information
 * provided by a repository (there are different repository implementation available).
 */
public class CalibrationProviderImpl implements CalibrationProvider, ActionListener {

    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss z");

    private CalibrationRepository repository;
    private Map<String, CalibrationMap> cache;

    public CalibrationProviderImpl(CalibrationRepository repository) {
        this.repository = repository;
        this.cache = new HashMap<String, CalibrationMap>(32);
        init();
    }

    @Override
    public List<Calibration> getCalibrations(CalibrationKey key) {
        final ConfigurationKey configKey = key.getConfig();
        final List<Calibration> cals;
        if (key instanceof CalibrationKeyImpl.WithWavelength) {
            final Double wavelength = ((CalibrationKeyImpl.WithWavelength) key).getWavelength();
            cals = getCalibrations(configKey.getInstrumentName(), configKey, wavelength);
        } else {
            cals = getCalibrations(configKey.getInstrumentName(), key.getConfig());
        }

        // Partition the calibrations into flats and arcs.
        final List<Calibration> flats = new ArrayList<>();
        final List<Calibration> arcs  = new ArrayList<>();
        cals.forEach( cal -> {
            if (cal.isFlat()) {
                flats.add(cal);
            } else {
                arcs.add(cal);
            }
        });

        // Return the flats before the arcs.
        final List<Calibration> result = new ArrayList<>(flats);
        result.addAll(arcs);
        return result;
    }

    @Override
    public Version getVersion(Calibration.Type type, String instrument) {
        return getMap(type, instrument).getVersion();
    }

    @Override
    public List<VersionInfo> getVersionInfo() {
        List<VersionInfo> versionInfos = new ArrayList<VersionInfo>();
        for (String instrument : SmartGcalService.getInstrumentNames()) {
            for (Calibration.Type type : SmartGcalService.getAvailableTypes(instrument)) {
                Version version = getVersion(type, instrument);
                VersionInfo versionInfo = new VersionInfo(instrument, type, version);
                versionInfos.add(versionInfo);
            }
        }
        return versionInfos;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        update();
    }

    public void update() {
        // after an update make sure that in-memory maps and updated files are in sync.
        // NOTE: if for some reason this is too much of a performance penalty it would be possible
        // to just re-read the files that are indeed newer than the ones in memory. Currently
        // it seems to work fine just like that.
        CalibrationProviderImpl newProvider = new CalibrationProviderImpl(repository);
        CalibrationProviderHolder.setProvider(newProvider);
        // UX-1426: remove update listener on this (old data) -> avoid memory leak!
        CalibrationUpdater.instance.removeListener(this);
        // add listener for updates on new data provider
        CalibrationUpdater.instance.addListener(newProvider);
    }

    public void init() {
        for (String instrument : SmartGcalService.getInstrumentNames()) {
            for (Calibration.Type type : SmartGcalService.getAvailableTypes(instrument)) {
                String mapKey = getKey(type, instrument);
                CalibrationMap map = readMap(type, instrument);
                this.cache.put(mapKey, map);
            }
        }
    }

    private List<Calibration> getCalibrations(String instrument, ConfigurationKey calibrationKey, double centralWavelength) {
        List<Calibration> calibrations = new ArrayList<Calibration>();
        for (Calibration.Type type : SmartGcalService.getAvailableTypes(instrument)) {
            CalibrationMap map = getMap(type, instrument);
            calibrations.addAll(map.get(calibrationKey, centralWavelength));
        }
        return calibrations;
    }

    private List<Calibration> getCalibrations(String instrument, ConfigurationKey calibrationKey) {
        List<Calibration> calibrations = new ArrayList<Calibration>();
        for (Calibration.Type type : SmartGcalService.getAvailableTypes(instrument)) {
            CalibrationMap map = getMap(type, instrument);
            calibrations.addAll(map.get(calibrationKey));
        }
        return calibrations;
    }

    private CalibrationMap getMap(Calibration.Type type, String instrument) {
        String mapKey = getKey(type, instrument);
        return cache.get(mapKey);
    }

    private CalibrationMap readMap(Calibration.Type type, String instrument) {
        try {
            CalibrationFile file = repository.getCalibrationFile(type, instrument);
            return CalibrationMapFactory.createFromData(instrument, file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getKey(Calibration.Type type, String instrument) {
        return instrument + "_" + type;
    }
}
