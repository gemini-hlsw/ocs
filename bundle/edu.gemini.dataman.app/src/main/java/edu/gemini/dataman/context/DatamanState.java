//
// $Id: DatamanState.java 130 2005-09-14 15:59:10Z shane $
//

package edu.gemini.dataman.context;

/**
 * Permanent state information required by the Dataman app.  This state
 * survies restarts of the Dataman app.
 */
public interface DatamanState {

    /**
     * The timestamp of the last modification to the raw directory that was
     * noticed by the Dataman app, if any.  This is used at startup to
     * determine which datasets in the raw storage area should be discovered.
     *
     * @return last modification time that was processed by the Dataman app, or
     * <code>-1</code> if none
     */
    long getRawLastModified();

    /**
     * Sets the last modification time to the given value.
     *
     * @param time time in milliseconds that should be recorded
     */
    void setRawLastModified(long time);

    /**
     * Increases the last modification time to the given value.  This method
     * differs from {@link #setRawLastModified(long)} in that it does nothing
     * if the current modification time has increased beyond the value
     * provided in the <code>time</code> parameter.
     *
     * @param time time in milliseconds that should be recorded
     */
    void increaseRawLastModified(long time);
}
