package edu.gemini.obslog.transfer;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.spModel.dataset.DatasetRecord;
import edu.gemini.spModel.obsclass.ObsClass;
import edu.gemini.spModel.obsrecord.UniqueConfig;

import java.util.Comparator;
import java.util.List;

//
// Gemini Observatory/AURA
// $Id: EObslogVisit.java,v 1.6 2006/12/05 14:56:16 gillies Exp $
//
public class EObslogVisit extends TransferBase {
    private static final long serialVersionUID = 1;

    private UniqueConfig _uc;
    private List<DatasetRecord> _dsetRecords;
    private ObsClass _obsClass;

    /**
     * The low-level data structure is a unique configuration of parameters from the database and one or more datasets.
     * For instance the parameter programID in system "ocs" is ocs.programID.
     */
    EObslogVisit(SPComponentType type, ObsClass obsClass, UniqueConfig uc, List<DatasetRecord> dsetRecords) {
        super(type);
        _obsClass = obsClass;
        _uc = uc;
        _dsetRecords = dsetRecords;
    }

    /**
     * A comparator that may be used to sort {@link edu.gemini.spModel.obsrecord.ObsVisit}s based upon
     * the "config time" of their first {@link edu.gemini.spModel.obsrecord.UniqueConfig}, if any.
     * The "config time" is equal to the time of the start dataset event for
     * the first dataset of the unique configuration.  ObsVisits without any
     * UniqueConfigs are sorted based upon start event time.
     * <p/>
     * <p>This comparator will generally yield the same sort order as
     *  edu.gemini.spModel.obsrecord.ObsVisit.START_TIME_COMPARATOR, except for old (pre-2005B) data. Prior
     * to 2005B, visit information wasn't kept cleanly and visits overlapped.
     */
    public static final Comparator<EObslogVisit> CONFIG_TIME_COMPARATOR = new Comparator<EObslogVisit>() {
        public int compare(EObslogVisit ov1, EObslogVisit ov2) {
            //EObslogVisit ov1 = (EObslogVisit) o1;
            //EObslogVisit ov2 = (EObslogVisit) o2;

           // long t1 = ov1.getStartTime();
           // long t2 = ov2.getObsVisit().getStartTime();

            edu.gemini.spModel.obsrecord.UniqueConfig ov1config = ov1._uc;
            long t1 = ov1config.getConfigTime();

            edu.gemini.spModel.obsrecord.UniqueConfig ov2config = ov2._uc;
            long t2 = ov2config.getConfigTime();

            if (t1 == t2) return 0;
            return t1 < t2 ? -1 : 1;
        }
    };

    public UniqueConfig getUniqueConfig() {
        return _uc;
    }

    public ObsClass getObsClass() {
        return _obsClass;
    }

    public List<DatasetRecord> getDatasetRecords() {
        return _dsetRecords;
    }

    public SPObservationID getObservationID() {
        if (_dsetRecords.size() ==  0) return null;
        return _dsetRecords.get(0).label().getObservationId();
    }

}
