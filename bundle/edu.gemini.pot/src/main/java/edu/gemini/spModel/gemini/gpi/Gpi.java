package edu.gemini.spModel.gemini.gpi;

import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.skycalc.Angle;
import edu.gemini.spModel.config.ConfigPostProcessor;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.ConfigSequence;
import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.core.MagnitudeBand;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.data.YesNoType;
import edu.gemini.spModel.data.config.DefaultParameter;
import edu.gemini.spModel.data.config.DefaultSysConfig;
import edu.gemini.spModel.data.config.ISysConfig;
import edu.gemini.spModel.data.config.StringParameter;
import edu.gemini.spModel.data.property.PropertyProvider;
import edu.gemini.spModel.data.property.PropertySupport;
import edu.gemini.spModel.gemini.calunit.calibration.CalDictionary;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.guide.GuideProbeConsumer;
import edu.gemini.spModel.guide.GuideProbeUtil;
import edu.gemini.spModel.guide.StandardGuideOptions;
import edu.gemini.spModel.obs.plannedtime.CommonStepCalculator;
import edu.gemini.spModel.obs.plannedtime.ExposureCalculator;
import edu.gemini.spModel.obs.plannedtime.PlannedTime;
import edu.gemini.spModel.obs.plannedtime.PlannedTime.CategorizedTime;
import edu.gemini.spModel.obs.plannedtime.PlannedTime.CategorizedTimeGroup;
import edu.gemini.spModel.obs.plannedtime.PlannedTime.Category;
import edu.gemini.spModel.obscomp.InstConfigInfo;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.seqcomp.SeqConfigNames;
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe;
import edu.gemini.spModel.target.obsComp.TargetObsCompConstants;
import edu.gemini.spModel.target.offset.OffsetPosBase;
import edu.gemini.spModel.type.*;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Stream;

import static edu.gemini.spModel.seqcomp.SeqConfigNames.INSTRUMENT_KEY;

/**
 * GPI - Gemini Planet Imager.
 * See OT tasks starting with OT-45.
 */
public class Gpi extends SPInstObsComp implements PropertyProvider, GuideProbeConsumer, PlannedTime.StepCalculator, ConfigPostProcessor {
    // for serialization
    private static final long serialVersionUID = 4L;

    private static final String GUIDING_PATH = TargetObsCompConstants.CONFIG_NAME + ":" + TargetObsCompConstants.GUIDE_WITH_OIWFS_PROP;

    private static boolean equal(double d1, double d2) {
        return Math.abs(d1 - d2) < 0.00001;
    }

    /**
     * ADC: See OT-48
     */
    public enum Adc implements DisplayableSpType, SequenceableSpType, LoggableSpType {
        IN("In"),
        OUT("Out"),
        ;

        public static Adc DEFAULT = IN;
        public static final ItemKey KEY = new ItemKey(INSTRUMENT_KEY, "adc");

        private String _displayValue;

        Adc(String name) {
            _displayValue = name;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return name();
        }

        @Override
        public String logValue() {
            return displayValue();
        }

        public String toString() {
            return displayValue();
        }

        public static Adc valueOf(String name, Adc value) {
            Adc res = SpTypeUtil.noExceptionValueOf(Adc.class, name);
            return res == null ? value : res;
        }
    }

    /**
     * ObservingMode: See OT-49, and OT-101 for the related table of settings for Filter, etc..
     * <p/>
     * 16/May/12 12:40 AM
     * The list of observation modes has changed as
     * <p/>
     * Remove all the NRM modes and add the following instead
     * Value Enum
     * Y_unblocked Y_unblocked
     * J_unblocked J_unblocked
     * H_unblocked H_unblocked
     * K1_unblocked K1_unblocked
     * K2_unblocked K2_unblocked
     * HS_unblocked HS_unblocked
     * HL_unblocked HL_unblocked
     * Hstar_unblocked Hstar_unblocked
     * H_SIWA H_SIWA
     * <p>
     * 23/April/2013
     * OT-102: Added Non-standard item
     * </p>
     */
    public enum ObservingMode implements DisplayableSpType, LoggableSpType, SequenceableSpType {

        // Y_coron
        CORON_Y_BAND("Coronograph Y-band", Filter.Y, false, Apodizer.APOD_Y, FPM.FPM_Y, Lyot.LYOT_080m12_03, 0.5, 3.0) {
            @Override public ObservingMode correspondingH() { return CORON_H_BAND; }
        },
        // J_coron
        CORON_J_BAND("Coronograph J-band", Filter.J, false, Apodizer.APOD_J, FPM.FPM_J, Lyot.LYOT_080m12_04, 0.5, 3.0) {
            @Override public ObservingMode correspondingH() { return CORON_H_BAND; }
        },
        // H_coron
        CORON_H_BAND("Coronograph H-band", Filter.H, false, Apodizer.APOD_H, FPM.FPM_H, Lyot.LYOT_080m12_04, 0.5, 3.0) {
            @Override public ObservingMode correspondingH() { return this; }
        },
        // K1_coron
        CORON_K1_BAND("Coronograph K1-band", Filter.K1, false, Apodizer.APOD_K1, FPM.FPM_K1, Lyot.LYOT_080m12_06_03, 0.5, 3.0) {
            @Override public ObservingMode correspondingH() { return CORON_H_BAND; }
        },
        // K2_coron
        CORON_K2_BAND("Coronograph K2-band", Filter.K2, false, Apodizer.APOD_K2, FPM.FPM_K1, Lyot.LYOT_080m12_07, 0.5, 3.0) {
            @Override public ObservingMode correspondingH() { return CORON_H_BAND; }
        },

        // H_starcor
        H_STAR("H_STAR", Filter.H, false, Apodizer.APOD_STAR, FPM.FPM_H, Lyot.LYOT_080m12_03) {
            @Override public ObservingMode correspondingH() { return this; }
        },
        // H_LIWAcor
        H_LIWA("H_LIWA", Filter.H, false, Apodizer.APOD_HL, FPM.FPM_K1, Lyot.LYOT_080m12_04) {
            @Override public ObservingMode correspondingH() { return this; }
        },

        // Y_direct
        DIRECT_Y_BAND("Y direct", Filter.Y, true, Apodizer.CLEAR, FPM.SCIENCE, Lyot.OPEN, 5.5, 7.5) {
            @Override public ObservingMode correspondingH() { return DIRECT_H_BAND; }
        },
        // J_direct
        DIRECT_J_BAND("J direct", Filter.J, true, Apodizer.CLEAR, FPM.SCIENCE, Lyot.OPEN, 5.5, 7.5) {
            @Override public ObservingMode correspondingH() { return DIRECT_H_BAND; }
        },
        // H_direct
        DIRECT_H_BAND("H direct", Filter.H, true, Apodizer.CLEAR, FPM.SCIENCE, Lyot.OPEN, 5.5, 7.5) {
            @Override public ObservingMode correspondingH() { return this; }
        },
        // K1_direct
        DIRECT_K1_BAND("K1 direct", Filter.K1, true, Apodizer.CLEAR, FPM.SCIENCE, Lyot.OPEN, 5.5, 7.5) {
            @Override public ObservingMode correspondingH() { return DIRECT_H_BAND; }
        },
        // K2_direct
        DIRECT_K2_BAND("K2 direct", Filter.K2, true, Apodizer.CLEAR, FPM.SCIENCE, Lyot.OPEN, 5.5, 7.5) {
            @Override public ObservingMode correspondingH() { return DIRECT_H_BAND; }
        },

        // NRM_Y
        NRM_Y("Non Redundant Mask Y", Filter.Y, true, Apodizer.NRM, FPM.SCIENCE, Lyot.OPEN) {
            @Override public ObservingMode correspondingH() { return NRM_H; }
        },
        // NRM_J
        NRM_J("Non Redundant Mask J", Filter.J, true, Apodizer.NRM, FPM.SCIENCE, Lyot.OPEN) {
            @Override public ObservingMode correspondingH() { return NRM_H; }
        },
        // NRM_H
        NRM_H("Non Redundant Mask H", Filter.H, true, Apodizer.NRM, FPM.SCIENCE, Lyot.OPEN) {
            @Override public ObservingMode correspondingH() { return this; }
        },
        // NRM_K1
        NRM_K1("Non Redundant Mask K1", Filter.K1, true, Apodizer.NRM, FPM.SCIENCE, Lyot.OPEN) {
            @Override public ObservingMode correspondingH() { return NRM_H; }
        },
        // NRM_K2
        NRM_K2("Non Redundant Mask K2", Filter.K2, true, Apodizer.NRM, FPM.SCIENCE, Lyot.OPEN) {
            @Override public ObservingMode correspondingH() { return NRM_H; }
        },

        // Dark mode
        DARK("Dark", Filter.H, false, Apodizer.APOD_H, FPM.FPM_H, Lyot.BLANK, 0.5, 3.0) {
            @Override public ObservingMode correspondingH() { return this; }
        },

        // Unblocked Modes
        UNBLOCKED_Y("Y Unblocked", Filter.Y, false, Apodizer.APOD_Y, FPM.SCIENCE, Lyot.LYOT_080m12_03, 4.0, 6.5) {
            @Override public ObservingMode correspondingH() { return this; }
        },
        UNBLOCKED_J("J Unblocked", Filter.J, false, Apodizer.APOD_J, FPM.SCIENCE, Lyot.LYOT_080m12_04, 4.0, 6.5) {
            @Override public ObservingMode correspondingH() { return this; }
        },
        UNBLOCKED_H("H Unblocked", Filter.H, false, Apodizer.APOD_H, FPM.SCIENCE, Lyot.LYOT_080m12_04, 4.0, 6.5) {
            @Override public ObservingMode correspondingH() { return this; }
        },
        UNBLOCKED_K1("K1 Unblocked", Filter.K1, false, Apodizer.APOD_K1, FPM.SCIENCE, Lyot.LYOT_080m12_06_03, 4.0, 6.5) {
            @Override public ObservingMode correspondingH() { return this; }
        },
        UNBLOCKED_K2("K2 Unblocked", Filter.K2, false, Apodizer.APOD_K2, FPM.SCIENCE, Lyot.LYOT_080m12_07, 4.0, 6.5) {
            @Override public ObservingMode correspondingH() { return this; }
        },

