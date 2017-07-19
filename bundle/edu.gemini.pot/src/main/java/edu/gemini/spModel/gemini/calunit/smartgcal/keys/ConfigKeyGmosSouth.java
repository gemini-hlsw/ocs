package edu.gemini.spModel.gemini.calunit.smartgcal.keys;

import edu.gemini.shared.util.immutable.DefaultImList;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.spModel.gemini.gmos.GmosCommonType;
import edu.gemini.spModel.gemini.gmos.GmosSouthType;
import edu.gemini.spModel.gemini.gmos.InstGmosSouth;

/**
 * Representation of GMOSS instrument configurations for calibration lookups.
 * Instances of this class will be used as lookup keys in the corresponding calibration map.
 */
public class ConfigKeyGmosSouth extends ConfigKeyGmos {

    private final int hashCode;
    private final GmosSouthType.DisperserSouth disperser;
    private final GmosSouthType.FilterSouth filter;
    private final GmosSouthType.FPUnitSouth focalPlaneUnit;

    public ConfigKeyGmosSouth(
            GmosSouthType.DisperserSouth disperser,
            GmosSouthType.FilterSouth filter,
            GmosSouthType.FPUnitSouth focalPlaneUnit,
            GmosCommonType.Binning xBin,
            GmosCommonType.Binning yBin,
            GmosCommonType.Order order,
            GmosCommonType.AmpGain gain) {
        super(xBin, yBin, order, gain);
        if (disperser == null)        throw new IllegalArgumentException(Values.DISPERSER.toString()+" must not be null");
        if (filter == null)           throw new IllegalArgumentException(Values.FILTER.toString()+" must not be null");
        if (focalPlaneUnit == null)   throw new IllegalArgumentException(Values.FPU.toString()+" must not be null");
        this.disperser = disperser;
        this.filter = filter;
        this.focalPlaneUnit = focalPlaneUnit;
        this.hashCode = calculateHashcode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConfigKeyGmosSouth that = (ConfigKeyGmosSouth) o;

        if (!super.equals(o)) return false;
        if (disperser != that.disperser) return false;
        if (filter != that.filter) return false;
        if (focalPlaneUnit != that.focalPlaneUnit) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    private int calculateHashcode() {
        // this is an immutable object therefore we calculate the hash code only once
        int hashCode = super.hashCode();
        hashCode = 31 * hashCode + disperser.hashCode();
        hashCode = 31 * hashCode + filter.hashCode();
        hashCode = 31 * hashCode + focalPlaneUnit.hashCode();
        return hashCode;
    }

    @Override
    public String getInstrumentName() {
        return InstGmosSouth.SP_TYPE.readableStr;
    }


    @Override
    public ImList<String> export() {
        return DefaultImList.create(
                disperser.sequenceValue(),
                filter.sequenceValue(),
                focalPlaneUnit.sequenceValue(),
                xBin.sequenceValue(),
                yBin.sequenceValue(),
                order.sequenceValue(),
                gain.sequenceValue());
    }
}
