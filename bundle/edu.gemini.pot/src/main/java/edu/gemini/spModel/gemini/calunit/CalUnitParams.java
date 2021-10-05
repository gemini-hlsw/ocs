package edu.gemini.spModel.gemini.calunit;

import edu.gemini.shared.util.StringUtil;
import edu.gemini.spModel.type.DisplayableSpType;
import edu.gemini.spModel.type.ObsoletableSpType;
import edu.gemini.spModel.type.SequenceableSpType;
import edu.gemini.spModel.type.SpTypeUtil;

import java.util.*;
import java.util.stream.Collectors;

public final class CalUnitParams {
    public enum LampType {
        arc,
        flat
    }

    /**
     * Lamps
     */
    public enum Lamp implements DisplayableSpType, SequenceableSpType {
        IR_GREY_BODY_HIGH("IR grey body - high", "GCALflat", LampType.flat),
        IR_GREY_BODY_LOW("IR grey body - low", "GCALflat", LampType.flat),
        QUARTZ("Quartz Halogen", "GCALflat", LampType.flat),
        AR_ARC("Ar arc", "Ar", LampType.arc),
        THAR_ARC("ThAr arc", "ThAr", LampType.arc),
        CUAR_ARC("CuAr arc", "CuAr", LampType.arc),
        XE_ARC("Xe arc", "Xe", LampType.arc),
        ;

//        KR_ARC =
//                new Lamp("Kr arc", "Kr", true);

        /** The default Lamp value **/
        public static final Lamp DEFAULT = IR_GREY_BODY_HIGH;

        private final String _displayValue;

        // Name expected by the TCC software
        private final String _tccName;

        // Set to true for arcs
        private final LampType _type;

        Lamp(String displayValue, String tccName, LampType lampType) {
            _displayValue = displayValue;
            _tccName      = tccName;
            _type         = lampType;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        public LampType type() {
            return _type;
        }

        public String toString() {
            return _displayValue;
        }

        /** Return a Lamp by name or the default if not found. **/
        public static Lamp getLamp(String name) {
            return getLamp(name, Lamp.DEFAULT);
        }

        /** Return a Lamp by name, or the given default value if not found. **/
        public static Lamp getLamp(String name, Lamp def) {
            return SpTypeUtil.oldValueOf(Lamp.class, name, def);
        }

        /** Return a Lamp by index **/
        public static Lamp getLampByIndex(int index) {
            return SpTypeUtil.valueOf(Lamp.class, index, DEFAULT);
        }

        /** Return the name expected by the TCC software for this lamp */
        public String getTccName() {
            return _tccName;
        }

        /** Return true if the lamp is an arc */
        public boolean isArc() { return _type == LampType.arc; }

        private static List<Lamp> _flatLamps;
        private static List<Lamp> _arcLamps;

        private static List<Lamp> _getLamps(LampType lampType) {
            return Arrays.stream(values()).filter(l -> l.type() == lampType)
                    .collect(Collectors.toList());
        }

        public synchronized static List<Lamp> flatLamps() {
            if (_flatLamps == null) _flatLamps = _getLamps(LampType.flat);
            return _flatLamps;
        }

        public synchronized static List<Lamp> arcLamps() {
            if (_arcLamps == null) _arcLamps = _getLamps(LampType.arc);
            return _arcLamps;
        }

        public static String show(Collection<Lamp> lamps, StringUtil.MapToString<Lamp> mapper) {
            return StringUtil.mkString(lamps, "", ",", "", mapper);
        }

        public static boolean contains(Collection<Lamp> lamps, LampType type) {
            for (Lamp l : lamps) if (l.type() == type) return true;
            return false;
        }

        public static boolean containsArc(Collection<Lamp> lamps) {
            return contains(lamps, LampType.arc);
        }

        public static List<Lamp> read(String formattedList) {
            String[] ar = formattedList.split(",");
            return Arrays.stream(ar).map(Lamp::getLamp).collect(Collectors.toList());
        }

        public static String write(final SortedSet<Lamp> lamps) {
            return show(lamps, Lamp::name);
        }
    }

    /**
     * Filters
     */
    public enum Filter implements DisplayableSpType, ObsoletableSpType, SequenceableSpType {

        NONE("none"),
        ND_10("ND1.0"),
        ND_16("ND1.6") {
            @Override public boolean isObsolete() { return true; }
        },
        ND_20("ND2.0"),
        ND_30("ND3.0"),
        ND_40("ND4.0"),
        ND_45("ND4-5"),
        ND_50("ND5.0") {
            @Override public boolean isObsolete() { return true; }
        },
        GMOS("GMOS balance"),
        HROS("HROS balance") {
            @Override public boolean isObsolete() { return true; }
        },
        NIR("NIR balance"),

        ;

        /** The default Filter value **/
        public static final Filter DEFAULT = NONE;

        private final String _displayValue;

        Filter(String displayVal) {
            _displayValue = displayVal;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        public String toString() {
            return _displayValue;
        }

        /** Return a Filter by name or the default if not found. **/
        static public Filter getFilter(String name) {
            return getFilter(name, DEFAULT);
        }

        /** Return a Filter by name or the given default if not found. **/
        static public Filter getFilter(String name, Filter def) {
            return SpTypeUtil.oldValueOf(Filter.class, name, def);
        }

        /** Return a Filter by index **/
        static public Filter getFilterByIndex(int index) {
            return SpTypeUtil.valueOf(Filter.class, index, DEFAULT);
        }
    }


    /**
     * Diffusers
     */
    public enum Diffuser implements DisplayableSpType, SequenceableSpType {
        IR("IR"),
        VISIBLE("visible"),
        ;

        /** The default Diffuser value **/
        public static final Diffuser DEFAULT = IR;

        private final String _displayValue;

        Diffuser(String displayValue) {
            _displayValue = displayValue;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        public String toString() {
            return _displayValue;
        }

        /** Return a Diffuser by name or the default if not found. **/
        public static Diffuser getDiffuser(String name) {
            return getDiffuser(name, DEFAULT);
        }

        /** Return a Diffuser by name or the given default if not found **/
        public static Diffuser getDiffuser(String name, Diffuser def) {
            return SpTypeUtil.oldValueOf(Diffuser.class, name, def);
        }

        /** Return a Diffuser by index **/
        public static Diffuser getDiffuserByIndex(int index) {
            return SpTypeUtil.valueOf(Diffuser.class, index, DEFAULT);
        }
    }


    /**
     * IR grey body shutter
     */
    public enum Shutter implements DisplayableSpType, SequenceableSpType {

        OPEN("Open"),
        CLOSED("Closed"),
        ;

        /** The default Shutter value **/
        public static final Shutter DEFAULT = OPEN;

        private final String _displayValue;

        Shutter(String displayValue) {
            _displayValue = displayValue;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        public String toString() {
            return _displayValue;
        }

        /** Return a Shutter by name or the default if not found. **/
        public static Shutter getShutter(String name) {
            return getShutter(name, DEFAULT);
        }

        /** Return a Shutter by name or the given default if not found **/
        public static Shutter getShutter(String name, Shutter def) {
            return SpTypeUtil.oldValueOf(Shutter.class, name, def);
        }

        /** Return a Shutter by index **/
        public static Shutter getShutterByIndex(int index) {
            return SpTypeUtil.valueOf(Shutter.class, index, DEFAULT);
        }
    }
}