        // Deprecated modes
        // Y_coron dark
        /*CORON_Y_BAND_DARK("Coronograph Y-band dark", Filter.Y, false, Apodizer.APOD_Y, FPM.FPM_Y, Lyot.BLANK, 0.5, 3.0),
        // J_coron dark
        CORON_J_BAND_DARK("Coronograph J-band dark", Filter.J, false, Apodizer.APOD_J, FPM.FPM_J, Lyot.BLANK, 0.5, 3.0),
        // H_coron dark
        CORON_H_BAND_DARK("Coronograph H-band dark", Filter.H, false, Apodizer.APOD_H, FPM.FPM_H, Lyot.BLANK, 0.5, 3.0),
        // K1_coron dark
        CORON_K1_BAND_DARK("Coronograph K1-band dark", Filter.K1, false, Apodizer.APOD_K1, FPM.FPM_K1, Lyot.LYOT_080m12_06_03, 0.5, 3.0),
        // K2_coron dark
        CORON_K2_BAND_DARK("Coronograph K2-band dark", Filter.K2, false, Apodizer.APOD_K2, FPM.FPM_K1, Lyot.LYOT_080m12_07, 0.5, 3.0),

        // H_starcor dark
        H_STAR_DARK("H_STAR dark", Filter.H, false, Apodizer.APOD_STAR, FPM.FPM_H, Lyot.BLANK),
        // H_LIWAcor dark
        H_LIWA_DARK("H_LIWA dark", Filter.H, false, Apodizer.APOD_HL, FPM.FPM_K1, Lyot.BLANK),

        // Y_direct dark
        DIRECT_Y_BAND_DARK("Y direct dark" , Filter.Y, true, Apodizer.CLEAR, FPM.SCIENCE, Lyot.BLANK, 5.5, 8.0),
        // J_direct dark
        DIRECT_J_BAND_DARK("J direct dark" , Filter.J, true, Apodizer.CLEAR, FPM.SCIENCE, Lyot.BLANK, 5.5, 8.0),
        // H_direct dark
        DIRECT_H_BAND_DARK("H direct dark" , Filter.H, true, Apodizer.CLEAR, FPM.SCIENCE, Lyot.BLANK, 5.5, 8.0),
        // K1_direct dark
        DIRECT_K1_BAND_DARK("K1 direct dark", Filter.K1, true, Apodizer.CLEAR, FPM.SCIENCE, Lyot.OPEN, 5.5, 8.0),
        // K2_direct dark
        DIRECT_K2_BAND_DARK("K2 direct dark", Filter.K2, true, Apodizer.CLEAR, FPM.SCIENCE, Lyot.OPEN, 5.5, 8.0),*/

        NONSTANDARD("Nonstandard") {
            @Override public ObservingMode correspondingH() { return this; }
        }
        ;

        public static final ItemKey KEY = new ItemKey(INSTRUMENT_KEY, "observingMode");
        public static final Option<ObservingMode>[] ENGINEERING_VALUES = engineeringValues();
        public static final Option<ObservingMode>[] NONENGINEERING_VALUES = nonEngineeringValues();
        public static final ObservingMode[] NO_CAL_MODES = noCalModes();

        private String _displayValue;
        private final String _logValue;
        private Filter _filter;
        private boolean _filterIterable; // OT-106: is the filter iterable with this observing mode?
        private Apodizer _apodizer;
        private FPM _fpm;
        private Lyot _lyot;
        private Option<Double> _brightLimitPrism;
        private Option<Double> _brightLimitWollaston;

        // Only for NONSTANDARD
        ObservingMode(String name) {
            _displayValue = name;
            _logValue = name;
            _brightLimitPrism = None.instance();
            _brightLimitWollaston = None.instance();
        }

        ObservingMode(String name, Filter filter, boolean filterIterable, Apodizer apodizer,
                              FPM fpm, Lyot lyot, Option<Double> brightLimitPrism, Option<Double> brightLimitWollaston) {
            _logValue = name;
            _displayValue = name;
            _filter = filter;
            _filterIterable = filterIterable;
            _apodizer = apodizer;
            _fpm = fpm;
            _lyot = lyot;
            _brightLimitPrism = brightLimitPrism;
            _brightLimitWollaston = brightLimitWollaston;
        }

        ObservingMode(String name, Filter filter, boolean filterIterable, Apodizer apodizer,
                      FPM fpm, Lyot lyot, double brightLimitPrism, double brightLimitWollaston) {
            this(name, filter, filterIterable, apodizer, fpm, lyot, new Some<>(brightLimitPrism), new Some<>(brightLimitWollaston));
        }

            /**
             * Gets the corresponding observing mode that uses the H filter.  This
             * is used in template creation.  See REL-1817 and GP_BP.txt
             */
        public abstract ObservingMode correspondingH();

        ObservingMode(String name, Filter filter, boolean filterIterable, Apodizer apodizer,
                              FPM fpm, Lyot lyot) {
            this(name, filter, filterIterable, apodizer, fpm, lyot, None.instance(), None.instance());
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return name();
        }

        public String toString() {
            return displayValue();
        }

        public String logValue() {
            return _logValue;
        }

        public static ObservingMode valueOf(String name, ObservingMode value) {
            ObservingMode res = SpTypeUtil.noExceptionValueOf(ObservingMode.class, name);
            return res == null ? value : res;
        }

        public static Option<ObservingMode> valueOf(String name, Option<ObservingMode> nvalue) {
            ObservingMode def = nvalue.isEmpty() ? null : nvalue.getValue();
            ObservingMode val = SpTypeUtil.oldValueOf(ObservingMode.class, name, def);
            Option<ObservingMode> none = None.instance();
            return val == null ? none : new Some<>(val);
        }

        public Filter getFilter() {
            return _filter;
        }

        /**
         * Returns the bright limit based on the given disperser, if defined.
         */
        public Option<Double> getBrightLimit(Disperser disperser) {
            if (disperser == Disperser.PRISM) return _brightLimitPrism;
            if (disperser == Disperser.WOLLASTON) return _brightLimitWollaston;
            return None.instance();
        }

        public boolean isFilterIterable() {
            return _filterIterable;
        }

        public Apodizer getApodizer() {
            return _apodizer;
        }

        public FPM getFpm() {
            return _fpm;
        }

        public Lyot getLyot() {
            return _lyot;
        }

        /** returns all values (OT-102) */
        @SuppressWarnings({"unchecked", "rawtypes"})
        private static Option<ObservingMode>[] engineeringValues() {
            ObservingMode[] ar = values();
            Option<ObservingMode>[] result = new Option[ar.length];
            for(int i = 0; i < ar.length; i++) {
                result[i] = new Some<>(ar[i]);
            }
            return result;
        }

        /** returns all values except NONSTANDARD (OT-102) */
        @SuppressWarnings({"unchecked", "rawtypes"})
        private static Option<ObservingMode>[] nonEngineeringValues() {
            ObservingMode[] ar = values();
            Option<ObservingMode>[] result = new Option[ar.length-1];
            for(int i = 0; i < ar.length-1; i++) {
                result[i] = new Some<>(ar[i]);
            }
            return result;
        }

        /** returns all direct and modes (OT-136, REL-1741) */
        private static ObservingMode[] noCalModes() {
            return new ObservingMode[] {DIRECT_H_BAND, DIRECT_J_BAND, DIRECT_K1_BAND, DIRECT_K2_BAND, DIRECT_Y_BAND,
                                        NRM_H, NRM_J, NRM_K1, NRM_K2, NRM_Y,
                                        UNBLOCKED_H, UNBLOCKED_J, UNBLOCKED_K1, UNBLOCKED_K2, UNBLOCKED_Y};
        }

        public static LoggableSpType byName(String value) {
            for (ObservingMode m: values()) {
                if (m.displayValue().equals(value)) {
                    return m;
                }
            }
            return NoObservingMode;
        }

        private static LoggableSpType NoObservingMode = new LoggableSpType() {
            @Override
            public String logValue() {
                return "Non-standard";
            }

            @Override
            public String name() {
                return "non-standard";
            }

            @Override
            public int ordinal() {
                return 0;
            }
        };
    }

    /**
     * Filter: see OT-62
     */
    public enum Filter implements DisplayableSpType, SequenceableSpType, LoggableSpType {
         Y(MagnitudeBand.Y$.MODULE$),
         J(MagnitudeBand.J$.MODULE$),
         H(MagnitudeBand.H$.MODULE$),
        K1(MagnitudeBand.K$.MODULE$),
        K2(MagnitudeBand.K$.MODULE$)
        ;

        /** The default filter value **/
        public static final Filter DEFAULT = H;
        public static final ItemKey KEY = new ItemKey(INSTRUMENT_KEY, "filter");

        // The related mag band
        private MagnitudeBand _band;

        Filter(MagnitudeBand band) {
            _band = band;

        }

        public String displayValue() {
            return name();
        }

        public String sequenceValue() {
            return name();
        }

        public MagnitudeBand getBand() {
            return _band;
        }

        public String toString() {
            return name();
        }

        @Override
        public String logValue() {
            return displayValue();
        }

        public static Option<Filter> byName(String name) {
            for (Filter m: values()) {
                if (m.displayValue().equals(name)) {
                    return new Some<>(m);
                }
            }
            return None.instance();
        }

    }


    /**
     * Disperser: see OT-50
     */
    public enum Disperser implements DisplayableSpType, SequenceableSpType, PartiallyEngineeringSpType, LoggableSpType {
        PRISM("Prism") {
            @Override
            public boolean isEngineering() {
                return false;
            }
        },
        WOLLASTON("Wollaston") {
            @Override
            public boolean isEngineering() {
                return false;
            }
        };

        /** The default Disperser value **/
        public static final Disperser DEFAULT = PRISM;
        public static final ItemKey KEY = new ItemKey(INSTRUMENT_KEY, "disperser");

        private final String _displayName;

        Disperser(String name) {
            _displayName = name;
        }

        public String displayValue() {
            return _displayName;
        }

        public String sequenceValue() {
            return name();
        }

        public String toString() {
            return displayValue();
        }

        @Override
        public String logValue() {
            return displayValue();
        }

        /** values() returns all values, but OPEN is only for use in the engineering component */
        public static Disperser[] nonEngineeringValues() {
            return new Disperser[] {PRISM, WOLLASTON};
        }
    }

    /**
     * Manual Detector Readout Area: see OT-53.
     */
    private static class ReadoutArea implements Serializable {
        private static final long serialVersionUID = 1L;
        private static final int MIN_VALUE = 0;
        private static final int MAX_VALUE = 2047;

        private int _detectorStartX;
        private int _detectorStartY;
        private int _detectorEndX;
        private int _detectorEndY;

        /**
         * Default constructor: same as FULL
         */
        public ReadoutArea() {
            _detectorStartX = MIN_VALUE;
            _detectorStartY = MIN_VALUE;
            _detectorEndX = MAX_VALUE;
            _detectorEndY = MAX_VALUE;
        }

        public ReadoutArea(int detectorStartX, int detectorStartY, int detectorEndX, int detectorEndY) {
            _detectorStartX = detectorStartX;
            _detectorStartY = detectorStartY;
            _detectorEndX = detectorEndX;
            _detectorEndY = detectorEndY;
        }

        public String toString() {
            return _detectorStartX + " " + _detectorStartY + " " + _detectorEndX + " " + _detectorEndY;
        }

