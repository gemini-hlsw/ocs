package edu.gemini.obslog.config.model;

import edu.gemini.obslog.core.OlSegmentType;

import java.io.Serializable;
import java.util.*;
import java.util.logging.Logger;

//
// Gemini Observatory/AURA
// $Id: OlBasicConfiguration.java,v 1.5 2005/12/11 15:54:15 gillies Exp $
//

/**
 * OlBasicConfiguration is the top level model of configuration information.
 */
public class OlBasicConfiguration implements OlConfiguration, Serializable {
    public static final Logger LOG = Logger.getLogger(OlBasicConfiguration.class.getName());

    private String _VERSION = null;
    private LogItems _allLogItems;
    private ObsLogs _allObsLogs;

    public OlBasicConfiguration(String version) {
        if (version == null) throw new NullPointerException();

        _VERSION = version;
    }

    // Internal class to model the keyed set of entries
    private class LogItems implements Serializable {
        private Map<String, OlLogItem> items = new HashMap<String, OlLogItem>();

        void addLogItem(OlLogItem logItem) {
            if (logItem == null) throw new NullPointerException();
            items.put(logItem.getKey(), logItem);
        }

        OlLogItem addLogItem(String key) {
            if (key == null) throw new NullPointerException();
            OlLogItem logItem = new OlBasicLogItem(key);
            addLogItem(logItem);
            return logItem;
        }

        /**
         * Returns the group specified by the group key
         *
         * @param key the entry
         * @return OlLogItem associated with the key or <code>null</code> if it doesn't exist.
         */
        OlLogItem getLogItem(String key) {
            return items.get(key);
        }

        /**
         * Returns the number of log items that have been created
         *
         * @return int number of <tt>LogItems</tt>
         */
        int getSize() {
            return items.size();
        }

        Iterator iterator() {
            return items.keySet().iterator();
        }
    }

    /**
     * This class is a <tt>Hashmap</tt> of instrument names to OlBasicLogEntries which are a list of entries.
     */
    private class ObsLogs implements Serializable {
        private Map<String, OlObsLogData> obsLogs = new HashMap<String, OlObsLogData>();

        void addLogData(OlObsLogData obsLogData) {
            obsLogs.put(obsLogData.getKey(), obsLogData);
        }

        OlObsLogData addLogData(String logKey) {
            OlObsLogData obsLogData = new OlBasicLogData(logKey);
            addLogData(obsLogData);
            return obsLogData;
        }

        OlLogItem addItemToObsLog(String logKey, String itemKey) throws OlModelException {
            OlObsLogData obsLogData = getLogData(logKey);
            if (obsLogData == null) {
                obsLogData = addLogData(logKey);
            }
            return obsLogData.addLogItem(itemKey);
        }

        /**
         * Returns a OlObsLogData for the given key or null if not present.
         *
         * @param logKey The name by which the obslog is referred
         * @return The logEntry.  Note that if the key hasn't been created, one will be added and it will be empty.
         */
        OlObsLogData getLogData(String logKey) {
            return obsLogs.get(logKey);
        }

        OlObsLogData getDataForLogByType(String narrowType) {
            // Temporary object for comparisons
            OlSegmentType testType = new OlSegmentType(narrowType);
            for (String key : obsLogs.keySet()) {
                OlObsLogData obsLogData = getLogData(key);
                if (obsLogData.getType().equals(testType)) return obsLogData;
            }
            return null;
        }

        /**
         * Return the number of ObsLogs
         *
         * @return int the number of ObsLogs
         */
        int getSize() {
            return obsLogs.size();
        }
    }

    // Internal class to model one LogEntry which is a list of OlLogItems
    // Key is the name of an instrument or obslog containing a list of logItems
    private class OlBasicLogData implements OlObsLogData, Serializable {
        private String _key;
        private List<OlLogItem> _items = new ArrayList<OlLogItem>();
        private OlSegmentType _type;

