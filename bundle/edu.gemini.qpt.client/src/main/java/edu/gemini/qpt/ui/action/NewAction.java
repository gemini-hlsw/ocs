package edu.gemini.qpt.ui.action;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.*;

import edu.gemini.ags.api.AgsMagnitude;
import edu.gemini.qpt.core.Block;
import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.core.ScheduleIO;
import edu.gemini.qpt.shared.sp.MiniModel;
import edu.gemini.qpt.core.util.LttsServicesClient;
import edu.gemini.qpt.ui.util.AbstractAsyncAction;
import edu.gemini.qpt.ui.util.CalendarPanel;
import edu.gemini.qpt.ui.util.ConfigErrorDialog;
import edu.gemini.qpt.ui.util.DefaultDirectory;
import edu.gemini.qpt.ui.util.DefaultSite;
import edu.gemini.qpt.ui.util.Platform;
import edu.gemini.qpt.ui.util.ProgressDialog;
import edu.gemini.qpt.ui.util.ProgressModel;
import edu.gemini.skycalc.TwilightBoundType;
import edu.gemini.skycalc.TwilightBoundedNight;
import edu.gemini.spModel.core.Peer;
import edu.gemini.spModel.core.Site;
import edu.gemini.ui.workspace.IShell;
import edu.gemini.util.security.auth.keychain.KeyChain;

/**
 * Executes a CloseAction if needed, then creates a new schedule, prompting the user to
 * choose a site. This action is always enabled.
 * @author rnorris
 */
public class NewAction extends AbstractAsyncAction {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(NewAction.class.getName());

    private final IShell shell;
    private final KeyChain authClient;
    private final AgsMagnitude.MagnitudeTable magTable;

