/*
 * Copyright 2003 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: ObsTargetDesc.java 46768 2012-07-16 18:58:53Z rnorris $
 */

package edu.gemini.spModel.target;

import edu.gemini.pot.sp.*;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.gemini.obscomp.SPProgram;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.obs.SPObservation;
import edu.gemini.spModel.obs.plannedtime.PlannedTimeSummaryService;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import edu.gemini.spModel.target.system.CoordinateParam.Units;
import edu.gemini.spModel.target.system.ICoordinate;
import edu.gemini.spModel.target.system.ITarget;
import edu.gemini.spModel.time.TimeAmountFormatter;
import edu.gemini.spModel.util.SPTreeUtil;
import jsky.coords.TargetDesc;
import jsky.coords.WorldCoords;

import java.util.logging.Logger;


/** Utility class to create a {@link TargetDesc} object describing a given observation target */
public class ObsTargetDesc extends TargetDesc {

    private static final Logger LOGGER = Logger.getLogger(ObsTargetDesc.class.getName());
    private String _obsId;
    private String _targetName;
    private String _timeStr;

    private ObsTargetDesc(String name, WorldCoords coords, String description,
                          String priority, String category, String obsId,
                          String targetName, String timeStr,
                          TargetDesc.ElConstraintType elType, double elMin, double elMax) {
        super(name, coords, description, priority, category,
                elType, elMin, elMax);
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

        SPTarget basePos = targetEnv.getBase();
        ITarget target = basePos.getTarget();
        ICoordinate c1 = target.getRa();
        ICoordinate c2 = target.getDec();
        double x = c1.getAs(Units.DEGREES);
        double y = c2.getAs(Units.DEGREES);
        WorldCoords pos = new WorldCoords(x, y, 2000.);
                 String targetName = basePos.getTarget().getName();

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

        String desc = obsId + "  [" + targetName + "]  " + timeStr;

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

        if (useTargetName)
            return new ObsTargetDesc(targetName, pos, desc, prio, category, obsId, targetName, timeStr, elType, min, max);
        else
            return new ObsTargetDesc(obsId, pos, desc, prio, category, obsId, targetName, timeStr, elType, min, max);
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

    /** Return the target's observation id */
    public String getObservationId() {
        return _obsId;
    }

    /** Return the target name */
    public String getTargetName() {
        return _targetName;
    }

    /** Return the total planned time as a string */
    public String getTimeStr() {
        return _timeStr;
    }

    /** Return an array of one or more Strings describing the target */
    public String[] getDescriptionFields() {
        return new String[]{_obsId, _targetName, _timeStr};
    }
}
