//
// $Id: NightObsTimesService.java 46768 2012-07-16 18:58:53Z rnorris $
//

package edu.gemini.spModel.obs;

import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.obsclass.ObsClass;
import edu.gemini.pot.sp.Instrument;
import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.obslog.ObsLog;
import edu.gemini.spModel.obsrecord.ObsVisit;
import edu.gemini.spModel.time.ChargeClass;
import edu.gemini.spModel.time.ObsTimes;
import edu.gemini.spModel.time.ObsTimeCharges;
import edu.gemini.skycalc.ObservingNight;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;


/**
 * Service used to extract the {@link ObsTimes observing times} for each night
 * that a particular observation was observed.
 */
public class NightObsTimesService {

    /**
     * Gets a collection of {@link ObsTimes} paired with the
     * {@link ObservingNight night} upon which they occurred.
     *
     * @param obs observation whose per/night times should be obtained
     *
     * @return collection of {@link NightObsTimes}, one per night upon which
     * the observation was observed
     */
    public static Collection<NightObsTimes> getObservingNightTimes(ISPObservation obs) {
        List<NightObsTimes> res = new ArrayList<NightObsTimes>();

        // Get the ObsRecord, if it exists.
        final ObsLog nodes = ObsLog.getIfExists(obs);
        if (nodes == null) return res;
        Site site = nodes.getExecRecord().getSite();

        // Figure out the main charge class and SiteDesc for the obs.
        ObsClass obsClass = ObsClassService.lookupObsClass(obs);
        ChargeClass chargeClass = obsClass.getDefaultChargeClass();

        // Figure out the instrument in use, if any.
        final Option<Instrument> inst = InstrumentService.lookupInstrument(obs);

        // Get the visits for the obs, if any.
        ObsVisit[] visits = nodes.getExecRecord().getVisits(inst, nodes.getQaRecord());
        if ((visits == null) || (visits.length == 0)) return res;

        // Start with the first visit, and figure out the ObservingNight and
        // the times associated with it.
        ObsVisit visit = visits[0];
        long endTime = visit.getEndTime();
        ObservingNight curNight = new ObservingNight(site, endTime);
        ObsTimes curTimes = visit.getObsTimes(chargeClass);

        // Now step through the remaining visits, summing up times for visits
        // that happened on the same night.
        for (int i=1; i<visits.length; ++i) {
            visit = visits[i];
            endTime = visit.getEndTime();
            ObservingNight tmpNight = new ObservingNight(site, endTime);
            ObsTimes tmpTimes = visit.getObsTimes(chargeClass);

            if (curNight.equals(tmpNight)) {
                // combine the obsTimes
                long total = tmpTimes.getTotalTime() + curTimes.getTotalTime();
                ObsTimeCharges otc = tmpTimes.getTimeCharges().addTimeCharges(curTimes.getTimeCharges());
                curTimes = new ObsTimes(total, otc);
            } else {
                // Add the last night to the results.
                res.add(new NightObsTimes(curNight, curTimes));

                // Move to the new night.
                curNight = tmpNight;
                curTimes = tmpTimes;
            }
        }

        // Add the last night.
        res.add(new NightObsTimes(curNight, curTimes));

        return res;
    }
}
