package edu.gemini.obslog.obslog;

import edu.gemini.obslog.config.model.OlLogItem;
import edu.gemini.obslog.core.OlSegmentType;
import java.util.logging.Logger;
import java.util.logging.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Gemini Observatory/AURA
 * $Id: OlBasicSegment.java,v 1.2 2006/12/05 14:56:16 gillies Exp $
 */
public abstract class OlBasicSegment implements IObservingLogSegment {
    private static final Logger LOG = Logger.getLogger(OlBasicSegment.class.getName());

    // The following list is a list of Maps that can be displayed via DisplayTag
    protected List<ConfigMap> _segmentData;

    // The items that can be displayed
    private List<OlLogItem> _logItems;

    // The segment type
    private OlSegmentType _type;

    public OlBasicSegment(OlSegmentType type, List<OlLogItem> logItems) {
        if (type == null|| logItems == null) throw new NullPointerException();
        _type = type;
        _logItems = logItems;
    }

    /**
     * Return the segment type
     *
     * @return an instance of <tt>{@link OlSegmentType}</tt>
     */
    public OlSegmentType getType() {
        return _type;
    }

    /**
     * Return the number of observations in the segment.
     *
     * @return the number of observations in the segment.
     */
    public int getSize() {
        return _getSegmentDataList().size();
    }

    /**
     * Returns the list to be used for storing observation data.   The contents of this <code>List</code> is objects
     * that are instances of <code>{@link edu.gemini.obslog.obslog.UniqueConfigMap}</code>.
     *
     * @return list of <code>TransferData</code> objects
     */
    protected List<ConfigMap> _getSegmentDataList() {
        if (_segmentData == null) {
            _segmentData = new ArrayList<ConfigMap>();
        }
        return _segmentData;
    }

     /**
     * Return a <code>List</code> of all items in the log configuration as <code>{@link OlLogItem}</code> instances.
     *
     * @return a <code>List</code> of log configuration items
     */
    public List<OlLogItem> getTableInfo() {
        return _logItems;
    }

    public List<OlLogItem> getVisibleTableInfo() {
        List<OlLogItem> l = new ArrayList<OlLogItem>();
        for (int i = 0, size = _logItems.size(); i < size; i++) {
            OlLogItem item = _logItems.get(i);
            if (item.isVisible()) l.add(item);
            LOG.log(Level.FINE, "Visible Info for: " + item.getProperty() + " : " + item.isVisible());
        }
        return l;
    }

    public void dump() {
        List<ConfigMap> rows = _getSegmentDataList();
        for (ConfigMap m : rows) {
            m.dump();
        }
    }

    public List<ConfigMap> getRows() {
        return new ArrayList<ConfigMap>(_getSegmentDataList());
    }

    /**
     * This method will merge the segment in the argument onto the current segment if the types are the same.  The rows
     * are added at the end of the target segment
     * @param segment the segment to merge
     * @return true if the merge was successful, else false
     */
    public boolean mergeSegment(IObservingLogSegment segment) {
        assert segment != null : "merged segment is null";
        if (segment.getType() != getType()) return false;

        _segmentData.addAll(segment.getRows());
        return true;
    }

    /**
     * Return a description of the segment.
     *
     * @return A caption suitable for a table.
     */
    public abstract String getSegmentCaption();

       /**
     * Decorate instrument specific items.
     * Called when the segment builder adds an observation to the log.
     *
     * @param map an instance of <tt>UniqueConfigMap</tt>
     */
    public abstract void decorateObservationData(ConfigMap map);
}
