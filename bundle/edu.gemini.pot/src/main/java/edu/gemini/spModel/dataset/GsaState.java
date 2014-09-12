//
// $Id: GsaState.java 11857 2008-07-30 15:38:42Z swalker $
//

package edu.gemini.spModel.dataset;

import edu.gemini.spModel.type.DisplayableSpType;
import edu.gemini.spModel.type.SpTypeUtil;

/**
 * The GsaState type represents where a dataset is in the GSA transfer
 * process.
 */
public enum GsaState implements DisplayableSpType {

    /**
     * The state of a newly discovered dataset, which is presumably unknown
     * to the GSA.
     */
    NONE("None", "none") {
        public void doAction(Action action) {
            action.none();
        }
    },

    /**
     * Pending GSA processing.
     */
    PENDING("Pending", "pending") {
        public void doAction(Action action) {
            action.pending();
        }
    },

    /**
     * Copying the dataset to the GSA transfer machine.
     */
    COPYING("Copying File", "copying", true) {
        public void doAction(Action action) {
            action.copying();
        }
    },

    /**
     * Copy of the dataset to the GSA transfer machine failed.
     */
    COPY_FAILED("Copy Failed", "copyFailed") {
        public void doAction(Action action) {
            action.copyFailed();
        }
    },

    /**
     * Verifying the acceptability of the dataaset using GSA provided software.
     */
    VERIFYING("Verifying File", "verifying", true) {
        public void doAction(Action action) {
            action.verifying();
        }
    },

    /**
     * GSA software does not accept the file.
     */
    VERIFY_FAILED("Verify Failed", "verifyFailed") {
        public void doAction(Action action) {
            action.verifyFailed();
        }
    },

    /**
     * Dataset is queued waiting for GSA software to pick it up and move it
     * to the GSA.
     */
    QUEUED("Queued", "queued") {
        public void doAction(Action action) {
            action.queued();
        }
    },

    /**
     * Dataset is being transferred to the GSA.
     */
    TRANSFERRING("Transferring", "transferring", true) {
        public void doAction(Action action) {
            action.transferring();
        }
    },

    /**
     * Could not determine the status of the file in the GSA for some reason,
     * or the GSA claims to have accepted the file but its CRC does not match.
     */
    TRANSFER_ERROR("Transfer Error", "error") {
        public void doAction(Action action) {
            action.error();
        }
    },

    /**
     * Despite having passed local validation, the GSA software is reporting
     * that the file has not been accepted for some reason.
     */
    REJECTED("Rejected", "rejected") {
        public void doAction(Action action) {
            action.rejected();
        }
    },

    /**
     * The file has been accepted in the GSA and its CRC matches.
     */
    ACCEPTED("Accepted", "accepted") {
        public void doAction(Action action) {
            action.accepted();
        }
    },
    ;

    /**
     * An interface for perfoming actions based upon a type value. Use of this
     * interface and the {@link GsaState#doAction} method allows switch
     * like statements on the possible values to be avoided.
     */
    public interface Action {
        /** Performs the action associated with a NONE state. */
        void none();

        /** Performs the action associated with the PENDING state. */
        void pending();

        /** Performs the action associated with a COPYING state. */
        void copying();

        /** Performs the action associated with a COPY_FAILED state. */
        void copyFailed();

        /** Performs the action associated with a VERIFYING state. */
        void verifying();

        /** Performs the action associated with a VERIFY_FAILED state. */
        void verifyFailed();

        /** Performs the action associated with a QUEUED state. */
        void queued();

        /** Performs the action associated with a TRANSFERRING state. */
        void transferring();

        /** Performs the action associated with a ERROR state. */
        void error();

        /** Performs the action associated with a REJECTED state. */
        void rejected();

        /** Performs the action associated with a ACCEPTED state. */
        void accepted();
    }


    private String _displayValue;
    private String _typeCode;
    private boolean _isTemporary;

    private GsaState(String displayValue, String typeCode) {
        this(displayValue, typeCode, false);
    }

    private GsaState(String displayValue, String typeCode, boolean isTemporary) {
        _displayValue = displayValue;
        _typeCode     = typeCode;
        _isTemporary  = isTemporary;
    }

    public abstract void doAction(GsaState.Action action);

    /**
     * Determines whether this GsaState is temporary.  Some GsaStates are
     * considered temporary, meaning that the software will automatically transition
     * them to a new GsaState without user action.
     *
     * @return <code>true</code> if this is a temporary state; <code>false</code>
     * otherwise
     */
    public boolean isTemporary() {
        return _isTemporary;
    }

    public String displayValue() {
        return _displayValue;
    }

    private String typeCode() {
        return _typeCode;
    }

    public String toString() {
        return displayValue();
    }

    /**
     * Converts a String returned by the {@link #toString} method to
     * a DataflowStatus
     *
     * @return converted dataset charge, or <code>null</code> if the string
     * could not be converted
     *
     * @throws NullPointerException if <code>str</code> is <code>null</code>
     */
    public static GsaState parseType(String str) {
        GsaState res = SpTypeUtil.noExceptionValueOf(GsaState.class, str);
        if (res != null) return res;

        // After the 2007B release, two GsaStates were taken away for
        // SCT-333 (and others were added).  Handle the two GsaStates that were
        // removed.  They were LIMBO and LIMBO_UPDATE, which map most closely to
        // TRANSFERRING now.
        if ("LIMBO".equals(str) || "LIMBO_UPDATE".equals(str)) {
            return TRANSFERRING;
        }

        // For backwards compatibility (pre 2006B), check the type code.  Old
        // programs were stored with the type code.
        for (GsaState cur : values()) {
            if (cur.typeCode().equals(str)) return cur;
        }
        return null;
    }
}
