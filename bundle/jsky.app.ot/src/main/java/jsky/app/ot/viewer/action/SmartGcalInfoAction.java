package jsky.app.ot.viewer.action;

import edu.gemini.spModel.gemini.calunit.smartgcal.CalibrationProviderHolder;
import edu.gemini.spModel.gemini.calunit.smartgcal.VersionInfo;
import edu.gemini.spModel.smartgcal.repository.CalibrationUpdateEvent;
import edu.gemini.spModel.smartgcal.repository.CalibrationUpdater;
import jsky.app.ot.viewer.SPViewer;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;

/** Show general information about smart gcal */
public final class SmartGcalInfoAction extends AbstractViewerAction {
    final UpdateDialog _dialog;

    public SmartGcalInfoAction(final SPViewer viewer) {
        super(viewer, "Smart Calibrations Info...");
        _dialog = new UpdateDialog(viewer);
    }

    public boolean computeEnabledState() throws Exception {
        return true;
    }

    public void actionPerformed(final ActionEvent e) {
        if (_dialog.isVisible()) {
            _dialog.toFront();
        } else {
            // everytime we make the dialog visible we have to make sure the information in
            // it is accurate (we don't update the dialog if it is not visible)
            _dialog.updateVersionInfo();
            // dialog goes in upper left corner of screen by default (?), the following line puts it in the middle
            // of the main screen (why is this needed? wrong frame?)
            final Component c = viewer.getParent();
            _dialog.setLocation(
                    c.getLocationOnScreen().x + c.getWidth() / 2 - _dialog.getWidth() / 2,
                    c.getLocationOnScreen().y + c.getHeight() / 2 - _dialog.getHeight() / 2);
            _dialog.setVisible(true);
        }
    }


    private static final class UpdateDialog extends JDialog implements ActionListener {
        private static final String INFO_PANEL = "INFO";
        private static final String UPDATE_PANEL = "UPDATE";

        private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        private static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
        private final JButton close = new JButton("Close");
        private final JButton update = new JButton("Check for Updates Now");

        private final JLabel nextUpdateLabel;
        private final JTable table;
        private final SPViewer viewer;
        private final CardLayout cardLayout;
        private final JPanel contentPane;

        private boolean manualUpdateInProgress;

        public UpdateDialog(final SPViewer viewer) {
            // create non-modal dialog
            super(viewer.getParentFrame(), "Smart Calibrations Information", false);

            setResizable(false);
            setAlwaysOnTop(false);
            manualUpdateInProgress = false;

            this.viewer = viewer;
            nextUpdateLabel = new JLabel("");

            // emtpy table for version information
            table = new JTable();
            final JPanel tablePane = new JPanel(new BorderLayout());
            tablePane.setLayout(new BorderLayout());
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            tablePane.add(table.getTableHeader(), BorderLayout.NORTH);
            tablePane.add(table, BorderLayout.CENTER);

            // panel with close and udpate button
            final JPanel buttonPane = new JPanel();
            buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.X_AXIS));
            buttonPane.add(Box.createRigidArea(new Dimension(30, 0)));
            buttonPane.add(update);
            buttonPane.add(Box.createHorizontalGlue());
            buttonPane.add(close);
            buttonPane.add(Box.createRigidArea(new Dimension(30, 0)));

            // info panel with version and next upate information
            final JPanel infoPane = new JPanel();
            infoPane.setLayout(new BoxLayout(infoPane, BoxLayout.Y_AXIS));
            infoPane.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
            final JLabel blafasel = new JLabel("Version information for smart calibrations currently in use.");
            blafasel.setAlignmentX(Component.CENTER_ALIGNMENT);
            nextUpdateLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            tablePane.setAlignmentX(Component.CENTER_ALIGNMENT);
            buttonPane.setAlignmentX(Component.CENTER_ALIGNMENT);
            infoPane.add(blafasel);
            infoPane.add(Box.createRigidArea(new Dimension(0, 10)));
            infoPane.add(tablePane);
            infoPane.add(Box.createRigidArea(new Dimension(0, 10)));
            infoPane.add(nextUpdateLabel);
            infoPane.add(Box.createRigidArea(new Dimension(0, 10)));
            infoPane.add(buttonPane);

            // update in progress panel
            final JPanel updatePane = new JPanel();
            updatePane.setLayout(new BoxLayout(updatePane, BoxLayout.Y_AXIS));
            updatePane.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
            final JLabel text1 = new JLabel("Smart calibration update in progress...");
            final JLabel text2 = new JLabel("Please wait.");
            final JProgressBar progressBar = new JProgressBar();
            text1.setAlignmentX(Component.CENTER_ALIGNMENT);
            text2.setAlignmentX(Component.CENTER_ALIGNMENT);
            progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
            progressBar.setIndeterminate(true);
            updatePane.add(Box.createRigidArea(new Dimension(0, 50)));
            updatePane.add(text1);
            updatePane.add(Box.createRigidArea(new Dimension(0, 20)));
            updatePane.add(progressBar);
            updatePane.add(Box.createRigidArea(new Dimension(0, 20)));
            updatePane.add(text2);

            // put everything together... (using card layout for switching between info and update in progress panel)
            contentPane = new JPanel();
            cardLayout = new CardLayout();
            contentPane.setLayout(cardLayout);
            contentPane.add(infoPane, INFO_PANEL);
            contentPane.add(updatePane, UPDATE_PANEL);
            setContentPane(contentPane);
            showInfoPane();

