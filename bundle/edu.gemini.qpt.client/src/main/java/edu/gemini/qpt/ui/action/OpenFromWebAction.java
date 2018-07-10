package edu.gemini.qpt.ui.action;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import edu.gemini.ags.api.AgsMagnitude;
import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.core.ScheduleIO;
import edu.gemini.qpt.core.util.LttsServicesClient;
import edu.gemini.qpt.ui.util.AbstractAsyncAction;
import edu.gemini.qpt.ui.util.CalendarPanel;
import edu.gemini.qpt.ui.util.ConfigErrorDialog;
import edu.gemini.qpt.ui.util.DefaultSite;
import edu.gemini.qpt.ui.util.Platform;
import edu.gemini.qpt.ui.util.ProgressDialog;
import edu.gemini.qpt.ui.util.ProgressModel;
import edu.gemini.qpt.ui.util.UnusedSemesterDialog;
import edu.gemini.skycalc.TwilightBoundType;
import edu.gemini.skycalc.TwilightBoundedNight;
import edu.gemini.spModel.core.Site;
import edu.gemini.ui.workspace.IShell;
import edu.gemini.util.security.auth.keychain.KeyChain;

/**
 * Executes a CloseAction if needed, them prompts for a file and attempts to open it. This
 * action is always enabled.
 * @author rnorris
 */
public class OpenFromWebAction extends AbstractAsyncAction {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(OpenFromWebAction.class.getName());

    private final IShell shell;
    private final KeyChain authClient;
    private final AgsMagnitude.MagnitudeTable magTable;

    public OpenFromWebAction(IShell shell, final KeyChain authClient, AgsMagnitude.MagnitudeTable magTable) {
        super("Open from Web...", authClient);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_O, Platform.MENU_ACTION_MASK | KeyEvent.SHIFT_DOWN_MASK));
        this.shell = shell;
    this.authClient = authClient;
    this.magTable = magTable;
    authClient.asJava().addListener(new Runnable() {
        public void run() {
            setEnabled(!authClient.asJava().isLocked());
        }
    });
    }

    @Override
    protected void asyncActionPerformed(ActionEvent e) {

        try {
            shell.getPeer().getGlassPane().setVisible(true);

            // Try to close. May fail so we check again.
            if (shell.getModel() != null)
                new CloseAction(shell, authClient).actionPerformed(e);

            if (shell.getModel() == null) {

                OpenFromWebDialog owd = new OpenFromWebDialog(shell.getPeer());

                if (OpenFromWebDialog.OK_OPTION == owd.showNewDialog()) {

                    ProgressModel pm = new ProgressModel("Opening...", 0);
                    pm.setIndeterminate(true);

                    ProgressDialog pd = new ProgressDialog(shell.getPeer(), getName(), false, pm);

                    try {

                        pd.setVisible(true);

                        pm.setMessage("Opening...");
                        URL url = owd.getURL();
                        Schedule sched = null;
                        LttsServicesClient.clearInstance();
                        for (int i = 0; sched == null ; i++) {
                            try {
                                sched = ScheduleIO.read(url, 1000, authClient, magTable);
                            } catch (TimeoutException te) {
                                pm.setMessage("Retrying (" + i + ") ...");
                                if (pm.isCancelled())
                                    throw te;
                            }
                        }

                        if (pm.isCancelled()) return;
                        LttsServicesClient.getInstance().showStatus(shell.getPeer());
                        shell.setModel(sched);
                    } catch (RemoteException re) {

                        LOGGER.log(Level.SEVERE, "Remote Exception", re);

                        pd.setVisible(false);
                        JOptionPane.showMessageDialog(
                            shell.getPeer(),
                            "There was a problem communicating with the database, sorry.",
                            "Database Error",
                            JOptionPane.ERROR_MESSAGE);


                    } catch (TimeoutException te) {

                        pd.setVisible(false);
                        JOptionPane.showMessageDialog(
                                shell.getPeer(),
                                "The database is not available right now, but I will continue searching for it.\n" +
                                "Try back in a few minutes.",
                                "Database Unavailable",
                                JOptionPane.ERROR_MESSAGE);

                    } catch (FileNotFoundException ex) {

                        pd.setVisible(false);
                        JOptionPane.showMessageDialog(shell.getPeer(),
                            "No plan for the requested night could be found at\n" + ex.getMessage(),
                            "Problem Opening Plan", JOptionPane.ERROR_MESSAGE);

                    } catch (IOException ex) {

                        LOGGER.log(Level.SEVERE, "Trouble opening plan.", ex);
                        pd.setVisible(false);
                        JOptionPane.showMessageDialog(shell.getPeer(),
                            "This plan could not be opened. The error was:\n" + ex.getMessage(),
                            "Problem Opening Plan", JOptionPane.ERROR_MESSAGE);

                    } finally {
                        pd.setVisible(false);
                        pd.dispose();
                    }

                    // Warn the user if there are misconfigured observations.
                    if (shell.getModel() != null) {
                        ConfigErrorDialog.show((Schedule) shell.getModel(), shell.getPeer());
                        UnusedSemesterDialog.show((Schedule) shell.getModel(), shell.getPeer());
                    }



                }

            }
        } finally {
            shell.getPeer().getGlassPane().setVisible(false);
        }
    }


}




