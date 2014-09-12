//
// $Id: DatasetFileEventImpl.java 144 2005-09-26 21:45:51Z shane $
//

package edu.gemini.datasetfile.impl;

import edu.gemini.datasetfile.DatasetFileEvent;
import edu.gemini.datasetfile.DatasetFileService;
import edu.gemini.datasetfile.DatasetFile;
import edu.gemini.spModel.dataset.DatasetLabel;

import java.io.File;

/**
 *
 */
public final class DatasetFileEventImpl implements DatasetFileEvent {
    private DatasetFileService _service;
    private DatasetFile _dsetFile;
    private File _file;
    private DatasetLabel _label;
    private String _message;

    DatasetFileEventImpl(DatasetFileService service, DatasetFile dsetFile) {
        _service = service;
        _dsetFile = dsetFile;
        if (dsetFile != null) _label = dsetFile.getDataset().getLabel();
    }

    DatasetFileEventImpl(DatasetFileService service, File file, DatasetLabel label, String message) {
        _service = service;
        _file    = file;
        _label   = label;
        _message = message;
    }

    public DatasetFileService getSource() {
        return _service;
    }

    public DatasetFile getDatasetFile() {
        return _dsetFile;
    }

    public File getFile() {
        return _file;
    }

    public DatasetLabel getLabel() {
        return _label;
    }

    public String getMessage() {
        return _message;
    }
}
