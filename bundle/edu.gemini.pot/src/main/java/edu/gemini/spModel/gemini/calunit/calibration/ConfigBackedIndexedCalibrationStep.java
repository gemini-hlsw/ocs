package edu.gemini.spModel.gemini.calunit.calibration;

import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.DefaultConfig;
import edu.gemini.spModel.gemini.calunit.CalUnitParams.Diffuser;
import edu.gemini.spModel.gemini.calunit.CalUnitParams.Filter;
import edu.gemini.spModel.gemini.calunit.CalUnitParams.Lamp;
import edu.gemini.spModel.gemini.calunit.CalUnitParams.Shutter;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import static edu.gemini.spModel.gemini.calunit.calibration.CalDictionary.*;


/**
 * An IndexedCalibrationStep backed by a Config object.
 */
public final class ConfigBackedIndexedCalibrationStep extends AbstractIndexedCalibrationStep {

    private final Config config;

    public ConfigBackedIndexedCalibrationStep(Config config) {
        this.config = new DefaultConfig(config);
    }

    /**
     * Gets (a copy of) the backing Config object.
     */
    public Config getConfig() {
       return new DefaultConfig(config);
    }

    @Override public Set<Lamp> getLamps() {
        Collection<Lamp> c = (Collection<Lamp>) config.getItemValue(LAMP_ITEM.key);
        if (c == null) return Collections.emptySet();
        return new TreeSet<Lamp>(c);
    }

    @Override public Shutter getShutter() {
        return (Shutter) config.getItemValue(SHUTTER_ITEM.key);
    }

    @Override public Filter getFilter() {
        return (Filter) config.getItemValue(FILTER_ITEM.key);
    }

    @Override public Diffuser getDiffuser() {
        return (Diffuser) config.getItemValue(DIFFUSER_ITEM.key);
    }

    @Override public Double getExposureTime() {
        return (Double) config.getItemValue(EXPOSURE_TIME_ITEM.key);
    }

    @Override public Integer getCoadds() {
        return (Integer) config.getItemValue(COADDS_ITEM.key);
    }

    @Override public Boolean isBasecalDay() {
        return (Boolean) config.getItemValue(BASECAL_DAY_ITEM.key);
    }

    @Override public Boolean isBasecalNight() {
        return (Boolean) config.getItemValue(BASECAL_NIGHT_ITEM.key);
    }

    @Override public String getObsClass() {
        return (String) config.getItemValue(OBS_CLASS_ITEM.key);
    }

    @Override public Integer getIndex() {
        return (Integer) config.getItemValue(STEP_COUNT_ITEM.key);
    }
}
