package edu.gemini.spModel.config.map;

import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.DefaultConfig;
import edu.gemini.spModel.config2.ItemEntry;

/**
 * Utility for mapping a Config's values.
 */
public class ConfigValMapUtil {
    public static Config mapValues(Config input, ConfigValMap map) {
        Config res = new DefaultConfig();
        for (ItemEntry entry : input.itemEntries()) {
            res.putItem(entry.getKey(), map.apply(entry.getItemValue()));
        }
        return res;
    }
}
