package jsky.util.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import javax.swing.*;

/**
 * Displays a progress bar and a text field together in a panel and is
 * intended to be used as a status panel, displaying the status of downloads, etc.
 */
public class StatusPanel extends JPanel implements StatusLogger, SwingConstants {

    /**
     * Displays progress of download
     */
    JProgressBar progressBar;

    /**
     * Text field used to display status information
     */
    JTextField textField;

    /**
     * Used to open a connection in a background thread
     */
    ConnectionUtil connectionUtil;

    /**
     * Construct a StatusPanel using the given layout positions.
     *
     * @param barPosition  BorderLayout position of the progress bar
     * @param textPosition BorderLayout position of the message text
     */
    public StatusPanel(String barPosition, String textPosition) {
        setLayout(new BorderLayout());

        progressBar = new JProgressBar();
        progressBar.setToolTipText("Download progress");
        progressBar.setBorder(BorderFactory.createLoweredBevelBorder());
        add(progressBar, barPosition);

        // make bar thin if displayed over or under message
        if (barPosition.equals(BorderLayout.NORTH) || barPosition.equals(BorderLayout.SOUTH)) {
            Dimension d = progressBar.getPreferredSize();
            d.height -= 4;
            progressBar.setPreferredSize(d);
        }

        textField = new JTextField(0);
        textField.setEditable(false);
        textField.setToolTipText("Progress messages");
        add(textField, textPosition);
        textField.setBackground(getBackground());
        textField.setBorder(BorderFactory.createLoweredBevelBorder());
    }


    /**
     * Construct a StatusPanel using the default layout.
     */
    public StatusPanel() {
        this(BorderLayout.WEST, BorderLayout.CENTER);
    }


    /**
     * Return the text field used to display the message
     */
    public JTextField getTextField() {
        return textField;
    }

    /**
     * Return the progress bar
     */
    public JProgressBar getProgressBar() {
        return progressBar;
    }

    /**
     * Set the text to display (short cut for: getTextField().setText(s))
     */
    public void setText(final String s) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> textField.setText(s));
            return;
        }
        textField.setText(s);
    }


    // -- These methods implement the StatusLogger interface --


    /**
     * Log or display the given message
     */
    public void logMessage(final String msg) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> setText(msg));
            return;
        }
        setText(msg);
    }

    /**
     * Set the percent done. A 0 value resets the bar and hides the percent value.
     */
    public void setProgress(final int percent) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> setProgress(percent));
            return;
        }

        progressBar.setIndeterminate(false);
        progressBar.setValue(percent);
        if (percent <= 0) {
            progressBar.setStringPainted(false);
        } else {
            progressBar.setStringPainted(true);
        }
    }


    /**
     * Return a connection to the given URL and log messages before and after
     * opening the connection.
     */
    public URLConnection openConnection(URL url) throws IOException {
        String host = url.getHost();
        URLConnection connection;
        if (host != null && host.length() != 0) {
            logMessage("Connect: " + host + ", waiting for reply.");
            connectionUtil = new ConnectionUtil(url);
            connection = connectionUtil.openConnection();
            logMessage("Connected to " + host);
        } else {
            connection = url.openConnection();
        }
        return connection;
    }


    /**
     * Return a input stream that will generate log messages showing
     * the progress of the read from the given stream.
     *
     * @param in   the input stream to be monitored
     * @param size the size in bytes of the date to be read, or 0 if not known
     */
    public ProgressBarFilterInputStream getLoggedInputStream(InputStream in, int size) {
        return new ProgressBarFilterInputStream(progressBar, textField, in, size);
    }


    /**
     * Return an input stream to use for reading from the given URL
     * that will generate log messages showing the progress of the read.
     *
     * @param url the URL to read
     */
    public ProgressBarFilterInputStream getLoggedInputStream(URL url) {
        return new ProgressBarFilterInputStream(progressBar, textField, url);
    }

    /**
     * Stop logging reads from the input stream returned from an
     * earlier call to getLoggedInputStream().
     *
     * @param in an input stream returned from getLoggedInputStream()
     */
    public void stopLoggingInputStream(ProgressBarFilterInputStream in) throws IOException {
        in.close();
        if (in.isInterrupted())
            throw new ProgressException("Reading was interrupted");
    }


    /**
     * Interrupt the connection
     */
    public void interrupt() {
        if (connectionUtil != null) {
            connectionUtil.interrupt();
            connectionUtil = null;
        }
    }

    /**
     * Start displaying something in the progress bar
     */
    public void start() {
        // make sure this is done in the event dispatch thread
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::start);
            return;
        }

        getProgressBar().setIndeterminate(true);
    }


    /**
     * Stop displaying anything in the progress bar.
     */
    public void stop() {
        // make sure this is done in the event dispatch thread
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::stop);
            return;
        }

        getProgressBar().setIndeterminate(false);
        setText("");
        getProgressBar().setStringPainted(false);
        getProgressBar().setValue(0);
    }
}


