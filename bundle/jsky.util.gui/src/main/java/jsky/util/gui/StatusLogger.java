package jsky.util.gui;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * This defines an interface for logging status messages. In a user interface,
 * this can be implemented as a progress bar and test message.
 *
 * @see jsky.util.gui.StatusPanel
 */
public interface StatusLogger {

    /** Log or display the given message */
    void logMessage(String msg);

    /** Set the percent done. */
    void setProgress(int percent);

    /**
     * Return a connection to the given URL and log messages before and after
     * opening the connection.
     */
    URLConnection openConnection(URL url) throws IOException;

    /**
     * Return a input stream that will generate log messages showing
     * the progress of the read from the given stream.
     *
     * @param in the input stream to be monitored
     * @param size the size in bytes of the date to be read, or 0 if not known
     */
    ProgressBarFilterInputStream getLoggedInputStream(InputStream in, int size) throws IOException;

    /**
     * Return an input stream to use for reading from the given URL
     * that will generate log messages showing the progress of the read.
     *
     * @param url the URL to read
     */
    ProgressBarFilterInputStream getLoggedInputStream(URL url) throws IOException;

    /**
     * Stop logging reads from the input stream returned from an
     * earlier call to getLoggedInputStream().
     *
     * @param in an input stream returned from getLoggedInputStream()
     */
    void stopLoggingInputStream(ProgressBarFilterInputStream in) throws IOException;

    /** Interrupt the connection */
    void interrupt();

    /**
     * Start displaying something in the progress bar
     */
    void start();

    /**
     * Stop displaying anything in the progress bar.
     */
    void stop();
}

