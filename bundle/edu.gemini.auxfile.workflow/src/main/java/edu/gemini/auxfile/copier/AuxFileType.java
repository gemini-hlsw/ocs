package edu.gemini.auxfile.copier;

import edu.gemini.auxfile.api.AuxFile;

import java.io.File;

/**
 * The supported types of auxiliary files.
 */
public enum AuxFileType {
    fits            ("FITS",                                true),
    sed             ("Spectral Energy Distribution",        false),
    other           ("other",                               true)
    ;

    private final String displayName;
    private final boolean sendNotification;

    AuxFileType(final String displayName, final boolean sendNotification) {
        this.displayName      = displayName;
        this.sendNotification = sendNotification;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean sendNotification() {
        return sendNotification;
    }

    public static AuxFileType getFileType(final File f) {
        return getFileType(f.getName());
    }

    public static AuxFileType getFileType(final AuxFile f) {
        return getFileType(f.getName());
    }

    public static AuxFileType getFileType(final String name) {
        if      (name.toLowerCase().contains(".fits")) return fits;
        else if (name.toLowerCase().endsWith(".sed"))  return sed;
        else return other;
    }
}
