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
// $Id: StoreInformationAction.java,v 1.4 2006/08/25 20:13:40 shane Exp $
//

public class StoreInformationAction extends ShowInformationAction {
    private static final Logger LOG = Logger.getLogger(StoreInformationAction.class.getName());

    /**
     * This method fetches the observing log from the database, populates its information
     * fields and stores it back to the database.
     *
     * @param planID the <code>SPProgramID</code> of the program that should be updated.
     */
    private boolean _storeLogInformation(SPProgramID planID, OlLogInformation info) throws OlPersistenceException {
        OlPersistenceManager pman = getPersistenceManager();

        NightlyRecord obsLogObj = pman.getNightlyRecord(planID);
        if (obsLogObj == null) return false;

        obsLogObj.setNightObservers(info.getNightObservers());
        obsLogObj.setSSA(info.getSsas());
        obsLogObj.setDataProc(info.getDataproc());
        obsLogObj.setDayObservers(info.getDayobserver());
        obsLogObj.setNightComment(info.getNightComment());
        obsLogObj.setFilePrefix(info.getFilePrefix());
        obsLogObj.setCCSoftwareVersion(info.getCCVersion());
        obsLogObj.setDCSoftwareVersion(info.getDCVersion());
        obsLogObj.setSoftwareVersionNote(info.getSoftwareComment());

        pman.setNightlyRecord(planID, obsLogObj);
        return true;
    }

    public String execute() {
        if (getPlanID() == null) {
            addActionError("Plan ID is null");
            LOG.severe("Plan ID is null");
        }
        LOG.fine("PlanID: " + getPlanID());

        SPProgramID spPlanID;
        try {
            spPlanID = SPProgramID.toProgramID(getPlanID());
        } catch (SPBadIDException ex) {
            addActionError("Failed to fetch plan ID: " + ex);
            return ERROR;
        }

        try {
            if (_storeLogInformation(spPlanID, getLogInformation()) == false) {
                addActionError("Write of log information failed for: " + getPlanID());
                return ERROR;
            }
        } catch (OlPersistenceException ex) {
            addActionError("Failed while storing log info: " + ex);
            return ERROR;
        }

        return SUCCESS;
    }

}
