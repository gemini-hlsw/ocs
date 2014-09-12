//
// $Id: ObsQaState.java 7011 2006-05-04 16:12:21Z shane $
//

package edu.gemini.spModel.obs;

import edu.gemini.spModel.dataset.DatasetQaState;
import edu.gemini.spModel.dataset.DatasetQaStateSums;
import edu.gemini.spModel.type.DisplayableSpType;
import edu.gemini.spModel.type.SpTypeUtil;

/**
 * The ObsQaState represents the status of the observation QA as a whole.
 * This is independent of individual
 * {@link edu.gemini.spModel.dataset.DatasetQaState dataset QA state}s.  Even
 * if one or more datasets have a FAIL state, the observation as a whole may
 * pass.
 */
public enum ObsQaState implements DisplayableSpType {

    UNDEFINED("Undefined", "undefined") {
        public void doAction(Action action) {
            action.undefined();
        }
    },

    PASS("Pass", "pass") {
        public void doAction(Action action) {
            action.pass();
        }
    },

    USABLE("Usable", "usable") {
        public void doAction(Action action) {
            action.usable();
        }
    },

    FAIL("Fail", "fail") {
        public void doAction(Action action) {
            action.fail();
        }
    },
    ;

    /**
     * An interface for perfoming actions based upon a type value. Use of this
     * interface and the {@link edu.gemini.spModel.obs.ObsQaState#doAction}
     * method allows switch like statements on the possible values to be
     * avoided.
     */
    public interface Action {
        /** Performs the action associated with an UNDEFINED charge. */
        void undefined();

        /** Performs the action associated with a PASS charge. */
        void pass();

        /** Performs the action associated with a USABLE charge. */
        void usable();

        /** Performs the action associated with a FAIL charge. */
        void fail();
    }

    public static ObsQaState computeDefault(DatasetQaStateSums sums) {
        int total = sums.getTotalDatasets();
        if (total == 0) return ObsQaState.UNDEFINED;

        int undef = sums.getCount(DatasetQaState.UNDEFINED) +
                    sums.getCount(DatasetQaState.CHECK);
        if (undef > 0) return ObsQaState.UNDEFINED;

        int pass  = sums.getCount(DatasetQaState.PASS);
        if (total == pass) return ObsQaState.PASS;

        int fail  = sums.getCount(DatasetQaState.FAIL);
        if (total == fail) return ObsQaState.FAIL;

        return ObsQaState.USABLE;
    }

    private String _displayValue;
    private String _typeCode;

    private ObsQaState(String displayValue, String typeCode) {
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

    public abstract void doAction(ObsQaState.Action action);

    /**
     * Converts a String returned by the {@link #toString} method to
     * a DatasetCharge
     *
     * @return converted dataset charge, or <code>null</code> if the string
     * could not be converted
     *
     * @throws NullPointerException if <code>str</code> is <code>null</code>
     */
    public static ObsQaState parseType(String str) {
        ObsQaState res = SpTypeUtil.noExceptionValueOf(ObsQaState.class, str);
        if (res != null) return res;

        // For backwards compatibility (pre 2006B), check the type code.  Old
        // programs were stored with the type code.
        for (ObsQaState cur : values()) {
            if (cur.typeCode().equals(str)) return cur;
        }
        return null;
    }
}
