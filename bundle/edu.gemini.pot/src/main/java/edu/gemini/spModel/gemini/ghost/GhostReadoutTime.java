/**
 * $Id: GhostsReadoutTime.java 38186 2011-10-24 13:21:33Z swalker $
 */

package edu.gemini.spModel.gemini.ghost;

import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.gemini.ghost.GhostType.*;
import edu.gemini.spModel.gemini.ghost.GhostType.DetectorManufacturer;

import java.util.HashMap;
import java.util.Map;

/** Maps Ghost instrument parameters to readout times */
public class GhostReadoutTime {

    //Maps the GhostReadoutKey to the overhead. Allows faster searchs.
    private static final Map<GhostReadoutKey, Double> map = new HashMap<GhostReadoutKey, Double>(500);


    //        ReadModeConfigured      spatialB   spectralB   detectorManufacturer,          overhead
    //        ---------------------------------------------------------------------
        static {
            // The binning for both detectors are equals because PI (Principal investigator) only provided to me the
            // the values for red detector. It onlys the 1x1 blue detector values are good.
            //1x1
            addEntry( ReadMode.STANDARD, 1, 1, DetectorManufacturer.BLUE, 95.1);
            addEntry( ReadMode.FAST, 1, 1, DetectorManufacturer.BLUE, 50.0);
            addEntry( ReadMode.BRIGTHTARGETS, 1, 1, DetectorManufacturer.BLUE, 20.7);
            addEntry( ReadMode.STANDARD, 1, 1, DetectorManufacturer.RED, 96.5);
            addEntry( ReadMode.FAST, 1, 1, DetectorManufacturer.RED, 49.2);
            addEntry( ReadMode.BRIGTHTARGETS, 1, 1, DetectorManufacturer.RED, 20.8);

            //1x2
            addEntry( ReadMode.STANDARD, 1, 2, DetectorManufacturer.BLUE, 191.1);
            addEntry( ReadMode.FAST, 1, 2, DetectorManufacturer.BLUE, 96.5);
            addEntry( ReadMode.BRIGTHTARGETS, 1, 2, DetectorManufacturer.BLUE, 39.7);
            addEntry( ReadMode.STANDARD, 1, 2, DetectorManufacturer.RED, 191.1);
            addEntry( ReadMode.FAST, 1, 2, DetectorManufacturer.RED, 96.5);
            addEntry( ReadMode.BRIGTHTARGETS, 1, 2, DetectorManufacturer.RED, 39.7);

            //1x4
            addEntry( ReadMode.STANDARD, 1, 4, DetectorManufacturer.BLUE, 380.3);
            addEntry( ReadMode.FAST, 1, 4, DetectorManufacturer.BLUE, 191.1);
            addEntry( ReadMode.BRIGTHTARGETS, 1, 4, DetectorManufacturer.BLUE, 77.5);
            addEntry( ReadMode.STANDARD, 1, 4, DetectorManufacturer.RED, 380.3);
            addEntry( ReadMode.FAST, 1, 4, DetectorManufacturer.RED, 191.1);
            addEntry( ReadMode.BRIGTHTARGETS, 1, 4, DetectorManufacturer.RED, 77.5);

            //1x8
            addEntry( ReadMode.STANDARD, 1, 8, DetectorManufacturer.BLUE, 758.8);
            addEntry( ReadMode.FAST, 1, 8, DetectorManufacturer.BLUE, 380.3);
            addEntry( ReadMode.BRIGTHTARGETS, 1, 8, DetectorManufacturer.BLUE, 153.2);
            addEntry( ReadMode.STANDARD, 1, 8, DetectorManufacturer.RED, 758.8);
            addEntry( ReadMode.FAST, 1, 8, DetectorManufacturer.RED, 380.3);
            addEntry( ReadMode.BRIGTHTARGETS, 1, 8, DetectorManufacturer.RED, 153.2);

            // 2x2
            addEntry( ReadMode.STANDARD, 2, 2, DetectorManufacturer.BLUE, 98.3);
            addEntry( ReadMode.FAST, 2, 2, DetectorManufacturer.BLUE, 51.0);
            addEntry( ReadMode.BRIGTHTARGETS, 2, 2, DetectorManufacturer.BLUE, 22.6);
            addEntry( ReadMode.STANDARD, 2, 2, DetectorManufacturer.RED, 98.3);
            addEntry( ReadMode.FAST, 2, 2, DetectorManufacturer.RED, 51.0);
            addEntry( ReadMode.BRIGTHTARGETS, 2, 2, DetectorManufacturer.RED, 22.6);

            // 2x4
            addEntry( ReadMode.STANDARD, 2, 4, DetectorManufacturer.BLUE, 192.9);
            addEntry( ReadMode.FAST, 2, 4, DetectorManufacturer.BLUE, 98.3);
            addEntry( ReadMode.BRIGTHTARGETS, 2, 4, DetectorManufacturer.BLUE, 41.5);
            addEntry( ReadMode.STANDARD, 2, 4, DetectorManufacturer.RED, 192.9);
            addEntry( ReadMode.FAST, 2, 4, DetectorManufacturer.RED, 98.3);
            addEntry( ReadMode.BRIGTHTARGETS, 2, 4, DetectorManufacturer.RED, 41.5);

            //2x8
            addEntry( ReadMode.STANDARD, 2, 8, DetectorManufacturer.BLUE, 382.2);
            addEntry( ReadMode.FAST, 2, 8, DetectorManufacturer.BLUE, 192.9);
            addEntry( ReadMode.BRIGTHTARGETS, 2, 8, DetectorManufacturer.BLUE, 79.4);
            addEntry( ReadMode.STANDARD, 2, 8, DetectorManufacturer.RED, 382.2);
            addEntry( ReadMode.FAST, 2, 8, DetectorManufacturer.RED, 192.9);
            addEntry( ReadMode.BRIGTHTARGETS, 2, 8, DetectorManufacturer.RED, 79.4);

            //4x4
            addEntry( ReadMode.STANDARD, 4, 4, DetectorManufacturer.BLUE, 102.0);
            addEntry( ReadMode.FAST, 4, 4, DetectorManufacturer.BLUE, 54.7);
            addEntry( ReadMode.BRIGTHTARGETS, 4, 4, DetectorManufacturer.BLUE, 26.3);
            addEntry( ReadMode.STANDARD, 4, 4, DetectorManufacturer.RED, 102.0);
            addEntry( ReadMode.FAST, 4, 4, DetectorManufacturer.RED, 54.7);
            addEntry( ReadMode.BRIGTHTARGETS, 4, 4, DetectorManufacturer.RED, 26.3);
        }

