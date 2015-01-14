package edu.gemini.ags.gems;

import edu.gemini.model.p1.immutable.SiderealTarget;
import edu.gemini.shared.skyobject.Magnitude;
import edu.gemini.shared.skyobject.SkyObject;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.core.Target;
import edu.gemini.spModel.target.SPTarget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility methods for local use
 */
public class GemsUtil {

    /**
     * Removes any duplicates from the list
     */
    public static List<Target.SiderealTarget> removeDuplicates(List<Target.SiderealTarget> list) {
        Map<String, Target.SiderealTarget> map = new HashMap<>(list.size());
        for(Target.SiderealTarget siderealTarget : list) {
            String name = siderealTarget.name();
            if (name != null) {
                map.put(name, siderealTarget);
            }
        }
        return new ArrayList<>(map.values());
    }

    /**
     * Returns a list of unique SkyObjects in the given search results.
     */
    public static List<Target.SiderealTarget> getUniqueSkyObjects(List<GemsCatalogSearchResults> list) {
        List<Target.SiderealTarget> result = new ArrayList<>();
        for(GemsCatalogSearchResults searchResults : list) {
            for(Target.SiderealTarget skyObject : searchResults.resultsAsJava()) {
                result.add(skyObject);
            }
        }
        return removeDuplicates(result);
    }
}
