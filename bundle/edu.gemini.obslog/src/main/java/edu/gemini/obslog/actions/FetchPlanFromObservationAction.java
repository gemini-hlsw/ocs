package edu.gemini.obslog.actions;

import com.opensymphony.xwork2.ActionSupport;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.util.NightlyProgIdGenerator;

//
// Gemini Observatory/AURA
// $Id: FetchPlanFromObservationAction.java,v 1.7 2005/08/22 14:14:10 gillies Exp $
//

public class FetchPlanFromObservationAction extends ActionSupport {

    private String _planID;
    private String _observationID;
    private String _utStart;

    // Notice that this planID is not the one in the base class.  This is the action that sets the
    // session variable once the fetch is successful
    public void setObservationID(String observationID) {
        _observationID = observationID;
    }

    public String getObservationID() {
        return _observationID;
    }

    public String getPlanID() {
        return _planID;
    }

    public void setPlanID(String planID) {
        _planID = planID;
    }

    public void setutstart(String utStart) {
        _utStart = utStart;
    }

    // Private method that looks at the utstart time and figures out what plan was used
    private SPProgramID _getPlan(String observationID, String utStartString) {
        long utstart = Long.parseLong(utStartString);

        // Use the first two letters of the observation to devise the site (just helps in testing.)
        String siteID = observationID.substring(0, 2);
        Site site;
        if (siteID.equals("GS")) {
            site = Site.GS;
        } else if (siteID.equals("GN")) {
            site = Site.GN;
        } else {
            return null;
        }


        SPProgramID planID = NightlyProgIdGenerator.getProgramID(NightlyProgIdGenerator.PLAN_ID_PREFIX,
                                                                 site,
                                                                 utstart);
        return planID;
    }

    public String execute() {
        if (_observationID == null) {
            addActionError("No observation ID was set.");
            return ERROR;
        }

        if (_utStart == null) {
            addActionError("No utstart time was set.");
            return ERROR;
        }

        SPProgramID planID = _getPlan(_observationID, _utStart);
        if (planID == null) {
            addActionError("Could not devise a plan from: " + _observationID);
            return ERROR;
        }
        _planID = planID.stringValue();

        return SUCCESS;
    }

}
