package edu.gemini.spModel.gemini.calunit.smartgcal.keys;


import edu.gemini.shared.util.immutable.DefaultImList;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.spModel.gemini.calunit.smartgcal.ConfigurationKey;
import edu.gemini.spModel.gemini.nifs.InstNIFS;
import edu.gemini.spModel.gemini.nifs.NIFSParams;

/**
 * Representation of NIFS instrument configurations for calibration lookups.
 * Instances of this class will be used as lookup keys in the corresponding calibration map.
 */
public class ConfigKeyNifs implements ConfigurationKey {

    public static enum Values implements ConfigurationKey.Values {
        DISPERSER("Disperser"),
        FILTER("Filter"),
        FOCAL_PLANE_MASK("Focal Plane Mask");

        private final String name;

        private Values(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    private final NIFSParams.Disperser disperser;
    private final NIFSParams.Filter filter;
    private final NIFSParams.Mask mask;
    private final int hashCode;

    public ConfigKeyNifs(
            NIFSParams.Disperser disperser,
            NIFSParams.Filter filter,
            NIFSParams.Mask mask) {
        if (disperser == null)  throw new IllegalArgumentException(Values.DISPERSER.toString()+" must not be null");
        if (filter == null)     throw new IllegalArgumentException(Values.FILTER.toString()+" must not be null");
        if (mask == null)       throw new IllegalArgumentException(Values.FOCAL_PLANE_MASK.toString()+" must not be null");
        this.disperser = disperser;
        this.filter = filter;
        this.mask = mask;

        this.hashCode = calculateHashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConfigKeyNifs that = (ConfigKeyNifs) o;

        if (disperser != that.disperser) return false;
        if (filter != that.filter) return false;
        if (mask != that.mask) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    private int calculateHashCode() {
        // this is an immutable object therefore we calculate the hash code only once
        int hc = 0;
        hc = disperser.hashCode();
        hc = 31 * hc + filter.hashCode();
        hc = 31 * hc + mask.hashCode();
        return hc;
    }

    @Override
    public String getInstrumentName() {
        return InstNIFS.SP_TYPE.readableStr;
    }


    @Override
    public ImList<String> export() {
        return DefaultImList.create(
                disperser.sequenceValue(),
                filter.sequenceValue(),
                mask.sequenceValue());
    }
}
