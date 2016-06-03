package edu.gemini.obslog.actions;

import edu.gemini.obslog.obslog.OlLogException;
import edu.gemini.obslog.obslog.functor.OlListPlanFunctor;


import java.util.List;
import java.util.logging.Logger;

//
// Gemini Observatory/AURA
// $Id: ListPlanAction.java,v 1.1 2005/12/11 15:54:15 gillies Exp $
//

public class ListPlanAction extends OlBaseAction {
    private static final Logger LOG = Logger.getLogger(ListPlanAction.class.getName());

    private List _plans;

    public List getPlans() {
        return _plans;
    }

    public String execute() {

        try {
            _plans = OlListPlanFunctor.create(getPersistenceManager().getDatabase(), _user);
        } catch (OlLogException ex) {
            addActionError("Plan request caused a log exception: " + ex);
            return ERROR;
//        } catch (RemoteException ex) {
//            ex.printStackTrace();
//            addActionError("Plan request caused a remote exception: " + ex.toString());
//            return ERROR;
        } 
        return SUCCESS;
    }

}
