package edu.gemini.obslog.actions;

import edu.gemini.obslog.obslog.OlLogException;
import edu.gemini.obslog.obslog.functor.OlListPlanFunctor;


import java.util.List;
import java.util.logging.Logger;

public class ListPlanAction extends OlBaseAction {

    public String execute() {
        try {
            OlListPlanFunctor.create(getPersistenceManager().getDatabase(), _user);
        } catch (OlLogException ex) {
            addActionError("Plan request caused a log exception: " + ex);
            return ERROR;
        }
        return SUCCESS;
    }

}
