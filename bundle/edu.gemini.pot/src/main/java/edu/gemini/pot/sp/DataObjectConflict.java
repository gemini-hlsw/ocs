package edu.gemini.pot.sp;

import edu.gemini.spModel.data.ISPDataObject;

import java.io.Serializable;

/**
 * Contains an alternative version of a data object along with an indication
 * of whether this version is the local/client version or the remote/server
 * version.
 */
public final class DataObjectConflict implements Serializable {
    public enum Perspective {
        LOCAL,
        REMOTE;

        /**
         * Obtains the opposite perspective.  If this is a LOCAL perspective,
         * returns REMOTE and vice versa.
         */
        public Perspective opposite() { return this == LOCAL ? REMOTE : LOCAL; }
    }

    public final Perspective perspective;
    public final ISPDataObject dataObject;

    public DataObjectConflict(Perspective p, ISPDataObject o) {
        this.perspective = p;
        this.dataObject  = o;
    }

    @Override public String toString() {
        return "DataObjectConflict: " + perspective;
    }
}
