package edu.gemini.spModel.obs;

import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.spModel.config.ConfigBridge;
import edu.gemini.spModel.config.map.ConfigValMapInstances;
import edu.gemini.spModel.config2.ConfigSequence;
import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.obsclass.ObsClass;

/**
 * A service used to determine the observing class for an observation.
 * Examines the execution sequence, considering the class of each observe
 * iterator in turn and selecting the one with the highest priority.
 */
public final class ObsClassService {

    private ObsClassService() {
    }

    /**
     * Determines the {@link ObsClass} for the given observation by examining
     * the ObsClass for each of its contained observe iterators.  The one with
     * the highest priority is returned, defaulting to {@link ObsClass#SCIENCE}.
     *
     * @param obs the observation whose class should be determined
     *
     * @return the highest priority observing class associated with any of the
     * observation's observe iterators
     *
     * @ if there is a problem communicating with the
     * database
     */
    public static ObsClass lookupObsClass(ISPObservation obs)  {
        // First check the cache.
        ObsClass obsClass = SPObsCache.getObsClass(obs);
        if (obsClass != null) return obsClass;

        // value not found in cache -> because of "automatic" mode for observe class in smart nodes we need to
        // fully calculate the sequence and take the obs classes from there (dataObject.getObsClass() will
        // return null for smart gcal nodes that are set to "automatic" for their observe class).
        ConfigSequence sequence = ConfigBridge.extractSequence(obs, null, ConfigValMapInstances.IDENTITY_MAP);
        Object[] obsClassStrings = sequence.getDistinctItemValues(new ItemKey("observe:class"));
        for (Object obsClassString : obsClassStrings) {
            if (obsClassString == null) {
                continue;
            }
            ObsClass oc = ObsClass.parseType((String)obsClassString);
            if ((obsClass == null) ||(oc.getPriority() < obsClass.getPriority())) {
                obsClass = oc;
            }
        }

        // default value is science
        if (obsClass == null) obsClass = ObsClass.SCIENCE;

        // Cache the results.
        SPObsCache.setObsClass(obs, obsClass);

        return obsClass;
    }
}
