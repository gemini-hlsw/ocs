//
// $Id: ChargeClass.java 7011 2006-05-04 16:12:21Z shane $
//

package edu.gemini.spModel.time;

import edu.gemini.spModel.type.DisplayableSpType;
import edu.gemini.spModel.type.SpTypeUtil;

/**
 * An enumerated type representing the entity who should be charged for a block
 * of time used on the telescope.
 */
public enum ChargeClass implements DisplayableSpType {

    /**
     * Time not charged.
     */
    NONCHARGED("Non-charged", "noncharged") {
        public void doAction(Action action) {
            action.nonCharged();
        }
    },

    /**
     * Time charged to a partner country/entity.
     */
    PARTNER("Partner", "partner") {
        public void doAction(Action action) {
            action.partner();
        }
    },

    /**
     * Time charged to the science program.
     */
    PROGRAM("Program", "program") {
        public void doAction(Action action) {
            action.program();
        }
    },
    ;


    /**
     * An interface for perfoming actions based upon a type value. Use of this
     * interface and the {@link edu.gemini.spModel.time.ChargeClass#doAction}
     * method allows switch like statements on the possible values to be
     * avoided.
     */
    public interface Action {

        /** Performs the action associated with a NONCHARGED charge. */
        void nonCharged();

        /** Performs the action associated with a PARTNER charge. */
        void partner();

        /** Performs the action associated with a PROGRAM charge. */
        void program();
    }

    /** The default value to use in the GUI */
    public static final ChargeClass DEFAULT = PROGRAM;

    private String _typeCode;
    private String _displayValue;

    private ChargeClass(String displayValue, String typeCode) {
        _typeCode     = typeCode;
        _displayValue = displayValue;
    }

    public abstract void doAction(ChargeClass.Action action);

    private String typeCode() {
        return _typeCode;
    }

    public String displayValue() {
        return _displayValue;
    }

    public String toString() {
        return _displayValue;
    }

    /**
     * Converts a String returned by the {@link #toString} method to
     * a ChargeClass
     *
     * @return converted charge, or <code>null</code> if the string
     * could not be converted
     *
     * @throws NullPointerException if <code>str</code> is <code>null</code>
     */
    public static ChargeClass parseType(String str) {
        ChargeClass cc = SpTypeUtil.noExceptionValueOf(ChargeClass.class, str);
        if (cc != null) return cc;

        // For backwards compatibility (pre 2006B), check the type code.  Old
        // programs were stored with the type code.
        for (ChargeClass cur : values()) {
            if (cur.typeCode().equals(str)) return cur;
        }
        return null;
    }
}