        /**
         * Creates an instance of this class from a string, as returned by toString()
         * @param s string returned by the toString() method in this class
         * @return a ReadoutArea wrapped in an Option, or None if there is an error
         */
        public static Option<ReadoutArea> valueOf(String s) {
            String[] ar = s.split(" ");
            if (ar.length == 4) {
                try {
                    int detectorStartX = Integer.valueOf(ar[0]);
                    int detectorStartY = Integer.valueOf(ar[1]);
                    int detectorEndX = Integer.valueOf(ar[2]);
                    int detectorEndY = Integer.valueOf(ar[3]);
                    return new Some<>(new ReadoutArea(detectorStartX, detectorStartY, detectorEndX, detectorEndY));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
            return None.instance();
        }

        public int getDetectorStartX() {
            return _detectorStartX;
        }

        public int getDetectorStartY() {
            return _detectorStartY;
        }

        public int getDetectorEndX() {
            return _detectorEndX;
        }

        public int getDetectorEndY() {
            return _detectorEndY;
        }

        public ReadoutArea setDetectorStartX(int newValue) {
            return new ReadoutArea(newValue, _detectorStartY, _detectorEndX, _detectorEndY);
        }
        public ReadoutArea setDetectorStartY(int newValue) {
            return new ReadoutArea(_detectorStartX, newValue, _detectorEndX, _detectorEndY);
        }
        public ReadoutArea setDetectorEndX(int newValue) {
            return new ReadoutArea(_detectorStartX, _detectorStartY, newValue, _detectorEndY);
        }
        public ReadoutArea setDetectorEndY(int newValue) {
            return new ReadoutArea(_detectorStartX, _detectorStartY, _detectorEndX, newValue);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ReadoutArea that = (ReadoutArea) o;

            return _detectorEndX == that._detectorEndX && _detectorEndY == that._detectorEndY && _detectorStartX == that._detectorStartX && _detectorStartY == that._detectorStartY;
        }

        @Override
        public int hashCode() {
            int result = _detectorStartX;
            result = 31 * result + _detectorStartY;
            result = 31 * result + _detectorEndX;
            result = 31 * result + _detectorEndY;
            return result;
        }

        /**
         * @return the area (w*h) of the readout area, but not less than 1.
         */
        public int getArea() {
            int a = (_detectorEndX - _detectorStartX) * (_detectorEndY - _detectorStartY);
            return a > 0 ? a : 1;
        }

        /**
         * @return the min exposure time based on the readout area according to OT-54
         */
        public double getMinimumExposureTimeSecs() {
            int area = getArea();
            return DEFAULT_EXPOSURE_TIME * area / DetectorReadoutArea.FULL.getReadoutArea().getArea();
        }
    }

    /**
     * DetectorReadoutArea: see OT-52.
     */
    enum DetectorReadoutArea implements DisplayableSpType {
        FULL("Full (2048x2048)", new ReadoutArea(0, 0, 2047, 2047)),
        CENTRAL_1024("Central (1024x1024)", new ReadoutArea(512, 512, 1535, 1535)),
        CENTRAL_512("Central (512x512)", new ReadoutArea(768, 768, 1279, 1279)),
        CENTRAL_256("Central (256x256)", new ReadoutArea(896, 896, 1151, 1151)),
        MANUAL("Manual", FULL.getReadoutArea()),
        ;

        public static final DetectorReadoutArea DEFAULT = FULL;
        private final String _displayName;
        private final ReadoutArea _readoutArea;

        DetectorReadoutArea(String name, ReadoutArea readoutArea) {
            _displayName = name;
            _readoutArea = readoutArea;
        }

        public String displayValue() {
            return _displayName;
        }

        public String toString() {
            return displayValue();
        }

        public ReadoutArea getReadoutArea() {
            return _readoutArea;
        }

        // Returns the DetectorReadoutArea matching the given ReadoutArea values.
        public static DetectorReadoutArea valueOf(ReadoutArea readoutArea) {
            for(DetectorReadoutArea detectorReadoutArea : values()) {
                if (detectorReadoutArea.getReadoutArea().equals(readoutArea)) {
                    return detectorReadoutArea;
                }
            }
            return MANUAL;
        }

    }


    /**
     * Shutter (Used for different shutters)
     */
    public enum Shutter implements DisplayableSpType, SequenceableSpType {
        OPEN("Open"),
        CLOSE("Close"),
        ;

        public static Shutter DEFAULT = OPEN;

        private String _displayValue;

        Shutter(String name) {
            _displayValue = name;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String toString() {
            return displayValue();
        }

        public String sequenceValue() {
            return name();
        }

        public static Shutter valueOf(String name, Shutter value) {
            Shutter res = SpTypeUtil.noExceptionValueOf(Shutter.class, name);
            return res == null ? value : res;
        }
    }

    /**
     * Apodizer (See OT-63)
     * <p/>
     * 1/May/13 12:47 PM
     * Updated to SRD 30
     * Value Enum
     * CLEAR CLEAR
     * CLEAR GP CLEARGP
     * APOD_Y_56 APOD_Y
     * APOD_J_56 APOD_J
     * APOD_H_56 APOD_H
     * APOD_K1_56 APOD_K1
     * APOD_K2_56 APOD_K2
     * NRM NRM
     * APOD_HL APOD_HL
     * APOD_star APOD_star
     * <p/>
     * And the default is CLEAR      *
     */
    public enum Apodizer implements DisplayableSpType, SequenceableSpType, LoggableSpType, ObsoletableSpType {
        CLEAR("Clear"),
        CLEARGP("CLEAR GP"),
        APOD_Y("APOD_Y_56"),
        APOD_J("APOD_J_56"),
        APOD_H("APOD_H_56"),
        APOD_K1("APOD_K1_56"),
        APOD_K2("APOD_K2_56"),
        NRM("NRM"),
        APOD_HL("APOD_HL"),
        APOD_STAR("APOD_star") {
            public boolean isObsolete() {
                return true;
            }
        },
        ND3("ND3")
        ;

        public static Apodizer DEFAULT = CLEAR;

        private String _displayValue;

        Apodizer(String name) {
            _displayValue = name;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String toString() {
            return displayValue();
        }

        public String sequenceValue() {
            return name();
        }

        @Override
        public boolean isObsolete() {
                    return false;
                }

        @Override
        public String logValue() {
            return displayValue();
        }

        public static Apodizer valueOf(String name, Apodizer value) {
            Apodizer res = SpTypeUtil.noExceptionValueOf(Apodizer.class, name);
            return res == null ? value : res;
        }

        public static Option<Apodizer> byName(String name) {
            for (Apodizer m: values()) {
                if (m.displayValue().equals(name)) {
                    return new Some<>(m);
                }
            }
            return None.instance();
        }

        public static Apodizer[] validValues() {
            return Arrays.stream(values()).filter(a -> !a.isObsolete()).toArray(Apodizer[]::new);
        }
    }

    /**
     * Lyot (See OT-64)
     * <p/>
     * <p/>
     * 1/May/13 List from SRD v30
     * Sorry forgot one value, here is the new list
     * <p/>
     Lyots	Name
     Blank        Blank
     080m12_03    080m12_03
     080m12_04    080m12_04
     080_04       080_04
     080m12_06    080m12_06
     080m12_04_c  080m12_04_c
     080m12_06_03 080m12_06_03
     080m12_07    080m12_07
     080m12_10    080m12_10
     Open         Open
     */
    public enum Lyot implements DisplayableSpType, SequenceableSpType, LoggableSpType {

        BLANK            ("Blank"),
        LYOT_080m12_03   ("080m12_03"),
        LYOT_080m12_04   ("080m12_04"),
        LYOT_080_04      ("080_04"),
        LYOT_080m12_06   ("080m12_06"),
        LYOT_080m12_04_c ("080m12_04_c"),
        LYOT_080m12_06_03("080m12_06_03"),
        LYOT_080m12_07   ("080m12_07"),
        LYOT_080m12_10   ("080m12_10"),
        OPEN             ("Open"),
        ;

        public static Lyot DEFAULT = BLANK;

        private String _displayValue;

        Lyot(String name) {
            _displayValue = name;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return name();
        }

        public String toString() {
            return displayValue();
        }

        @Override
        public String logValue() {
            return displayValue();
        }

        public static Lyot valueOf(String name, Lyot value) {
            Lyot res = SpTypeUtil.noExceptionValueOf(Lyot.class, name);
            return res == null ? value : res;
        }

        public static Option<Lyot> byName(String name) {
            for (Lyot m: values()) {
                if (m.displayValue().equals(name)) {
                    return new Some<>(m);
                }
            }
            return None.instance();
        }
    }


    /**
     * Artificial Source: See OT-69
     */
    public enum ArtificialSource implements DisplayableSpType, SequenceableSpType {
        ON("On") {
            public boolean toBoolean() {
                return true;
            }
        },
        OFF("Off") {
            public boolean toBoolean() {
                return false;
            }
        },
        ;

        public static ArtificialSource DEFAULT = OFF;

        private String _displayValue;

        ArtificialSource(String name) {
            _displayValue = name;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String toString() {
            return displayValue();
        }

        public String sequenceValue() {
            return name();
        }

        public static ArtificialSource valueOf(String name, ArtificialSource value) {
            ArtificialSource res = SpTypeUtil.noExceptionValueOf(ArtificialSource.class, name);
            return res == null ? value : res;
        }

        public abstract boolean toBoolean();

        public static ArtificialSource fromBoolean(boolean value) {
            return value ? ON : OFF;
        }
    }

    /**
     * PupilCamera: See OT-70
     */
    public enum PupilCamera implements DisplayableSpType, SequenceableSpType {
        IN("In"),
        OUT("Out"),
        ;

        public static PupilCamera DEFAULT = OUT;

        private String _displayValue;

        PupilCamera(String name) {
            _displayValue = name;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String toString() {
            return displayValue();
        }

        public String sequenceValue() {
            return name();
        }

        public static PupilCamera valueOf(String name, PupilCamera value) {
            PupilCamera res = SpTypeUtil.noExceptionValueOf(PupilCamera.class, name);
            return res == null ? value : res;
        }
    }

    /**
     * FPM: See OT-72
     * <p/>
     * 1/May/13 List from SRD v30
     * <p/>
     * Value Enum
     * Open Open
     * 50umPIN 50umPIN
     * WITH_DOT WITH_DOT
     * SCIENCE SCIENCE
     * FPM_K1 FPM_K1
     * FPM_H FPM_H
     * FPM_J FPM_J
     * FPM_Y FPM_Y
     */
    public enum FPM implements DisplayableSpType, SequenceableSpType, LoggableSpType {

        OPEN("Open"),
        F50umPIN("50umPIN"),
        WITH_DOT("WITH_DOT"),
        SCIENCE("SCIENCE"),
        FPM_K1("FPM_K1"),
        FPM_H("FPM_H"),
        FPM_J("FPM_J"),
        FPM_Y("FPM_Y"),
        ;

        public static FPM DEFAULT = OPEN;

        private String _displayValue;

        FPM(String name) {
            _displayValue = name;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String toString() {
            return displayValue();
        }

        @Override
        public String logValue() {
            return displayValue();
        }

        public String sequenceValue() {
            return name();
        }

        public static FPM valueOf(String name, FPM value) {
            FPM res = SpTypeUtil.noExceptionValueOf(FPM.class, name);
            return res == null ? value : res;
        }

        public static Option<FPM> byName(String name) {
            for (FPM m: values()) {
                if (m.displayValue().equals(name)) {
                    return new Some<>(m);
                }
            }
            return None.instance();
        }
    }

    /**
     * Detector Sampling Mode: OT-91
     */
    enum DetectorSamplingMode implements DisplayableSpType, SequenceableSpType {
        FAST("Fast"),
        SINGLE_CDS("Single CDS"),
        MULTIPLE_CDS("Multiple CDS"),
        UTR("UTR"),
        ;

        public static DetectorSamplingMode DEFAULT = UTR;

        private String _displayValue;

        DetectorSamplingMode(String name) {
            _displayValue = name;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String toString() {
            return displayValue();
        }

        public String sequenceValue() {
            return name();
        }

        public static DetectorSamplingMode valueOf(String name, DetectorSamplingMode value) {
            DetectorSamplingMode res = SpTypeUtil.noExceptionValueOf(DetectorSamplingMode.class, name);
            return res == null ? value : res;
        }
    }


    /**
     * Cassegrain (PositionAngle): see OT-84.
     */
    private enum Cassegrain implements DisplayableSpType, SequenceableSpType {
        A0(0),
        A180(180),
        ;

        public static final Cassegrain DEFAULT = A0;
        private final String _displayName;
        private final int _angle;

        Cassegrain(int angle) {
            _angle = angle;
            _displayName = angle + " deg E of N";
        }

        public String displayValue() {
            return _displayName;
        }

        public String sequenceValue() {
            return name();
        }

        public String toString() {
            return displayValue();
        }

        public double getAngle() {
            return _angle;
        }

        public static Cassegrain valueOf(double d) {
            if (equal(d, 0)) return Cassegrain.A0;
            if (equal(d, 180)) return Cassegrain.A180;
            return Cassegrain.DEFAULT;
        }
    }


    // Other default values
    public static final boolean DEFAULT_ASTROMETRIC_FIELD = false;
    public static final double DEFAULT_EXPOSURE_TIME = 1.49; // OT-54
    public static final int MAX_EXPOSURE_TIME = 60; // OT-78

    public static final long READOUT_PER_EXPOSURE_MS = 3000; // REL-1717
    public static final double READOUT_OVERHEAD_SEC    = 10.0; // REL-1717

    public static final double MIN_ARTIFICIAL_SOURCE_ATTENUATION = 2.4; // OT-123
    public static final double MAX_ARTIFICIAL_SOURCE_ATTENUATION = 60.0; // OT-123
    public static final double DEFAULT_ARTIFICIAL_SOURCE_ATTENUATION = MAX_ARTIFICIAL_SOURCE_ATTENUATION; // OT-139
    public static final double CALIBRATION_ARTIFICIAL_SOURCE_ATTENUATION = 25.5; // REL-1736
    public static final int DEFAULT_MCDS_COUNT = 1;
    public static final boolean DEFAULT_ALWAYS_RESTORE_MODEL = false;
    public static final boolean DEFAULT_USE_AO = true;
    public static final boolean DEFAULT_USE_CAL = true;
    public static final boolean DEFAULT_AO_OPTIMIZE = true;
    public static final boolean DEFAULT_ALIGN_FPM_PINHOLE_BIAS = false;

    // OT-95: overhead times for single or multiple changes in a sequence
    public static final double SINGLE_CHANGE_OVERHEAD_SECS = 30;
    public static final double MULTI_CHANGE_OVERHEAD_SECS = 60;

    // REL-2208 Overhead to move the half wave plate
    public static final double HALFWAVE_PLATE_CHANGE_OVERHEAD_SECS = 5;

    // OT-129: Science area (square) size in arcsec
    public static final double SCIENCE_AREA_ARCSEC = 2.8;

    public static final ItemKey HALFWAVE_PLATE_ANGLE_KEY = new ItemKey(INSTRUMENT_KEY, "halfWavePlateAngle");

    /**
     * This obs component's SP type.
     */
    public static final SPComponentType SP_TYPE =
            SPComponentType.INSTRUMENT_GPI;


    // Property descriptors
    private static final Map<String, PropertyDescriptor> PRIVATE_PROP_MAP = new TreeMap<>();
    public static final Map<String, PropertyDescriptor> PROPERTY_MAP = Collections.unmodifiableMap(PRIVATE_PROP_MAP);

    /**
     * The name of the GPI instrument configuration
     */
    public static final String INSTRUMENT_NAME_PROP = "GPI";

    private static PropertyDescriptor initProp(String propName, boolean query, boolean iter) {
        PropertyDescriptor pd = PropertySupport.init(propName, Gpi.class, query, iter);
        PRIVATE_PROP_MAP.put(pd.getName(), pd);
        return pd;
    }

    private static PropertyDescriptor initProp(String propName, String displayName, boolean query, boolean iter) {
        PropertyDescriptor pd = PropertySupport.init(propName, displayName, Gpi.class, query, iter);
        PRIVATE_PROP_MAP.put(pd.getName(), pd);
        return pd;
    }

    // Properties
    public static final PropertyDescriptor EXPOSURE_TIME_PROP;
    public static final PropertyDescriptor POS_ANGLE_PROP;
    public static final PropertyDescriptor COADDS_PROP;

    // _ENUM props below are mainly for OT Browser use
    public static final PropertyDescriptor ASTROMETRIC_FIELD_PROP;
    public static final PropertyDescriptor ASTROMETRIC_FIELD_ENUM_PROP;

    public static final PropertyDescriptor ADC_PROP;
    public static final PropertyDescriptor OBSERVING_MODE_PROP;
    public static final PropertyDescriptor DISPERSER_PROP;
    public static final PropertyDescriptor HALF_WAVE_PLATE_ANGLE_VALUE_PROP;

    public static final PropertyDescriptor DETECTOR_READOUT_AREA_PROP;
    public static final PropertyDescriptor READOUT_AREA_PROP;

    public static final PropertyDescriptor DETECTOR_STARTX_PROP;
    public static final PropertyDescriptor DETECTOR_STARTY_PROP;
    public static final PropertyDescriptor DETECTOR_ENDX_PROP;
    public static final PropertyDescriptor DETECTOR_ENDY_PROP;

    // Engineering properties
    public static final PropertyDescriptor FILTER_PROP;
    public static final PropertyDescriptor ENTRANCE_SHUTTER_PROP;
    public static final PropertyDescriptor SCIENCE_ARM_SHUTTER_PROP;
    public static final PropertyDescriptor CAL_ENTRANCE_SHUTTER_PROP;
    public static final PropertyDescriptor REFERENCE_ARM_SHUTTER_PROP;

    public static final PropertyDescriptor APODIZER_PROP;
    public static final PropertyDescriptor LYOT_PROP;

    public static final PropertyDescriptor IR_LASER_LAMP_PROP;
    public static final PropertyDescriptor VISIBLE_LASER_LAMP_PROP;
    public static final PropertyDescriptor SUPER_CONTINUUM_LAMP_PROP;
    public static final PropertyDescriptor ARTIFICIAL_SOURCE_ATTENUATION_PROP;

    public static final PropertyDescriptor PUPUL_CAMERA_PROP;
    public static final PropertyDescriptor FPM_PROP;
    public static final PropertyDescriptor DETECTOR_SAMPLING_MODE_PROP;
    public static final PropertyDescriptor MCDS_COUNT_PROP;
    public static final PropertyDescriptor ALWAYS_RESTORE_MODEL_PROP;

    public static final PropertyDescriptor USE_AO_PROP;
    public static final PropertyDescriptor USE_CAL_PROP;

    public static final PropertyDescriptor AO_OPTIMIZE_PROP;
    public static final PropertyDescriptor ALIGN_FPM_PINHOLE_BIAS_PROP;

    public static final String MAG_H_PROP = "magH";
    public static final String MAG_I_PROP = "magI";

    static {
        final boolean query_yes = true;
        final boolean iter_yes = true;
        final boolean query_no = false;
        final boolean iter_no = false;

        EXPOSURE_TIME_PROP = initProp("exposureTime", query_yes, iter_yes);
        POS_ANGLE_PROP = initProp("posAngle", "Cassegrain Angle", query_no, iter_no);
        COADDS_PROP = initProp("coadds", query_yes, iter_yes);

        ASTROMETRIC_FIELD_PROP = initProp("astrometricField", query_no, iter_no);
        ASTROMETRIC_FIELD_ENUM_PROP = initProp("astrometricFieldEnum", "Astrometric Field", query_yes, iter_no);

        ADC_PROP = initProp("adc", "ADC", query_yes, iter_no);

        OBSERVING_MODE_PROP = initProp("observingMode", query_yes, iter_no);
        PropertySupport.setWrappedType(OBSERVING_MODE_PROP, ObservingMode.class);

        DISPERSER_PROP = initProp("disperser", query_yes, iter_yes);

        // used when halfWavePlateAngle set to MANUAL or Disperser set to PRISM
        HALF_WAVE_PLATE_ANGLE_VALUE_PROP = initProp("halfWavePlateAngle", query_no, iter_yes);
        PropertySupport.setVolatile(HALF_WAVE_PLATE_ANGLE_VALUE_PROP, true);

        DETECTOR_READOUT_AREA_PROP = initProp("detectorReadoutArea", "Detector Readout ROI", query_no, iter_no);
        // used when detectorReadoutArea is set to MANUAL
        READOUT_AREA_PROP = initProp("readoutArea", query_no, iter_no);
        PropertySupport.setWrappedType(READOUT_AREA_PROP, ReadoutArea.class);
        PropertySupport.setVolatile(READOUT_AREA_PROP, true);

        DETECTOR_STARTX_PROP = initProp("detectorStartX", query_no, iter_no);
        DETECTOR_STARTY_PROP = initProp("detectorStartY", query_no, iter_no);
        DETECTOR_ENDX_PROP = initProp("detectorEndX", query_no, iter_no);
        DETECTOR_ENDY_PROP = initProp("detectorEndY", query_no, iter_no);

        // Engineering properties
        FILTER_PROP = initProp("filter", query_no, iter_yes);
        FILTER_PROP.setExpert(true);
        PropertySupport.setVolatile(FILTER_PROP, true);

        ENTRANCE_SHUTTER_PROP = initProp("entranceShutter", query_no, iter_no);
        ENTRANCE_SHUTTER_PROP.setExpert(true);

        SCIENCE_ARM_SHUTTER_PROP = initProp("scienceArmShutter", query_no, iter_no);
        SCIENCE_ARM_SHUTTER_PROP.setExpert(true);

        CAL_ENTRANCE_SHUTTER_PROP = initProp("calEntranceShutter", query_no, iter_no);
        CAL_ENTRANCE_SHUTTER_PROP.setExpert(true);

        REFERENCE_ARM_SHUTTER_PROP = initProp("referenceArmShutter", query_no, iter_no);
        REFERENCE_ARM_SHUTTER_PROP.setExpert(true);

        APODIZER_PROP = initProp("apodizer", query_no, iter_no);
        APODIZER_PROP.setExpert(true);
        PropertySupport.setVolatile(APODIZER_PROP, true);

        LYOT_PROP = initProp("lyot", query_no, iter_no);
        LYOT_PROP.setExpert(true);
        PropertySupport.setVolatile(LYOT_PROP, true);

        IR_LASER_LAMP_PROP = initProp("irLaserLamp", query_no, iter_no);
        IR_LASER_LAMP_PROP.setExpert(true);

        VISIBLE_LASER_LAMP_PROP = initProp("visibleLaserLamp", query_no, iter_no);
        VISIBLE_LASER_LAMP_PROP.setExpert(true);

        SUPER_CONTINUUM_LAMP_PROP = initProp("superContinuumLamp", query_no, iter_no);
        SUPER_CONTINUUM_LAMP_PROP.setExpert(true);

        ARTIFICIAL_SOURCE_ATTENUATION_PROP = initProp("artificialSourceAttenuation", query_no, iter_no);
        ARTIFICIAL_SOURCE_ATTENUATION_PROP.setExpert(true);
        PropertySupport.setWrappedType(ARTIFICIAL_SOURCE_ATTENUATION_PROP, Double.class);
        PropertySupport.setVolatile(ARTIFICIAL_SOURCE_ATTENUATION_PROP, true);

        PUPUL_CAMERA_PROP = initProp("pupilCamera", query_no, iter_no);
        PUPUL_CAMERA_PROP.setExpert(true);

        FPM_PROP = initProp("fpm", "FPM", query_no, iter_no);
        FPM_PROP.setExpert(true);
        PropertySupport.setVolatile(FPM_PROP, true);

        DETECTOR_SAMPLING_MODE_PROP = initProp("detectorSamplingMode", query_no, iter_no);
        DETECTOR_SAMPLING_MODE_PROP.setExpert(true);

        MCDS_COUNT_PROP = initProp("mcdsCount", query_no, iter_no);
        MCDS_COUNT_PROP.setExpert(true);
        PropertySupport.setWrappedType(MCDS_COUNT_PROP, Integer.class);

        ALWAYS_RESTORE_MODEL_PROP = initProp("alwaysRestoreModel",
                "<html>Always restore M1/M2 model from stored model",
                query_no, iter_no);
        ALWAYS_RESTORE_MODEL_PROP.setExpert(true);

        USE_AO_PROP = initProp("useAo",
                "Use AO loop",
                query_no, iter_no);
        USE_AO_PROP.setExpert(true);

        USE_CAL_PROP = initProp("useCal",
                "Use CAL loop",
                query_no, iter_no);
        USE_CAL_PROP.setExpert(true);

        AO_OPTIMIZE_PROP = initProp("aoOptimize",
                "AO Optimize",
                query_no, iter_no);
        AO_OPTIMIZE_PROP.setExpert(true);

        ALIGN_FPM_PINHOLE_BIAS_PROP = initProp("alignFpmPinholeBias",
                "Align FPM/Pinhole/Bias",
                query_no, iter_no);
        ALIGN_FPM_PINHOLE_BIAS_PROP.setExpert(true);

    }

    // instrument properties
    private boolean _astrometricField = DEFAULT_ASTROMETRIC_FIELD; // OT-47
    private Adc _adc = Adc.DEFAULT; // OT-48
    private Option<ObservingMode> _observingMode = None.instance(); // OT-49
    private boolean _observingModeOverride = false; // OT-101: Used to keep track of _observingMode related settings
    private Disperser _disperser = Disperser.DEFAULT; // OT-50

    // value if _disperser is WOLLASTON
    private double      _halfWavePlateAngleValue = 0;

    // OT-52: predefined values
    private DetectorReadoutArea _detectorReadoutArea = DetectorReadoutArea.DEFAULT;
    // OT-53: value if DetectorReadoutArea.MANUAL selected
    private Option<ReadoutArea> _readoutArea = None.instance();

    // Engineering settings
    private Filter _filter = Filter.DEFAULT; // OT-62

    private Shutter _entranceShutter = Shutter.DEFAULT; // OT-57
    private Shutter _scienceArmShutter = Shutter.DEFAULT; // OT-58
    private Shutter _calEntranceShutter = Shutter.DEFAULT; // OT-59
    private Shutter _referenceArmShutter = Shutter.DEFAULT; // OT-61

    private Apodizer _apodizer = Apodizer.DEFAULT; // OT-64
    private Lyot _lyot = Lyot.DEFAULT; // OT-64

    private boolean _irLaserLamp = ArtificialSource.OFF.toBoolean(); // OT-66
    private boolean _visibleLaserLamp  = ArtificialSource.OFF.toBoolean(); // OT-67
    private boolean _superContinuumLamp = ArtificialSource.OFF.toBoolean(); // OT-68, OT-123
    private Double _artificialSourceAttenuation = DEFAULT_ARTIFICIAL_SOURCE_ATTENUATION; // OT-68

    private PupilCamera _pupilCamera = PupilCamera.DEFAULT; // OT-70
    private FPM _fpm = FPM.DEFAULT; // OT-72

    private DetectorSamplingMode _detectorSamplingMode = DetectorSamplingMode.DEFAULT; // OT-91
    private Option<Integer> _mcdsCount = None.instance(); // OT-91

    private boolean _alwaysRestoreModel = DEFAULT_ALWAYS_RESTORE_MODEL; // OT-65
    // OT-136 useAo and useCal
    private boolean _useAo = DEFAULT_USE_AO;
    private boolean _useCal = DEFAULT_USE_CAL;
    // REL-2012 Ao Optimize and Align FPM/Pinhole/Bias
    private boolean _aoOptimize = DEFAULT_AO_OPTIMIZE;
    private boolean _alignFpmPinholeBias = DEFAULT_ALIGN_FPM_PINHOLE_BIAS;

    public Gpi() {
        super(SP_TYPE);
        // Override the default exposure time
        _exposureTime = DEFAULT_EXPOSURE_TIME;
    }

    /**
     * Implementation of the clone method.
     */
    public Object clone() {
        // No problems cloning here since private variables are immutable
        return super.clone();
    }

    @Override
    public Map<String, PropertyDescriptor> getProperties() {
        return PROPERTY_MAP;
    }

    @Override
    public Set<Site> getSite() {
        return Site.SET_GS;
    }

    @Override
    public String getPhaseIResourceName() {
        return "gemGPI";
    }

    public static double getImagingSetupSec() {
        // OT-94: "Default from GPI goals are 10 minutes but could be changed at commissioning"
        return 10 * 60;
    }

    @Override
    public double getSetupTime(ISPObservation obs) {
        return getImagingSetupSec();
    }

    public boolean isAstrometricField() {
        return _astrometricField;
    }

    public void setAstrometricField(boolean newValue) {
        boolean oldValue = isAstrometricField();
        if (oldValue != newValue) {
            _astrometricField = newValue;
            firePropertyChange(ASTROMETRIC_FIELD_PROP, oldValue, newValue);
        }
    }

    public YesNoType getAstrometricFieldEnum() {
        return YesNoType.fromBoolean(_astrometricField);
    }

    public void setAstrometricFieldEnum(YesNoType newValue) {
        setAstrometricField(newValue.toBoolean());
    }

    public Adc getAdc() {
        return _adc;
    }

    public void setAdc(Adc newValue) {
        Adc oldValue = getAdc();
        if (oldValue != newValue) {
            _adc = newValue;
            firePropertyChange(ADC_PROP, oldValue, newValue);
        }
    }

    private static double offsetVal(final Config c, final ItemKey key, double cur) {
        // of course the p and q are stored as strings in the sequence ....
        return c.containsItem(key) ? Double.parseDouble(c.getItemValue(key).toString()) : cur;
    }

    public ConfigSequence postProcessSequence(ConfigSequence in) {
        final Config[] steps = in.getAllSteps();
        double p = 0;
        double q = 0;
        final ItemKey obsClassKey = new ItemKey(CalDictionary.OBS_KEY, InstConstants.OBS_CLASS_PROP);
        final ItemKey obsTypeKey  = new ItemKey(CalDictionary.OBS_KEY, InstConstants.OBSERVE_TYPE_PROP);
        for (Config c:steps) {
            p = offsetVal(c, OffsetPosBase.TEL_P_KEY, p);
            q = offsetVal(c, OffsetPosBase.TEL_Q_KEY, q);
            if (p == 0  && q == 0) {
                c.putItem(new ItemKey(GUIDING_PATH), StandardGuideOptions.instance.getDefaultActive());
            } else {
                c.putItem(new ItemKey(GUIDING_PATH), StandardGuideOptions.instance.getDefaultInactive());
            }

            if (c.containsItem(obsClassKey)) {
                // REL-1736 If the sequence is a calibration acquisition, set the ASU on
                if (c.getItemValue(obsClassKey).equals("acqCal")) {
                    ItemKey asuKey = new ItemKey(new ItemKey(InstConstants.INSTRUMENT_NAME_PROP), ARTIFICIAL_SOURCE_ATTENUATION_PROP.getName());
                    ItemKey scKey = new ItemKey(new ItemKey(InstConstants.INSTRUMENT_NAME_PROP), SUPER_CONTINUUM_LAMP_PROP.getName());
                    c.putItem(asuKey, CALIBRATION_ARTIFICIAL_SOURCE_ATTENUATION);
                    c.putItem(scKey, ArtificialSource.ON);

                    // REL-1743 Set guiding to off if it is an acquisition sequence
                    c.putItem(new ItemKey(GUIDING_PATH), StandardGuideOptions.instance.getDefaultOff());
                }
            }
            if (c.containsItem(obsTypeKey)) {
                // REL-1709 If the sequence is a calibration, set the useAo and useCal to false
                Object obsTypeValue = c.getItemValue(obsTypeKey);
                if (obsTypeValue.equals("DARK") || obsTypeValue.equals("FLAT")|| obsTypeValue.equals("ARC")) {
                    ItemKey useAoKey = new ItemKey(new ItemKey(InstConstants.INSTRUMENT_NAME_PROP), USE_AO_PROP.getName());
                    c.putItem(useAoKey, Boolean.FALSE);
                    ItemKey calAoKey = new ItemKey(new ItemKey(InstConstants.INSTRUMENT_NAME_PROP), USE_CAL_PROP.getName());
                    c.putItem(calAoKey, Boolean.FALSE);

                    // REL-1743 Set guiding to off if it is a calibration sequence
                    c.putItem(new ItemKey(GUIDING_PATH), StandardGuideOptions.instance.getDefaultOff());
                }
            }
        }

        return new ConfigSequence(steps);
    }

    public Option<ObservingMode> getObservingMode() {
        return _observingMode;
    }

    public void setObservingMode(Option<ObservingMode> newValue) {
        Option<ObservingMode> oldValue = getObservingMode();
        if (newValue == null) newValue = None.instance(); // because of OT-102 handling in editor
        if (oldValue != newValue) {
            _observingMode = newValue;
            // OT-101: auto set these items when the ObservingMode changes
            if (!newValue.isEmpty() && newValue.getValue() != ObservingMode.NONSTANDARD) {
                ObservingMode observingMode = newValue.getValue();
                _setFilter(observingMode.getFilter());
                _setApodizer(observingMode.getApodizer());
                _setFpm(observingMode.getFpm());
                _setLyot(observingMode.getLyot());
                _observingModeOverride = false;
                // OT-136 Set useAo and useCal according to the mode
                _observingMode.foreach(obsMode -> {
                    setUseAo(true);
                    setUseCal(true);
                    for (ObservingMode m: ObservingMode.NO_CAL_MODES) {
                        if (m.equals(obsMode)) {
                            setUseCal(false);
                            break;
                        }
                    }
                });
                if (_observingMode.exists(obsMode -> obsMode == ObservingMode.DARK)) {
                    setUseAo(false);
                    setUseCal(false);
                }
            }
            firePropertyChange(OBSERVING_MODE_PROP, oldValue, newValue);
        }
    }


    public Disperser getDisperser() {
        return _disperser;
    }

    public void setDisperser(Disperser newValue) {
        Disperser oldValue = getDisperser();
        if (oldValue != newValue) {
            _disperser = newValue;
            firePropertyChange(DISPERSER_PROP, oldValue, newValue);
            if (_disperser != Disperser.WOLLASTON) {
                setHalfWavePlateAngle(0);
            }
        }
    }

    public double getHalfWavePlateAngle() {
        return _halfWavePlateAngleValue;
    }

    public void setHalfWavePlateAngle(double newValue) {
        if (_disperser != Disperser.WOLLASTON && newValue != 0) {
            newValue = 0;
        }

        double oldValue = getHalfWavePlateAngle();
        if (oldValue != newValue) {
            _halfWavePlateAngleValue = newValue;
            firePropertyChange(HALF_WAVE_PLATE_ANGLE_VALUE_PROP, oldValue, newValue);
        }
    }


    /**
     * Returns the predefined readout area settings
     */
    public DetectorReadoutArea getDetectorReadoutArea() {
        return _detectorReadoutArea;
    }

    /**
     * Sets the predefined readout area settings
     */
    public void setDetectorReadoutArea(DetectorReadoutArea newValue) {
        DetectorReadoutArea oldValue = getDetectorReadoutArea();
        if (oldValue != newValue) {
            _detectorReadoutArea = newValue;
            firePropertyChange(DETECTOR_READOUT_AREA_PROP, oldValue, newValue);
            if (_detectorReadoutArea != DetectorReadoutArea.MANUAL) {
                setReadoutArea(new Some<>(_detectorReadoutArea.getReadoutArea()));
            }
        }
    }

    /**
     * Returns the custom readout area settings
     */
    public Option<ReadoutArea> getReadoutArea() {
        return _readoutArea;
    }

    /**
     * Sets the custom readout area settings
     */
    public void setReadoutArea(Option<ReadoutArea> newValue) {
        Option<ReadoutArea> oldValue = getReadoutArea();
        if (oldValue != newValue) {
            _readoutArea = newValue;
            firePropertyChange(READOUT_AREA_PROP, oldValue, newValue);
        }
    }

    // Returns the predefined or custom (manual) readout area settings
    private ReadoutArea _getReadoutArea() {
        if (_detectorReadoutArea != DetectorReadoutArea.MANUAL) {
            return _detectorReadoutArea.getReadoutArea();
        }
        if (!_readoutArea.isEmpty()) {
            return _readoutArea.getValue();
        }
        return DetectorReadoutArea.DEFAULT.getReadoutArea();
    }

    public int getDetectorStartX() {
        return _getReadoutArea().getDetectorStartX();
    }

    public void setDetectorStartX(int newValue) {
        if (_detectorReadoutArea == DetectorReadoutArea.MANUAL) {
            if (_readoutArea.isEmpty() || _readoutArea.getValue().getDetectorStartX() != newValue) {
                ReadoutArea readoutArea = _readoutArea.isEmpty() ? new ReadoutArea() : _readoutArea.getValue();
                setReadoutArea(new Some<>(readoutArea.setDetectorStartX(newValue)));
            }
        }
    }

    public int getDetectorStartY() {
        return _getReadoutArea().getDetectorStartY();
    }

    public void setDetectorStartY(int newValue) {
        if (_detectorReadoutArea == DetectorReadoutArea.MANUAL) {
            if (_readoutArea.isEmpty() || _readoutArea.getValue().getDetectorStartY() != newValue) {
                ReadoutArea readoutArea = _readoutArea.isEmpty() ? new ReadoutArea() : _readoutArea.getValue();
                setReadoutArea(new Some<>(readoutArea.setDetectorStartY(newValue)));
            }
        }
    }

    public int getDetectorEndX() {
        return _getReadoutArea().getDetectorEndX();
    }

    public void setDetectorEndX(int newValue) {
        if (_detectorReadoutArea == DetectorReadoutArea.MANUAL) {
            if (_readoutArea.isEmpty() || _readoutArea.getValue().getDetectorEndX() != newValue) {
                ReadoutArea readoutArea = _readoutArea.isEmpty() ? new ReadoutArea() : _readoutArea.getValue();
                setReadoutArea(new Some<>(readoutArea.setDetectorEndX(newValue)));
            }
        }
    }

    public int getDetectorEndY() {
        return _getReadoutArea().getDetectorEndY();
    }

    public void setDetectorEndY(int newValue) {
        if (_detectorReadoutArea == DetectorReadoutArea.MANUAL) {
            if (_readoutArea.isEmpty() || _readoutArea.getValue().getDetectorEndY() != newValue) {
                ReadoutArea readoutArea = _readoutArea.isEmpty() ? new ReadoutArea() : _readoutArea.getValue();
                setReadoutArea(new Some<>(readoutArea.setDetectorEndY(newValue)));
            }
        }
    }

    public Filter getFilter() {
        return _filter;
    }

    public void setFilter(Filter newValue) {
        Filter oldValue = getFilter();
        if (oldValue != newValue) {
            _filter = newValue;
            _observingModeOverride = true;
            firePropertyChange(FILTER_PROP, oldValue, newValue);
            setObservingMode(new Some<>(ObservingMode.NONSTANDARD));
        }
    }

    private void _setFilter(Filter newValue) {
        Filter oldValue = getFilter();
        if (oldValue != newValue) {
            _filter = newValue;
            firePropertyChange(FILTER_PROP, oldValue, newValue);
        }
    }

    public Shutter getEntranceShutter() {
        return _entranceShutter;
    }

    public void setEntranceShutter(Shutter newValue) {
        Shutter oldValue = getEntranceShutter();
        if (oldValue != newValue) {
            _entranceShutter = newValue;
            firePropertyChange(ENTRANCE_SHUTTER_PROP, oldValue, newValue);
        }
    }

    public Shutter getScienceArmShutter() {
        return _scienceArmShutter;
    }

    public void setScienceArmShutter(Shutter newValue) {
        Shutter oldValue = getScienceArmShutter();
        if (oldValue != newValue) {
            _scienceArmShutter = newValue;
            firePropertyChange(SCIENCE_ARM_SHUTTER_PROP, oldValue, newValue);
        }
    }

    public Shutter getCalEntranceShutter() {
        return _calEntranceShutter;
    }

    public void setCalEntranceShutter(Shutter newValue) {
        Shutter oldValue = getCalEntranceShutter();
        if (oldValue != newValue) {
            _calEntranceShutter = newValue;
            firePropertyChange(CAL_ENTRANCE_SHUTTER_PROP, oldValue, newValue);
        }
    }

    public Shutter getReferenceArmShutter() {
        return _referenceArmShutter;
    }

    public void setReferenceArmShutter(Shutter newValue) {
        Shutter oldValue = getReferenceArmShutter();
        if (oldValue != newValue) {
            _referenceArmShutter = newValue;
            firePropertyChange(REFERENCE_ARM_SHUTTER_PROP, oldValue, newValue);
        }
    }

    public Apodizer getApodizer() {
        return _apodizer;
    }

    public void setApodizer(Apodizer newValue) {
        Apodizer oldValue = getApodizer();
        if (oldValue != newValue) {
            _apodizer = newValue;
            _observingModeOverride = true;
            firePropertyChange(APODIZER_PROP, oldValue, newValue);
            setObservingMode(new Some<>(ObservingMode.NONSTANDARD));
        }
    }

    private void _setApodizer(Apodizer newValue) {
        Apodizer oldValue = getApodizer();
        if (oldValue != newValue) {
            _apodizer = newValue;
            firePropertyChange(APODIZER_PROP, oldValue, newValue);
        }
    }

    public Lyot getLyot() {
        return _lyot;
    }

    public void setLyot(Lyot newValue) {
        Lyot oldValue = getLyot();
        if (oldValue != newValue) {
            _lyot = newValue;
            _observingModeOverride = true;
            firePropertyChange(LYOT_PROP, oldValue, newValue);
            setObservingMode(new Some<>(ObservingMode.NONSTANDARD));
        }
    }

    private void _setLyot(Lyot newValue) {
        Lyot oldValue = getLyot();
        if (oldValue != newValue) {
            _lyot = newValue;
            firePropertyChange(LYOT_PROP, oldValue, newValue);
        }
    }

    public boolean isIrLaserLamp() {
        return _irLaserLamp;
    }

    public void setIrLaserLamp(boolean newValue) {
        boolean oldValue = isIrLaserLamp();
        if (oldValue != newValue) {
            _irLaserLamp = newValue;
            if (!isSuperContinuumLamp() && !isIrLaserLamp() && !isVisibleLaserLamp()) {
                setArtificialSourceAttenuation(DEFAULT_ARTIFICIAL_SOURCE_ATTENUATION);
            }
            firePropertyChange(IR_LASER_LAMP_PROP, oldValue, newValue);
        }
    }

    public ArtificialSource getIrLaserLampEnum() {
        return ArtificialSource.fromBoolean(_irLaserLamp);
    }


    public boolean isVisibleLaserLamp() {
        return _visibleLaserLamp;
    }

    public void setVisibleLaserLamp(boolean newValue) {
        boolean oldValue = isVisibleLaserLamp();
        if (oldValue != newValue) {
            _visibleLaserLamp = newValue;
            if (!isSuperContinuumLamp() && !isIrLaserLamp() && !isVisibleLaserLamp()) {
                setArtificialSourceAttenuation(DEFAULT_ARTIFICIAL_SOURCE_ATTENUATION);
            }
            firePropertyChange(VISIBLE_LASER_LAMP_PROP, oldValue, newValue);
        }
    }

    public ArtificialSource getVisibleLaserLampEnum() {
        return ArtificialSource.fromBoolean(_visibleLaserLamp);
    }

    public boolean isSuperContinuumLamp() {
        return _superContinuumLamp;
    }

    public void setSuperContinuumLamp(boolean newValue) {
        boolean oldValue = isSuperContinuumLamp();
        if (oldValue != newValue) {
            _superContinuumLamp = newValue;
            if (!isSuperContinuumLamp() && !isIrLaserLamp() && !isVisibleLaserLamp()) {
                setArtificialSourceAttenuation(DEFAULT_ARTIFICIAL_SOURCE_ATTENUATION);
            }
            firePropertyChange(SUPER_CONTINUUM_LAMP_PROP, oldValue, newValue);
        }
    }

    public ArtificialSource getSuperContinuumLampEnum() {
        return ArtificialSource.fromBoolean(_superContinuumLamp);
    }

    public Double getArtificialSourceAttenuation() {
        return _artificialSourceAttenuation;
    }

    public void setArtificialSourceAttenuation(Double newValue) {
        Double oldValue = getArtificialSourceAttenuation();
        if (!oldValue.equals(newValue)) {
            _artificialSourceAttenuation = newValue;
            firePropertyChange(ARTIFICIAL_SOURCE_ATTENUATION_PROP, oldValue, newValue);
        }
    }

    public PupilCamera getPupilCamera() {
        return _pupilCamera;
    }

    public void setPupilCamera(PupilCamera newValue) {
        PupilCamera oldValue = getPupilCamera();
        if (oldValue != newValue) {
            _pupilCamera = newValue;
            firePropertyChange(PUPUL_CAMERA_PROP, oldValue, newValue);
        }
    }

    public FPM getFpm() {
        return _fpm;
    }

    public void setFpm(FPM newValue) {
        FPM oldValue = getFpm();
        if (oldValue != newValue) {
            _fpm = newValue;
            _observingModeOverride = true;
            firePropertyChange(FPM_PROP, oldValue, newValue);
            setObservingMode(new Some<>(ObservingMode.NONSTANDARD));
        }
    }

    private void _setFpm(FPM newValue) {
        FPM oldValue = getFpm();
        if (oldValue != newValue) {
            _fpm = newValue;
            firePropertyChange(FPM_PROP, oldValue, newValue);
        }
    }

    public DetectorSamplingMode getDetectorSamplingMode() {
        return _detectorSamplingMode;
    }

    public void setDetectorSamplingMode(DetectorSamplingMode newValue) {
        DetectorSamplingMode oldValue = getDetectorSamplingMode();
        if (oldValue != newValue) {
            _detectorSamplingMode = newValue;
            firePropertyChange(DETECTOR_SAMPLING_MODE_PROP, oldValue, newValue);
            if (newValue == DetectorSamplingMode.MULTIPLE_CDS) {
                if (_mcdsCount.isEmpty()) {
                    setMcdsCount(new Some<>(DEFAULT_MCDS_COUNT));
                }
            } else {
                setMcdsCount(None.<Integer>instance());
            }
        }
    }

    public Option<Integer> getMcdsCount() {
        return _mcdsCount;
    }

    public void setMcdsCount(Option<Integer> newValue) {
        if (!newValue.isEmpty() && newValue.getValue() < 1) {
            newValue = new Some<>(1);
        }
        Option<Integer> oldValue = getMcdsCount();
        if (oldValue != newValue) {
            _mcdsCount = newValue;
            firePropertyChange(MCDS_COUNT_PROP, oldValue, newValue);
        }
    }


    public boolean isAlwaysRestoreModel() {
        return _alwaysRestoreModel;
    }

    public void setAlwaysRestoreModel(boolean newValue) {
        boolean oldValue = isAlwaysRestoreModel();
        if (oldValue != newValue) {
            _alwaysRestoreModel = newValue;
            firePropertyChange(ALWAYS_RESTORE_MODEL_PROP, oldValue, newValue);
        }
    }

    public boolean isUseAo() {
        return _useAo;
    }

    public void setUseAo(boolean newValue) {
        boolean oldValue = isUseAo();
        if (oldValue != newValue) {
            _useAo = newValue;
            firePropertyChange(USE_AO_PROP, oldValue, newValue);
        }
    }

    public boolean isUseCal() {
        return _useCal;
    }

    public void setUseCal(boolean newValue) {
        boolean oldValue = isUseCal();
        if (oldValue != newValue) {
            _useCal = newValue;
            firePropertyChange(USE_CAL_PROP, oldValue, newValue);
        }
    }

    public boolean isAoOptimize() {
        return _aoOptimize;
    }

    public void setAoOptimize(boolean newValue) {
        boolean oldValue = isAoOptimize();
        if (oldValue != newValue) {
            _aoOptimize = newValue;
            firePropertyChange(AO_OPTIMIZE_PROP, oldValue, newValue);
        }
    }

    public boolean isAlignFpmPinholeBias() {
        return _alignFpmPinholeBias;
    }

    public void setAlignFpmPinholeBias(boolean newValue) {
        boolean oldValue = isAlignFpmPinholeBias();
        if (oldValue != newValue) {
            _alignFpmPinholeBias = newValue;
            firePropertyChange(ALIGN_FPM_PINHOLE_BIAS_PROP, oldValue, newValue);
        }
    }

    /**
     * @return the min exposure time in seconds
     */
    public double getMinimumExposureTimeSecs() {
        return _getReadoutArea().getMinimumExposureTimeSecs();
    }

    /**
     * @return the max exposure time in seconds
     */
    public int getMaximumExposureTimeSecs() {
        return MAX_EXPOSURE_TIME;
    }

    /**
     * Return a parameter set describing the current state of this object.
     */
    public ParamSet getParamSet(PioFactory factory) {
        ParamSet paramSet = super.getParamSet(factory);

        Pio.addParam(factory, paramSet, ASTROMETRIC_FIELD_ENUM_PROP, getAstrometricFieldEnum().name());
        Pio.addParam(factory, paramSet, ADC_PROP, getAdc().name());

        if (_observingModeOverride) {
            Pio.addParam(factory, paramSet, OBSERVING_MODE_PROP, getObservingMode().getValue().name());
            Pio.addParam(factory, paramSet, FILTER_PROP, getFilter().name());
            Pio.addParam(factory, paramSet, APODIZER_PROP, getApodizer().name());
            Pio.addParam(factory, paramSet, LYOT_PROP, getLyot().name());
            Pio.addParam(factory, paramSet, FPM_PROP, getFpm().name());
        } else {
            if (!getObservingMode().isEmpty()) {
                Pio.addParam(factory, paramSet, OBSERVING_MODE_PROP, getObservingMode().getValue().name());
            }
        }

        Pio.addParam(factory, paramSet, DISPERSER_PROP, getDisperser().name());

        Pio.addParam(factory, paramSet, DETECTOR_STARTX_PROP, String.valueOf(getDetectorStartX()));
        Pio.addParam(factory, paramSet, DETECTOR_STARTY_PROP, String.valueOf(getDetectorStartY()));
        Pio.addParam(factory, paramSet, DETECTOR_ENDX_PROP, String.valueOf(getDetectorEndX()));
        Pio.addParam(factory, paramSet, DETECTOR_ENDY_PROP, String.valueOf(getDetectorEndY()));

        Pio.addParam(factory, paramSet, ENTRANCE_SHUTTER_PROP, getEntranceShutter().name());
        Pio.addParam(factory, paramSet, SCIENCE_ARM_SHUTTER_PROP, getScienceArmShutter().name());
        Pio.addParam(factory, paramSet, CAL_ENTRANCE_SHUTTER_PROP, getCalEntranceShutter().name());
        Pio.addParam(factory, paramSet, REFERENCE_ARM_SHUTTER_PROP, getReferenceArmShutter().name());
        Pio.addParam(factory, paramSet, IR_LASER_LAMP_PROP, getIrLaserLampEnum().name());
        Pio.addParam(factory, paramSet, VISIBLE_LASER_LAMP_PROP, getVisibleLaserLampEnum().name());
        Pio.addParam(factory, paramSet, SUPER_CONTINUUM_LAMP_PROP, getSuperContinuumLampEnum().name());
        Pio.addParam(factory, paramSet, ARTIFICIAL_SOURCE_ATTENUATION_PROP, String.valueOf(getArtificialSourceAttenuation()));
        Pio.addParam(factory, paramSet, PUPUL_CAMERA_PROP, getPupilCamera().name());
        Pio.addParam(factory, paramSet, DETECTOR_SAMPLING_MODE_PROP, getDetectorSamplingMode().name());
        if (!_mcdsCount.isEmpty()) {
            Pio.addParam(factory, paramSet, MCDS_COUNT_PROP, String.valueOf(getMcdsCount().getValue()));
        }
        Pio.addParam(factory, paramSet, ALWAYS_RESTORE_MODEL_PROP, String.valueOf(isAlwaysRestoreModel()));
        Pio.addParam(factory, paramSet, USE_AO_PROP, String.valueOf(isUseAo()));
        Pio.addParam(factory, paramSet, USE_CAL_PROP, String.valueOf(isUseCal()));
        Pio.addParam(factory, paramSet, AO_OPTIMIZE_PROP, String.valueOf(isAoOptimize()));
        Pio.addParam(factory, paramSet, ALIGN_FPM_PINHOLE_BIAS_PROP, String.valueOf(isAlignFpmPinholeBias()));

        return paramSet;
    }

    /**
     * Set the state of this object from the given parameter set.
     */
    public void setParamSet(ParamSet paramSet) {
        super.setParamSet(paramSet);

        String v;

        v = Pio.getValue(paramSet, ASTROMETRIC_FIELD_ENUM_PROP);
        if (v != null) setAstrometricFieldEnum(YesNoType.valueOf(v));

        v = Pio.getValue(paramSet, ADC_PROP);
        if (v != null) setAdc(Adc.valueOf(v));

        v = Pio.getValue(paramSet, OBSERVING_MODE_PROP.getName());
        if (v != null) {
            if (v.endsWith("_DARK") || v.endsWith("dark")) {
                setObservingMode(new Some<>(ObservingMode.DARK));
            } else {
                setObservingMode(ObservingMode.valueOf(v, getObservingMode()));
            }
        }

        v = Pio.getValue(paramSet, DISPERSER_PROP);
        if (v != null) setDisperser(Disperser.valueOf(v));

        ReadoutArea a = DetectorReadoutArea.DEFAULT.getReadoutArea();
        int startX = Pio.getIntValue(paramSet, DETECTOR_STARTX_PROP.getName(), a.getDetectorStartX());
        int startY = Pio.getIntValue(paramSet, DETECTOR_STARTY_PROP.getName(), a.getDetectorStartY());
        int endX = Pio.getIntValue(paramSet, DETECTOR_ENDX_PROP.getName(), a.getDetectorEndX());
        int endY = Pio.getIntValue(paramSet, DETECTOR_ENDY_PROP.getName(), a.getDetectorEndY());
        a = new ReadoutArea(startX, startY, endX, endY);
        setDetectorReadoutArea(DetectorReadoutArea.valueOf(a));
        if (getDetectorReadoutArea() == DetectorReadoutArea.MANUAL) {
            setReadoutArea(new Some<>(a));
        }

        v = Pio.getValue(paramSet, FILTER_PROP);
        if (v != null) setFilter(Filter.valueOf(v));

        v = Pio.getValue(paramSet, ENTRANCE_SHUTTER_PROP);
        if (v != null) setEntranceShutter(Shutter.valueOf(v));

        v = Pio.getValue(paramSet, SCIENCE_ARM_SHUTTER_PROP);
        if (v != null) setScienceArmShutter(Shutter.valueOf(v));

        v = Pio.getValue(paramSet, CAL_ENTRANCE_SHUTTER_PROP);
        if (v != null) setCalEntranceShutter(Shutter.valueOf(v));

        v = Pio.getValue(paramSet, REFERENCE_ARM_SHUTTER_PROP);
        if (v != null) setReferenceArmShutter(Shutter.valueOf(v));

        v = Pio.getValue(paramSet, APODIZER_PROP);
        if (v != null) setApodizer(Apodizer.valueOf(v));

        v = Pio.getValue(paramSet, LYOT_PROP);
        if (v != null) setLyot(Lyot.valueOf(v));

        v = Pio.getValue(paramSet, IR_LASER_LAMP_PROP);
        if (v != null) setIrLaserLamp(ArtificialSource.valueOf(v).toBoolean());

        v = Pio.getValue(paramSet, VISIBLE_LASER_LAMP_PROP);
        if (v != null) setVisibleLaserLamp(ArtificialSource.valueOf(v).toBoolean());

        v = Pio.getValue(paramSet, SUPER_CONTINUUM_LAMP_PROP);
        if (v != null) setSuperContinuumLamp(ArtificialSource.valueOf(v).toBoolean());

        setArtificialSourceAttenuation(Pio.getDoubleValue(paramSet,
            ARTIFICIAL_SOURCE_ATTENUATION_PROP.getName(), DEFAULT_ARTIFICIAL_SOURCE_ATTENUATION));

        v = Pio.getValue(paramSet, PUPUL_CAMERA_PROP);
        if (v != null) setPupilCamera(PupilCamera.valueOf(v));

        v = Pio.getValue(paramSet, FPM_PROP);
        if (v != null) setFpm(FPM.valueOf(v));

        v = Pio.getValue(paramSet, DETECTOR_SAMPLING_MODE_PROP);
        if (v != null) setDetectorSamplingMode(DetectorSamplingMode.valueOf(v));

        if (getDetectorSamplingMode() == DetectorSamplingMode.MULTIPLE_CDS) {
            v = Pio.getValue(paramSet, MCDS_COUNT_PROP);
            if (v != null) {
                setMcdsCount(new Some<>(Pio.getIntValue(paramSet,
                        MCDS_COUNT_PROP.getName(), 0)));
            }
        }
        setAlwaysRestoreModel(Pio.getBooleanValue(paramSet, ALWAYS_RESTORE_MODEL_PROP.getName(),
                DEFAULT_ALWAYS_RESTORE_MODEL));
        setUseAo(Pio.getBooleanValue(paramSet, USE_AO_PROP.getName(),
                DEFAULT_USE_AO));
        setUseCal(Pio.getBooleanValue(paramSet, USE_CAL_PROP.getName(),
                DEFAULT_USE_CAL));
        setAoOptimize(Pio.getBooleanValue(paramSet, AO_OPTIMIZE_PROP.getName(),
                DEFAULT_AO_OPTIMIZE));
        setAlignFpmPinholeBias(Pio.getBooleanValue(paramSet, ALIGN_FPM_PINHOLE_BIAS_PROP.getName(),
                DEFAULT_ALIGN_FPM_PINHOLE_BIAS));
    }

    /**
     * Return the configuration for this component.
     */
    public ISysConfig getSysConfig() {
        ISysConfig sc = new DefaultSysConfig(SeqConfigNames.INSTRUMENT_CONFIG_NAME);

        // Fill in the current values.
        sc.putParameter(StringParameter.getInstance(ISPDataObject.VERSION_PROP, getVersion()));
        sc.putParameter(DefaultParameter.getInstance(COADDS_PROP, getCoadds()));
        sc.putParameter(DefaultParameter.getInstance(POS_ANGLE_PROP, getPosAngle()));
        sc.putParameter(DefaultParameter.getInstance(InstConstants.EXPOSURE_TIME_PROP, getExposureTime()));

        sc.putParameter(DefaultParameter.getInstance(ASTROMETRIC_FIELD_PROP, isAstrometricField()));
        sc.putParameter(DefaultParameter.getInstance(ADC_PROP, getAdc()));

        if (_observingModeOverride) {
            sc.putParameter(DefaultParameter.getInstance(OBSERVING_MODE_PROP, getObservingMode().getValue()));
            sc.putParameter(DefaultParameter.getInstance(FILTER_PROP, getFilter()));
            sc.putParameter(DefaultParameter.getInstance(APODIZER_PROP, getApodizer()));
            sc.putParameter(DefaultParameter.getInstance(LYOT_PROP, getLyot()));
            sc.putParameter(DefaultParameter.getInstance(FPM_PROP, getFpm()));
        } else {
            if (!getObservingMode().isEmpty()) {
                sc.putParameter(DefaultParameter.getInstance(OBSERVING_MODE_PROP, getObservingMode().getValue()));
            }
        }

        sc.putParameter(DefaultParameter.getInstance(DISPERSER_PROP, getDisperser()));
        sc.putParameter(DefaultParameter.getInstance(HALF_WAVE_PLATE_ANGLE_VALUE_PROP, getHalfWavePlateAngle()));
        sc.putParameter(DefaultParameter.getInstance(DETECTOR_STARTX_PROP, getDetectorStartX()));
        sc.putParameter(DefaultParameter.getInstance(DETECTOR_STARTY_PROP, getDetectorStartY()));
        sc.putParameter(DefaultParameter.getInstance(DETECTOR_ENDX_PROP, getDetectorEndX()));
        sc.putParameter(DefaultParameter.getInstance(DETECTOR_ENDY_PROP, getDetectorEndY()));

        sc.putParameter(DefaultParameter.getInstance(ENTRANCE_SHUTTER_PROP, getEntranceShutter()));
        sc.putParameter(DefaultParameter.getInstance(SCIENCE_ARM_SHUTTER_PROP, getScienceArmShutter()));
        sc.putParameter(DefaultParameter.getInstance(CAL_ENTRANCE_SHUTTER_PROP, getCalEntranceShutter()));
        sc.putParameter(DefaultParameter.getInstance(REFERENCE_ARM_SHUTTER_PROP, getReferenceArmShutter()));

        sc.putParameter(DefaultParameter.getInstance(IR_LASER_LAMP_PROP, getIrLaserLampEnum()));
        sc.putParameter(DefaultParameter.getInstance(VISIBLE_LASER_LAMP_PROP, getVisibleLaserLampEnum()));
        sc.putParameter(DefaultParameter.getInstance(SUPER_CONTINUUM_LAMP_PROP, getSuperContinuumLampEnum()));
        sc.putParameter(DefaultParameter.getInstance(ARTIFICIAL_SOURCE_ATTENUATION_PROP, getArtificialSourceAttenuation()));

        sc.putParameter(DefaultParameter.getInstance(PUPUL_CAMERA_PROP, getPupilCamera()));
        sc.putParameter(DefaultParameter.getInstance(DETECTOR_SAMPLING_MODE_PROP, getDetectorSamplingMode()));
        if (getDetectorSamplingMode() == DetectorSamplingMode.MULTIPLE_CDS && !getMcdsCount().isEmpty()) {
            sc.putParameter(DefaultParameter.getInstance(MCDS_COUNT_PROP, getMcdsCount().getValue()));
        }
        sc.putParameter(DefaultParameter.getInstance(ALWAYS_RESTORE_MODEL_PROP, isAlwaysRestoreModel()));
        sc.putParameter(DefaultParameter.getInstance(USE_AO_PROP, isUseAo()));
        sc.putParameter(DefaultParameter.getInstance(USE_CAL_PROP, isUseCal()));
        sc.putParameter(DefaultParameter.getInstance(AO_OPTIMIZE_PROP, isAoOptimize()));
        sc.putParameter(DefaultParameter.getInstance(ALIGN_FPM_PINHOLE_BIAS_PROP, isAlignFpmPinholeBias()));

        return sc;
    }

    /**
     * Return a list of InstConfigInfo objects describing the instrument's
     * queryable configuration parameters.
     */
    public static List<InstConfigInfo> getInstConfigInfo() {
        List<InstConfigInfo> configInfo = new LinkedList<>();

        configInfo.add(new InstConfigInfo(ASTROMETRIC_FIELD_ENUM_PROP));
        configInfo.add(new InstConfigInfo(DISPERSER_PROP));
        configInfo.add(new InstConfigInfo(ADC_PROP));
        configInfo.add(new InstConfigInfo(OBSERVING_MODE_PROP));
        configInfo.add(new InstConfigInfo(COADDS_PROP));
        configInfo.add(new InstConfigInfo(EXPOSURE_TIME_PROP));

        return configInfo;
    }

    public boolean hasGuideProbes() {
        // OT-93: GPI Target Environment should only allow base position and user targets
        return false;
    }

    private static final Collection<GuideProbe> ANTI_GUIDERS = GuideProbeUtil.instance.createCollection(
            GpiOiwfsGuideProbe.instance, PwfsGuideProbe.pwfs1, PwfsGuideProbe.pwfs2);

    /**
     * The GPI OIWFS should be linked to the base position, but that is not done here in the OT.
     * This definition specifies that OIWFS is not an option for GPI in the OT.
     * See OT-81.
     */
    @Override
    public Collection<GuideProbe> getConsumedGuideProbes() {
        return ANTI_GUIDERS;
    }

    @Override
    public CategorizedTimeGroup calc(Config cur, Option<Config> prev) {
        Collection<CategorizedTime> times = new ArrayList<>();

        // OT-95: component change times: 30 for 1, 60 for more than 1 change
        int numChanges = 0;

        // REL-2208 Halfwave plate changes add 5s instead of 30 seconds
        int halfWavePlateChanges = 0;
        final String halfWavePlateExchangeKey = HALFWAVE_PLATE_ANGLE_KEY.getName();

        for(ItemKey key : cur.getKeys()) {
            if (key.getName().equals(halfWavePlateExchangeKey)
                                && PlannedTime.isUpdated(cur, prev, key)) {
                halfWavePlateChanges++;
            } else if (key.getParent() != null && key.getParent().getName().equals(SeqConfigNames.INSTRUMENT_KEY.getName())
                    && PlannedTime.isUpdated(cur, prev, key)) {
                numChanges++;
            }
        }

        if (halfWavePlateChanges > 0) {
            // If there are changes add 5 secs
            times.add(CategorizedTime.fromSeconds(Category.CONFIG_CHANGE,
                    HALFWAVE_PLATE_CHANGE_OVERHEAD_SECS, "halfwave plate angle change"));
        }
        if (numChanges == 1) {
            times.add(CategorizedTime.fromSeconds(Category.CONFIG_CHANGE,
                    SINGLE_CHANGE_OVERHEAD_SECS, "single component change"));
        } else if (numChanges > 1) {
            times.add(CategorizedTime.fromSeconds(Category.CONFIG_CHANGE,
                    MULTI_CHANGE_OVERHEAD_SECS, "multiple component change"));
        }

        final int coadds = ExposureCalculator.instance.coadds(cur);

        final CategorizedTime readoutTime = CategorizedTime.fromSeconds(Category.READOUT, READOUT_OVERHEAD_SEC).add(READOUT_PER_EXPOSURE_MS * coadds);

        times.add(readoutTime);

        final double rawExposureTime = ExposureCalculator.instance.exposureTimeSec(cur);
        final double totalExposureTime = rawExposureTime * coadds;
        times.add(CategorizedTime.fromSeconds(Category.EXPOSURE, totalExposureTime));

        times.add(Category.DHS_OVERHEAD); // REL-1678

        return CommonStepCalculator.instance.calc(cur, prev).addAll(times);
    }

    /**
     * {@inheritDoc}
     */
    @Override public double[] getScienceArea() {
        // See OT-129
        return new double[]{SCIENCE_AREA_ARCSEC, SCIENCE_AREA_ARCSEC};
    }

    private static final Angle PWFS1_VIG = Angle.arcmins(5.3);
    private static final Angle PWFS2_VIG = Angle.arcmins(4.3);

    @Override public Angle pwfs1VignettingClearance() { return PWFS1_VIG; }
    @Override public Angle pwfs2VignettingClearance() { return PWFS2_VIG; }

}
