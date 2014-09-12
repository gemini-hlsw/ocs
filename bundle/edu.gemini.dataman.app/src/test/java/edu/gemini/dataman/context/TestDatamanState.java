//
// $
//

package edu.gemini.dataman.context;

/**
 * Test implementation for the {@link DatamanState} interface.
 */
public class TestDatamanState implements DatamanState {
    private long lastModified = 0;

    public synchronized long getRawLastModified() {
        return lastModified;
    }

    public synchronized void setRawLastModified(long time) {
        lastModified = time;
    }

    public synchronized void increaseRawLastModified(long time) {
        if (lastModified < time) {
            lastModified = time;
        }
    }
}
