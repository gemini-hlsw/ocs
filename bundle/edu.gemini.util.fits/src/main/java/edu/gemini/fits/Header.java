//
// $Id: Header.java 37 2005-08-20 17:46:18Z shane $
//

package edu.gemini.fits;

import java.util.List;
import java.util.Set;

/**
 * A FITS header is a list of {@link HeaderItem}, with some additional methods
 * geared toward fast lookup by keyword.
 */
public interface Header extends List<HeaderItem> {
    /**
     * Finds the first {@link HeaderItem} associated with the given
     * <code>keyword</code>, if any.  If more than one HeaderItem has the same
     * keyword, only the first is returned.
     *
     * @param keyword keyword of the HeaderItem to retrieve
     *
     * @return first {@link HeaderItem} whose keyword matches
     * <code>keyword</code>; <code>null</code> if none
     */
    HeaderItem get(String keyword);

    /**
     * Returns all the {@link HeaderItem}s whose keyword matches the given
     * <code>keyword</code> argument.  HeaderItems are retrieved in the order
     * that they appear in the FITS header.  The return value may be
     * manipulated by the caller without impacting the internal state of this
     * Header
     *
     * @param keyword keyword of the HeaderItem(s) to retrieve
     *
     * @return List of HeaderItem with the given <code>keyword</code> in the
     * order that they appear in the FITS header; <code>null</code> if there
     * are no matching HeadterItems
     */
    List<HeaderItem> getAll(String keyword);

    /**
     * Gets a Set containing all the keywords of all the HeaderItems in this
     * Header.
     *
     * @return Set of all keywords of all HeaderItems in this Header
     */
    Set<String> getKeywords();


    /**
     * Return the relative location of this header in a particular FITS file.
     * The Primary Header will return 0, whereas  extensions will return
     * an index indicating the extension number.
     * @return index of this header. 0 is for the primary header, and a
     * positive integer is used to uniquely identify an extension
     */
    int getIndex();
}
