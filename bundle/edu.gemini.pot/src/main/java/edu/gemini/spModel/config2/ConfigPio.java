package edu.gemini.spModel.config2;

import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioParseException;

import java.util.List;

/**
 * A (maybe) temporary class for ParamSet I/O for configs.  Only works with
 * ParamSets whose values are Strings.
 */
public final class ConfigPio {
    public static final String CONFIG_PARAM_SET = "config";
    public static final String ITEM_PARAM_SET   = "item";
    public static final String KEY_PARAM        = "key";
    public static final String VALUE_PARAM      = "value";

    public static ParamSet toParamSet(PioFactory factory, Config config) {
        ParamSet paramSet = factory.createParamSet(CONFIG_PARAM_SET);

        ItemEntry[] entries = config.itemEntries();
        for (ItemEntry entry : entries) {
            String key = entry.getKey().toString();
            // Would break for items that are arbitrary objects.
            String value = entry.getItemValue().toString();

            ParamSet configParamSet = factory.createParamSet(ITEM_PARAM_SET);

            Pio.addParam(factory, configParamSet, KEY_PARAM, key);
            Pio.addParam(factory, configParamSet, VALUE_PARAM, value);

            paramSet.addParamSet(configParamSet);
        }

        return paramSet;
    }

    public static Config toConfig(ParamSet ps) throws PioParseException {
        Config c = new DefaultConfig();

        List<ParamSet> itemParamSets = ps.getParamSets(ITEM_PARAM_SET);
        for (ParamSet itemParamSet : itemParamSets) {
            String keyStr = Pio.getValue(itemParamSet, KEY_PARAM);
            if (keyStr == null) {
                throw new PioParseException("missing '" + KEY_PARAM + "'");
            }
            ItemKey key = new ItemKey(keyStr);

            String valueStr = Pio.getValue(itemParamSet, VALUE_PARAM);
            if (valueStr == null) {
                throw new PioParseException("missing '" + VALUE_PARAM + "'");
            }

            // here we assume the value actually is a string ...
            c.putItem(key, valueStr);
        }

        return c;
    }

}
