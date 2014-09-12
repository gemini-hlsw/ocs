//
// $Id: DefaultTemplate.java 135 2005-09-14 23:27:17Z shane $
//

package edu.gemini.datasetrecord.util;

import edu.gemini.datasetrecord.DatasetRecordTemplate;
import edu.gemini.spModel.dataset.DatasetQaState;
import edu.gemini.spModel.dataset.DatasetFileState;
import edu.gemini.spModel.dataset.GsaState;

/**
 * Default implementation of the {@link DatasetRecordTemplate} available for
 * clients who wish to use it.
 */
public class DefaultTemplate implements DatasetRecordTemplate {
    private String _comment;
    private DatasetQaState _qaState;
    private DatasetFileState _fileState;
    private GsaState _gsaState;
    private Long _syncTime;

    public DefaultTemplate() {
    }

    public DefaultTemplate(DefaultTemplate that) {
        _qaState    = that._qaState;
        _fileState  = that._fileState;
        _gsaState   = that._gsaState;
        _syncTime   = that._syncTime;
        _comment    = that._comment;
    }

    public String getComment() {
        return _comment;
    }

    public void setComment(String comment) {
        _comment = comment;
    }

    public DatasetQaState getQaState() {
        return _qaState;
    }

    public void setQaState(DatasetQaState state) {
        _qaState = state;
    }

    public Long getSyncTime() {
        return _syncTime;
    }

    public void setSyncTime(Long syncTime) {
        _syncTime = syncTime;
    }

    public DatasetFileState getDatasetFileState() {
        return _fileState;
    }

    public void setDatasetFileState(DatasetFileState state) {
        _fileState = state;
    }

    public GsaState getGsaState() {
        return _gsaState;
    }

    public void setGsaState(GsaState dataflowState) {
        _gsaState = dataflowState;
    }
}
