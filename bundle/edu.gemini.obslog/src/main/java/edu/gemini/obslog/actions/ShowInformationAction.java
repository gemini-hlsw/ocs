package edu.gemini.obslog.actions;

import edu.gemini.obslog.database.OlPersistenceException;
import edu.gemini.obslog.database.OlPersistenceManager;
import edu.gemini.obslog.obslog.OlLogInformation;
import edu.gemini.spModel.core.SPBadIDException;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.gemini.plan.NightlyRecord;

import java.util.logging.Logger;

//
// Gemini Observatory/AURA
// $Id: ShowInformationAction.java,v 1.5 2006/08/25 20:13:40 shane Exp $
//

public class ShowInformationAction extends OlBaseAction {
    private static final Logger LOG = Logger.getLogger(ShowInformationAction.class.getName());

    private String _planID;

    private OlLogInformation _logInformation = new OlLogInformation();

    public OlLogInformation getLogInformation() {
        if (_logInformation == null) {
            LOG.severe("Log information is null.");
            _logInformation = new OlLogInformation();
        }
        return _logInformation;
    }

    public void setPlanID(String planID) {
        _planID = planID;
    }

    public String getPlanID() {
        return _planID;
    }

    public void setLogInformation(OlLogInformation logInformation) {
        _logInformation = logInformation;
    }

    protected OlLogInformation _getLogInformation(SPProgramID planID) throws OlPersistenceException {
        OlPersistenceManager pman = getPersistenceManager();

        NightlyRecord obsLogObj = pman.getNightlyRecord(planID);
        if (obsLogObj == null) return null;

        OlLogInformation logInformation = getLogInformation();

        logInformation.setNightObservers(obsLogObj.getNightObservers());
        logInformation.setSsas(obsLogObj.getSSA());
        logInformation.setDataproc(obsLogObj.getDataProc());
        logInformation.setDayobserver(obsLogObj.getDayObservers());
        logInformation.setNightComment(obsLogObj.getNightComment());
        logInformation.setFilePrefix(obsLogObj.getFilePrefix());
        logInformation.setCCVersion(obsLogObj.getCCSoftwareVersion());
        logInformation.setDCVersion(obsLogObj.getDCSoftwareVersion());
        logInformation.setSoftwareComment(obsLogObj.getSoftwareVersionNote());

        return logInformation;
    }

    public String execute() {
        if (_planID == null) {
            addActionError("Plan ID is null");
            LOG.severe("Plan ID is null");
        }
        LOG.fine("PlanID: " + _planID);

        SPProgramID spPlanID;
        try {
            spPlanID = SPProgramID.toProgramID(_planID);
        } catch (SPBadIDException ex) {
            addActionError("Failed to fetch plan ID: " + ex);
            return ERROR;
        }

        try {
            if (_getLogInformation(spPlanID) == null) {
                addActionError("Returned log information was null for: " + _planID);
                return ERROR;
            }
        } catch (OlPersistenceException ex) {
            addActionError("Failed while fetching log info: " + ex);
            return ERROR;
        }

        return SUCCESS;
    }

}
