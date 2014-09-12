package edu.gemini.spModel.gemini.calunit.calibration;

import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.DefaultConfig;
import edu.gemini.spModel.gemini.calunit.calibration.CalDictionary.Item;

import java.util.Collection;

import static edu.gemini.spModel.gemini.calunit.calibration.CalDictionary.FUNDAMENTAL_ITEMS;
import static edu.gemini.spModel.gemini.calunit.calibration.CalDictionary.ITEMS;

/**
 * CalUnitConfig facilitates working with Config objects for GCal.
 */
public final class CalConfigFactory {
    private CalConfigFactory() {}

    private static Config toConfig(IndexedCalibrationStep that, Collection<Item> items) {
        Config config = new DefaultConfig();

        for (Item item : items) {
            Object val = item.ext.apply(that);
            if (val == null) continue;
            if ((val instanceof Collection) && ((Collection) val).size() == 0) continue;
            config.putItem(item.key, val);
        }

        return config;
    }

    public static Config complete(IndexedCalibrationStep that) {
        return toConfig(that, ITEMS);
    }

    public static Config complete(CalibrationStep that, int stepCount) {
        return complete(new DelegatingIndexedCalibrationStep(that, stepCount));
    }

    public static Config complete(Config input) {
        return complete(new ConfigBackedIndexedCalibrationStep(input));
    }

    public static Config minimal(IndexedCalibrationStep that) {
        return toConfig(that, FUNDAMENTAL_ITEMS);
    }

    public static Config minimal(CalibrationStep that, int stepCount) {
        return minimal(new DelegatingIndexedCalibrationStep(that, stepCount));
    }

    public static Config minimal(Config input) {
        return minimal(new ConfigBackedIndexedCalibrationStep(input));
    }

/*
    public static ConfigFactory extract(IConfig iconfig) {
        Config config = new DefaultConfig();

        ISysConfig sysConfig = iconfig.getSysConfig(CAL_SYS);
        if (sysConfig != null) {
            for (Item it : ITEMS) updateConfig(it, config, sysConfig);
        }
        return new ConfigFactory(config);
    }

    private static void updateConfig(Item item, Config dst, ISysConfig src) {
        Object val = src.getParameterValue(item.propName);
        if (val != null) dst.putItem(item.key, val);
    }
*/

}
