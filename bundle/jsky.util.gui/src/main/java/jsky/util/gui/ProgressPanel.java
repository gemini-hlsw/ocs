package jsky.util.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import jsky.util.Resources;

/**
 * A panel to display while a download or other background operation is in
 * progress.
 * <p>
 * This class is designed to be usable from any thread and all GUI access is done
 * synchronously in the event dispatching thread.
 */
public class ProgressPanel extends JPanel implements ActionListener, StatusLogger {

    // Parent of this window (frame or internal frame), used to close the window
    private Component _parent;

    // The title string
    private String _title;

    // Displays the title
    private JLabel _titleLabel;

    // Displays the active GIF icon
    private JLabel _iconLabel;

    // Button to interrupt the task
    private JButton _stopButton;

    // Displays the progress bar and status text
    private StatusPanel _statusPanel;

    // If set, this is the current input stream being monitored
    private ProgressBarFilterInputStream _loggedInputStream;

    // Set to true if the stop button was pressed
    private boolean _interrupted;

    // Used to create a new progress panel in the event dispatching thread
    private static ProgressPanel _newPanel;


    /**
     * Initialize a progress panel with the given title string.
     *
     * @param parent the parent frame or internal frame, used to close the window
     * @param title the title string
     */
    public ProgressPanel(Component parent, String title) {
        this._parent = parent;
        this._title = title;
        init();
    }

    /** Default constructor */
    public ProgressPanel() {
        this(null, "Download in Progress...");
    }

    /** Return the dialog stop button */
    public JButton getStopButton() {return _stopButton;}


    /**
     * Initialize the progress panel. This method may be called from any
     * thread, but will always run in the event dispatching thread.
     */
    protected void init() {
        // make sure this is done in the event dispatch thread
        if (!SwingUtilities.isEventDispatchThread()) {
            invokeAndWait(ProgressPanel.this::init);
            return;
        }
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEtchedBorder());
        JPanel top = new JPanel();
        top.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        top.setLayout(new BorderLayout());
        _titleLabel = new JLabel(_title, SwingConstants.CENTER);
        _titleLabel.setForeground(Color.black);
        top.add(_titleLabel, BorderLayout.WEST);
        _iconLabel = new JLabel(Resources.getIcon("TaskStatusOn.gif"));
        top.add(_iconLabel, BorderLayout.EAST);

        JPanel center = new JPanel();
        _stopButton = new JButton("Stop");
        _stopButton.addActionListener(this);
        center.add(_stopButton);
        top.add(center, BorderLayout.SOUTH);

        _statusPanel = new StatusPanel();
        _statusPanel.getTextField().setColumns(25);

