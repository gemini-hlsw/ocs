package edu.gemini.spModel.gemini.calunit.calibration;

import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.ConfigSequence;
import edu.gemini.spModel.config2.DefaultConfig;

import edu.gemini.spModel.gemini.calunit.calibration.CalDictionary.Item;
import static edu.gemini.spModel.gemini.calunit.calibration.CalDictionary.CAL_SYS;
import static edu.gemini.spModel.gemini.calunit.calibration.CalDictionary.FUNDAMENTAL_ITEMS;

import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;

import java.util.*;

/**
 * CalUnitConfig facilitates working with Config objects for GCal.
 */
public final class CalConfigPio {
    public static final String  SEQUENCE_NAME   = "calibrationSequence";

    private CalConfigPio() {}

    public static ParamSet toParamset(ConfigSequence cs, PioFactory factory) {
        ParamSet paramSet = factory.createParamSet(SEQUENCE_NAME);

        for (Iterator<Config> it = cs.compactIterator(); it.hasNext(); ) {
            Config c = it.next();
            paramSet.addParamSet(toParamSet(c, factory));
        }

        return paramSet;
    }

    public static ParamSet toParamSet(Config config, PioFactory factory) {
        ParamSet paramSet = factory.createParamSet(CAL_SYS);

        for (Item item : FUNDAMENTAL_ITEMS) {
            Object val = config.getItemValue(item.key);
            if (val != null) {
                Pio.addParam(factory, paramSet, item.propName, item.show.apply(val));
            }
        }

        return paramSet;
    }

    public static ConfigSequence toConfigSequence(ParamSet paramSet) {
        ConfigSequence cs = new ConfigSequence();
        for (Object obj : paramSet.getChildren()) {
            cs.addStep(toConfig((ParamSet) obj));
        }
        return cs;
    }

    public static Config toConfig(ParamSet paramSet) {
        Config config = new DefaultConfig();

        for (Item item : FUNDAMENTAL_ITEMS) {
            String str = Pio.getValue(paramSet, item.propName);
            if (str != null) {
                config.putItem(item.key, item.read.apply(str));
            }
        }

        return config;
    }

}
