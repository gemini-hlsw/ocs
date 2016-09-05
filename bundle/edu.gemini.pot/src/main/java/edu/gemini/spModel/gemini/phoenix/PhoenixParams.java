package edu.gemini.spModel.gemini.phoenix;

import edu.gemini.spModel.type.DisplayableSpType;
import edu.gemini.spModel.type.SequenceableSpType;
import edu.gemini.spModel.type.SpTypeUtil;
import edu.gemini.spModel.type.ObsoletableSpType;

/**
 * This class provides data types for the Phoenix components.
 */
public final class PhoenixParams {

    // Make the constructor private.
    private PhoenixParams() {}

    /**
     * Class for Filters.
     */
    public enum Filter implements DisplayableSpType, SequenceableSpType, ObsoletableSpType {

        M1930("M1930"),
        M2030("M2030"),
        M2150("M2150"),

        L2462("L2462"),
        L2734("L2734"),
        L2870("L2870"),
        L3010("L3010"),
        L3100("L3100"),
        L3290("L3290"),

        K4132("K4132") {
            @Override public boolean isObsolete() { return true; }
        },
        K4220("K4220"),
        K4308("K4308"),
        K4396("K4396"),
        K4484("K4484"),
        K4578("K4578"),
        K4667("K4667"),
        K4748("K4748"),

        H6073("H6073"),
        H6420("H6420"),

        J7799("J7799"),
        J8265("J8265") {
            @Override public boolean isObsolete() { return true; }
        },
        J9232("J9232"),
        J9440("J9440"),
        J9671("J9671") {
            @Override public boolean isObsolete() { return true; }
        },
        ;

        /** The default Filter value **/
        public final static Filter DEFAULT = Filter.K4396;

        private final String _displayValue;

        Filter(final String displayValue) {
            _displayValue = displayValue;

        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        /** Return the Filter by searching through the known types. **/
        public static Filter getFilter(final String name) {
            return getFilter(name, DEFAULT);
        }

        /** Return a Filter by name giving a value to return upon error **/
        static public Filter getFilter(final String name, final Filter nvalue) {
            return SpTypeUtil.oldValueOf(Filter.class, name, nvalue);
        }

        /** Return a Filter by index **/
        static public Filter getFilterByIndex(final int index) {
            return SpTypeUtil.valueOf(Filter.class, index, DEFAULT);
        }
    }


    /**
     * Masks
     */
    public enum Mask implements DisplayableSpType, SequenceableSpType {

        MASK_1("0.17 arcsec slit", 0.17),
        MASK_2("0.25 arcsec slit", 0.25),
        MASK_3("0.34 arcsec slit", 0.34),
        ;

        // the length of the mask
        public static final double SLIT_LENGTH = 14.0;

        /* The default Mask value */
        public static final Mask DEFAULT = MASK_3;

        private final double _slitWidth;
        private final String _displayValue;

        Mask(final String displayValue, final double slitWidth) {
            _displayValue = displayValue;
            _slitWidth = slitWidth;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        /** Return a Mask by index **/
        static public Mask getMaskByIndex(final int index) {
            return SpTypeUtil.valueOf(Mask.class, index, DEFAULT);
        }

        /** Return a Mask by name giving a value to return upon error **/
        static public Mask getMask(final String name, final Mask nvalue) {
            return SpTypeUtil.oldValueOf(Mask.class, name, nvalue);
        }

        public double getSlitWidth() {
            return _slitWidth;
        }
    }

}


