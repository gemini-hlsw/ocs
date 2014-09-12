//
// $Id: DsetRecordEventImpl.java 617 2006-11-22 21:39:46Z shane $
//

package edu.gemini.datasetrecord.impl;

import edu.gemini.datasetrecord.DatasetRecordEvent;
import edu.gemini.datasetrecord.DatasetRecordService;
import edu.gemini.spModel.dataset.DatasetRecord;

/**
 *
 */
final class DsetRecordEventImpl implements DatasetRecordEvent {

    private DatasetRecordService _src;
    private DatasetRecord _oldVersion;
    private DatasetRecord _newVersion;

    public DsetRecordEventImpl(DatasetRecordService src, DatasetRecord oldVersion, DatasetRecord newVersion) {
        _src = src;
        _oldVersion = oldVersion;
        _newVersion = newVersion;
    }

    public DatasetRecordService getSource() {
        return _src;
    }

    public DatasetRecord getOldVersion() {
        return _oldVersion;
    }

    public DatasetRecord getNewVersion() {
        return _newVersion;
    }
}
