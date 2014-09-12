package edu.gemini.spModel.gemini.calunit.calibration;

import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.data.config.DefaultParameter;
import edu.gemini.spModel.data.config.DefaultSysConfig;
import edu.gemini.spModel.data.config.IConfig;
import edu.gemini.spModel.data.config.ISysConfig;

import edu.gemini.spModel.gemini.calunit.calibration.CalDictionary.Item;
import edu.gemini.spModel.obscomp.InstConstants;

import static edu.gemini.spModel.gemini.calunit.calibration.CalDictionary.ITEMS;
import static edu.gemini.spModel.gemini.calunit.calibration.CalDictionary.SYSTEM_KEYS;

import java.util.*;


/**
 * Utility for IConfigBuilder implementations.  Supports updating an IConfig
 * appropriately based upon a Config object.
 */
public final class CalConfigBuilderUtil {

    private CalConfigBuilderUtil() {}

    public static void updateIConfig(Config config, IConfig cur, IConfig prev) {
        // Fill maps from system key to ISysConfig for each system that we
        // use.  Create new ISysConfig objects if they don't already exist in
        // the corresponding IConfig.
        Map<ItemKey, ISysConfig> curSysConfigMap  = new HashMap<ItemKey, ISysConfig>();
        Map<ItemKey, ISysConfig> prevSysConfigMap = new HashMap<ItemKey, ISysConfig>();
        for (ItemKey sysKey : SYSTEM_KEYS) {
            curSysConfigMap.put(sysKey, getSysConfig(cur, sysKey.getName()));
            prevSysConfigMap.put(sysKey, getSysConfig(prev, sysKey.getName()));
        }

        // Apply the values to the cur sys config as needed.
        for (Item item : ITEMS) {
            ISysConfig curSysConfig  = curSysConfigMap.get(item.key.getParent());
            ISysConfig prevSysConfig = prevSysConfigMap.get(item.key.getParent());
            updateSysConfig(config, item, curSysConfig, prevSysConfig);
        }

        // Append any new sys config objects to the current IConfig.
        for (ItemKey sysKey : SYSTEM_KEYS) {
            appendSysConfig(cur, curSysConfigMap.get(sysKey));
        }
    }

    /**
     * Removes any already present calibration values from the given IConfig.
     */
    public static void clear(IConfig cur) {
        for (Item item : ITEMS) {
            String sysName = item.key.getParent().getName();
            String parName = item.propName;
            cur.removeParameter(sysName, parName);
        }
    }

    private static ISysConfig getSysConfig(IConfig iconfig, String name) {
        ISysConfig sysConfig = iconfig.getSysConfig(name);
        return (sysConfig == null) ? new DefaultSysConfig(name) : sysConfig;
    }

    private static void updateSysConfig(Config config, Item item, ISysConfig cur, ISysConfig prev) {
        // If not found in the config, there is nothing to add.
        Object curVal = config.getItemValue(item.key);
        if (curVal == null) return;
        updateSysConfig(item.propName, curVal, cur, prev, item.aspect == CalDictionary.DataAspect.meta);
    }

    private static void updateSysConfig(String propName, Object curVal, ISysConfig cur, ISysConfig prev, boolean metadata) {
        // If the previous value is the same, don't repeat it in the current config.
        if (!metadata) {
            Object prevVal = prev.getParameterValue(propName);
            if (curVal.equals(prevVal)) return;
        }

        // Update the current config with the new value.
        cur.putParameter(DefaultParameter.getInstance(propName, curVal));
    }

    private static void appendSysConfig(IConfig iconfig, ISysConfig sysConfig) {
        if ((sysConfig.getParameterCount() > 0) && (iconfig.getSysConfig(sysConfig.getSystemName()) == null)) {
            iconfig.appendSysConfig(sysConfig);
        }
    }

    public static boolean isCalStep(Config c) {
        Object obsType = c.getItemValue(CalDictionary.OBS_TYPE_ITEM.key);
        return InstConstants.FLAT_OBSERVE_TYPE.equals(obsType) ||
               InstConstants.ARC_OBSERVE_TYPE.equals(obsType);
    }
}
