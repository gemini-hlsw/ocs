package edu.gemini.wdba.shared;

import edu.gemini.pot.sp.SPObservationID;

import java.io.Serializable;

/**
 * Creaqted by Gemini Observatory HLDG
 */
public final class QueuedObservation implements Comparable, Serializable {
    private SPObservationID _obsId;
    private String _title;

    public QueuedObservation(SPObservationID obsId, String title) {
        if (obsId == null) throw new NullPointerException();
        _obsId = obsId;
        _title = title;
    }

    public SPObservationID getId() {
        return _obsId;
    }

    public String getTitle() {
        return _title;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final QueuedObservation that = (QueuedObservation) o;

        if (!_obsId.equals(that._obsId)) return false;
        return (_title == null) ? (that._title == null) : _title.equals(that._title);
    }

    public int hashCode() {
        int result = _obsId.hashCode();
        result = 29 * result + (_title == null ? 0 : _title.hashCode());
        return result;
    }

    public int compareTo(Object o) {
        QueuedObservation that = (QueuedObservation) o;

        int res = _obsId.compareTo(that._obsId);
        if (res != 0) return res;

        if (_title == null) {
            return (that._title == null) ? 0 : -1;
        } else if (that._title == null) {
            return 1;
        }
        return _title.compareTo(that._title);
    }
}

