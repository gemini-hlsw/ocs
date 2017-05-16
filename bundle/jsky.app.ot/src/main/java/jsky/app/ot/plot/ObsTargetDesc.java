/*
 * Copyright 2003 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: ObsTargetDesc.java 46768 2012-07-16 18:58:53Z rnorris $
 */

package jsky.app.ot.plot;

import edu.gemini.pot.sp.*;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.skycalc.Interval;
import edu.gemini.skycalc.Union;
import edu.gemini.spModel.gemini.obscomp.SPProgram;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.obs.SPObservation;
import edu.gemini.spModel.obs.TimingWindowSolver;
import edu.gemini.spModel.obs.plannedtime.PlannedTimeSummaryService;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.Asterism;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import edu.gemini.spModel.time.TimeAmountFormatter;
import edu.gemini.spModel.util.SPTreeUtil;
import jsky.plot.TargetDesc;
import jsky.coords.WorldCoords;

import java.util.function.BiFunction;
import java.util.function.Function;


/** Utility class to create a {@link TargetDesc} object describing a given observation target */
public class ObsTargetDesc extends TargetDesc {

    private String _obsId;
    private String _targetName;
    private String _timeStr;

    private ObsTargetDesc(String name, Function<Option<Long>, Option<WorldCoords>> coords,
                          String priority, String category, String obsId,
                          String targetName, String timeStr,
                          TargetDesc.ElConstraintType elType, double elMin, double elMax,
                          BiFunction<Long, Long, Union<Interval>> timingWindows) {
        super(name, coords, priority, category, elType, elMin, elMax, timingWindows);
        _obsId = obsId;
        _targetName = targetName;
        _timeStr = timeStr;
    }


    /**
     * create a {@link TargetDesc} object describing a given observation's target.
     *
     * @param db the database to use
     * @param obs the observation to take the target from
     * @param useTargetName if true, use the target name in the result, otherwise the observation id
     *
     * @return the TargetDesc instance, or null if the observation does not have a target env.
     */
    public static TargetDesc getTargetDesc(IDBDatabaseService db, ISPObservation obs, boolean useTargetName)
             {

        TargetEnvironment targetEnv = _findTargetEnv(obs);
        if (targetEnv == null) return null;

        Asterism asterism = targetEnv.getAsterism();

        Function<Option<Long>, Option<WorldCoords>> pos = op ->
            asterism.getRaDegrees(op).flatMap(x ->
            asterism.getDecDegrees(op).map(y ->
                new WorldCoords(x, y, 2000.)
            ));

                 String targetName = asterism.name();

        String obsId = "";
        SPObservationID spObsId = obs.getObservationID();
        if (spObsId != null) {
            obsId = spObsId.stringValue();
        }

        long totalPlannedTime = _getTotalPlannedTime(obs);
        String timeStr = TimeAmountFormatter.getHMSFormat(totalPlannedTime);

        // LORD OF DESTRUCTION: DataObjectManager get without set
        SPObservation spObs = (SPObservation) obs.getDataObject();
        String prio = spObs.getPriority().displayValue();
        ISPProgram prog = db.lookupProgram(obs.getProgramKey());
        // LORD OF DESTRUCTION: DataObjectManager get without set
        SPProgram spProg = (SPProgram) prog.getDataObject();
        String category = spProg.getQueueBand();
        if (category.length() == 0)
            category = "Default";
        else
            category = "Band " + category;

        // Observing conditions
        SPSiteQuality siteQuality = _findSiteQuality(obs);
        TargetDesc.ElConstraintType elType = null;
        double min = 0.0;
        double max = 0.0;
        if (siteQuality != null) {
            elType = TargetDesc.ElConstraintType.valueOf(siteQuality.getElevationConstraintType().name());
            min = siteQuality.getElevationConstraintMin();
            max = siteQuality.getElevationConstraintMax();
        }

        // Function (start timestamp, end timestamp) => Union<Interval>.
        // The Union indicates the time intervals where the target can be
        // observed according to the observation's timing windows.
        final BiFunction<Long, Long, Union<Interval>> timingWindows = (s, e) ->
            (siteQuality == null) ?
                new Union<>(new Interval(s, e)) :
                new TimingWindowSolver(siteQuality.getTimingWindows()).solve(s, e);

        final String name = useTargetName ? targetName : obsId;
        return new ObsTargetDesc(name, pos, prio, category, obsId, targetName, timeStr, elType, min, max, timingWindows);
    }

    private static TargetEnvironment _findTargetEnv(ISPObservation obs)  {
        ISPObsComponent targetNode = SPTreeUtil.findTargetEnvNode(obs);
        if (targetNode == null) return null;

        // LORD OF DESTRUCTION: DataObjectManager get without set
        TargetObsComp targetObsComp = (TargetObsComp) targetNode.getDataObject();
        return targetObsComp.getTargetEnvironment();
    }

    private static SPSiteQuality _findSiteQuality(ISPObservation obs)  {
        ISPObsComponent obsCondNode = SPTreeUtil.findObsCondNode(obs);
        if (obsCondNode == null) {
            return null;
        }
        // LORD OF DESTRUCTION: DataObjectManager get without set
        return (SPSiteQuality) obsCondNode.getDataObject();
    }


    // Get the total planned time in seconds for the given observation
    private static long _getTotalPlannedTime(ISPObservation obs) {
        return PlannedTimeSummaryService.getTotalTime(obs).getPiTime();
    }

    /** Return an array of one or more Strings describing the target */
    public String[] getDescriptionFields() {
        return new String[]{_obsId, _targetName, _timeStr};
    }
}
