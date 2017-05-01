//
// $Id$
//

package edu.gemini.spModel.obs;

import edu.gemini.pot.sp.*;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.skycalc.TwilightBoundType;
import edu.gemini.skycalc.TwilightBoundedNight;
import edu.gemini.skycalc.*;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.obs.plannedtime.PlannedTimeSummaryService;
import edu.gemini.spModel.obs.plannedtime.PlannedTimeSummary;
import edu.gemini.spModel.target.env.Asterism;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.too.Too;
import jsky.coords.WorldCoords;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Scheduling information for a given observation.  Includes all the information
 * needed to make a good estimation for whether the target can be observed in
 * the current or coming night.  Inspired by SCT-233.
 */
public final class ObsSchedulingReport implements Serializable {
    public enum Disposition {
        rising,
        setting,
        unknown
    }

    private static final Union<Interval> EMPTY = new Union<Interval>();

    private final SPObservationID _id;
    private final SPObservation _dataObj;
    private final SPSiteQuality _siteQuality;
    private final WorldCoords _coords;
    private final List<SPSiteQuality.TimingWindow> _windows;
    private final PlannedTimeSummary _plannedTime;

    private final Site _site;

    private final long _curTime;
    private final TwilightBoundedNight _night;
    private final double _airmass;
    private final double _altitude;
    private final Disposition _disposition;
    private final Union<Interval> _elevationRanges;
    private final Union<Interval> _timingRanges;

    /**
     * Constructs an ObsSchedulingReport for the current time.
     *
     * @param obs the observation associated with the TOO
     * @param site the location of the telescope to observe the TOO
     */
    public ObsSchedulingReport(ISPObservation obs, Site site)  {
        this(obs, site, System.currentTimeMillis());
    }

    /**
     * Constructs an ObsSchedulingReport using a specified time.
     *
     * @param obs the observation associated with the TOO
     * @param site the location of the telescope to observe the TOO
     * @param time time of interest
     */
    public ObsSchedulingReport(ISPObservation obs, Site site, long time)  {
        _id      = obs.getObservationID();
        _site    = site;
        _dataObj = (SPObservation) obs.getDataObject();

        SPSiteQuality   sq = null;
        WorldCoords coords = null;
        for (ISPObsComponent obsComp : obs.getObsComponents()) {
            SPComponentType type = obsComp.getType();
            if (SPSiteQuality.SP_TYPE.equals(type)) {
                sq = (SPSiteQuality) obsComp.getDataObject();
            } else if (TargetObsComp.SP_TYPE.equals(type)) {
                coords = getCoordinates(obsComp, _dataObj.getSchedulingBlockStart());
            }
        }
        _siteQuality = sq;
        _coords      = coords;
        _curTime     = time;
        _windows     = getTimingWindows(_siteQuality, obs, _curTime);
        _plannedTime = PlannedTimeSummaryService.getTotalTime(obs);

        // All remaining context information depends upon knowing the site.
        // If the site isn't defined, then there is nothing else to do.
        _night = (site == null) ? null : TwilightBoundedNight.forTime(TwilightBoundType.NAUTICAL, _curTime, _site);

        if ((_coords == null) || (_night == null)) {
            _airmass         = 0.0;
            _altitude        = 0.0;
            _disposition     = Disposition.unknown;
            _elevationRanges = EMPTY;
            _timingRanges    = EMPTY;
        } else {
            final ImprovedSkyCalc calc = new ImprovedSkyCalc(_site);
            calc.calculate(_coords, new Date(_curTime), false);
            _airmass = calc.getAirmass();
            _altitude = calc.getAltitude();

            // are we rising or setting?  Compare the elevation now to a
            // few minutes in the future
            calc.calculate(_coords, new Date(_curTime + 60000), false);
            final double futureAlt = calc.getAltitude();
            _disposition = (_altitude < futureAlt) ? Disposition.rising : Disposition.setting;

            // Get the time ranges in which the target meets specified
            // elevation constraints (defaults to air mass 1 - 2).
            final Solver s = getElevationConstraintSolver(_site, _coords, _siteQuality);
            _elevationRanges = s.solve(_night.getStartTime(), _night.getEndTime());

            // Get the time during the night in which the timing windows are met.
            Solver s0 = new TimingWindowSolver(_windows);
            _timingRanges = s0.solve(_night.getStartTime(), _night.getEndTime());
        }
    }

    private static WorldCoords getCoordinates(ISPObsComponent obsComp, Option<Long> when)  {
        TargetObsComp env = (TargetObsComp) obsComp.getDataObject();
        Asterism asterism = env.getAsterism();

        return
            asterism.getRaDegrees(when).flatMap(ra ->
            asterism.getDecDegrees(when).map(dec ->
                new WorldCoords(ra, dec))).getOrNull();
        }

