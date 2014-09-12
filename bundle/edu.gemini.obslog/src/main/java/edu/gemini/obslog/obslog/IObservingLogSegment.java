package edu.gemini.obslog.obslog;

import edu.gemini.obslog.config.model.OlLogItem;
import edu.gemini.obslog.core.OlSegmentType;

import java.util.List;

//
// Gemini Observatory/AURA
// $Id: IObservingLogSegment.java,v 1.2 2006/12/05 14:56:16 gillies Exp $
//

public interface IObservingLogSegment {

    /**
     * Return a String that describes the segment.
     *
     * @return A string suitable for a table caption
     */
    String getSegmentCaption();

    /**
     * Return the type of the segment.
     *
     * @return an instance of <code>OlSegmentType</code>.
     */
    OlSegmentType getType();

    /**
     * Return the number of observations in the segment.
     */
    int getSize();

    /**
     * Return all rows.
     */
    List<ConfigMap> getRows();

    /**
     * Merge the segment that is the argument to the current segment if the types are the same.  The argument segment
     * is added to the end of the current segment.
     * @param segment the segment to be merged to the current segment
     * @return true if successful, else false
     */
    boolean mergeSegment(IObservingLogSegment segment);

    /**
     * Return the log items for this segment as {@link edu.gemini.obslog.config.model.OlLogItem OlLogItem} objects.
     *
     * @return <tt>List</tt> of <tt>OlLogItem</tt>s.
     */
    List<OlLogItem> getTableInfo();

    /**
     * Return the log items for this segment as {@link edu.gemini.obslog.config.model.OlLogItem OlLogItem} objects.
     * Only the visible items are returned.
     *
     * @return <tt>List</tt> of visible log items.
     */
    List<OlLogItem> getVisibleTableInfo();

    /**
     * Diagnostic to dump contents of one segment.
     */
    void dump();

}
