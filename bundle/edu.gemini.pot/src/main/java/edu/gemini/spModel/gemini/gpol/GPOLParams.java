// Copyright 1997-2002
// Association for Universities for Research in Astronomy, Inc.,
//
// $Id: GPOLParams.java 7083 2006-05-26 22:44:42Z shane $
//
package edu.gemini.spModel.gemini.gpol;

import edu.gemini.spModel.type.DisplayableSpType;
import edu.gemini.spModel.type.LoggableSpType;
import edu.gemini.spModel.type.SequenceableSpType;
import edu.gemini.spModel.type.SpTypeUtil;

public final class GPOLParams {
    /**
     * Calibrator
     */
    public static enum Calibrator implements DisplayableSpType, LoggableSpType, SequenceableSpType {
        NONE("None"),
        WIRE_GRID("Wire-grid"),
        UV_POLAROID("UV polaroid"),
        OPTICAL_POLAROID("Optical polaroid"),
        ;

        /** The default Calibrator value **/
        public static Calibrator DEFAULT = NONE;

        private String _displayValue;

        private Calibrator(String displayValue) {
            _displayValue = displayValue;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String logValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        /** Return a Calibrator by name or the default if not found. **/
        public static Calibrator getCalibrator(String name) {
            return getCalibrator(name, DEFAULT);
        }

        /** Return a Calibrator by name or the given default if not found **/
        public static Calibrator getCalibrator(String name, Calibrator def) {
            return SpTypeUtil.oldValueOf(Calibrator.class, name, def);
        }

        /** Return a Calibrator by index **/
        public static Calibrator getCalibratorByIndex(int index) {
            return SpTypeUtil.valueOf(Calibrator.class, index, DEFAULT);
        }
    }

    /**
     * Modulator
     */
    public static enum Modulator implements DisplayableSpType, LoggableSpType, SequenceableSpType {

        OPTICAL_NEAR_IR("Optical/near-IR"),
        THERMAL_IR("Thermal IR"),
        ;

        /** The default Modulator value **/
        public static Modulator DEFAULT = OPTICAL_NEAR_IR;

        private String _displayValue;

        private Modulator(String name) {
            _displayValue = name;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String logValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        /** Return a Modulator by name or the default if not found. **/
        public static Modulator getModulator(String name) {
            return getModulator(name, DEFAULT);
        }

        /** Return a Modulator by name, or the given default value if not found. **/
        public static Modulator getModulator(String name, Modulator def) {
            return SpTypeUtil.oldValueOf(Modulator.class, name, def);
        }

        /** Return a Modulator by index **/
        public static Modulator getModulatorByIndex(int index) {
            return SpTypeUtil.valueOf(Modulator.class, index, DEFAULT);
        }
    }

    /**
     * Angle
     */
    public static enum Angle implements DisplayableSpType, LoggableSpType, SequenceableSpType {
        ANGLE1("0.0"),
        ANGLE2("45.0"),
        ANGLE3("22.5"),
        ANGLE4("67.5"),
        ;

        /** The default Angle value **/
        public static Angle DEFAULT = ANGLE1;

        private String _displayValue;

        private Angle(String name) {
            _displayValue = name;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String logValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        /** Return a Angle by name or the default if not found. **/
        public static Angle getAngle(String name) {
            return getAngle(name, DEFAULT);
        }

        /** Return a Angle by name, or the given default value if not found. **/
        public static Angle getAngle(String name, Angle def) {
            return SpTypeUtil.oldValueOf(Angle.class, name, def);
        }

        /** Return a Angle by index **/
        public static Angle getAngleByIndex(int index) {
            return SpTypeUtil.valueOf(Angle.class, index, DEFAULT);
        }
    }
}


