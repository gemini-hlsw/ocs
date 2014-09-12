package edu.gemini.spModel.gemini.calunit.smartgcal.maps;


import edu.gemini.spModel.gemini.calunit.smartgcal.Calibration;
import edu.gemini.spModel.gemini.calunit.smartgcal.ConfigurationKey;
import edu.gemini.spModel.gemini.calunit.smartgcal.Version;
import edu.gemini.spModel.gemini.calunit.smartgcal.keys.WavelengthRange;
import edu.gemini.spModel.gemini.calunit.smartgcal.keys.WavelengthRangeSet;

import java.util.*;

/**
 * A specialised calibration map that allows to store different lists of calibrations for disjoint wavelength
 * ranges for every key (combination of instrument configuration attributes).
 *
 */
public abstract class CentralWavelengthMap extends BaseCalibrationMap {

    public static final String WAVELENGTH_RANGE_NAME = "Central Wavelength";
    
    private Map<ConfigurationKey, WavelengthRangeSet> rangesMap;

    public CentralWavelengthMap(Version version) {
        this(version, 200);
    }

    public CentralWavelengthMap(Version version, int size) {
        super(version);
        rangesMap = new HashMap<ConfigurationKey, WavelengthRangeSet>(size);
    }

    /**
     * Adds a new calibration value for a key and a wavelength range.
     * The wavelength range is taken from the propoerties.
     * @param key
     * @param properties
     * @param calibration
     * @return
     */
    public Calibration put(ConfigurationKey key, Properties properties, Calibration calibration) {
        // get wavelength range, it must exist
        String wavelengthRange = properties.getProperty(WAVELENGTH_RANGE_NAME);
        if (wavelengthRange == null) {
            throw new IllegalArgumentException("value for '" + WAVELENGTH_RANGE_NAME + "' is missing");
        }

        // get or create a wavelength range set for the given key (instrument configuration)
        WavelengthRangeSet rangeSet = rangesMap.get(key);
        if (rangeSet == null) {
            rangeSet = new WavelengthRangeSet();
            rangesMap.put(key, rangeSet);
        }

        // get or create the range corresponding to the wavelength range string
        WavelengthRange range = WavelengthRange.parse(wavelengthRange);

        // add the calibration to the range in the range set
        rangeSet.add(range, calibration);

        return calibration;
    }

    // implement interface but this method must not be called on wavelength map
    public List<Calibration> get(ConfigurationKey key) {
        throw new RuntimeException();
    }

    /**
     * Gets a list of calibrations for a key and a wavelength from the map.
     * @param key
     * @param wavelength
     * @return
     */
    public List<Calibration> get(ConfigurationKey key, Double wavelength) {
        // get range set for this key
        WavelengthRangeSet rangeSet = rangesMap.get(key);
        if(rangeSet == null) {
            // return empty list if we don't have any calibrations for this key
            return new ArrayList<Calibration>();
        }
        // if we have calibrations for this key get the ones for the given wavelength from the set
        return rangeSet.findCalibrations(wavelength);
    }
}
