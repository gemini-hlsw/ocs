// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: TableWidget.java 6719 2005-11-08 19:35:36Z brighton $
//
package jsky.util.gui;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.util.Vector;





/**
 * Was an extension of the Marimba TableWidget to support row selection
 * and action observers. Now this class is derived from JTable.
 */
public class TableWidget extends RowManipulateTableWidget {

    // Observers
    private Vector _watchers = new Vector();

    /** Default constructor */
    public TableWidget() {
        getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    int i = getSelectionModel().getMinSelectionIndex();
                    if (i >= 0)
                        _notifySelect(i);
                }
            }
        });
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setShowHorizontalLines(false);
    }


    /**
     * Add a watcher.  Watchers are notified when an item is selected.
     */
    public synchronized final void addWatcher(TableWidgetWatcher tww) {
        if (_watchers.contains(tww)) {
            return;
        }
        _watchers.addElement(tww);
    }

    /**
     * Delete a watcher.
     */
    public synchronized final void deleteWatcher(TableWidgetWatcher tww) {
        _watchers.removeElement(tww);
    }

    /**
     * Delete all watchers.
     */
    public synchronized final void deleteWatchers() {
        _watchers.removeAllElements();
    }


    /**
     * Select the given row and notify observers
     */
    public void selectRowAt(int rowIndex) {
        getSelectionModel().addSelectionInterval(rowIndex, rowIndex);
    }

    /**
     * Notify observers when a row is selected.
     *
     * @param rowIndex the index of the row that was selected
     */
    protected void _notifySelect(int rowIndex) {
        Vector v;
        synchronized (this) {
            v = (Vector) _watchers.clone();
        }

        int cnt = v.size();
        for (int i = 0; i < cnt; ++i) {
            TableWidgetWatcher tww = (TableWidgetWatcher) v.elementAt(i);
            tww.tableRowSelected(this, rowIndex);
        }
    }


    /**
     * test main
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame("TableWidget");

        TableWidget table = new TableWidget();
        String[] headers = new String[]{"One", "Two", "Three", "Four"};
        table.setColumnHeaders(headers);
        Vector[] v = new Vector[5];
        for (int i = 0; i < v.length; i++) {
            v[i] = new Vector(4);
            for (int j = 0; j < headers.length; j++)
                v[i].add("cell " + i + ", " + j);
        }
        table.setRows(v);
        table.addWatcher(new TableWidgetWatcher() {
            public void tableRowSelected(TableWidget twe, int rowIndex) {
                System.out.println("tableRowSelected: " + rowIndex);
            }

            public void tableAction(TableWidget twe, int colIndex, int rowIndex) {
                System.out.println("tableAction: " + rowIndex);
            }
        });

        frame.getContentPane().add(new JScrollPane(table), BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
        frame.addWindowListener(new BasicWindowMonitor());
    }
}

