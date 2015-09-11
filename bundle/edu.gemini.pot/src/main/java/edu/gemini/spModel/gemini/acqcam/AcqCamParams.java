// Copyright 1997-2000
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: AcqCamParams.java 7038 2006-05-17 14:24:43Z gillies $
//
package edu.gemini.spModel.gemini.acqcam;

import edu.gemini.spModel.type.DisplayableSpType;
import edu.gemini.spModel.type.SequenceableSpType;
import edu.gemini.spModel.type.SpTypeUtil;

/**
 * This class provides data types for the AcqCam components.
 */
public final class AcqCamParams {


    /**
     * ColorFilters
     */
    public static enum ColorFilter implements DisplayableSpType, SequenceableSpType {

        // Note: neutral is using a default value that probably is not too useful
        NEUTRAL("Neutral", ".500"),
        U_G0151("U_G0151", ".366"),
        B_G0152("B_G0152", ".425"),
        V_G0153("V_G0153", ".534"),
        R_G0154("R_G0154", ".636"),
        I_G0155("I_G0155", ".798"),
        ;

        /** The default ColorFilter value **/
        public static ColorFilter DEFAULT = R_G0154;

        // String value for central wavelength, used by TCC
        private String _wavelength;
        private String _displayValue;

        private ColorFilter(String displayValue, String wavelength) {
            _displayValue = displayValue;
            _wavelength = wavelength;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        /**
         * Return the value for the central wavelength as a String
         */
        public String getCentralWavelength() {
            return _wavelength;
        }

        /** Return a ColorFilter by index **/
        static public ColorFilter getColorFilterByIndex(int index) {
            return SpTypeUtil.valueOf(ColorFilter.class, index, DEFAULT);
        }

        /** Return a ColorFilter by name **/
        static public ColorFilter getColorFilter(String name) {
            return getColorFilter(name, DEFAULT);
        }

        /** Return a ColorFilter by name giving a value to return upon error **/
        static public ColorFilter getColorFilter(String name, ColorFilter nvalue) {
            return SpTypeUtil.oldValueOf(ColorFilter.class, name, nvalue);
        }
    }

    /** An array holding the String names of the ColorFilters **/
    /**
    public static String[] COLOR_FILTER_TYPES = {
        ColorFilter.NEUTRAL.name(),
        ColorFilter.U_G0151.name(),
        ColorFilter.B_G0152.name(),
        ColorFilter.V_G0153.name(),
        ColorFilter.R_G0154.name(),
        ColorFilter.I_G0155.name()
    };
     **/


    /**
     * Neutral Density Filters
     */
    public static enum NDFilter implements DisplayableSpType, SequenceableSpType {

        NEUTRAL("Neutral"),
        ND001_G0156("ND001_G0156"),
        ND01_G0157("ND01_G0157"),
        ND1_G0158("ND1_G0158"),
        ND25_G0159("ND25_G0159"),
        OPEN("Open"),
        ;

        /** The default NDFilter value **/
        public static NDFilter DEFAULT = OPEN;
        private String _displayValue;

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        private NDFilter(String displayValue) {
            _displayValue = displayValue;
        }

        /** Return a NDFilter by index **/
        static public NDFilter getNDFilterByIndex(int index) {
            return SpTypeUtil.valueOf(NDFilter.class, index, DEFAULT);
        }

        /** Return a NDFilter by name **/
        static public NDFilter getNDFilter(String name) {
            return getNDFilter(name, DEFAULT);
        }

        /** Return a NDFilter by name giving a value to return upon error **/
        static public NDFilter getNDFilter(String name, NDFilter nvalue) {
            return SpTypeUtil.oldValueOf(NDFilter.class, name, nvalue);
        }
    }

    /** An array holding the String names of the NDFilters **/
    /**
    public static String[] NDFILTER_TYPES = {
        NDFilter.NEUTRAL.name(),
        NDFilter.ND001_G0156.name(),
        NDFilter.ND01_G0157.name(),
        NDFilter.ND1_G0158.name(),
        NDFilter.ND25_G0159.name()
    };
**/

    /**
     * Binning values.
     */
    public static enum Binning implements DisplayableSpType, SequenceableSpType {

        OFF("Off"),
        ON("On"),
        ;

