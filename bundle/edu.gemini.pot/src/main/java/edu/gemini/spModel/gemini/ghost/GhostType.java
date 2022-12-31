package edu.gemini.spModel.gemini.ghost;

import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.data.config.DefaultParameter;
import edu.gemini.spModel.data.config.IParameter;
import edu.gemini.spModel.gemini.gmos.GmosCommonType;
import edu.gemini.spModel.ictd.Ictd;
import edu.gemini.spModel.ictd.IctdTracking;
import edu.gemini.spModel.ictd.IctdType;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.type.*;

import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.*;

import static edu.gemini.spModel.seqcomp.SeqConfigNames.INSTRUMENT_KEY;

/**
 * This class provides data types for the GMOS components.
 */
public class GhostType {
    private GhostType() {
        // defeat construction
    }

    //public static final double E2V_PIXEL_SIZE = 0.06667; // microns/pixel

    public static final double SIZE_ONE_FIBER_SR_PIXELS = 2.7;  // Size of one fiber in pixels.
    public static final double SIZE_ONE_FIBER_HR_PIXELS = 1.62;  // Size of one fiber in pixels.

    public static final double E2V_PIXEL_SIZE = 0.4; // arcsec/pixel

    public enum DetectorManufacturer implements DisplayableSpType {
        BLUE("E2V_CCD231-84-1-G57", "Blue", E2V_PIXEL_SIZE, 4096, 4112, 0.0003263888888888889),
        RED ("E2V_CCD231-C6-1-G58", "Red", E2V_PIXEL_SIZE, 6144, 4096, 0.00022916666666666666);

        public static final GhostType.DetectorManufacturer DEFAULT = RED;
        public static final ItemKey KEY = new ItemKey(INSTRUMENT_KEY, "detectorManufacturer");

        private final String _displayValue;
        private final String _manufacter;
        private final double _pixelSize;
        private final int _xSize;
        private final int _ySize;

        private final double _darkCurrent;

        DetectorManufacturer(final String manufacter, final String displayValue,
                             final double pixelSize, final int xSize, final int ySize, final double darkCurrent) {
            this._manufacter = manufacter;
            this._displayValue = displayValue;
            this._pixelSize = pixelSize;
            this._xSize = xSize;
            this._ySize = ySize;
            this._darkCurrent = darkCurrent;
        }

        public String displayValue() {
            return _displayValue;
        }

        /**
         * arcsec/pixel
         */
        public double pixelSize() {
            return _pixelSize;
        }

        public int getXsize() {
            return _xSize;
        }

        public int getYsize() {
            return _ySize;
        }

        public double get_darkCurrent() { return _darkCurrent;}

        public String getManufacter() {
            return _manufacter;
        }

    }

    public enum Resolution implements DisplayableSpType, LoggableSpType, SequenceableSpType {
        STANDARD("SR"),
        HIGH("HR");

        public static final GhostType.Resolution DEFAULT = Resolution.STANDARD;

        private String _displayValue;


        Resolution(String displayValue) {
            _displayValue = displayValue;
        }
        public String displayValue() {
            return String.valueOf(_displayValue);
        }

        public String sequenceValue() {
            return displayValue();
        }
        public String logValue() {
            return displayValue();
        }
        public String get_displayValue() {
            return _displayValue;
        }

        public static GhostType.Resolution getResolution(String name) {
            return Resolution.getResolution(name, Resolution.DEFAULT);
        }

        /** Return a Binning by name giving a value to return upon error **/
        public static GhostType.Resolution getResolution(String name, GhostType.Resolution nvalue) {
            return SpTypeUtil.oldValueOf(GhostType.Resolution.class, name, nvalue);
        }

        public static GhostType.Resolution getResolutionByDisplayValue(String value) {
            for(GhostType.Resolution constant : GhostType.Resolution.class.getEnumConstants()) {
                if (constant.get_displayValue().equals(value))
                    return constant;
            }
            return DEFAULT;
        }

        /** Return a Binning value by index **/
        public static GhostType.Resolution getResolutionByIndex(int index) {
            return SpTypeUtil.valueOf(GhostType.Resolution.class, index, DEFAULT);
        }

    }

