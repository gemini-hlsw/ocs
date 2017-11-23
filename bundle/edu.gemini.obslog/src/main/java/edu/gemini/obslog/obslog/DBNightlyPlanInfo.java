package edu.gemini.obslog.obslog;

import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DBNightlyPlanInfo implements Serializable, Comparable<DBNightlyPlanInfo> {

    private static final long serialVersionUID = 1L;

    // The nightly record title
    private String _title;

    // the nightly record id
    private String _planID;

    // last modified timestamp
    private long _timestamp;

    private static DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MMM-dd HH:mm:ss")
            .withZone(ZoneId.of("UTC"));

    private String _lastModified;

    /**
     * Construct a DBNightlyPlanInfo transfer object
     *
     * @param title     the plan title
     * @param planID    the plan id
     * @param timestamp the plan's last modification time
     */
    public DBNightlyPlanInfo(String title, String planID, long timestamp) {
        _title = title;
        _planID = planID;
        _timestamp = timestamp;
        _lastModified = dateFormat.format(Instant.ofEpochMilli(_timestamp));
    }

    public String getTitle() {
        return _title;
    }

    public String getPlanID() {
        return _planID;
    }

    public String getLastmodified() {
        return _lastModified;
    }

    public long getTimestamp() {
        return _timestamp;
    }

    public int compareTo(DBNightlyPlanInfo o) {
        if (_planID != null) {
            return _planID.compareTo(o._planID);
        }
        return 0;
    }

}
