package edu.gemini.spModel.smartgcal.repository;

import java.awt.event.ActionEvent;
import java.io.Serializable;
import java.util.List;

public class CalibrationUpdateEvent extends ActionEvent implements Serializable {

    public static final String UPDATED = "UPDATED";
    private final List<String> updatedFiles;
    private final List<String> failedFiles;

    public CalibrationUpdateEvent(Object o, List<String> udpatedFiles, List<String> failedFiles) {
        super(o, ActionEvent.ACTION_PERFORMED, UPDATED);
        this.updatedFiles = udpatedFiles;
        this.failedFiles = failedFiles;
    }

    public List<String> getUpdatedFiles() {
        return updatedFiles;
    }

    public List<String> getFailedFiles() {
        return failedFiles;
    }

}