package edu.gemini.obslog.obslog.functor;

import edu.gemini.pot.sp.ISPNightlyRecord;
import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.spdb.*;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.obslog.obslog.DBNightlyPlanInfo;
import edu.gemini.obslog.obslog.OlLogException;

import java.security.Principal;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;


public class OlListPlanFunctor extends DBAbstractQueryFunctor implements IDBParallelFunctor {

    private static final Logger LOG = Logger.getLogger(OlListPlanFunctor.class.getName());
    private List<DBNightlyPlanInfo> _result;

    List<DBNightlyPlanInfo> getPlans() {
        if (_result == null) {
            _result = new ArrayList<>();
        }
        return _result;
    }

    public void execute(IDBDatabaseService db, ISPNode node, Set<Principal> principals) {

        ISPNightlyRecord nightlyRecord = (ISPNightlyRecord) node;
        String title;
        String planIDString = null;
        long timestamp;
        ISPDataObject spPlan = nightlyRecord.getDataObject();
        title = spPlan.getTitle();
        SPProgramID planID = nightlyRecord.getProgramID();
        if (planID != null) {
            planIDString = planID.stringValue();
        }
        timestamp = nightlyRecord.lastModified();
        if (LOG.isLoggable(Level.FINE))
	    LOG.log(Level.FINE, "Adding: " + planIDString);

        getPlans().add(new DBNightlyPlanInfo(title, planIDString, timestamp));
    }

    /**
     * Gets the created <tt>IObservingLog</tt> instance.
     *
     * @return the log
     */
    List<DBNightlyPlanInfo> getResult() {
        return getPlans();
    }

    public static List<DBNightlyPlanInfo> create(IDBDatabaseService db, Set<Principal> user) throws OlLogException {
        OlListPlanFunctor lf = new OlListPlanFunctor();

        IDBQueryRunner runner = db.getQueryRunner(user);
        lf = runner.queryNightlyPlans(lf);

        if (lf.getException() != null) {
            throw new OlLogException(lf.getException());
        }

        List<DBNightlyPlanInfo> planList = lf.getResult();

        // This sorts the list on the planID
        Collections.sort(planList);
        // I'm reversing it so that by default the most recent is first
        Collections.reverse(planList);
        return planList;
    }

    public void mergeResults(Collection<IDBFunctor> functorCollection) {
        List<DBNightlyPlanInfo> res = new ArrayList<DBNightlyPlanInfo>();
        for (IDBFunctor f : functorCollection) {
            res.addAll(((OlListPlanFunctor) f).getResult());
        }
        Collections.sort(res);
        Collections.reverse(res);
        _result = res;
    }
}

