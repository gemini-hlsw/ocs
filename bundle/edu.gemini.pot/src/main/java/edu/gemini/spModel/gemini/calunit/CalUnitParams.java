// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: CalUnitParams.java 38751 2011-11-16 19:37:18Z swalker $
//
package edu.gemini.spModel.gemini.calunit;

import edu.gemini.shared.util.StringUtil;
import edu.gemini.shared.util.StringUtil.MapToString;

import edu.gemini.spModel.type.DisplayableSpType;
import edu.gemini.spModel.type.ObsoletableSpType;
import edu.gemini.spModel.type.SequenceableSpType;
import edu.gemini.spModel.type.SpTypeUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class CalUnitParams {
    public static enum LampType {
        arc,
        flat;
    }

    /**
     * Lamps
     */
    public static enum Lamp implements DisplayableSpType, SequenceableSpType {
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
        public static Lamp DEFAULT = IR_GREY_BODY_HIGH;

        private String _displayValue;

        // Name expected by the TCC software
        private String _tccName;

        // Set to true for arcs
        private LampType _type;

        private Lamp(String displayValue, String tccName, LampType lampType) {
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
            List<Lamp> res = new ArrayList<Lamp>();
            for (Lamp l : values()) {
                if (l.type() == lampType) res.add(l);
            }
            return res;
        }

        public synchronized static List<Lamp> flatLamps() {
            if (_flatLamps == null) _flatLamps = _getLamps(LampType.flat);
            return _flatLamps;
        }

        public synchronized static List<Lamp> arcLamps() {
            if (_arcLamps == null) _arcLamps = _getLamps(LampType.arc);
            return _arcLamps;
        }

        public static MapToString<Lamp> NAME_MAPPER = new MapToString<Lamp>() {
            @Override public String apply(Lamp l) { return l.name(); }
        };

        public static MapToString<Lamp> DISPLAY_MAPPER = new MapToString<Lamp>() {
            @Override public String apply(Lamp l) { return l.displayValue(); }
        };

        public static MapToString<Lamp> TCC_MAPPER = new MapToString<Lamp>() {
            @Override public String apply(Lamp l) { return l.getTccName(); }
        };

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
            List<Lamp> lst = new ArrayList<Lamp>(ar.length);
            for (String anAr : ar) lst.add(Lamp.getLamp(anAr));
            return lst;
        }
    }

    /**
     * Filters
     */
    public static enum Filter implements DisplayableSpType, ObsoletableSpType, SequenceableSpType {

        NONE("none"),
        ND_10("ND1.0"),
        ND_16("ND1.6", true),
        ND_20("ND2.0"),
        ND_30("ND3.0"),
        ND_40("ND4.0"),
        ND_45("ND4-5"),
        ND_50("ND5.0", true),
        GMOS("GMOS balance"),
        HROS("HROS balance", true),
        NIR("NIR balance"),

        ;

        /** The default Filter value **/
        public static Filter DEFAULT = NONE;

        private String _displayValue;
        private boolean _obsolete;

        private Filter(String displayVal) {
            this(displayVal, false);
        }

        private Filter(String displayVal, boolean obsolete) {
            _displayValue = displayVal;
            _obsolete     = obsolete;
        }

        public String displayValue() {
            return _displayValue;
        }

        public boolean isObsolete() {
            return _obsolete;
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
    public static enum Diffuser implements DisplayableSpType, SequenceableSpType {
        IR("IR"),
        VISIBLE("visible"),
        ;

        /** The default Diffuser value **/
        public static Diffuser DEFAULT = IR;

        private String _displayValue;

        private Diffuser(String displayValue) {
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
    public static enum Shutter implements DisplayableSpType, SequenceableSpType {

        OPEN("Open"),
        CLOSED("Closed"),
        ;

        /** The default Shutter value **/
        public static Shutter DEFAULT = OPEN;

        private String _displayValue;

        private Shutter(String displayValue) {
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


