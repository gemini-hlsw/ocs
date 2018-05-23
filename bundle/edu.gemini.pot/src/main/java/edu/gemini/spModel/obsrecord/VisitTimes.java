//
// $Id: VisitTimes.java 7011 2006-05-04 16:12:21Z shane $
//

package edu.gemini.spModel.obsrecord;

import edu.gemini.spModel.time.ObsTimeCharge;
import edu.gemini.spModel.time.ChargeClass;
import edu.gemini.spModel.time.ObsTimeCharges;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

/**
 * VisitTimes is an internal (package private) class used in returning the
 * time required to execute a particular visit.  Contains a long for all the
 * time that was spent outside of executing a dataset, and an array of
 * ObsTimeCharge containing the total times obtaining datasets. The dataset
 * times are split among the charge classes associated with the charge class
 * of the observe iterator that produced the data.
 */
final class VisitTimes implements Serializable {

    /**
     * Creates a VisitTimes instance wherein all time is noncharged.
     */
    static VisitTimes noncharged(long time) {
        final VisitTimes vt = new VisitTimes();
        vt.addClassifiedTime(ChargeClass.NONCHARGED, time);
        return vt;
    }

    private long _unclassifiedTime;
    private long[] _classifiedTimes = new long[ChargeClass.values().length];

    long getUnclassifiedTime() {
        return _unclassifiedTime;
    }

    void addUnclassifiedTime(long time) {
        _unclassifiedTime += time;
    }

    ObsTimeCharge[] getClassifiedTimes() {
        ChargeClass[] allChargeClasses = ChargeClass.values();
        ObsTimeCharge[] charges = new ObsTimeCharge[allChargeClasses.length];
        for (int i=0; i<allChargeClasses.length; ++i) {
            charges[i] = new ObsTimeCharge(_classifiedTimes[i], allChargeClasses[i]);
        }
        return charges;
    }

    void addClassifiedTime(ChargeClass cclass, long time) {
        _classifiedTimes[cclass.ordinal()] += time;
    }

    long getClassifiedTime(ChargeClass cclass) {
        return _classifiedTimes[cclass.ordinal()];
    }

    void addVisitTimes(VisitTimes vt) {
        _unclassifiedTime += vt._unclassifiedTime;
        for (int i=0; i<_classifiedTimes.length; ++i) {
            _classifiedTimes[i] += vt._classifiedTimes[i];
        }
    }

    ObsTimeCharges getTimeCharges(ChargeClass mainChargeClass) {
        ObsTimeCharge[] charges = getClassifiedTimes();
        int index = mainChargeClass.ordinal();
        charges[index] = charges[index].addTime(_unclassifiedTime);
        return new ObsTimeCharges(charges);
    }

    public long getTotalTime() {
        long result = _unclassifiedTime;
        for (long t : _classifiedTimes) result += t;
        return result;
    }

    /**
     * Gets the amount of time that is charged to the program or partner (or
     * unclassified which is eventually charged to one or the other).
     */
    public long getChargedTime() {
        return getTotalTime() - getClassifiedTime(ChargeClass.NONCHARGED);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_unclassifiedTime, _classifiedTimes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final VisitTimes that = (VisitTimes) o;
        return Objects.equals(_unclassifiedTime, that._unclassifiedTime) &&
                Objects.deepEquals(_classifiedTimes, that._classifiedTimes);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("VisitTimes{");
        sb.append("_unclassifiedTime=").append(_unclassifiedTime);
        sb.append(", _classifiedTimes=").append(Arrays.toString(_classifiedTimes));
        sb.append('}');
        return sb.toString();
    }
}
