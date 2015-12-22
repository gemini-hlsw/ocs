package edu.gemini.spModel.util;

import edu.gemini.pot.sp.*;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.core.SPBadIDException;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.data.ISPDataObject;
import java.util.logging.Logger;
import java.util.logging.Level;


import java.util.Iterator;
import java.util.List;

/**
 * Searches for an observation based on a search string.
 * <p>
 * If the value is a number, use it as an observation number, search the
 * observations for it and show the observation. Even if it is hidden or in a
 * group it should be scrolled to, displayed and selected.
 * If the value is a string, assume it is a title and search all observation titles
 * doing the same thing (i.e. scroll, display, select.)
 * <p>
 * The result of this functor is an observation, which may be null if no matches
 * were found.
 */
public class DBSearchService {
    private static final Logger LOG = Logger.getLogger(DBSearchService.class.getName());

    // The search string: numeric to search by id, text for title
    private String _searchString;

    // Set to true if the contents of group nodes should be searched
    private boolean _searchGroups;

    // The observation, if found
    private ISPObservation _result;

    // If not null, start the search after this observation
    private ISPObservation _startHere;

    /**
     * Initialize with the search parameters.
     */
    private DBSearchService(boolean searchGroups, String searchString, ISPObservation startHere) {
        _searchGroups = searchGroups;
        _searchString = searchString;
        _startHere = startHere;
    }

    private void execute(IDBDatabaseService db, ISPNode node) {
        if ((_searchString == null) || (_searchString.length() == 0)) {
            return;
        }

        if (!(node instanceof ISPProgram)) return;
        ISPProgram prog = (ISPProgram) node;
        SPObservationID obsId = _getObsId(prog, _searchString);
        if (obsId != null) {
            ISPObservation obs = db.lookupObservationByID(obsId);
            if (obs != null) {
                if (_searchGroups ||
                    (obs.getParent() instanceof ISPProgram)) {
                    _result = obs;
                    return; // we found an obs by id, so stop
                }
            }
        }

        _searchString = _searchString.toLowerCase();
        if (_searchGroups) {
            _result = _search(prog.getAllObservations());
        } else {
            _result = _search(prog.getObservations());
        }
    }

    private SPObservationID _getObsId(ISPProgram prog, String searchString) {

        // If there is no program id, there are no observation ids either.
        SPProgramID progId = prog.getProgramID();
        if (progId == null) return null;

        // See if we're searching by number, and if so figure out the number.
        int obsNum = -1;
        try {
            obsNum = Integer.parseInt(searchString);
        } catch(NumberFormatException e) {
        }

        if (obsNum <= 0) {
            // The search string isn't a positive integer, see if it is a
            // complete observation id.
            SPObservationID obsId = null;
            try {
                obsId = new SPObservationID(searchString);
            } catch (Exception ex) {
            }

            if ((obsId != null) && !progId.equals(obsId.getProgramID())) {
                // got to be searching in the same program ...
                return null;
            }

            // either is a valid obs id or null, indicating that the search
            // string can't be interpreted as an obs id
            return obsId;
        }

        // Now we have a positive integer, create an observation id from it.
        try {
            return new SPObservationID(progId, obsNum);
        } catch (SPBadIDException ex) {
            // won't happen since we have a prog id and a valid #
            LOG.log(Level.WARNING, ex.getMessage(), ex);
        }
        return null;
    }


    // Return the first observation that matches the searchString
    private ISPObservation _search(List<ISPObservation> obsList)  {

        Iterator<ISPObservation> it = obsList.iterator();
        if (_startHere != null) {
            SPNodeKey startKey = _startHere.getNodeKey();

            while(it.hasNext()) {
                ISPObservation obs = it.next();
                SPNodeKey obsKey = obs.getNodeKey();
                if (startKey.equals(obsKey)) break;
            }
            if (!it.hasNext()) {
                it = obsList.iterator(); // wrap around
            }
        }

        // Try to match on the title itself.
        while(it.hasNext()) {
            ISPObservation obs = it.next();

            ISPDataObject dataObj = obs.getDataObject();
            if (dataObj == null) continue;
            String title = dataObj.getTitle();
            if (title == null) continue;
            title = title.toLowerCase();
            if (title.contains(_searchString)) {
                return obs;
            }
        }

        return null;
    }

    private ISPObservation getResult() {
        return _result;
    }

    /**
     * Search the given container (program node) for observations matching the given
     * search string and return the first best match.
     *
     * @param node the root program node
     * @param searchString if numeric, the observation id, otherwise the start of the title
     * @param startHere if not null, start the search after this observation
     * @return the first observation found, or null.
     * @
     */
    public static ISPObservation search(IDBDatabaseService db, ISPContainerNode node, boolean searchGroups,
                                        String searchString, ISPObservation startHere) {

        DBSearchService f = new DBSearchService(searchGroups, searchString, startHere);
        f.execute(db, node);
        return f.getResult();
    }
}
