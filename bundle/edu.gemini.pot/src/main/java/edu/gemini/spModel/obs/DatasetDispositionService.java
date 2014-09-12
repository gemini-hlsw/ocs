//
// $Id: DatasetDispositionService.java 46768 2012-07-16 18:58:53Z rnorris $
//

package edu.gemini.spModel.obs;

import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.spModel.obslog.ObsLog;
import edu.gemini.spModel.dataset.DatasetDisposition;



/**
 * A service used to determine the dataflow step for an observation.  Requires
 * finding the {@link edu.gemini.spModel.obsrecord.ObsExecRecord} associated with
 * the Observation and asking it to calculate the minimum step.
 */
public final class DatasetDispositionService {

    private DatasetDispositionService() {
    }

    /**
     * Determines the {@link edu.gemini.spModel.dataset.DatasetDisposition} for
     * the given observation by finding the
     * {@link edu.gemini.spModel.obsrecord.ObsExecRecord} and asking it to
     * calculate the minimum DataflowStep of any of its datasets.
     *
     * @param obs the observation whose dataflow step should be determined
     *
     * @return the lowest priority dataflow step in the observation; <code>
     * null</code> if there are no datasets in the observation
     *
     * @throws java.rmi.RemoteException if there is a problem communicating
     * with the database
     */
    public static DatasetDisposition lookupDatasetDisposition(ISPObservation obs)  {
        // First check the cache.
        DatasetDisposition dis = SPObsCache.getDatasetDisposition(obs);
        if (dis != null) return dis;

        // Compute the minimum step.
        final ObsLog log = ObsLog.getIfExists(obs);
        if (log != null) dis = log.getMinimumDisposition();

        // Cache the results.
        SPObsCache.setDatasetDisposition(obs, dis);
        return dis;
    }
}
