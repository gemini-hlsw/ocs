package edu.gemini.spModel.gemini.calunit.smartgcal.keys;

import edu.gemini.spModel.gemini.calunit.smartgcal.ConfigurationKey;
import edu.gemini.spModel.gemini.gmos.GmosCommonType;

/**
 * Base class for representation of GMOS instrument configurations for calibration lookups.
 * Instances of this class will be used as lookup keys in the corresponding calibration map.
 */
public abstract class ConfigKeyGmos implements ConfigurationKey {

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

    protected final GmosCommonType.Binning xBin;
    protected final GmosCommonType.Binning yBin;
    protected final GmosCommonType.Order order;
    protected final GmosCommonType.AmpGain gain;

    public ConfigKeyGmos(
            GmosCommonType.Binning xBin,
            GmosCommonType.Binning yBin,
            GmosCommonType.Order order,
            GmosCommonType.AmpGain gain) {
        if (xBin == null)   throw new IllegalArgumentException(Values.XBIN.toString()+" must not be null");
        if (yBin == null)   throw new IllegalArgumentException(Values.YBIN.toString()+" must not be null");
        if (order == null)  throw new IllegalArgumentException(Values.ORDER.toString()+" must not be null");
        if (gain == null)   throw new IllegalArgumentException(Values.GAIN.toString()+" must not be null");
        this.xBin = xBin;
        this.yBin = yBin;
        this.order = order;
        this.gain = gain;
    }

    @Override
    public boolean equals(Object o) {
        // Note: This equals() will be used in comparisons of GMOSN- and GMOSSCalibrationKeys.
        if (this == o) return true;
        if (!(o instanceof ConfigKeyGmos)) return false;

        ConfigKeyGmos that = (ConfigKeyGmos) o;

        if (gain != that.gain) return false;
        if (order != that.order) return false;
        if (xBin != that.xBin) return false;
        if (yBin != that.yBin) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int hashCode = xBin.hashCode();
        hashCode = 31 * hashCode + yBin.hashCode();
        hashCode = 31 * hashCode + order.hashCode();
        hashCode = 31 * hashCode + gain.hashCode();
        return hashCode;
    }
}