@SuppressWarnings("serial")
class OpenFromWebDialog extends JDialog {

    private Site site = DefaultSite.get();
    private int result;
    private File file;

    private final CalendarPanel calendarPanel;

    static final int OK_OPTION = JOptionPane.OK_OPTION;
    static final int CANCEL_OPTION = JOptionPane.CANCEL_OPTION;

    public File getFile() {
        return file;
    }

    public OpenFromWebDialog(Frame owner) {
        super(owner, "Open from Web", true);
        setBackground(Color.LIGHT_GRAY);
        setLayout(new BorderLayout());

        final long startDate = System.currentTimeMillis(), endDate = startDate;
        calendarPanel = new CalendarPanel(DefaultSite.get().timezone(), startDate, endDate);

        add(new JPanel(new BorderLayout(8, 8)) {{
            setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

            add(new JPanel(new BorderLayout(4, 4)) {{
                add(new JLabel("Select a site:"), BorderLayout.WEST);
                add(new JComboBox(new Site[] { Site.GS, Site.GN }) {{
                    setSelectedItem(site);
                    addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            site = (Site) getSelectedItem();
                            DefaultSite.set(site);
                            calendarPanel.setTimeZone(site.timezone());
                        }
                    });
                }}, BorderLayout.CENTER);
            }}, BorderLayout.NORTH);


            add(OpenFromWebDialog.this.calendarPanel, BorderLayout.CENTER);

            add(new JPanel(new BorderLayout(0, 0)) {{

                add(new JPanel(new FlowLayout(FlowLayout.RIGHT)) {{
                    add(new JButton("Open") {{
                        OpenFromWebDialog.this.getRootPane().setDefaultButton(this);
                        addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                result = OK_OPTION;
                                OpenFromWebDialog.this.setVisible(false);
                            }
                        });
                    }});
                    add(new JButton("Cancel") {{
    //                    NewDialog.this.getRootPane().set;
                        addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                result = CANCEL_OPTION;
                                OpenFromWebDialog.this.setVisible(false);
                            }
                        });
                    }});
                }}, BorderLayout.EAST);



            }}, BorderLayout.SOUTH);

        }});

        pack();

    }

    @Override
    public void setVisible(boolean b) {
        setLocationRelativeTo(getParent());
        super.setVisible(b);
    }

    public int showNewDialog() {
        setVisible(true);
        dispose();
        return result;
    }

    public URL getURL() {

        // Get the night for the selected day
        TwilightBoundedNight night = new TwilightBoundedNight(TwilightBoundType.NAUTICAL, calendarPanel.getStartDate(), site);

        // Get yy mmm dd
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(night.getEndTime());

        // Directory chain
        String[] PARTS = {
            "http://internal.gemini.edu/science",
            site == Site.GS ? "GSqueue" : "GNqueue",
            "qpt-plans",
            Integer.toString(cal.get(Calendar.YEAR)),
            String.format("%02d", cal.get(Calendar.MONTH) + 1),
            String.format("%02d", cal.get(Calendar.DAY_OF_MONTH)),
        };

        StringBuilder buf = new StringBuilder();
        for (String s: PARTS) {
            buf.append(s).append("/");
        }

        DateFormat df = new SimpleDateFormat("yyyyMMdd");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        buf.append(df.format(cal.getTime()));
        buf.append(".qpt");


        try {
            return new URL(buf.toString());
        } catch (MalformedURLException e) {
            // Should never happen
            throw new RuntimeException(e);
        }

    }

}


