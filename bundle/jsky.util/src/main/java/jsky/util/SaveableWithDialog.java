package jsky.util;

/**
 * An interface for widgets that can pop up a dialog to save their
 * contents to a file.
 */
public interface SaveableWithDialog {

    /**
     * Display a dialog to save the contents of this object to a file.
     */
    void saveAs();
}
