//
// $Id: DatasetFileState.java 7011 2006-05-04 16:12:21Z shane $
//

package edu.gemini.spModel.dataset;

import edu.gemini.spModel.type.DisplayableSpType;
import edu.gemini.spModel.type.SpTypeUtil;

/**
 * The DatasetFileState represents the status of the dataset file in the
 * working directory.
 */
public enum DatasetFileState implements DisplayableSpType {

    OK("Okay", "ok") {
        public void doAction(Action action) {
            action.ok();
        }
    },

    MISSING("Missing", "missing") {
        public void doAction(Action action) {
            action.missing();
        }
    },

    BAD("Bad", "bad") {
        public void doAction(Action action) {
            action.bad();
        }
    },

    TENTATIVE("Tentative", "tentative") {
        public void doAction(Action action) {
            action.tentative();
        }
    },

    ;

    /**
     * An interface for perfoming actions based upon a type value. Use of this
     * interface and the {@link DatasetFileState#doAction} method allows switch
     * like statements on the possible values to be avoided.
     */
    public interface Action {
        /** Performs the action associated with a TENTATIVE state. */
        void tentative();

        /** Performs the action associated with an OK state. */
        void ok();

        /** Performs the action associated with a MISSING state. */
        void missing();

        /** Performs the action associated with a BAD state. */
        void bad();
    }

    private String _displayValue;
    private String _typeCode;

    private DatasetFileState(String displayValue, String typeCode) {
        _displayValue = displayValue;
        _typeCode     = typeCode;
    }

    public String displayValue() {
        return _displayValue;
    }

    private String typeCode() {
        return _typeCode;
    }

    public String toString() {
        return _displayValue;
    }

    public abstract void doAction(DatasetFileState.Action action);


    /**
     * Converts a String returned by the {@link #toString} method to
     * a DatasetFileState
     *
     * @return converted dataset charge, or <code>null</code> if the string
     * could not be converted
     *
     * @throws NullPointerException if <code>str</code> is <code>null</code>
     */
    public static DatasetFileState parseType(String str) {
        DatasetFileState res = SpTypeUtil.noExceptionValueOf(DatasetFileState.class, str);
        if (res != null) return res;

        // For backwards compatibility (pre 2006B), check the type code.  Old
        // programs were stored with the type code.
        for (DatasetFileState cur : values()) {
            if (cur.typeCode().equals(str)) return cur;
        }
        return null;}
}
