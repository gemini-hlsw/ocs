package edu.gemini.spModel.gemini.calunit.calibration;

import edu.gemini.spModel.data.config.IConfig;
import edu.gemini.spModel.gemini.calunit.CalUnitParams;
import edu.gemini.spModel.gemini.calunit.calibration.CalDictionary.Item;

import static edu.gemini.spModel.gemini.calunit.calibration.CalDictionary.*;
import static edu.gemini.spModel.gemini.calunit.calibration.CalDictionary.PropertyKind.*;

import java.util.Set;

/**
 * IndexedCalibrationStep backed by old sequence model IConfigs
 */
public class IConfigBackedIndexedCalibrationStep extends AbstractIndexedCalibrationStep {
    private IConfig current;
    private IConfig prev;

    public IConfigBackedIndexedCalibrationStep(IConfig current, IConfig prev) {
        this.current = current;
        this.prev    = prev;
    }

    private Object getItemValue(Item item) {
        return (item.kind == fundamental) ? getFundamentalItemValue(item) : getDerivedItemValue(item);
    }

    private Object getFundamentalItemValue(Item item) {
        String sysName  = item.key.getParent().getName();
        String propName = item.propName;

        // First try the current config.
        Object val = current.getParameterValue(sysName, propName);

        // Failing that, try the previous config value.
        if ((val == null) && (prev != null)) val = prev.getParameterValue(sysName, propName);

        return val;
    }

    private Object getDerivedItemValue(Item item) {
        return item.ext.apply(this);
    }

    @Override public Integer getIndex() {
        return (Integer) getItemValue(STEP_COUNT_ITEM);
    }

    @Override public String getObsClass() {
        return (String) getItemValue(OBS_CLASS_ITEM);
    }

    @Override public Boolean isBasecalNight() {
        return (Boolean) getItemValue(BASECAL_NIGHT_ITEM);
    }

    @Override public Boolean isBasecalDay() {
        return (Boolean) getItemValue(BASECAL_DAY_ITEM);
    }

    @Override public Set<CalUnitParams.Lamp> getLamps() {
        return (Set<CalUnitParams.Lamp>) getItemValue(LAMP_ITEM);
    }

    @Override public CalUnitParams.Shutter getShutter() {
        return (CalUnitParams.Shutter) getItemValue(SHUTTER_ITEM);
    }

    @Override public CalUnitParams.Filter getFilter() {
        return (CalUnitParams.Filter) getItemValue(FILTER_ITEM);
    }

    @Override public CalUnitParams.Diffuser getDiffuser() {
        return (CalUnitParams.Diffuser) getItemValue(DIFFUSER_ITEM);
    }

    @Override public Double getExposureTime() {
        return (Double) getItemValue(EXPOSURE_TIME_ITEM);
    }

    @Override public Integer getCoadds() {
        return (Integer) getItemValue(COADDS_ITEM);
    }
}