    public NewAction(IShell shell, final KeyChain authClient, AgsMagnitude.MagnitudeTable magTable) {
        super("New Plan...", authClient);
        this.shell = shell;
        this.authClient = authClient;
        this.magTable = magTable;
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_N, Platform.MENU_ACTION_MASK));
        authClient.asJava().addListener(() -> setEnabled(!authClient.asJava().isLocked()));
    }

    public void asyncActionPerformed(ActionEvent e) {

        shell.getPeer().getGlassPane().setVisible(true);

        ProgressModel pm = new ProgressModel("Creating...", 0);
        pm.setIndeterminate(true);

        ProgressDialog pd = new ProgressDialog(shell.getPeer(), getName(), false, pm);

        try {

            // First close the current schedule, if any. This will prompt the
            // user to confirm and save as necessary. If the model is null
            // after this point, we're ready to create a new one.
            new CloseAction(shell, authClient).actionPerformed(e);
            if (shell.getModel() == null) {

                NewDialog nd = new NewDialog(shell.getPeer(), authClient);

                if (nd.showNewDialog() == NewDialog.OK_OPTION) {
                    TwilightBoundedNight tbn = getTwilightBoundedNight(nd.getSite(), nd.getStartDate());
                    pd.setVisible(true);
                    File templateFile = nd.getFile();
                    Schedule template = null;
                    pm.setMessage("Querying database...");
                    MiniModel miniModel = null;
                    for (int i=1; (miniModel == null) && (template == null); i++) {
                        if (pm.isCancelled()) return;
                        try {
                            if (templateFile != null) {
                                pm.setMessage("Reading template...");
                                template = ScheduleIO.read(templateFile, 1000, authClient, magTable);
                            } else {
                                miniModel = MiniModel.newInstance(authClient, nd.getAuthPeer(), tbn.getEndTime(), magTable);
                            }
                            if (pm.isCancelled()) return;
                            LttsServicesClient.newInstance(tbn.getStartTime(), nd.getAuthPeer());
                        } catch (TimeoutException te) {
                            pm.setMessage("Retrying (" + i + ") ...");
                            if (pm.isCancelled()) throw te;
                        }
                    }

                    if (pm.isCancelled()) return;

                    Schedule sched;

                    if (template != null) {

                        sched = template;
                        if (sched.getSite() != nd.getSite()) {
                            throw new IOException("Schedule and template sites do not match.");
                        }

                        // Only handle the case where there is at least one block, and push everything
                        // relative to the first block. Complain if that's not the case.
                        if (sched.getBlocks().isEmpty())
                            throw new IOException("Template has no scheduling blocks to move.");

                        pm.setMessage("Applying template...");

                        // Calculate the offset
                        Block oldStart = sched.getBlocks().first();
                        sched.removeBlock(Long.MIN_VALUE, Long.MAX_VALUE);
                        sched.addObservingNights(nd.getStartDate(), nd.getEndDate());
                        Block newStart = sched.getBlocks().first();
                        long offset = newStart.getStart() - oldStart.getStart(); // move all allocs forward by this amount
                        sched.moveAllocs(offset);
                        // Done .. ?
                    } else {

                        sched = new Schedule(miniModel);

                        sched.addVariant("Photometric, Super Seeing, Dry", (byte)  50, (byte)  20, (byte)  50, null, false);

                        sched.addVariant("Photometric, Super Seeing, Wet", (byte)  50, (byte)  20, (byte) 100, null, false);
                        sched.addVariant("Thin Cirrus, Super Seeing",      (byte)  70, (byte)  20, (byte)   0, null, false);

                        sched.addVariant("Photometric, Good Seeing, Dry",  (byte)  50, (byte)  70, (byte)  50, null, false);
                        sched.addVariant("Photometric, Good Seeing, Wet",  (byte)  50, (byte)  70, (byte) 100, null, false);
                        sched.addVariant("Thin Cirrus, Good Seeing",       (byte)  70, (byte)  70, (byte)   0, null, false);

                        sched.addVariant("Photometric, Poor Seeing, Dry",  (byte)  50, (byte)  85, (byte)  50, null, false);
                        sched.addVariant("Photometric, Poor Seeing, Wet",  (byte)  50, (byte)  85, (byte) 100, null, false);
                        sched.addVariant("Thin Cirrus, Poor Seeing",       (byte)  70, (byte)  85, (byte)   0, null, false);

                        sched.addVariant("Terrible Seeing",                (byte)   0, (byte) 100, (byte)   0, null, false);
                        sched.addVariant("Thick Clouds",                   (byte) 100, (byte)   0, (byte)   0, null, false);

                        sched.addObservingNights(nd.getStartDate(), nd.getEndDate());

                    }

                    if (LttsServicesClient.getInstance() != null) {
                        LttsServicesClient.getInstance().showStatus(shell.getPeer());
                    }

                    pm.setMessage("Opening model...");
                    shell.setModel(sched);
                }

            }
        } catch (RemoteException re) {

            LOGGER.log(Level.SEVERE, "Remote Exception", re);

            pd.setVisible(false);
            JOptionPane.showMessageDialog(
                shell.getPeer(),
                "There was a problem communicating with the database, sorry.",
                "Database Error",
                JOptionPane.ERROR_MESSAGE);


        } catch (IOException ioe) {

            pd.setVisible(false);
            JOptionPane.showMessageDialog(
                shell.getPeer(),
                "There was a problem with the selected template. The message was:\n" + ioe.getMessage(),
                "I/O Error",
                JOptionPane.ERROR_MESSAGE);


        } catch (TimeoutException te) {

            pd.setVisible(false);
            JOptionPane.showMessageDialog(
                    shell.getPeer(),
                    "The database is not available right now, but I will continue searching for it.\n" +
                    "Try back in a few minutes.",
                    "Database Unavailable",
                    JOptionPane.ERROR_MESSAGE);

        } finally {

            pd.setVisible(false);
            pd.dispose();
            shell.getPeer().getGlassPane().setVisible(false);

        }

        // Warn the user if there are misconfigured observations.
        if (shell.getModel() != null)
            ConfigErrorDialog.show((Schedule) shell.getModel(), shell.getPeer());


    }

    // Returns the dates for the beginning and end of the night at the site
    private TwilightBoundedNight getTwilightBoundedNight(Site site, long startDate) {
        return new TwilightBoundedNight(TwilightBoundType.OFFICIAL, startDate, site);
    }
}

@SuppressWarnings("serial")
class NewDialog extends JDialog {

    private Peer peer;
    private int result;
    private File file;

    private final CalendarPanel calendarPanel;

    static final int OK_OPTION = JOptionPane.OK_OPTION;
    static final int CANCEL_OPTION = JOptionPane.CANCEL_OPTION;

    public File getFile() {
        return file;
    }

