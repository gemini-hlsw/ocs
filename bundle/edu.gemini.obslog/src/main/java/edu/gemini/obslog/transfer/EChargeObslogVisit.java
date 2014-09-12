package edu.gemini.obslog.transfer;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.dataset.DatasetRecord;
import edu.gemini.spModel.obsclass.ObsClass;
import edu.gemini.spModel.obsrecord.ObsVisit;
import edu.gemini.spModel.time.ObsTimeCharges;

import java.util.Comparator;
import java.util.List;

//
// Gemini Observatory/AURA
// $Id: EChargeObslogVisit.java,v 1.1 2005/12/11 15:54:15 gillies Exp $
//

public final class EChargeObslogVisit extends EObslogVisit {
    private static final long serialVersionUID = 1;

    private long _startTime;
    private long _endTime;
    private ObsTimeCharges _timeCharges;

    /**
     * The low-level data structure is a unique configuration of parameters from the database and one or more datasets.
     * For instance the parameter programID in system "ocs" is ocs.programID.
     */
    EChargeObslogVisit(SPComponentType type, ObsClass obsClass, ObsVisit visit, List<DatasetRecord> dsetRecords, ObsTimeCharges timeCharges) {
        super(type, obsClass, visit.getUniqueConfigs()[0], dsetRecords);
        _timeCharges = timeCharges;
        _startTime =  visit.getStartTime();
        _endTime  = visit.getEndTime();
    }

    /**
     * A comparator that may be used to sort {@link edu.gemini.spModel.obsrecord.ObsVisit}s based upon
     * the "config time" of their first {@link edu.gemini.spModel.obsrecord.UniqueConfig}, if any.
     * The "config time" is equal to the time of the start dataset event for
     * the first dataset of the unique configuration.  ObsVisits without any
     * UniqueConfigs are sorted based upon start event time.
     * <p/>
     * <p>This comparator will generally yield the same sort order as
     * {@link edu.gemini.obslog.transfer.EObslogVisit.START_TIME_COMPARATOR}, except for old (pre-2005B) data. Prior
     * to 2005B, visit information wasn't kept cleanly and visits overlapped.
     */
    public static final Comparator<EChargeObslogVisit> CONFIG_TIME_COMPARATOR = new Comparator<EChargeObslogVisit>() {
        public int compare(EChargeObslogVisit ov1, EChargeObslogVisit ov2) {
            return EObslogVisit.CONFIG_TIME_COMPARATOR.compare(ov1, ov2);
        }
    };

    public ObsTimeCharges getObsTimeCharges() {
        return _timeCharges;
    }

    /**
     * Return the start of the visit
     * @return start time as a long
     */
    public long getVisitStartTime() {
        return _startTime;
    }

    /**
     * Return the end time of the visit
     * @return end time as a long
     */
    public long getVisitEndTime() {
        return _endTime;
    }
}
