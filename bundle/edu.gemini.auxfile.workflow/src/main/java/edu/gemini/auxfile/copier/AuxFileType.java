package edu.gemini.auxfile.copier;

import java.io.File;

/**
 * The supported types of auxiliary files.
 */
public enum AuxFileType {
    fits("FITS"),
    other("other"),
    ;

    private String _displayName;

    private AuxFileType(String displayName) {
        _displayName = displayName;
    }

    public String getDisplayName() {
        return _displayName;
    }

    public static AuxFileType getFileType(File f) {
        String name = f.getName();
        return name.toLowerCase().contains(".fits") ? fits : other;
    }
}