    NewDialog(Frame owner, final KeyChain authClient) {
        super(owner, "New Plan", true);

        // Peers
        final Set<Peer> peerSet = authClient.asJava().peers();
        peer = null;

        // Search the list of peers for the default site. DefaultSite.get() always returns a non-null value.
        for (Peer p : peerSet) {
            if (p.site == DefaultSite.get()) {
                peer = p;
                break;
            }
        }

        // It is possible (but should not happen) that the list of peers only has one entry in it and its site
        // is not the same as that returned by DefaultSite: in this case, simply pick the first peer from the list.
        if (peer == null && !peerSet.isEmpty())
            peer = peerSet.iterator().next();

        setBackground(Color.LIGHT_GRAY);
        setLayout(new BorderLayout());

        final long startDate = System.currentTimeMillis(), endDate = startDate;
        this.calendarPanel = new CalendarPanel(DefaultSite.get().timezone(), startDate, endDate);

        add(new JPanel(new BorderLayout(8, 8)) {{
            setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

            add(new JPanel(new BorderLayout(4, 4)) {{
                add(new JLabel("Select a site:"), BorderLayout.WEST);
                add(new JComboBox<Peer>(peerSet.toArray(new Peer[peerSet.size()])) {{
                    // If no peers are configured, peer will be null.
                    if (peer != null) setSelectedItem(peer);
                    addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            peer = (Peer) getSelectedItem();
                            DefaultSite.set(peer.site);
                            calendarPanel.setTimeZone(peer.site.timezone());
                        }
                    });
                    setRenderer(new DefaultListCellRenderer() {
                        public Component getListCellRendererComponent(JList<?> jList, Object o, int i, boolean b, boolean b2) {
                            final JLabel c = (JLabel) super.getListCellRendererComponent(jList, o, i, b, b2);
                            final Peer peer = (Peer) o;
                            c.setText(peer.site != null ? peer.site.displayName : peer.toString());
                            return c;
                        }
                    });
                    // TODO: better renderer
                }}, BorderLayout.CENTER);
            }}, BorderLayout.NORTH);


            add(NewDialog.this.calendarPanel, BorderLayout.CENTER);

            add(new JPanel(new BorderLayout(0, 0)) {{

                add(new JPanel(new FlowLayout(FlowLayout.RIGHT)) {{
                    add(new JButton("Create") {{
                        NewDialog.this.getRootPane().setDefaultButton(this);
                        addActionListener(e -> {
                            result = OK_OPTION;
                            NewDialog.this.setVisible(false);
                        });
                    }});
                    add(new JButton("Cancel") {{
                        addActionListener(e -> {
                            result = CANCEL_OPTION;
                            NewDialog.this.setVisible(false);
                        });
                    }});
                }}, BorderLayout.EAST);

                final String NONE = "\u00ABnone\u00BB";

                final JLabel templateLabel = new JLabel(NONE); // ElementFactory.createLabel("�none�");
                templateLabel.setEnabled(false);

                add(new JPanel(new FlowLayout(FlowLayout.RIGHT)) {{
                    add(new JButton("Template...") {{
                        addActionListener(e -> {
                            JFileChooser chooser = new JFileChooser(DefaultDirectory.get());
                            chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
                                @Override
                                public String getDescription() {
                                    return "QPT Files";
                                }
                                @Override
                                public boolean accept(File file1) {
                                    return file1.getName().endsWith(".qpt") || file1.isDirectory();
                                }
                            });
                            if (JFileChooser.APPROVE_OPTION == chooser.showOpenDialog(NewDialog.this)) {
                                templateLabel.setEnabled(true);
                                templateLabel.setText(chooser.getSelectedFile().getName());
                                file = chooser.getSelectedFile();
                            } else {
                                templateLabel.setEnabled(false);
                                templateLabel.setText(NONE);
                                file = null;
                            }
                        });
                    }});
                }}, BorderLayout.WEST);

                add(templateLabel, BorderLayout.CENTER);

            }}, BorderLayout.SOUTH);

        }});

        pack();

    }

    @Override
    public void setVisible(boolean b) {
        setLocationRelativeTo(getParent());
        super.setVisible(b);
    }

    int showNewDialog() {
        setVisible(true);
        dispose();
        return result;
    }

    long getStartDate() {
        return calendarPanel.getStartDate();
    }

    long getEndDate() {
        return calendarPanel.getEndDate();
    }

    Peer getAuthPeer() {
        return peer;
    }

    public Site getSite() {
        return peer.site;
    }

    protected JRootPane createRootPane() {
        KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        JRootPane rootPane = new JRootPane();
        rootPane.registerKeyboardAction(e -> NewDialog.this.setVisible(false), stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
        return rootPane;
    }

}

