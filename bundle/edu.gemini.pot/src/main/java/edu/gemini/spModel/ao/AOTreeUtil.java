package edu.gemini.spModel.ao;

import edu.gemini.pot.sp.*;

/**
 * Utility class with operations on the science program tree model that are tailored
 * to specifically provide AO features.
 */
public class AOTreeUtil {

    /**
     * Return true if the given component is an adaptive optics instrument component.
     */
    public static boolean isAOInstrument(ISPObsComponent obsComp) {
        return (obsComp.getType().broadType.equals(AOConstants.AO_BROAD_TYPE));
    }

    /**
     * Find and return the AO system obs component node for the given observation.
     *
     * param obs is the observation that should be checked for an AO component.
     */
    public static ISPObsComponent findAOSystem(ISPObservation obs) {
        if (obs != null) {
            for (ISPObsComponent obsComp : obs.getObsComponents()) {
                if (isAOInstrument(obsComp)) {
                    return obsComp;
                }
            }
        }
        return null;
    }
}

