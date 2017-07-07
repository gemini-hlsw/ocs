package edu.gemini.spModel.gemini.calunit.smartgcal.keys;


import edu.gemini.shared.util.immutable.DefaultImList;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.spModel.gemini.calunit.smartgcal.ConfigurationKey;
import edu.gemini.spModel.gemini.gpi.Gpi;

/**
 * Representation of GPI instrument configurations for calibration lookups.
 * Instances of this class will be used as lookup keys in the corresponding calibration map.
 */
public class ConfigKeyGpi implements ConfigurationKey {

    public static enum Values implements ConfigurationKey.Values {
        MODE("Mode"),
        DISPERSER("Disperser");

        private final String name;
        private Values(String name) {
            this.name = name;
        }
        public String toString() {
            return name;
        }
    }

    private final Gpi.ObservingMode mode;
    private final Gpi.Disperser disperser;
    private final int hashCode;

    public ConfigKeyGpi(Gpi.ObservingMode mode, Gpi.Disperser disperser) {
        if (mode == null)           throw new IllegalArgumentException(Values.MODE.toString()+" must not be null");
        if (disperser == null)      throw new IllegalArgumentException(Values.DISPERSER.toString()+" must not be null");
        this.mode = mode;
        this.disperser = disperser;
        this.hashCode = calculateHashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConfigKeyGpi that = (ConfigKeyGpi) o;

        if (mode != that.mode) return false;
        if (disperser != that.disperser) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    private int calculateHashCode() {
        // this is an immutable object therefore we calculate the hash code only once
        int hc = 0;
        hc = mode.hashCode();
        hc = 31 * hc + disperser.hashCode();
        return hc;
    }

    @Override
    public String getInstrumentName() {
        return Gpi.SP_TYPE.readableStr;
    }


    @Override
    public ImList<String> export() {
        return DefaultImList.create(
                mode.sequenceValue(),
                disperser.sequenceValue());
    }
}
