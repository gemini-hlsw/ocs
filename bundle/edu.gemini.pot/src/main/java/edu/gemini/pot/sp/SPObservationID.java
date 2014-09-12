//
// $Id: SPObservationID.java 7324 2006-08-25 21:21:11Z shane $
//

package edu.gemini.pot.sp;

import edu.gemini.spModel.core.SPBadIDException;
import edu.gemini.spModel.core.SPProgramID;

import java.io.Serializable;


public final class SPObservationID implements Comparable, Serializable {
    static final long serialVersionUID = 3888785156260179952L;

    private static final String SEP = "-";

    private SPProgramID _programID;
    private int _obsNumber;

    public SPObservationID(SPProgramID programID, int obsNumber)
            throws SPBadIDException {
        _programID = programID;
        _obsNumber = obsNumber;

        if (obsNumber < 0) {
            throw new SPBadIDException("obsNumber cannot be negative: " + obsNumber);
        }
    }

    public SPObservationID(String observationID) throws SPBadIDException {
        int sepPos = observationID.lastIndexOf(SEP);
        if ((sepPos <= 0) || ((sepPos + 1) == observationID.length())) {
            throw new SPBadIDException("obs id '" + observationID +
                                       "' does not end with \\-[0-9]+");
        }

        _programID = SPProgramID.toProgramID(observationID.substring(0, sepPos));
        String indexStr = observationID.substring(sepPos + 1);
        try {
            _obsNumber = Integer.parseInt(indexStr);
        } catch (NumberFormatException ex) {
            throw new SPBadIDException("obs id '" + observationID +
                                       "' does not end with \\-[0-9]+");
        }
    }

    public SPProgramID getProgramID() {
        return _programID;
    }

    public int getObservationNumber() {
        return _obsNumber;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof SPObservationID)) return false;

        SPObservationID that = (SPObservationID) obj;
        if (!this._programID.equals(that._programID)) return false;
        if (this._obsNumber != that._obsNumber) return false;

        return true;
    }

    public int hashCode() {
        int res = _programID.hashCode();
        return res * 37 + _obsNumber;
    }

    public int compareTo(Object other) {
        SPObservationID that = (SPObservationID) other;
        int res = _programID.compareTo(that._programID);
        if (res != 0) return res;
        return _obsNumber - that._obsNumber;
    }

    /**
     * Gets the id as a string.
     */
    public String stringValue() {
        return toString();
    }

    public String toString() {
        StringBuffer buf = new StringBuffer(_programID.stringValue());
        buf.append(SEP);
        buf.append(_obsNumber);
        return buf.toString();
    }
}

