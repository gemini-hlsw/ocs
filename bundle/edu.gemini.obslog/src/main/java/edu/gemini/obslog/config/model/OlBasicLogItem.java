package edu.gemini.obslog.config.model;

import java.io.Serializable;
import java.util.logging.Logger;


public class OlBasicLogItem implements OlLogItem, Serializable {
    public static final Logger LOG = Logger.getLogger(OlBasicLogItem.class.getName());

    private String _key;
    private String _heading = OlLogItem.DEFAULT_ENTRY;
    private String _property = OlLogItem.DEFAULT_ENTRY;
    private String _sequenceName = OlLogItem.DEFAULT_ENTRY;
    private boolean _isVisible = true;

    /**
     * Creates a basic observing log entry cell model.
     *
     * @param key the key for this entry
     * @throws java.lang.NullPointerException if <code>key</code> or <code>group</code> is <code>null</code>.
     * @throws java.lang.IllegalArgumentException
     *                                        if the length of <code>key</code> or <code>group</code> is zero.
     */
    public OlBasicLogItem(String key) {
        if (key == null) throw new NullPointerException();
        if (key.length() == 0) throw new IllegalArgumentException("Entry must have a non-zero length key.");

        _key = key;
    }

    /**
     * Return the entry key that for this entry.
     */
    public String getKey() {
        return _key;
    }

    /**
     * Set the column heading.
     *
     * @param heading the name of the heading that will appear in a column that has this entry
     */
    public void setColumnHeading(String heading) {
        _heading = heading;
    }

    /**
     * Return the column heading that matches entries of this type.
     */
    public String getColumnHeading() {
        return _heading;
    }

    /**
     * Set the item name for this entry
     *
     * @param itemName the low-level name for this entry
     */
    public void setProperty(String itemName) {
        _property = itemName;
    }

    /**
     * Return the item name for this entry.
     *
     * @return a string name for this entry
     */
    public String getProperty() {
        return _property;
    }

    /**
     * Set the sequence name for this item.
     *
     * @param sequenceName
     */
    public void setSequenceName(String sequenceName) {
        _sequenceName = sequenceName;
    }

    /**
     * Return the sequence name for this item.
     *
     * @return
     */
    public String getSequenceName() {
        return _sequenceName;
    }

    /**
     * Should this item be visible in the log.
     *
     * @return true if it should be visible
     */
    public boolean isVisible() {
        return _isVisible;
    }

    /**
     * Set the visibility in the logs.
     *
     * @param visible true means is visible
     */
    public void setVisible(boolean visible) {
        _isVisible = visible;
    }

    public void dump() {
        LOG.info("property:     " + getProperty());
        LOG.info("heading:      " + getColumnHeading());
        LOG.info("sequenceName: " + getSequenceName());
        LOG.info("isVisible:    " + isVisible());
    }

}
