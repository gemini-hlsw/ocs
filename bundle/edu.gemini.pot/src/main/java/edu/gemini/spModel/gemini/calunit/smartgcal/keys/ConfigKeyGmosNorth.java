package edu.gemini.spModel.gemini.calunit.smartgcal.keys;

import edu.gemini.shared.util.immutable.DefaultImList;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.spModel.gemini.gmos.GmosCommonType;
import edu.gemini.spModel.gemini.gmos.GmosNorthType;
import edu.gemini.spModel.gemini.gmos.InstGmosNorth;

/**
 * Representation of GMOSN instrument configurations for calibration lookups.
 * Instances of this class will be used as lookup keys in the corresponding calibration map.
 */
public class ConfigKeyGmosNorth extends ConfigKeyGmos {

    private final int hashCode;
    private final GmosNorthType.DisperserNorth disperser;
    private final GmosNorthType.FilterNorth filter;
    private final GmosNorthType.FPUnitNorth focalPlaneUnit;

    public ConfigKeyGmosNorth(
            GmosNorthType.DisperserNorth disperser,
            GmosNorthType.FilterNorth filter,
            GmosNorthType.FPUnitNorth focalPlaneUnit,
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
        this.hashCode = caluculateHashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConfigKeyGmosNorth that = (ConfigKeyGmosNorth) o;

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

    private int caluculateHashCode() {
        int hashCode = super.hashCode();
        hashCode = 31 * hashCode + disperser.hashCode();
        hashCode = 31 * hashCode + filter.hashCode();
        hashCode = 31 * hashCode + focalPlaneUnit.hashCode();
        return hashCode;
    }

    @Override
    public String getInstrumentName() {
        return InstGmosNorth.SP_TYPE.readableStr;
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
