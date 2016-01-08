package jsky.util.gui;

import javax.swing.*;

import jsky.util.FileUtil;

import java.io.FilterInputStream;
import java.io.InputStream;
import java.net.URL;
import java.io.IOException;

/**
 * Monitors reading from a given stream or URL and updates a given progress
 * bar and text field to show the amount of data read so far.
 */
public class ProgressBarFilterInputStream extends FilterInputStream {

    /**
     * The progress bar to use
     */
    private final JProgressBar progressBar;

    /**
     * Text field used to display status information
     */
    private final JTextField statusField;

    /**
     * The model for the progress bar
     */
    private DefaultBoundedRangeModel model;

    /**
     * The number of bytes read so far
     */
    private int nread = 0;

    /**
     * The size of the data in bytes, if known, otherwise 0
     */
    private int size = 0;

    /**
     * Time in ms of last update (used to slow down text field updates)
     */
    private long updateTime = 0L;

    /**
     * Set this to interrupt the reading and throw an exception
     */
    private volatile boolean interrupted = false;


    /**
     * Constructs an object to monitor the progress of an input stream
     * using a given progress bar and text field.
     *
     * @param progressBar the progress bar to use
     * @param statusField text field used to display status information
     * @param in          the input stream to be monitored
     * @param size        the size in bytes of the date to be read, or 0 if not known
     */
    public ProgressBarFilterInputStream(JProgressBar progressBar, JTextField statusField, InputStream in, int size) {
        super(in);
        this.progressBar = progressBar;
        this.statusField = statusField;
        setSize(size);
    }

    /**
     * Constructs an object to monitor the progress of an input stream
     * using a given progress bar and text field.
     *
     * @param progressBar the progress bar to use
     * @param statusField text field used to display status information
     * @param url         the URL to read
     */
    public ProgressBarFilterInputStream(JProgressBar progressBar, JTextField statusField, URL url) {
        super(FileUtil.makeURLStream(url));
        this.progressBar = progressBar;
        this.statusField = statusField;

        progressBar.setIndeterminate(true);
        statusField.setText("Connect: Host " + url.getHost());
        try {
            int size = url.openConnection().getContentLength();
            progressBar.setIndeterminate(false);
            setSize(size);
        } catch (Exception e) {
            statusField.setText(e.getMessage());
            progressBar.setIndeterminate(false);
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        statusField.setText("Connected to Host " + url.getHost());
    }

    /**
     * Interrupt the reading (causes the next read() to throw an exception).
     * This is normally called when a Stop or Cancel button is pushed.
     */
    public void interrupt() {
        interrupted = true;
        progressBar.setIndeterminate(false);
        progressBar.setStringPainted(false);
        if (model != null)
            model.setValue(0);
        statusField.setText("Reading interrupted.");
    }

    /**
     * Return true if reading was interrupted
     */
    public boolean isInterrupted() {
        return interrupted;
    }

    /**
     * Throw an exception if interrupt() was called on this stream.
     */
    public void checkForInterrupt() throws IOException {
        if (interrupted) {
            throw new ProgressException("Reading interrupted");
        }
    }

    /**
     * Set the size of the data to read
     */
    public void setSize(final int size) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> setSize(size));
            return;
        }

        this.size = size;
        if (size <= 0) {
            model = null;
            progressBar.setIndeterminate(true);
            progressBar.setStringPainted(false);
        } else {
            progressBar.setIndeterminate(false);
            model = new DefaultBoundedRangeModel(0, 0, 0, size);
            progressBar.setModel(model);
            progressBar.setStringPainted(true);
        }
    }


    /**
     * Set the number of bytes that have been read, update the display (but not
     * too often) and check for interrupt requests.
     */
    protected void setNumBytesRead(final int n) {
        nread = n;

        // delay update to improve performance
        long t = System.currentTimeMillis();
        if ((t - updateTime) > 200) {
            if (!SwingUtilities.isEventDispatchThread()) {
                SwingUtilities.invokeLater(() -> setNumBytesRead(n));
                return;
            }

            if (model != null) {
                progressBar.setIndeterminate(false);
                model.setValue(nread);
            }
            statusField.setText("Reading File: " + nread + " bytes");
            updateTime = t;
        }
    }


    /**
     * Reset the progress bar to the idle state
     */
    public void clear() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::clear);
            return;
        }

        nread = 0;
        updateTime = 0L;

        progressBar.setIndeterminate(false);
        progressBar.setStringPainted(false);
        statusField.setText("Document Done");
        if (model != null)
            model.setValue(0);
    }


    /**
     * Return the size of the data to read
     */
    public int getSize() {
        return size;
    }


    /**
     * Overrides <code>FilterInputStream.read</code>
     * to update the progress bar after the read.
     */
    public int read() throws IOException {
        checkForInterrupt();
        int c = in.read();
        if (c >= 0)
            setNumBytesRead(nread + 1);
        else
            clear();

        return c;
    }


    /**
     * Overrides <code>FilterInputStream.read</code>
     * to update the progress bar after the read.
     */
    public int read(byte b[], int off, int len) throws IOException {
        checkForInterrupt();
        int nr = in.read(b, off, len);
        if (nr > 0)
            setNumBytesRead(nread + nr);
        else if (nr == -1)
            clear();
        return nr;
    }


    /**
     * Overrides <code>FilterInputStream.skip</code>
     * to update the progress bar after the skip.
     */
    public long skip(long n) throws IOException {
        checkForInterrupt();
        long nr = in.skip(n);
        if (nr > 0)
            setNumBytesRead(nread + (int) nr);
        return nr;
    }


    /**
     * Overrides <code>FilterInputStream.close</code>
     * to close the progress bar as well as the stream.
     */
    public void close() throws IOException {
        in.close();
        clear();
    }


    /**
     * Overrides <code>FilterInputStream.reset</code>
     * to reset the progress bar as well as the stream.
     */
    public synchronized void reset() throws IOException {
        in.reset();
        clear();
    }
}