    /**
     * This class represent a key composed of those GMOS parameters that uniquely defines a particular
     * overhead. For every set of possible configurations declared in the lookup table
     * there is one overhead value associated. The GmosReadoutKey groups those parameters that
     * are associated with one specific overhead (a Double value)
     */

    private static final class GhostReadoutKey {

        private final ReadMode _ampSpeed;
        private final int _yBing;
        private final int _xBing;
        private final GhostType.DetectorManufacturer _detectorManufacturer;

        private GhostReadoutKey(ReadMode ampSpeed,
                                int yBing, int xBing,
                                DetectorManufacturer detectorManufacturer) {
            _ampSpeed = ampSpeed;
            _yBing = yBing;
            _xBing = xBing;
            _detectorManufacturer = detectorManufacturer;
        }

        public String toString() {
            StringBuilder buf = new StringBuilder();
            buf.append("AmpReadMode:");
            buf.append(_ampSpeed);
            buf.append("\t");
            buf.append("_yBing:");
            buf.append(_yBing);
            buf.append("\t");
            buf.append("_xBing:");
            buf.append(_xBing);
            buf.append("\t");
            buf.append("DetectorManufacturer:");
            buf.append(_detectorManufacturer);
            buf.append("\t");
            return buf.toString();
        }

        public int hashCode() {
            int res =  _ampSpeed.hashCode();
            res = 37*res + _yBing;
            res = 37*res + _xBing;
            res = 37*res + _detectorManufacturer.hashCode();
            return res;
        }

        public boolean equals(Object other) {
            if (other == null) return false;
            if (other.getClass() != this.getClass()) return false;
            GhostReadoutKey that = (GhostReadoutKey)other;
            if (!this._ampSpeed.equals(that._ampSpeed)) return false;
            if (this._yBing != that._yBing) return false;
            if (this._xBing != that._xBing) return false;
            if (!this._detectorManufacturer.equals(that._detectorManufacturer)) return false;
            return true;
        }


    }

    // addEntry( AmpReadMode.SLOW, 1, 1, AmpGain.LOW, DetectorManufacturer.RED, 95.1);
    private static void addEntry(ReadMode ampCount, int _yBing, int _xBing,
                                 DetectorManufacturer detectorManufacturer,
                                 double readoutTime) {
        GhostReadoutKey _readoutKey = new GhostReadoutKey(ampCount, _yBing, _xBing, detectorManufacturer);
        map.put(_readoutKey, readoutTime );
    }


    /**
     * Return the amount of time it takes in seconds to readout an image, based on the
     * configuration in the sequence and any custom ROI settings.
     *
     * @param config the current configuration
     */

    public static double getReadoutOverhead(Config config) {
        final GhostType.ReadMode readMode                = (GhostType.ReadMode) config.getItemValue(GhostType.ReadMode.KEY);
        final GhostType.DetectorManufacturer detMan      = (GhostType.DetectorManufacturer) config.getItemValue(GhostType.DetectorManufacturer.KEY);
        final GhostType.Binning              xBin        = (GhostType.Binning) config.getItemValue(InstGhost.X_BIN_KEY);
        final GhostType.Binning              yBin        = (GhostType.Binning) config.getItemValue(InstGhost.Y_BIN_KEY);


        System.out.println("readmode displayValue" + readMode.displayValue());
        System.out.println("detMan value" + detMan.displayValue() + "  " + detMan.getManufacter());
        System.out.println("bining, x: " + xBin.getValue() + " y: " + yBin.getValue());
        final GhostReadoutKey key = new GhostReadoutKey(readMode, yBin.getValue(), xBin.getValue(), detMan);
        final Double d = map.get(key);

        double overhead = (d == null) ? 0 : d;
        //overhead = 1 + overhead * rows / detMan.getYsize();


        return overhead;
    }

}
