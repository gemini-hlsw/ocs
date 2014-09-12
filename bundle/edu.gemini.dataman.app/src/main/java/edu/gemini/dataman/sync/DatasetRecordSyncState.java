//
// $Id: DatasetRecordSyncState.java 617 2006-11-22 21:39:46Z shane $
//

package edu.gemini.dataman.sync;

import edu.gemini.spModel.dataset.*;

import java.io.Serializable;

// TODO: since DatasetRecord is now immutable, this class doesn't really need
// to exist anymore.  All it is doing at this point is providing a slightly
// different API into the dataset information.

/**
 *
 */
final class DatasetRecordSyncState implements Serializable {
    private static final long serialVersionUID = 1;

    private final DatasetRecord rec;

    public DatasetRecordSyncState(DatasetRecord rec) { this.rec = rec; }

    public DatasetLabel getLabel() { return rec.qa.label; }

    public String getFilename() {
        final String name = rec.exec.getDataset().getDhsFilename();
        return name.endsWith(".fits") ? name : String.format("%s.fits", name);
    }

    public DatasetQaState getQaState() { return rec.qa.qaState; }
    public DatasetFileState getFileState() { return rec.exec.fileState; }
    public GsaState getGsaState() { return rec.exec.gsaState; }
    public long getSyncTime() { return rec.exec.syncTime; }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("DatasetRecordSyncState [");
        buf.append("label=").append(getLabel());
        buf.append(", filename=").append(getFilename());
        buf.append(", qaState=").append(getGsaState());
        buf.append(", fileState=").append(getFileState());
        buf.append(", gsaState=").append(getGsaState());
        buf.append(", syncTime=").append(getSyncTime());
        buf.append("]");
        return buf.toString();
    }
}
