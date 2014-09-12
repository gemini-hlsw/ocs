//
// $Id: DsetRecordChange.java 695 2006-12-12 22:24:34Z shane $
//

package edu.gemini.datasetrecord.impl.trigger;

import edu.gemini.spModel.dataset.DatasetLabel;
import edu.gemini.spModel.dataset.DatasetRecord;

import java.io.Serializable;

/**
 * A description of a change to a DatasetRecord.  Holds the original and updated
 * values of the DatasetRecord.
 */
public final class DsetRecordChange implements Serializable {
    public final DatasetRecord oldRecord;
    public final DatasetRecord newRecord;

    public DsetRecordChange(DatasetRecord oldVersion, DatasetRecord newVersion) {
        oldRecord = oldVersion;
        newRecord = newVersion;
    }

    private static boolean _equals(Object o1, Object o2) {
        if (o1 == null) return o2 == null;
        return o2 != null && o1.equals(o2);
    }

    private static void _diff(DatasetLabel label, String name, Object o1, Object o2, StringBuilder buf) {
        if (_equals(o1, o2)) return;

        if (buf.length() == 0) {
            buf.append(label);
            buf.append(": ");
        } else {
            buf.append(", ");
        }
        buf.append('{');
        buf.append(name);
        buf.append(": ");
        buf.append(o1);
        buf.append(" -> ");
        buf.append(o2);
        buf.append('}');
    }

    /**
     * Gets a human readable String detailing the differences between the old
     * and new DatasetRecords.
     */
    public String getDifferences() {
        final StringBuilder buf = new StringBuilder();

        if (newRecord == null) {
            if (oldRecord != null) buf.append(oldRecord.getLabel()).append(" deleted");
        } else if (oldRecord == null) {
            buf.append(newRecord.getLabel()).append(" added");
        } else {
            final DatasetLabel label = newRecord.getLabel();
            _diff(label, "dataset",    oldRecord.exec.dataset,   newRecord.exec.dataset,   buf);
            _diff(label, "file state", oldRecord.exec.fileState, newRecord.exec.fileState, buf);
            _diff(label, "qa state",   oldRecord.qa.qaState,     newRecord.qa.qaState,     buf);
            _diff(label, "gsa state",  oldRecord.exec.gsaState,  newRecord.exec.gsaState,  buf);
            _diff(label, "sync time",  oldRecord.exec.syncTime,  newRecord.exec.syncTime,  buf);
            _diff(label, "comment",    oldRecord.qa.comment,     newRecord.qa.comment,     buf);
        }

        return buf.toString();
    }
}
