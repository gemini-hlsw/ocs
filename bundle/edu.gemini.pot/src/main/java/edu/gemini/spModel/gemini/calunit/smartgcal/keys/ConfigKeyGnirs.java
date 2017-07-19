package edu.gemini.spModel.gemini.calunit.smartgcal.keys;


import edu.gemini.shared.util.immutable.DefaultImList;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.spModel.gemini.calunit.smartgcal.ConfigurationKey;
import edu.gemini.spModel.gemini.calunit.smartgcal.CalibrationProvider;
import edu.gemini.spModel.gemini.gnirs.GNIRSParams;
import edu.gemini.spModel.gemini.gnirs.InstGNIRS;

/**
 * Representation of GNIRS instrument configurations for calibration lookups.
 * Instances of this class will be used as lookup keys in the corresponding calibration map.
 */
public class ConfigKeyGnirs implements ConfigurationKey {

    public static enum Values implements ConfigurationKey.Values {
        MODE("Mode"),
        PIXEL_SCALE("Pixel Scale"),
        DISPERSER("Disperser"),
        CROSS_DISPERSED("Cross Dispersed"),
        SLIT_WIDTH("Focal Plane Unit"),
        WELL_DEPTH("Well Depth");

        private final String name;

        private Values(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    private final CalibrationProvider.GNIRSMode mode;
    private final GNIRSParams.PixelScale pixelScale;
    private final GNIRSParams.Disperser disperser;
    private final GNIRSParams.CrossDispersed crossDispersed;
    private final GNIRSParams.SlitWidth slitWidth;
    private final GNIRSParams.WellDepth wellDepth;
    private final int hashCode;

    public ConfigKeyGnirs(
            CalibrationProvider.GNIRSMode mode,
            GNIRSParams.PixelScale pixelScale,
            GNIRSParams.Disperser disperser,
            GNIRSParams.CrossDispersed crossDispersed,
            GNIRSParams.SlitWidth slitWidth,
            GNIRSParams.WellDepth wellDepth) {
        if (mode == null)           throw new IllegalArgumentException(Values.MODE.toString()+" must not be null");
        if (pixelScale == null)     throw new IllegalArgumentException(Values.PIXEL_SCALE.toString()+" must not be null");
        if (disperser == null)      throw new IllegalArgumentException(Values.DISPERSER.toString()+" must not be null");
        if (crossDispersed == null) throw new IllegalArgumentException(Values.CROSS_DISPERSED.toString()+" must not be null");
        if (slitWidth == null)      throw new IllegalArgumentException(Values.SLIT_WIDTH.toString()+" must not be null");
        if (wellDepth == null)      throw new IllegalArgumentException(Values.WELL_DEPTH.toString()+" must not be null");
        this.mode = mode;
        this.pixelScale = pixelScale;
        this.disperser = disperser;
        this.crossDispersed = crossDispersed;
        this.slitWidth = slitWidth;
        this.wellDepth = wellDepth;

        this.hashCode = calculateHashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConfigKeyGnirs that = (ConfigKeyGnirs) o;

        if (mode != that.mode) return false;
        if (crossDispersed != that.crossDispersed) return false;
        if (disperser != that.disperser) return false;
        if (pixelScale != that.pixelScale) return false;
        if (slitWidth != that.slitWidth) return false;
        if (wellDepth != that.wellDepth) return false;

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
        hc = 31 * hc + pixelScale.hashCode();
        hc = 31 * hc + disperser.hashCode();
        hc = 31 * hc + crossDispersed.hashCode();
        hc = 31 * hc + slitWidth.hashCode();
        hc = 31 * hc + wellDepth.hashCode();
        return hc;
    }

    @Override
    public String getInstrumentName() {
        return InstGNIRS.SP_TYPE.readableStr;
    }


    @Override
    public ImList<String> export() {
        return DefaultImList.create(
                mode.name(),
                pixelScale.sequenceValue(),
                disperser.sequenceValue(),
                crossDispersed.sequenceValue(),
                slitWidth.sequenceValue(),
                wellDepth.sequenceValue());
    }

}