    public enum AmpGain implements LoggableSpType, SequenceableSpType {
        LOW("Low"),
        HIGH("HIGH"),
        ;

        public static final GhostType.AmpGain DEFAULT = GhostType.AmpGain.LOW;
        public static final ItemKey KEY = new ItemKey(INSTRUMENT_KEY, "gainChoice");

        private String _displayValue;

        AmpGain(String displayValue) {
            _displayValue = displayValue;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        public String logValue() {
            return _displayValue;
        }

        /** Return an AmpName by name **/
        public static GhostType.AmpGain getAmpGain(String name) {
            return GhostType.AmpGain.getAmpGain(name, GhostType.AmpGain.DEFAULT);
        }

        /** Return an AmpGain by name giving a value to return upon error **/
        public static GhostType.AmpGain getAmpGain(String name, GhostType.AmpGain nvalue) {
            return SpTypeUtil.oldValueOf(GhostType.AmpGain.class, name, nvalue);
        }
    }


    /**
     * CCD ReadoutSpead indicates speed of CCD readout.
     * Standard --> Slow read and low gain
     * FAST --> Slow read and low gain
     * BrightTargets --> Fast read and high gain.
     */

    public enum DetectorReadMode {
        SLOW,
        FAST;
    }
    public enum ReadMode implements DisplayableSpType, SequenceableSpType {
        STANDARD("Standard Science", DetectorReadMode.SLOW, AmpGain.LOW),
        FAST("Fast Read", DetectorReadMode.FAST, AmpGain.LOW),
        BRIGTHTARGETS("Bright Targets", DetectorReadMode.FAST, AmpGain.HIGH),
        ;

        private String _displayValue;

        private DetectorReadMode _readMode;

        public String get_displayValue() {
            return _displayValue;
        }

        public void set_displayValue(String _displayValue) {
            this._displayValue = _displayValue;
        }

        public DetectorReadMode get_readMode() {
            return _readMode;
        }

        public void set_readMode(DetectorReadMode _readMode) {
            this._readMode = _readMode;
        }

        public AmpGain get_ampGain() {
            return _ampGain;
        }

        public void set_ampGain(AmpGain _ampGain) {
            this._ampGain = _ampGain;
        }

        private AmpGain _ampGain;

        public static final ReadMode DEFAULT = STANDARD;
        public static final ItemKey KEY = new ItemKey(INSTRUMENT_KEY, "readMode");

        ReadMode(String displayValue, DetectorReadMode detReadMode, AmpGain amp) {
            _displayValue = displayValue;
            _readMode = detReadMode;
            _ampGain = amp;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }


        /** Return an AmpSpeed by name giving a value to return upon error **/
        public static ReadMode getReadMode(String name, ReadMode nvalue) {
            return SpTypeUtil.oldValueOf(ReadMode.class, name, nvalue);
        }
    }

    /**
     * CCD Bin factor.
     */
    public enum Binning implements DisplayableSpType, LoggableSpType, SequenceableSpType {
        ONE(1),
        TWO(2),
        FOUR(4),
        EIGHT(8),
        ;

        public static final Binning DEFAULT = Binning.ONE;

        private int _value;

        Binning(int value) {
            _value = value;
        }

        public String displayValue() {
            return String.valueOf(_value);
        }

        public String sequenceValue() {
            return displayValue();
        }

        public String logValue() {
            return displayValue();
        }

        /** Return the integer binning value **/
        public int getValue() {
            return _value;
        }

        /** Return a Binning by name **/
        public static Binning getBinning(String name) {
            return Binning.getBinning(name, Binning.DEFAULT);
        }

        /** Return a Binning by name giving a value to return upon error **/
        public static Binning getBinning(String name, Binning nvalue) {
            return SpTypeUtil.oldValueOf(Binning.class, name, nvalue);
        }

        public static Binning getBinningByValue(int value) {
            for(Binning constant : Binning.class.getEnumConstants()) {
                if (constant.getValue() == value)
                    return constant;
            }
            return DEFAULT;
        }

        /** Return a Binning value by index **/
        public static Binning getBinningByIndex(int index) {
            return SpTypeUtil.valueOf(Binning.class, index, DEFAULT);
        }
    }
}
