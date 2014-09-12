//
// $Id: HeaderItemUtil.java 37 2005-08-20 17:46:18Z shane $
//

package edu.gemini.fits;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility methods for working with {@link HeaderItem}.
 */
public final class HeaderItemUtil {
    private HeaderItemUtil() {
    }

    /**
     * Creates a Map of {@link HeaderItem} where the keys are the String
     * keywords of the HeaderItem.  Obviously, if two {@link HeaderItem}s in
     * the collection have the same keyword, only one will appear in the Map.
     * No guarantees are made about <em>which</em> one will appear.
     *
     * @param items the {@link HeaderItem}s to hash into a Map
     *
     * @return Map of {@link HeaderItem} keyed by keyword
     */
    public static <H extends HeaderItem> Map<String, H> hash(Collection<H> items) {
        Map<String, H> res = new HashMap<String, H>();

        for ( H item : items ) {
            res.put(item.getKeyword(), item);
        }

        return res;
    }
}
