/*
 * Copyright 2002 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: SessionQueuePanel.java 22963 2009-12-01 19:00:38Z swalker $
 */

package jsky.app.ot.session;

import edu.gemini.wdba.shared.QueuedObservation;
import jsky.util.gui.DialogUtil;
import jsky.util.gui.SwingUtil;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;


/**
 * Dialog for manipulating the OT observation session queue.
 *
 * @author Allan Brighton
 * @version $Revision: 22963 $
 */
public class SessionQueuePanel extends SessionQueuePanelForm implements ChangeListener {

    // Top level window (or internal frame) for the session queue
    private static Component _frame;

    // The session queue panel
    private static SessionQueuePanel _panel;

    /**
     * If no SessionQueuePanel exists, create one in a suitable frame and return a
     * reference to the panel. Otherwise, just return the existing reference.
     */
    public static SessionQueuePanel getInstance() {
        if (_panel != null) return _panel;

        _frame = new SessionQueueFrame();
        _panel = ((SessionQueueFrame) _frame).getSessionQueuePanel();

        return _panel;
    }

    private SessionQueueTableModel tm = new SessionQueueTableModel();

    /**
     * Constructor
     */
    SessionQueuePanel() {
        super();

        sessionTable.setModel(tm);
        sessionTable.getColumnModel().getColumn(0).setMaxWidth(200);
        sessionTable.getColumnModel().getColumn(0).setPreferredWidth(200);
        sessionTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                int[] rows = sessionTable.getSelectedRows();
                removeButton.setEnabled(rows != null && rows.length != 0);
            }
        });

        SessionQueue.INSTANCE.addChangeListener(SessionQueuePanel.this);
        _initialConnection();
    }

    // This is here because the initial connection takes a while.  So things redraw and then the update comes through.
    // In all other cases, this is not used
    private void _initialConnection() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                _updateQueueDisplay();
            }
        });
    }

    /**
     * Called whenever the session queue is changed
     */
    public void stateChanged(ChangeEvent e) {
        _updateQueueDisplay();
    }

    private void _updateQueueDisplay() {
        try {
            java.util.List<QueuedObservation> obsList = SessionQueue.INSTANCE.getObservationsAndTitles();
            tm.setQueuedObservations(obsList);
        } catch (Exception ex) {
            DialogUtil.error(ex);
        }
    }


    /**
     * Show the frame containing this panel
     */
    public void showFrame() {
        SwingUtil.showFrame(_frame);
        // Events that happen while the dialog is closed are ignored in Java 6
        // apparently.  Calling tableDataChanged here to force the JTable to
        // update.
        tm.fireTableDataChanged();
    }


    void removeButton_actionPerformed(ActionEvent e) {
        final SessionQueue sq = SessionQueue.INSTANCE;
        int[] rows = sessionTable.getSelectedRows();
        if (rows == null) return;

        // Get the obs ids to remove first.
        String[] obsIds = new String[rows.length];
        int i = 0;
        for (int row : rows) {
            obsIds[i++] = tm.getId(row).toString();
        }

        // Remove the observations one-by-one.
        for (String obsId : obsIds) {
            try {
                sq.removeObservation(obsId);
            } catch (Exception ex) {
                DialogUtil.error(ex);
            }
        }
    }

    void updateButton_actionPerformed(ActionEvent e) {
        _updateQueueDisplay();
    }

    void closeButton_actionPerformed(ActionEvent e) {
        _frame.setVisible(false);
    }
}