        add(top, BorderLayout.NORTH);
        add(_statusPanel, BorderLayout.SOUTH);
    }


    /** Run the given Runnable synchronously in the event dispatching thread. */
    protected static void invokeAndWait(Runnable r) {
        try {
            SwingUtilities.invokeAndWait(r);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Set the parent frame or internal frame, used to close the window
     */
    public void setParent(Component parent) {
        this._parent = parent;
    }

    /**
     * Set the title string.
     */
    public void setTitle(final String title) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> setTitle(title));
            return;
        }
        this._title = title;
        _titleLabel.setText(title);
    }

    /** Log or display the given message */
    public void logMessage(final String msg) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> _statusPanel.setText(msg));
            return;
        }
        _statusPanel.setText(msg);
    }

    /** Set the status text to display. */
    public void setText(final String s) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> _statusPanel.setText(s));
            return;
        }
        _statusPanel.setText(s);
    }


    /** Add a listener to be called when the user presses the stop button. */
    public void addActionListener(ActionListener l) {
        _stopButton.addActionListener(l);
    }

    /**
     * Called when the Stop button is pressed.
     */
    public void actionPerformed(ActionEvent e) {
        _interrupted = true;
        stop();
    }

    /** Return true if the stop button was pressed */
    public boolean isInterrupted() {
        return _interrupted;
    }

    /** Interrupt the connection */
    public void interrupt() {
        _statusPanel.interrupt();
    }

    /**
     * Return a connection to the given URL and log messages before and after
     * opening the connection.
     */
    public URLConnection openConnection(URL url) throws IOException {
        start();
        URLConnection connection = _statusPanel.openConnection(url);
        if (_interrupted)
            throw new ProgressException("Interrupted");
        return connection;
    }


    /**
     * Display the progress panel. This method may be called from any
     * thread, but will always run in the event dispatching thread.
     */
    public void start() {
        // make sure this is done in the event dispatch thread
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::start);
            return;
        }

        _interrupted = false;
        if (_parent instanceof JFrame)
            ((JFrame) _parent).setState(Frame.NORMAL);
        _parent.setVisible(true);
        _statusPanel.getProgressBar().setIndeterminate(true);
        BusyWin.setBusy(true, _parent);
    }


    /**
     * Stop displaying the progress panel. This method may be called
     * from any thread, but will always run in the event dispatching
     * thread.
     */
    public void stop() {
        // make sure this is done in the event dispatch thread
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(ProgressPanel.this::stop);
            return;
        }

        BusyWin.setBusy(false, _parent);
        if (_loggedInputStream != null) {
            _loggedInputStream.interrupt();
            _loggedInputStream = null;
        }
        _parent.setVisible(false);
        _statusPanel.interrupt();
        _statusPanel.getProgressBar().setIndeterminate(false);
        _statusPanel.setText("");
        _statusPanel.getProgressBar().setStringPainted(false);
        _statusPanel.getProgressBar().setValue(0);
    }


    /**
     * Make a ProgressPanel and frame (or internal frame) and return the panel.
     *
     * @param title the title string
     * @param window window to display the dialog over, may be null
     */
    public static ProgressPanel makeProgressPanel(final String title, final Component window) {
        if (!SwingUtilities.isEventDispatchThread()) {
            invokeAndWait(() -> _newPanel = ProgressPanel.makeProgressPanel(title, window));
            return _newPanel;
        }

        // get the parent frame so that the dialog won't be hidden behind it
        Frame parent;
        JDesktopPane desktop = DialogUtil.getDesktop();
        if (desktop != null)
            parent = SwingUtil.getFrame(desktop);
        else
            parent = SwingUtil.getFrame(window);
        ProgressPanelDialog f = new ProgressPanelDialog(title, parent);
        f.setVisible(true);
        return f.getProgressPanel();
    }


    /**
     * Make a ProgressPanel and frame (or internal frame) and return the panel.
     *
     * @param title the title string
     */
    public static ProgressPanel makeProgressPanel(String title) {
        return makeProgressPanel(title, null);
    }


    /**
     * Make a ProgressPanel and frame (or internal frame) and return the panel.
     */
    public static ProgressPanel makeProgressPanel() {
        return makeProgressPanel("Downloading data...");
    }

    /** Set the percent done. A 0 value resets the bar and hides the percent value. */
    public void setProgress(final int percent) {
        _statusPanel.setProgress(percent);
    }


    /**
     * Return a input stream that will generate log messages showing
     * the progress of the read from the given stream.
     *
     * @param in the input stream to be monitored
     * @param size the size in bytes of the date to be read, or 0 if not known
     */
    public ProgressBarFilterInputStream getLoggedInputStream(InputStream in, int size) throws IOException {
        if (_interrupted) {
            throw new ProgressException("Interrupted");
        }
        _loggedInputStream = _statusPanel.getLoggedInputStream(in, size);
        return _loggedInputStream;
    }


    /**
     * Return an input stream to use for reading from the given URL
     * that will generate log messages showing the progress of the read.
     *
     * @param url the URL to read
     */
    public ProgressBarFilterInputStream getLoggedInputStream(URL url) throws IOException {
        if (_interrupted) {
            throw new ProgressException("Interrupted");
        }
        _loggedInputStream = _statusPanel.getLoggedInputStream(url);
        return _loggedInputStream;
    }

    /**
     * Stop logging reads from the input stream returned from an
     * earlier call to getLoggedInputStream().
     *
     * @param in an input stream returned from getLoggedInputStream()
     */
    public void stopLoggingInputStream(ProgressBarFilterInputStream in) throws IOException {
        _loggedInputStream = null;
        _statusPanel.stopLoggingInputStream(in);
    }

    /**
     * Show a final message to the user and perform the stop operation
     * when the user hits the button.
     * @param title Message in the main panel
     * @param text Message in the status panel
     * @param buttonLabel label to use for the button in this state
     */
    public void stopWithMessage(String title, String text, String buttonLabel) {
        setProgress(100);
        setText(text);
        setTitle(title);
        _iconLabel.setIcon(null);
        getStopButton().setText(buttonLabel);
        doLayout();
        getStopButton().removeActionListener(this); //removes the current action listener
        getStopButton().addActionListener(event -> stop());
    }

}

