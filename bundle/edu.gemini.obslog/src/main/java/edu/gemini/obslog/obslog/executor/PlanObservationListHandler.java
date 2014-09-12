package edu.gemini.obslog.obslog.executor;

import edu.gemini.pot.sp.SPObservationID;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Gemini Observatory/AURA
 * $Id: PlanObservationListHandler.java,v 1.1 2005/12/11 15:54:15 gillies Exp $
 * This class processes a list of observations from the nightly plan and provides a sifted and organized list
 * for further processing.  At this moment it:
 * <ul>
 * <li>Removes duplicates
 * <li>Orders observations
 * </ul>
 * Note that this doesn't cause harm because typically, the visits are sorted by time after retrieval from the
 * database.
 */
public class PlanObservationListHandler {
    private List<SPObservationID> _originalList;
    private List<SPObservationID> _finalList;
    private SPObservationID _current;

    PlanObservationListHandler(List<SPObservationID> observationIDs) {
        if (observationIDs == null) throw new NullPointerException("Original observation list is null");
        _originalList = new ArrayList<SPObservationID>(observationIDs);
        int size = _originalList.size();
        _current = (size > 0) ? _originalList.get(size - 1) : null;
    }

    List<SPObservationID> getOriginalList() {
        return _originalList;
    }

    int getOriginalListSize() {
        return getOriginalList().size();
    }

    List<SPObservationID> getFinalList() {
        if (_finalList == null) {
            _finalList = new ArrayList<SPObservationID>();
        }
        return _finalList;
    }

    int getFinalListSize() {
        return getFinalList().size();
    }

    SPObservationID getCurrent() {
        return _current;
    }

    /**
     * This method is a temporary patch to remove adjacent duplicates in the observation list in the nightly plan
     * caused by observers pausing and continuing which causes multiple startSequence entries in the nightly plan.
     * So only the first should be kept.
     *
     * @return <String> list of observations with adjacent duplicates removed
     */
    public List<SPObservationID> apply() {
        int size = _originalList.size();
        if (size == 0) {
            return getFinalList();
        }

        // Copy it, sort it, remove duplicates
        Set<SPObservationID> idSet = new LinkedHashSet<SPObservationID>(_originalList);
        // Simply returns the ordered set as a list
        _finalList = new ArrayList<SPObservationID>(idSet);
        return _finalList;
    }
}