        OlBasicLogData(String key) {
            if (key == null) throw new NullPointerException("A OlObsLogData must have a key");
            _key = key;
            _type = new OlSegmentType(key);
        }

        public String getKey() {
            return _key;
        }

        public OlSegmentType getType() {
            return _type;
        }

        public OlLogItem addLogItem(String key) throws OlModelException {
            // Check to see if the logItem exists since they all should.
            // If the _ogItem doesn't exist, it's a configuration error so throw
            OlLogItem logItem = _getLogItems().getLogItem(key);
            if (logItem == null) {
                String message = "Attempt to add a undefined _logItem: " + key;
                LOG.severe(message);
                throw new OlModelException(message);
            }
            _items.add(logItem);
            return logItem;
        }

        public OlLogItem getLogItem(String itemKey) {
            for (int i = 0, size = _items.size(); i < size; i++) {
                OlLogItem item = _items.get(i);
                if (item.getKey().equals(itemKey)) return item;
            }
            return null;
        }

        public OlLogItem getBySequenceName(String sequenceName) {
            for (int i = 0, size = _items.size(); i < size; i++) {
                OlLogItem item = _items.get(i);
                if (sequenceName.equals(item.getSequenceName())) return item;
            }
            return null;
        }

        public List<OlLogItem> getLogTableData() {
            return _items;
        }

        public Iterator iterator() {
            return _items.iterator();
        }

        /**
         * Returns the number of entries in a OlObsLogData
         *
         * @return int the number of entries
         */
        public int getSize() {
            return _items.size();
        }
    }

    public String getVersion() {
        return _VERSION;
    }

    private ObsLogs _getObsLogs() {
        if (_allObsLogs == null) {
            _allObsLogs = new ObsLogs();
        }
        return _allObsLogs;
    }

    private LogItems _getLogItems() {
        if (_allLogItems == null) {
            _allLogItems = new LogItems();
        }
        return _allLogItems;
    }

    /**
     * public void addObsLogLogEntry(String logKey, String entryKey) throws OlModelException {
     * OlObsLogData logEntry = _logEntries.getLogEntry(logKey);
     * logEntry.addEntryKey(entryKey);
     * <p/>
     * <p/>
     * <p/>
     * <p/>
     * public int getNumberLogEntries() {
     * return _getLogEntries().getSize();
     * }
     * public void addLogItem(String entryKey) {
     * if (entryKey == null) throw new IllegalArgumentException();
     * OlBasicLogItem entry = new OlBasicLogItem(entryKey);
     * _getEntries().addEntry(entry);
     * }
     */

    public OlLogItem getItemInObsLog(String logKey, String itemKey) {
        if (logKey == null || itemKey == null) throw new NullPointerException();

        OlObsLogData obsLogData = _getObsLogs().getLogData(logKey);
        if (obsLogData == null) return null;
        return obsLogData.getLogItem(itemKey);
    }

    public OlLogItem addItemToObsLog(String logKey, String itemKey) throws OlModelException {
        return _getObsLogs().addItemToObsLog(logKey, itemKey);
    }

    public OlLogItem getLogItem(String itemKey) {
        return _getLogItems().getLogItem(itemKey);
    }

    public OlLogItem addLogItem(String itemKey) {
        return _getLogItems().addLogItem(itemKey);
    }

    public OlObsLogData getDataForLog(String logKey) {
        return _getObsLogs().getLogData(logKey);
    }

    public OlObsLogData getDataForLogByType(String logKey) {
        return _getObsLogs().getDataForLogByType(logKey);
    }

    public int getNumberObsLogs() {
        return _getObsLogs().getSize();
    }

    public int getNumberLogItems() {
        return _getLogItems().getSize();
    }

    public Iterator getObsLogItems(String logKey) {
        OlObsLogData obsLogData = _getObsLogs().getLogData(logKey);
        if (obsLogData == null) return new ArrayList().iterator();
        return obsLogData.iterator();
    }

    public Iterator getItems() {
        return _getLogItems().iterator();
    }
}
