//
// $Id: ObsClass.java 44610 2012-04-18 18:57:38Z swalker $
//

package edu.gemini.spModel.obsclass;

import edu.gemini.spModel.time.ChargeClass;
import edu.gemini.spModel.type.DisplayableSpType;
import edu.gemini.spModel.type.LoggableSpType;
import edu.gemini.spModel.type.SequenceableSpType;

/**
 * The class of an observe and (considering all the observes in an observation)
 * of the observation itself.
 */
public enum ObsClass implements DisplayableSpType, LoggableSpType, SequenceableSpType {

    /**
     * Science observations, charged to the science program.
     */
    SCIENCE("science", "Science", 0, ChargeClass.PROGRAM, "SCI") {
        public void doAction(Action action) {
            action.science();
        }
        public boolean isCalibration() { return false; }
    },

    /**
     * Nighttime calibrations that are charged to the program.
     */
    PROG_CAL("progCal", "Nighttime Program Calibration", 1, ChargeClass.PROGRAM, "NCAL") {
        public void doAction(Action action) {
            action.progCal();
        }
        public boolean isCalibration() { return true; }
    },

    /**
     * Nighttime calibrations that are charged to the partner.
     */
    PARTNER_CAL("partnerCal", "Nighttime Partner Calibration", 2, ChargeClass.PARTNER, "PCAL") {
        public void doAction(Action action) {
            action.partnerCal();
        }
        public boolean isCalibration() { return true; }
    },

    /**
     * Acquisition, charged to science program.
     */
    ACQ("acq", "Acquisition", 3, ChargeClass.PROGRAM, "ACQ") {
        public void doAction(Action action) {
            action.acq();
        }
        public boolean isCalibration() { return false; }
    },

    /**
     * Acquisition Calibration, charged to science program.
     */
    ACQ_CAL("acqCal", "Acquisition Calibration", 4, ChargeClass.PARTNER, "ACAL") {
        public void doAction(Action action) {
            action.acqCal();
        }
        public boolean isCalibration() { return true; }
    },

    /**
     * Daytime calibration, charged to Gemini.
     */
    DAY_CAL("dayCal", "Daytime Calibration", 5, ChargeClass.NONCHARGED, "DCAL") {
        public void doAction(Action action) {
            action.dayCal();
        }
        public boolean isCalibration() { return true; }
    },

    ;

    /**
     * An interface for perfoming actions based upon a type value. Use of this
     * interface and the {@link ObsClass#doAction(edu.gemini.spModel.obsclass.ObsClass.Action)}
     * method allows switch like statements on the possible values to be
     * avoided.
     */
    public interface Action {
        /** Performs the action associated with a SCIENCE class. */
        void science();

        /** Performs the action associated with a PROG_CAL class. */
        void progCal();

        /** Performs the action associated with a PARTNER_CAL class. */
        void partnerCal();

        /** Performs the action associated with an ACQ class. */
        void acq();

        /** Performs the action associated with an ACQ_CAL class. */
        void acqCal();

        /** Performs the action associated with a DAY_CAL class. */
        void dayCal();
    }

    private String _headerValue;
    private String _displayValue;
    private int _priority;
    private ChargeClass _charge;
    private String _logValue;


    private ObsClass(String headerVal, String displayVal, int priority, ChargeClass charge, String logValue) {
        _headerValue  = headerVal;
        _displayValue = displayVal;
        _priority     = priority;
        _charge       = charge;
        _logValue     = logValue;
    }

    public abstract void doAction(ObsClass.Action action);
    public abstract boolean isCalibration();

    public String displayValue() {
        return _displayValue;
    }

    public String headerValue() {
        return _headerValue;
    }

    public String sequenceValue() {
        return _headerValue;
    }

    /**
     * Gets the priority of this observe class compared to others.  May be used,
     * for example, in selecting the highest priority among a collection of
     * ObsClass objects.  For instance, the observation's ObsClass is the
     * highest priority ObsClass of any of its observe iterators.
     *
     * @return integer priority of this observe class
     */
    public int getPriority() {
        return _priority;
    }

    /**
     * Gets the default charge class for this observe class.
     */
    public ChargeClass getDefaultChargeClass() {
        return _charge;
    }

    /**
     * Gets abbreviated "log value", which appears in the observing log, for
     * this ObsClass.
     */
    public String logValue() {
        return _logValue;
    }

    public String toString() {
        return displayValue();
    }

    /**
     * This method is needed by the data manager and was added to merge the FITS ObsClass with this one.
     * The only programs that are charged are those in ChargeClass PROGRAM.
     * @return
     */
    public boolean shouldChargeProgram() {
        return _charge.equals(ChargeClass.PROGRAM);
    }


    /**
     * Converts a string name into an ObsClass.  This is like the builtin
     * valueOf() method on enum, but returns <code>null</code> instead of
     * throwing an exception if it is an unknown string and handles backwards
     * compatibility with pre 2006B programs.
     *
     * @param str string to convert to an ObsClass
     *
     * @return ObsClass associated with the <code>str</code> argument
     */
    public static ObsClass parseType(String str) {
        try {
            return valueOf(str);
        } catch (Exception ex) {
            // ignore
        }

        // handle backwards compatibility
        for (ObsClass oc : values()) {
            if (str.equals(oc.headerValue())) return oc;
        }
        return null;
    }
}