    private static ElevationConstraintSolver getElevationConstraintSolver(
            Site site, WorldCoords coords, SPSiteQuality siteQuality) {

        if (siteQuality == null) {
            return ElevationConstraintSolver.forAirmass(site, coords);
        }

        final SPSiteQuality.ElevationConstraintType type;
        type = siteQuality.getElevationConstraintType();

        final double min = siteQuality.getElevationConstraintMin();
        final double max = siteQuality.getElevationConstraintMax();

        switch (type) {
            case HOUR_ANGLE:
                return ElevationConstraintSolver.forHourAngle(site, coords, min, max);
            case AIRMASS:
                return ElevationConstraintSolver.forAirmass(site, coords, min, max);
        }
        return ElevationConstraintSolver.forAirmass(site, coords);
    }

    private static List<SPSiteQuality.TimingWindow> getTimingWindows(SPSiteQuality siteQuality, ISPObservation obs, long time) {
        final List<SPSiteQuality.TimingWindow> twList;
        twList = (siteQuality == null) ? null : siteQuality.getTimingWindows();
        if ((twList != null) && (twList.size() > 0)) return twList;

        // No timing window is specified, so we need a default.
        final List<SPSiteQuality.TimingWindow> res = new ArrayList<SPSiteQuality.TimingWindow>();
        final long duration = Too.get(obs).getDefaultWindowDuration();
        if (duration > 0) {
            final SPSiteQuality.TimingWindow defWindow;
            defWindow = new SPSiteQuality.TimingWindow(time, duration, 0, 0);
            res.add(defWindow);
        }
        return res;
    }

    public Site getSite() { return _site; }
    public SPObservationID getObservationId() { return _id; }
    public SPObservation getObservationDataObject() { return _dataObj; }
    public List<SPSiteQuality.TimingWindow> getTimingWindows() { return _windows; }
    public PlannedTimeSummary getPlannedTime() { return _plannedTime; }
    public SPSiteQuality getSiteQuality() { return _siteQuality; }
    public WorldCoords getCoordinates() { return _coords; }

    /**
     * Gets the time that corresponds to the other information in the context.
     * The air mass, disposition, etc. are all relative to this time.
     */
    public long getCurrentTime() { return _curTime; }

    public TwilightBoundedNight getNight() { return _night; }
    public double getAirmass() { return _airmass; }
    public double getAltitude() { return _altitude; }

    public Disposition getDisposition() { return _disposition; }

    /**
     * Gets the time interval(s) during the night at which this target
     * meets specified elevation constraints (defaults to air mass 2.0 or better).
     */
    public Union<Interval> getElevationIntervals() {
        return new Union<Interval>(_elevationRanges);
    }

    /**
     * Gets the time interval(s) during the night at which this target
     * meets the specified timing constraints (defaulting to the values
     * specified in SCT-211, one week for standard TOO, one day for rapid TOO).
     */
    public Union<Interval> getTimingIntervals() {
        return new Union<Interval>(_timingRanges);
    }

    public String dump() {
        final StringBuilder buf = new StringBuilder();

        buf.append("Target of Opportunity Event\n");
        buf.append("\ttime........: ").append(new Date(_curTime)).append("\n");
        buf.append("\tobs.........: ").append(_id).append(" (").append(_dataObj.getTitle()).append(")\n");
        buf.append("\tairmass.....: ").append(_airmass).append(" (").append(_disposition).append(")\n");
        buf.append("\tsite quality: ");
        if (_siteQuality == null) {
            buf.append("<unspecified>\n");
        } else {
            buf.append("BG=").append(_siteQuality.getSkyBackground().getPercentage());
            buf.append(", CC=").append(_siteQuality.getCloudCover().getPercentage());
            buf.append(", IQ=").append(_siteQuality.getImageQuality().getPercentage());
            buf.append(", WV=").append(_siteQuality.getWaterVapor().getPercentage());
            buf.append("\n");
        }

        buf.append("\twindows.....: ");
        for (SPSiteQuality.TimingWindow win : _windows) {
            buf.append(win).append(" ");
        }
        buf.append("\n");

        buf.append("\tplanned time: ").append(_plannedTime).append("\n");

        buf.append("\televation...: ");
        for (Interval i : _elevationRanges) {
            buf.append("<").append(new Date(i.getStart())).append("  -->  ");
            buf.append(new Date(i.getEnd())).append("> ");
        }
        buf.append("\n");

        buf.append("\ttiming..: ");
        for (Interval i : _timingRanges) {
            buf.append("<").append(new Date(i.getStart())).append("  -->  ");
            buf.append(new Date(i.getEnd())).append("> ");
        }
        buf.append("\n");

        return buf.toString();
    }
}