            close.addActionListener(this);
            update.addActionListener(this);
            // make this a listener for any updates; we need to update this dialog
            // in case a backup is run in the background
            CalibrationUpdater.getInstance().addListener(this);
        }

        private void showInfoPane() {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    cardLayout.show(contentPane, INFO_PANEL);
                }
            });
        }

        private void showUpdateInProgress() {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    cardLayout.show(contentPane, UPDATE_PANEL);
                }
            });
        }

        public void updateVersionInfo() {
            final long nextUpdate;
            // show the versions that are currently in use by the CalibrationProvider
            // (i.e. the in-memory calibration maps)
            final java.util.List<VersionInfo> versionInfos;
            versionInfos = CalibrationProviderHolder.getProvider().getVersionInfo();
            nextUpdate = CalibrationUpdater.instance.nextUpdate();

            // update GUI
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    setVersionInfo(versionInfos);
                    setNextUpdate(nextUpdate);
                    pack();
                }
            });
        }


        private void setNextUpdate(final long nextUpdate) {
            final Date nextUpdateDate = new Date(System.currentTimeMillis() + nextUpdate);
            nextUpdateLabel.setText(
                    String.format("Next automatic update: %s at %s",
                            dateFormat.format(nextUpdateDate),
                            timeFormat.format(nextUpdateDate))
            );
        }

        private void setVersionInfo(final java.util.List<VersionInfo> versionInfos) {
            final TableModel dataModel = new AbstractTableModel() {
                public int getColumnCount() {
                    return 5;
                }

                public int getRowCount() {
                    return versionInfos.size();
                }

                public String getColumnName(final int col) {
                    switch (col) {
                        case 0:
                            return "Instrument";
                        case 1:
                            return "Type";
                        case 2:
                            return "Date";
                        case 3:
                            return "Time";
                        case 4:
                            return "Version";
                        default:
                            throw new InternalError("invalid col");
                    }
                }

                public Object getValueAt(final int row, final int col) {
                    switch (col) {
                        case 0:
                            return versionInfos.get(row).getInstrument();
                        case 1:
                            return versionInfos.get(row).getType();
                        case 2:
                            return dateFormat.format(versionInfos.get(row).getVersion().getTimestamp());
                        case 3:
                            return timeFormat.format(versionInfos.get(row).getVersion().getTimestamp());
                        case 4:
                            return versionInfos.get(row).getVersion().getRevision();
                        default:
                            throw new InternalError("invalid col");
                    }
                }
            };
            table.setModel(dataModel);
            table.getColumnModel().getColumn(0).setPreferredWidth(60);
            table.getColumnModel().getColumn(1).setPreferredWidth(40);
            table.getColumnModel().getColumn(2).setPreferredWidth(80);
            table.getColumnModel().getColumn(3).setPreferredWidth(70);
            table.getColumnModel().getColumn(4).setPreferredWidth(50);

            pack();
        }

        @Override
        public void actionPerformed(final ActionEvent actionEvent) {
            if (actionEvent.getSource() == close) {
                close();
            } else if (actionEvent.getSource() == update) {
                startUpdate();
            } else if (actionEvent instanceof CalibrationUpdateEvent) {
                updateFinished((CalibrationUpdateEvent) actionEvent);
            } else {
                // how did we get here? or: "this should never happen..."
                throw new InternalError("unexpected event");
            }
        }

        private void close() {
            // hide dialog
            this.setVisible(false);
        }

        private void startUpdate() {
            // do a local calibration update
            final boolean updateStarted = CalibrationUpdater.getInstance().updateNowInBackground();
            if (updateStarted) {
                // yes, this update was started manually... (as opposed to automatic background updates)
                manualUpdateInProgress = true;
                // replace info pane with indefinite progress bar "update in progress"
                showUpdateInProgress();
            } else {
                // there was already an update running...
                JOptionPane.showMessageDialog(
                        viewer,
                        "There is already an update running in the background.\nPlease try again later.",
                        "Smart Calibrations Update",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        }

        private void updateFinished(final CalibrationUpdateEvent event) {
            // if dialog is currently visible update the information displayed in it
            // (if dialog is hidden, it will be udpated when it is made visible the next time)
            if (this.isVisible()) {
                updateVersionInfo();
            }

            // during automatic background updates we don't show any feedback to the user;
            // in case of a manual update, we will give him a bit more information
            if (manualUpdateInProgress) {
                // manual update just finished
                manualUpdateInProgress = false;

                // replace progress bar with version info table
                showInfoPane();

                // if needed show error message
                final java.util.List<String> updatedFiles = event.getUpdatedFiles();
                final java.util.List<String> failedFiles = event.getFailedFiles();
                if (failedFiles.size() > 0) {
                    final StringBuilder sb = new StringBuilder();
                    if (updatedFiles.size() > 0) {
                        sb.append("The following smart calibrations have been updated:\n\n");
                        for (final String updatedCalibration : updatedFiles) {
                            sb.append("        ").append(updatedCalibration);
                            sb.append("\n");
                        }
                        sb.append("\n");
                    }
                    sb.append("Updating the following smart calibrations failed:\n\n");
                    for (final String failedCalibration : failedFiles) {
                        sb.append("        ").append(failedCalibration);
                        sb.append("\n");
                    }
                    sb.append("\nPlease try again later.\n");
                    JOptionPane.showMessageDialog(
                            viewer,
                            sb.toString(),
                            "Smart Calibrations Update",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }

    }

}
