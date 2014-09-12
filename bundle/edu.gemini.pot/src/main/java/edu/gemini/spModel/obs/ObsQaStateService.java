//
// $Id: ObsQaStateService.java 46768 2012-07-16 18:58:53Z rnorris $
//

package edu.gemini.spModel.obs;

import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.spModel.dataset.DatasetQaStateSums;



/**
 * A service used to determine the {@link ObsQaState} that should be associated
 * with a particular observation.  The ObsQaState is either defaulted based upon
 * the individual {@link edu.gemini.spModel.dataset.DatasetQaState}s for its
 * datasets, or else explicitly set on the observation.
 *
 * <p>Making the determination of ObsQaState potentially involves finding the
 * {@link edu.gemini.spModel.obsrecord.ObsExecRecord} associated with the
 * observation to determine the individual dataset
 * {@link edu.gemini.spModel.dataset.DatasetQaState}s.
 */
public final class ObsQaStateService {

    private ObsQaStateService() {
    }

    public static ObsQaState getObsQaState(ISPObservation obs)
             {

        // Determine whether this observation has overriden the QA state.
        // If so, use the explicitly set ObsQaState.
        SPObservation dataObj = (SPObservation) obs.getDataObject();
        if (dataObj == null) return ObsQaState.UNDEFINED;
        if (dataObj.isOverrideQaState()) {
            return dataObj.getOverriddenObsQaState();
        }

        // Not overriden, so default based upon the datasets themselves.
        DatasetQaStateSums sums;
        sums = DatasetQaStateSumsService.sumDatasetQaStates(obs);
        if (sums == null) return ObsQaState.UNDEFINED;
        return ObsQaState.computeDefault(sums);
    }
}
