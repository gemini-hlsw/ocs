//
// $Id: DatasetQaStateSumsService.java 46768 2012-07-16 18:58:53Z rnorris $
//

package edu.gemini.spModel.obs;

import edu.gemini.pot.sp.ISPObsExecLog;
import edu.gemini.pot.sp.ISPObsQaLog;
import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.spModel.obslog.ObsExecLog;
import edu.gemini.spModel.obslog.ObsQaLog;
import edu.gemini.spModel.dataset.DatasetQaStateSums;


/**
 * A service used to determine the number of datasets in each of the various
 * {@link edu.gemini.spModel.dataset.DatasetQaState}s that exist in the
 * observation.  This involves finding the
 * {@link edu.gemini.spModel.obsrecord.ObsExecRecord} associated with
 * the Observation and asking it to do the calculation.
 */
public final class DatasetQaStateSumsService {

    private DatasetQaStateSumsService() {
    }

    /**
     * Determines the number of datasets in each of the various
     * {@link edu.gemini.spModel.dataset.DatasetQaState}s that exist in the
     * given observation.
     *
     * @param obs the observation whose dataset counts should be determined
     *
     * @return the number of datasets in each of the various
     * {@link edu.gemini.spModel.dataset.DatasetQaState}s
     *
     * @throws java.rmi.RemoteException if there is a problem communicating
     * with the database
     */
    public static DatasetQaStateSums sumDatasetQaStates(ISPObservation obs) {

        // First check the cache.
        DatasetQaStateSums sums = SPObsCache.getDatasetQaStateSums(obs);
        if (sums != null) return sums;

        // Compute the result
        final ISPObsExecLog el = obs.getObsExecLog();
        final ISPObsQaLog   qa = obs.getObsQaLog();
        if ((el == null) || (qa == null)) {
            sums = DatasetQaStateSums.ZERO_SUMS;
        } else {
            sums = ((ObsQaLog) qa.getDataObject()).sumDatasetQaStates((ObsExecLog) el.getDataObject());
        }

        // Cache the results.
        SPObsCache.setDatasetQaStateSums(obs, sums);
        return sums;
    }
}
