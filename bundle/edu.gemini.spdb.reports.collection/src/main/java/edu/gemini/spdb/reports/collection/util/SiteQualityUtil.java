//
// $
//

package edu.gemini.spdb.reports.collection.util;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.ISPProgram;
import edu.gemini.shared.util.TimeValue;
import edu.gemini.shared.util.immutable.Tuple2;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.obs.ObsClassService;
import edu.gemini.spModel.obs.plannedtime.PlannedTimeSummaryService;
import edu.gemini.spModel.obs.plannedtime.PlannedTimeSummary;
import edu.gemini.spModel.obsclass.ObsClass;


import java.util.HashMap;
import java.util.Map;

/**
 * A utility used to calculate the dominating observing conditions in a program.
 * See SCT-286.
 */
public class SiteQualityUtil {

    // A class to hold just the observing conditions part of the site quality
    private static final class ObsConditions {

        private final SPSiteQuality.ImageQuality  iq;
        private final SPSiteQuality.CloudCover    cc;
        private final SPSiteQuality.SkyBackground sb;
        private final SPSiteQuality.WaterVapor    wv;

        ObsConditions(SPSiteQuality sq) {
            iq = sq.getImageQuality();
            cc = sq.getCloudCover();
            sb = sq.getSkyBackground();
            wv = sq.getWaterVapor();
        }

        public boolean equals(Object other) {
            if (!(other instanceof ObsConditions)) return false;

            ObsConditions that = (ObsConditions) other;
            if (iq != that.iq) return false;
            if (cc != that.cc) return false;
            if (sb != that.sb) return false;
            if (wv != that.wv) return false;

            return true;
        }

        public int hashCode() {
            int res = iq.hashCode();
            res += 37*res + cc.hashCode();
            res += 37*res + sb.hashCode();
            res += 37*res + wv.hashCode();

            return res;
        }

        public String toString() {
            return String.format("%s / %s / %s / %s",
                    convertPercentage(iq.getPercentage()),
                    convertPercentage(cc.getPercentage()),
                    convertPercentage(sb.getPercentage()),
                    convertPercentage(wv.getPercentage()));
        }

        private static String convertPercentage(byte percentage) {
            if (percentage == 100) return "--";
            return String.valueOf(percentage);
        }
    }

    public static String getDominatingConditions(ISPProgram progShell)  {

        // Record the total execution time of all obs with the same observing
        // conditions in a map keyed by observing conditions.
        Map<ObsConditions, Long> cMap = new HashMap<ObsConditions, Long>();
        for (ISPObservation obs : progShell.getAllObservations()) {
            considerObs(obs, cMap);
        }
        for(Tuple2<SPSiteQuality, TimeValue> sq: ReportUtils.getTemplateConditions(progShell)){
           considerSq(sq._1(), sq._2(), cMap);
        }
        // Find the entry with the maximum amount of time and call it
        // "dominating".  See SCT-286.
        long max = Long.MIN_VALUE;
        ObsConditions domintating = null;
        for (Map.Entry<ObsConditions, Long> me : cMap.entrySet()) {
            if (me.getValue() > max) {
                domintating = me.getKey();
                max = me.getValue();
            }
        }
        if (domintating == null) return "";
        return domintating.toString();
    }

    private static void considerSq(SPSiteQuality spSiteQuality, TimeValue timeValue, Map<ObsConditions, Long> cMap) {
        // Extract the observing conditions we care about.
        ObsConditions oc = new ObsConditions(spSiteQuality);

        // Figure out the execution time for this observation.
        long execTime = timeValue.getMilliseconds();

        // Look for an existing time sum associated with these obs conditions.
        Long totalTime = cMap.get(oc);
        if (totalTime == null) {
            // This is the first science observation with these observing
            // conditions, so set the total to be the exec time for this obs
            totalTime = execTime;
        } else {
            // Sum the execution time for this obs to the total.
            totalTime += execTime;
        }
        cMap.put(oc, totalTime);
    }

    private static void considerObs(ISPObservation obsShell,
                                    Map<ObsConditions, Long> cMap)  {
        // Only consider science observations.
        ObsClass obsClass = ObsClassService.lookupObsClass(obsShell);
        if (ObsClass.SCIENCE != obsClass) return;

        // Look through the obs components to find the site quality
        ISPObsComponent sqComponent = null;
        for (ISPObsComponent obsComp : obsShell.getObsComponents()) {
            if (SPSiteQuality.SP_TYPE.equals(obsComp.getType())) {
                sqComponent = obsComp;
                break;
            }
        }
        if (sqComponent == null) return;

        // Extract the observing conditions we care about.
        SPSiteQuality sq = (SPSiteQuality) sqComponent.getDataObject();
        ObsConditions oc = new ObsConditions(sq);

        // Figure out the execution time for this observation.
        PlannedTimeSummary pt = PlannedTimeSummaryService.getTotalTime(obsShell);
        long execTime = pt.getExecTime();

        // Look for an existing time sum associated with these obs conditions.
        Long totalTime = cMap.get(oc);
        if (totalTime == null) {
            // This is the first science observation with these observing
            // conditions, so set the total to be the exec time for this obs
            totalTime = execTime;
        } else {
            // Sum the execution time for this obs to the total.
            totalTime += execTime;
        }
        cMap.put(oc, totalTime);
    }

}
