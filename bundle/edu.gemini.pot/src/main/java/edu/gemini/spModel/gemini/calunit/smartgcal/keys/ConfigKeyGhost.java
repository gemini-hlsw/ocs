package edu.gemini.spModel.gemini.calunit.smartgcal.keys;

import edu.gemini.shared.util.immutable.DefaultImList;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.spModel.gemini.calunit.smartgcal.ConfigurationKey;
import edu.gemini.spModel.gemini.ghost.GhostType;
import edu.gemini.spModel.gemini.gmos.GmosCommonType;
import edu.gemini.spModel.gemini.gmos.GmosSouthType;
import edu.gemini.spModel.gemini.gmos.InstGmosSouth;

/**
 * Base class for representation of GMOS instrument configurations for calibration lookups.
 * Instances of this class will be used as lookup keys in the corresponding calibration map.
 */
public class ConfigKeyGhost implements ConfigurationKey {

    @Override
    public String getInstrumentName() {
        return InstGmosSouth.SP_TYPE.readableStr;
    }

    @Override
    public ImList<String> export() {
        return DefaultImList.create(
                xBin.sequenceValue(),
                yBin.sequenceValue(),
                gain.sequenceValue());
    }

    public static enum Values implements ConfigurationKey.Values {
        DISPERSER("Disperser"),
        FILTER("Filter"),
        FPU("Focal Plane Unit"),
        XBIN("Xbin"),
        YBIN("Ybin"),
        ORDER("Order"),
        GAIN("Gain");

        private String name;

        private Values(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    protected final GhostType.Binning xBin;
    protected final GhostType.Binning yBin;
    protected final GhostType.AmpGain gain;
    private final int hashCode;

    public ConfigKeyGhost(
            GhostType.Binning xBin,
            GhostType.Binning yBin,
            GhostType.AmpGain gain) {
        if (xBin == null)   throw new IllegalArgumentException(Values.XBIN.toString()+" must not be null");
        if (yBin == null)   throw new IllegalArgumentException(Values.YBIN.toString()+" must not be null");

        if (gain == null)   throw new IllegalArgumentException(Values.GAIN.toString()+" must not be null");
        this.xBin = xBin;
        this.yBin = yBin;
        this.gain = gain;

        this.hashCode = calculateHashcode();
    }

    private int calculateHashcode() {
        // this is an immutable object therefore we calculate the hash code only once
        int hashCode = super.hashCode();
        return hashCode;
    }

    @Override
    public boolean equals(Object o) {
        // Note: This equals() will be used in comparisons of GMOSN- and GMOSSCalibrationKeys.
        if (this == o) return true;
        if (!(o instanceof ConfigKeyGhost)) return false;

        ConfigKeyGhost that = (ConfigKeyGhost) o;

        if (gain != that.gain) return false;
        if (xBin != that.xBin) return false;
        if (yBin != that.yBin) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int hashCode = xBin.hashCode();
        hashCode = 31 * hashCode + yBin.hashCode();
        hashCode = 31 * hashCode + gain.hashCode();
        return hashCode;
    }
}
