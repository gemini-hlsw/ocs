/**
 * $Id: GhostsReadoutTime.java 38186 2011-10-24 13:21:33Z swalker $
 */

package edu.gemini.spModel.gemini.ghost;

import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.gemini.ghost.GhostType;
import edu.gemini.spModel.gemini.ghost.GhostType.AmpGain;
import edu.gemini.spModel.gemini.ghost.GhostType.DetectorManufacturer;

import edu.gemini.spModel.gemini.ghost.InstGhost;

import java.util.HashMap;
import java.util.Map;

/** Maps Ghost instrument parameters to readout times */
public class GhostReadoutTime {

    //Maps the GhostReadoutKey to the overhead. Allows faster searchs.
    private static final Map<GhostReadoutKey, Double> map = new HashMap<GhostReadoutKey, Double>(500);

    //        ampCount          ampSpeed         ROI                          Xbin   Ybin  ampGain      detectorManufacturer, overhead
    //        ---------------------------------------------------------------------
        static {
            /*
            addEntry( AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 1, 1, AmpGain.LOW, DetectorManufacturer.E2V, 123.874);
           */
        }

    /**
     * This class represent a key composed of those GMOS parameters that uniquely defines a particular
     * overhead. For every set of possible configurations declared in the lookup table
     * there is one overhead value associated. The GmosReadoutKey groups those parameters that
     * are associated with one specific overhead (a Double value)
     */
    private static final class GhostReadoutKey {
        /*private final GhostType.AmpCount _ampCount;
        private final GhostType.AmpReadMode _ampSpeed;
        private final GhostType.BuiltinROI _builtinROI;
        private final int _xBin;
        private final int _yBin;
        private final GhostType.AmpGain _ampGain;
        private final GhostType.DetectorManufacturer _detectorManufacturer;

        private GhostReadoutKey(AmpCount ampCount, AmpReadMode ampSpeed,
                                BuiltinROI builtinROI, int xBin, int yBin,
                                AmpGain ampGain, DetectorManufacturer detectorManufacturer) {
            _ampCount = ampCount;
            _ampSpeed = ampSpeed;
            _builtinROI = builtinROI;
            _xBin = xBin;
            _yBin = yBin;
            _ampGain = ampGain;
            _detectorManufacturer = detectorManufacturer;
        }

        public String toString() {
            StringBuilder buf = new StringBuilder();
            buf.append("AmpCount:");
            buf.append(_ampCount);
            buf.append("\t");
            buf.append("AmpReadMode:");
            buf.append(_ampSpeed);
            buf.append("\t");
            buf.append("AmpGain:");
            buf.append(_ampGain);
            buf.append("\t");
            buf.append("builtinROI:");
            buf.append(_builtinROI);
            buf.append("\t");
            buf.append("xBin:");
            buf.append(_xBin);
            buf.append("\t");
            buf.append("yBin:");
            buf.append(_yBin);
            buf.append("\t");
            buf.append("DetectorManufacturer:");
            buf.append(_detectorManufacturer);
            buf.append("\t");
            return buf.toString();
        }

        public int hashCode() {
            int res = _ampCount.hashCode();
            res = 37*res + _ampSpeed.hashCode();
            res = 37*res + _ampGain.hashCode();
            res = 37*res + _builtinROI.hashCode();
            res = 37*res + _xBin;
            res = 37*res + _yBin;
            res = 37*res + _detectorManufacturer.hashCode();
            return res;
        }

        public boolean equals(Object other) {
            if (other == null) return false;
            if (other.getClass() != this.getClass()) return false;
            GhostReadoutKey that = (GhostReadoutKey)other;
            if (!this._ampCount.equals(that._ampCount)) return false;
            if (!this._ampSpeed.equals(that._ampSpeed)) return false;
            if (!this._builtinROI.equals(that._builtinROI)) return false;
            if (this._xBin != that._xBin) return false;
            if (this._yBin != that._yBin) return false;
            if (!this._detectorManufacturer.equals(that._detectorManufacturer)) return false;
            return this._ampGain.equals(that._ampGain);
        }

         */
    }
/*

    private static void addEntry(AmpCount ampCount, AmpReadMode ampSpeed,
                            BuiltinROI builtinROI, int xBin, int yBin,
                            AmpGain ampGain, DetectorManufacturer detectorManufacturer,
                            double readoutTime) {
        GhostReadoutKey _readoutKey = new GhostReadoutKey(ampCount, ampSpeed, builtinROI, xBin, yBin, ampGain, detectorManufacturer);
        map.put(_readoutKey, readoutTime );
    }
    */


    /**
     * Return the amount of time it takes in seconds to readout an image, based on the
     * configuration in the sequence and any custom ROI settings.
     *
     * @param config the current configuration
     * @param customRoiList non-empty if custom ROIS are defined for the instrument
     */
    /*
    public static double getReadoutOverhead(Config config, GhostType.CustomROIList customRoiList) {
        final GhostType.AmpCount             ampCount    = (GhostType.AmpCount) config.getItemValue(GhostType.AmpCount.KEY);
        final GhostType.AmpGain              ampGain     = (GhostType.AmpGain) config.getItemValue(GhostType.AmpGain.KEY);
        final GhostType.AmpReadMode          ampReadMode = (GhostType.AmpReadMode) config.getItemValue(GhostType.AmpReadMode.KEY);
        final GhostType.BuiltinROI           builtinROI  = (GhostType.BuiltinROI) config.getItemValue(GhostType.BuiltinROI.KEY);
        final GhostType.DetectorManufacturer detMan      = (GhostType.DetectorManufacturer) config.getItemValue(GhostType.DetectorManufacturer.KEY);
        final GhostType.Binning              xBin        = (GhostType.Binning) config.getItemValue(InstGhost.X_BIN_KEY);
        final GhostType.Binning              yBin        = (GhostType.Binning) config.getItemValue(InstGhost.Y_BIN_KEY);

        final GhostType.BuiltinROI roiKey = (builtinROI == GhostType.BuiltinROI.CUSTOM) ? GhostType.BuiltinROI.FULL_FRAME : builtinROI;

        final GhostReadoutKey key = new GhostReadoutKey(ampCount, ampReadMode, roiKey, xBin.getValue(), yBin.getValue(), ampGain, detMan);
        final Double d = map.get(key);

        double overhead = (d == null) ? 0 : d;
        if (builtinROI == GhostType.BuiltinROI.CUSTOM) {
            // REL-1385
            final int rows = customRoiList.totalUnbinnedRows();
            if (rows > 0) {
                overhead = 1 + overhead * rows / detMan.getYsize();
            }
        }
        return overhead;
    }
    */
}
