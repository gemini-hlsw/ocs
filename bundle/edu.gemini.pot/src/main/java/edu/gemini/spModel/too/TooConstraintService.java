//
// $
//

package edu.gemini.spModel.too;

import edu.gemini.pot.sp.*;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;


import java.util.List;

/**
 *
 */
public class TooConstraintService {

    private static ISPObsComponent _findComponent(ISPObservation obs) {
        //noinspection unchecked
        List<ISPObsComponent> compList = obs.getObsComponents();
        for (ISPObsComponent comp : compList) {
            SPComponentType curType = comp.getType();
            if (curType.equals(SPSiteQuality.SP_TYPE)) return comp;
        }
        return null;
    }

    private static final class SiteQualityTuple {
        private final ISPObsComponent siteQuality;
        private final boolean add;

        SiteQualityTuple(ISPObsComponent siteQuality, boolean add) {
            this.siteQuality = siteQuality;
            this.add         = add;
        }

        public boolean add() {
            return add;
        }

        public ISPObsComponent siteQuality() {
            return siteQuality;
        }
    }

    public static SiteQualityTuple findOrCreate(ISPObservation obs, ISPFactory factory) throws SPException {
        boolean add = false;
        ISPObsComponent obsComp = _findComponent(obs);
        if (obsComp == null) {
            obsComp = factory.createObsComponent(obs.getProgram(), SPSiteQuality.SP_TYPE, null);
            add = true;
        }
        return new SiteQualityTuple(obsComp, add);
    }

    /**
     * Adds a timing window to the given TOO observation, creating the
     * observing conditions if necessary.  Can be used to ensure that the
     * default timing window is applied if none is specified (<code>win</code>
     * is <code>null</code>) and none are already present in the observation.
     * After this call, the observation will have a timing window of some
     * sort assuming that the TOO type has a positive default timing window
     * duration.
     *
     * @param obs presumably TOO observation to which the timing window should
     * be added
     * @param factory factory to use for creating the observing conditions
     * component if necessary
     * @param win timing window (if <code>null</code> and there are no existing
     * timing windows, a default window is created and added
     */
    public static void addTimingWindow(ISPObservation obs, ISPFactory factory, SPSiteQuality.TimingWindow win) throws SPException {
        SiteQualityTuple tup = findOrCreate(obs, factory);

        SPSiteQuality sq = (SPSiteQuality) tup.siteQuality().getDataObject();
        List<SPSiteQuality.TimingWindow> existingWindows = sq.getTimingWindows();
        if ((existingWindows != null) && (existingWindows.size() > 0) && (win == null)) {
            // there is an existing timing window and no new one was specified
            // so quit here
            return;
        }

        // Add a new timing window.  If none specified, use default values for
        // the type of TOO observation.
        long start;
        long duration;
        if (win == null) {
            TooType tooType = Too.get(obs);
            start    = System.currentTimeMillis();
            duration = tooType.getDefaultWindowDuration();
            if (duration <=0) return;  // too type has no timing window
            win = new SPSiteQuality.TimingWindow(start, duration, 0, 0);
        }

        sq.addTimingWindow(win);
        tup.siteQuality.setDataObject(sq);
        if (tup.add) obs.addObsComponent(0, tup.siteQuality);
    }

    public static void setElevationConstraint(ISPObservation obs, ISPFactory factory, SPSiteQuality.ElevationConstraintType type, double min, double max) throws SPException {
        SiteQualityTuple tup = findOrCreate(obs, factory);
        SPSiteQuality sq = (SPSiteQuality) tup.siteQuality().getDataObject();

        if (type == null) type = SPSiteQuality.ElevationConstraintType.NONE;

        sq.setElevationConstraintType(type);
        sq.setElevationConstraintMin(min);
        sq.setElevationConstraintMax(max);

        tup.siteQuality.setDataObject(sq);
        if (tup.add) obs.addObsComponent(0, tup.siteQuality);
    }
}
