// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: ListBoxWidget.java 18743 2009-03-12 22:15:39Z swalker $
//
package jsky.util.gui;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Vector;


public class ListBoxWidget extends JList {

    // Observers
    private Vector _watchers = new Vector();

    /** Default Constructor */
    public ListBoxWidget() {
        getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting())
                    _notifySelect(getSelectedIndex());
            }
        });

        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    _notifyAction(locationToIndex(e.getPoint()));
                }
            }
        });
        setModel(new DefaultListModel());
    }

    /**
     * Add a watcher.  Watchers are notified when an item is selected.
     */
    public synchronized final void addWatcher(ListBoxWidgetWatcher watcher) {
        if (_watchers.contains(watcher)) {
            return;
        }
        _watchers.addElement(watcher);
    }

    /**
     * Delete a watcher.
     */
    public synchronized final void deleteWatcher(ListBoxWidgetWatcher watcher) {
        _watchers.removeElement(watcher);
    }

    /**
     * Delete all watchers.
     */
    public synchronized final void deleteWatchers() {
        _watchers.removeAllElements();
    }

    //
    // Get a copy of the _watchers Vector.
    //
    private synchronized final Vector _getWatchers() {
        return (Vector) _watchers.clone();
    }

    //
    // Notify watchers that an item has been selected.
    //
    private void _notifySelect(int index) {
        Vector v = _getWatchers();
        int cnt = v.size();
        for (int i = 0; i < cnt; ++i) {
            ListBoxWidgetWatcher watcher = (ListBoxWidgetWatcher) v.elementAt(i);
            watcher.listBoxSelect(this, index, getSelectedValue());
        }
    }

    //
    // Notify watchers that an item has been double-clicked.
    //
    private void _notifyAction(int index) {
        Vector v = _getWatchers();
        int cnt = v.size();
        for (int i = 0; i < cnt; ++i) {
            ListBoxWidgetWatcher watcher = (ListBoxWidgetWatcher) v.elementAt(i);
            watcher.listBoxAction(this, index, getSelectedValue());
        }
    }

    /**
     * Focus at the selected item.  I couldn't find an easy way to do this.
     */
    public void focusAtSelectedItem() {
    }


    /** Set the contents of the list */
    public void setRows() {
    }


    /**
     * test main
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame("ListBoxWidget");

        ListBoxWidget list = new ListBoxWidget();
        DefaultListModel model = new DefaultListModel();
        for (int i = 0; i < 50; i++) {
            model.addElement("row " + i);
        }
        list.setModel(model);
        list.addWatcher(new ListBoxWidgetWatcher() {
            public void listBoxSelect(ListBoxWidget lbwe, int index, Object val) {
                System.out.println("listBoxSelect: " + index);
            }

            public void listBoxAction(ListBoxWidget lbwe, int index, Object val) {
                System.out.println("listBoxAction: " + index);
            }
        });

        frame.getContentPane().add(new JScrollPane(list), BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
        frame.addWindowListener(new BasicWindowMonitor());
    }

    /** Select the given value */
    public void setValue(int i) {
        getSelectionModel().clearSelection();
        if (i >= 0)
            getSelectionModel().addSelectionInterval(i, i);
    }

    /** Select the given value */
    public void setValue(Object s) {
        setValue(((DefaultListModel) getModel()).indexOf(s));
    }

    /** Set the contents of the list */
    public void setChoices(java.util.List lst) {
        DefaultListModel model = new DefaultListModel();
        for (Object obj : lst) model.addElement(obj);
        setModel(model);
    }

    /** Set the contents of the list */
    public void setChoices(Object[] ar) {
        DefaultListModel model = new DefaultListModel();
        for (Object o : ar) model.addElement(o);
        setModel(model);
    }

    /** Clear  out the list */
    public void clear() {
        ((DefaultListModel) getModel()).clear();
    }

}

