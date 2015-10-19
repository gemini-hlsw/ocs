//
// $Id: DatasetQaState.java 7011 2006-05-04 16:12:21Z shane $
//

package edu.gemini.spModel.dataset;

import edu.gemini.spModel.type.DisplayableSpType;
import edu.gemini.spModel.type.SpTypeUtil;

/**
 * The DatasetQaState represents the status of the dataset with respect to
 * time accounting.
 */
public enum DatasetQaState implements DisplayableSpType {
    UNDEFINED("Undefined", "undefined", false) {
        public void doAction(Action action) {
            action.undefined();
        }
    },

    PASS("Pass", "pass", true) {
        public void doAction(Action action) {
            action.pass();
        }
    },

    USABLE("Usable", "usable", true) {
        public void doAction(Action action) {
            action.usable();
        }
    },

    FAIL("Fail", "fail", true) {
        public void doAction(Action action) {
            action.fail();
        }
    },

    CHECK("Check", "check", false) {
        public void doAction(Action action) {
            action.check();
        }
    },

    ;

    /**
     * An interface for perfoming actions based upon a type value. Use of this
     * interface and the {@link DatasetQaState#doAction} method allows switch
     * like statements on the possible values to be avoided.
     */
    public interface Action {
        /** Performs the action associated with an UNDEFINED state. */
        void undefined();

        /** Performs the action associated with a CHECK state. */
        void check();

        /** Performs the action associated with a PASS state. */
        void pass();

        /** Performs the action associated with a USABLE state. */
        void usable();

        /** Performs the action associated with a FAIL state. */
        void fail();
    }

    private boolean _isFinal;
    private String _displayValue;
    private String _typeCode;

    DatasetQaState(String displayValue, String typeCode, boolean isFinal) {
        _displayValue = displayValue;
        _typeCode     = typeCode;
        _isFinal      = isFinal;
    }

    public String displayValue() {
        return _displayValue;
    }

    private String typeCode() {
        return _typeCode;
    }

    /**
     * Returns <code>true</code> if this QA state is considered "final".  In
     * other words, it may be changed to something else in the future but is
     * not a temporary, transient QA state that is intended to be set to a
     * different state in the future.
     */
    public boolean isFinal() {
        return _isFinal;
    }

    public abstract void doAction(DatasetQaState.Action action);

    public String toString() {
        return _displayValue;
    }

    /**
     * Converts a String returned by the {@link #toString} method to
     * a DatasetCharge
     *
     * @return converted dataset charge, or <code>null</code> if the string
     * could not be converted
     *
     * @throws NullPointerException if <code>str</code> is <code>null</code>
     */
    public static DatasetQaState parseType(String str) {
        DatasetQaState res = SpTypeUtil.noExceptionValueOf(DatasetQaState.class, str);
        if (res != null) return res;

        // For backwards compatibility (pre 2006B), check the type code.  Old
        // programs were stored with the type code.
        for (DatasetQaState cur : values()) {
            if (cur.typeCode().equals(str)) return cur;
        }
        return null;
    }
}
