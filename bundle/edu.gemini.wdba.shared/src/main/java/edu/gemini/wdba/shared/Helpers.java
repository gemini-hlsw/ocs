package edu.gemini.wdba.shared;

import edu.gemini.pot.sp.SPObservationID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Creaqted by Gemini Observatory HLDG
 */
public class Helpers {

    // The name of the id attribute in the Map of observation IDs and Titles
    // and the name of the title attribute in the Map of observation IDs and Titles
    enum MapKey {
        OBS_ID,
        TITLE,
    }

    public static List<Map<String, String>> toListOfMaps(List<QueuedObservation> qobsList) {
        List<Map<String, String>> result = new ArrayList<Map<String, String>>(qobsList.size());
        for (QueuedObservation qo : qobsList) {
            result.add(queuedObsToMap(qo));
        }
        return result;
    }

    public static Map<String, String> queuedObsToMap(QueuedObservation qo) {
        Map<String, String> result = new HashMap<String, String>();
        if (qo == null) return result;

        result.put(MapKey.OBS_ID.name(), qo.getId().stringValue());
        result.put(MapKey.TITLE.name(), qo.getTitle());
        return result;
    }

    public static QueuedObservation toQueuedObservation(Map<String, String> hash) throws WdbaHelperException {
        if (hash.size() == 0) return null;

        // Set the observation ID
        SPObservationID obsId;
        String obsIdString = _getValue(MapKey.OBS_ID, hash);
        try {
            obsId = new SPObservationID(obsIdString);
        } catch (Exception ex) {
            throw WdbaHelperException.create("illegal OBS_ID: " + obsIdString, ex);
        }

        // Title
        String title = _getValue(MapKey.TITLE, hash);

        return new QueuedObservation(obsId, title);
    }

    private static String _getValue(MapKey key, Map<String, String> hash) throws WdbaHelperException {
        String val = hash.get(key.name());
        if (val == null) throwMissing(key);
        return val;
    }

    private static void throwMissing(MapKey key) throws WdbaHelperException {
        throw new WdbaHelperException("missing '" + key.name() + "'");
    }


    public static List<QueuedObservation> toListOfQueuedObservation(List<Map<String, String>> obsList) throws WdbaHelperException {
        List<QueuedObservation> result = new ArrayList<QueuedObservation>(obsList.size());

        for (Map<String, String> oneMap : obsList) {
            QueuedObservation oneResult = toQueuedObservation(oneMap);
            if (oneResult != null) result.add(oneResult);
        }

        return result;
    }
}