        /** The default Binning value **/
        public static Binning DEFAULT = OFF;
        private String _displayValue;

        private Binning(String displayValue) {
            _displayValue = displayValue;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        /** Return a Binning by index **/
        static public Binning getBinningByIndex(int index) {
            return SpTypeUtil.valueOf(Binning.class, index, DEFAULT);
        }

        /** Return a Binning by name **/
        static public Binning getBinning(String name) {
            return getBinning(name, DEFAULT);
        }

        /** Return a Binning by name giving a value to return upon error **/
        static public Binning getBinning(String name, Binning nvalue) {
            return SpTypeUtil.oldValueOf(Binning.class, name, nvalue);
        }
    }


    /**
     * Lens values.
     */
    public static enum Lens implements DisplayableSpType, SequenceableSpType {

        AC("Acquisition Camera"),
        HRWFS("High Resolution Wavefront Sensor"),
        ;

        /** The default Lens value **/
        public static Lens DEFAULT = AC;
        private String _displayValue;

        private Lens(String displayValue) {
            _displayValue = displayValue;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        /** Return a Lens by index **/
        static public Lens getLensByIndex(int index) {
            return SpTypeUtil.valueOf(Lens.class, index, DEFAULT);
        }

        /** Return a Lens by name **/
        static public Lens getLens(String name) {
            return getLens(name, DEFAULT);
        }

        /** Return a Lens by name giving a value to return upon error **/
        static public Lens getLens(String name, Lens nvalue) {
            return SpTypeUtil.oldValueOf(Lens.class, name, nvalue);
        }
    }


    /**
     * Windowing values.
     */
    public static enum Windowing implements DisplayableSpType, SequenceableSpType {

        OFF("Off"),
        ON("On"),
        ;

        /** The default Windowing value **/
        public static Windowing DEFAULT = OFF;
        private String _displayValue;

        private Windowing(String displayValue) {
            _displayValue = displayValue;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        /** Return a Windowing by index **/
        static public Windowing getWindowingByIndex(int index) {
            return SpTypeUtil.valueOf(Windowing.class, index, DEFAULT);
        }

        /** Return a Windowing by name **/
        static public Windowing getWindowing(String name) {
            return getWindowing(name, DEFAULT);
        }

        /** Return a Windowing by name giving a value to return upon error **/
        static public Windowing getWindowing(String name, Windowing nvalue) {
            return SpTypeUtil.oldValueOf(Windowing.class, name, nvalue);
        }
    }


    /**
     * Overscan region sizes.
     */
    public static enum Overscan implements DisplayableSpType, SequenceableSpType {

        ONE("1"),
        TWO("2"),
        THREE("3"),
        FOUR("4"),
        FIVE("5"),
        SIX("6"),
        SEVEN("7"),
        EIGHT("8"),
        ;

        public static final Overscan DEFAULT = ONE;
        private String _displayValue;

        private Overscan(String displayValue) {
            _displayValue = displayValue;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        /** Return an Overscan by name **/
        static public Overscan getOverscan(String name) {
            return getOverscan(name, DEFAULT);
        }

        /** Return an Overscan by name giving a value to return upon error **/
        static public Overscan getOverscan(String name, Overscan nvalue) {
            return SpTypeUtil.oldValueOf(Overscan.class, name, nvalue);
        }

        /** Return an Overscan by index **/
        static public Overscan getOverscanByIndex(int index) {
            return SpTypeUtil.valueOf(Overscan.class, index, DEFAULT);
        }
    }

    /**
     * Cass Rotator
     */
    public static enum CassRotator implements DisplayableSpType, SequenceableSpType {

        FOLLOWING("Following"),
        FIXED("Fixed"),
        ;

        /** The default CassRotator value **/
        public static final CassRotator DEFAULT = FOLLOWING;
        private String _displayValue;

        private CassRotator(String displayValue) {
            _displayValue = displayValue;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }
        
        /** Return an CassRotator by name **/
        static public CassRotator getCassRotator(String name) {
            return getCassRotator(name, DEFAULT);
        }

        /** Return an CassRotator by name giving a value to return upon error **/
        static public CassRotator getCassRotator(String name, CassRotator value) {
            return SpTypeUtil.oldValueOf(CassRotator.class, name, value);
        }
    }
}


