package edu.gemini.spModel.gemini.calunit.smartgcal.maps;


import edu.gemini.shared.util.immutable.DefaultImList;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.spModel.gemini.calunit.smartgcal.Calibration;
import edu.gemini.spModel.gemini.calunit.smartgcal.ConfigurationKey;
import edu.gemini.spModel.gemini.calunit.smartgcal.Version;
import edu.gemini.spModel.gemini.calunit.smartgcal.keys.WavelengthRange;
import edu.gemini.spModel.gemini.calunit.smartgcal.keys.WavelengthRangeSet;

import java.util.*;
import java.util.stream.Stream;

/**
 * A specialised calibration map that allows to store different lists of calibrations for disjoint wavelength
 * ranges for every key (combination of instrument configuration attributes).
 *
 */
public abstract class CentralWavelengthMap extends BaseCalibrationMap {

    public static final String WAVELENGTH_RANGE_NAME = "Central Wavelength";

    protected final Map<ConfigurationKey, WavelengthRangeSet> rangesMap;

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

    private static String exportWavelength(double wl) {
        return Long.toString(Math.round(wl * 1000));
    }

    /**
     * Export the calibration map to a list of String suitable for writing to a
     * configuration file.
     */
    @Override
    public Stream<ImList<String>> export() {
        return rangesMap.entrySet().stream().flatMap(me0 -> {
            final ImList<String>   calKey = me0.getKey().export();
            final WavelengthRangeSet rset = me0.getValue();

            return rset.getRangeMap().entrySet().stream().flatMap(me1 -> {
                final WavelengthRange          wr = me1.getKey();
                final ImList<Calibration> calList = me1.getValue();
                final ImList<String>       prefix =
                        calKey.append(exportWavelength(wr.getMin()))
                              .append(exportWavelength(wr.getMax()));

                return calList.stream().map(cal -> prefix.append(cal.export()));
            });
        });
    }
}
