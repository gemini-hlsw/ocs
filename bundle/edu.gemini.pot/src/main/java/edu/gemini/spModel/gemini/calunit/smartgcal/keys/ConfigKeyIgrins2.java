package edu.gemini.spModel.gemini.calunit.smartgcal.keys;

import edu.gemini.shared.util.immutable.DefaultImList;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.spModel.gemini.calunit.smartgcal.ConfigurationKey;
import edu.gemini.spModel.gemini.igrins2.Igrins2;

public class ConfigKeyIgrins2 {

    public static final ConfigurationKey INSTANCE = new ConfigurationKey() {
        @Override public String getInstrumentName() {
            return Igrins2.SP_TYPE().readableStr;
        }

        @Override public ImList<String> export() {
            return DefaultImList.create();
        }
    };

}
