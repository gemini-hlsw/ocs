package edu.gemini.spModel.gemini.calunit.smartgcal.keys;

import edu.gemini.shared.util.immutable.DefaultImList;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.spModel.gemini.calunit.smartgcal.ConfigurationKey;
import edu.gemini.spModel.gemini.flamingos2.Flamingos2;

/**
 * Representation of Flamingos2 instrument configurations for calibration lookups.
 * Instances of this class will be used as lookup keys in the corresponding calibration map.
 */
public class ConfigKeyFlamingos2 implements ConfigurationKey {

    public static enum Values implements ConfigurationKey.Values {
        DISPERSER("Disperser"),
        FILTER("Filter"),
        FPU("Focal Plane Unit");

        private final String name;

        private Values(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    private final int hashCode;
    private final Flamingos2.Disperser disperser;
    private final Flamingos2.Filter filter;
    private final Flamingos2.FPUnit fpUnit;

    public ConfigKeyFlamingos2(Flamingos2.Disperser disperser, Flamingos2.Filter filter, Flamingos2.FPUnit fpUnit) {
        if (disperser == null)  throw new IllegalArgumentException(Values.DISPERSER.toString()+" must not be null");
        if (filter == null)     throw new IllegalArgumentException(Values.FILTER.toString()+" must not be null");
        if (fpUnit == null)     throw new IllegalArgumentException(Values.FPU.toString()+" must not be null");
        this.disperser = disperser;
        this.filter = filter;
        this.fpUnit = fpUnit;
        this.hashCode = caluculateHashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConfigKeyFlamingos2 that = (ConfigKeyFlamingos2) o;

        if (disperser != that.disperser) return false;
        if (filter != that.filter) return false;
        if (fpUnit != that.fpUnit) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    private int caluculateHashCode() {
        int hashCode = 0;
        hashCode = 31 * hashCode + disperser.hashCode();
        hashCode = 31 * hashCode + filter.hashCode();
        hashCode = 31 * hashCode + fpUnit.hashCode();
        return hashCode;
    }

    @Override
    public String getInstrumentName() {
        return Flamingos2.SP_TYPE.readableStr;
    }

    @Override
    public ImList<String> export() {
        return DefaultImList.create(
                disperser.sequenceValue(),
                filter.sequenceValue(),
                fpUnit.sequenceValue()
        );
    }
}
