package jsky.util.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

public class ListBoxWidget<T> extends JList<T> {

    // Observers
    private final java.util.List<ListBoxWidgetWatcher<T>> _watchers = new ArrayList<>();

    /** Default Constructor */
    public ListBoxWidget() {
        getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting())
                _notifySelect(getSelectedIndex());
        });

        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    _notifyAction(locationToIndex(e.getPoint()));
                }
            }
        });
        setModel(new DefaultListModel<>());
    }

    /**
     * Add a watcher.  Watchers are notified when an item is selected.
     */
    public synchronized final void addWatcher(ListBoxWidgetWatcher<T> watcher) {
        if (_watchers.contains(watcher)) {
            return;
        }
        _watchers.add(watcher);
    }

    /**
     * Delete a watcher.
     */
    public synchronized final void deleteWatcher(ListBoxWidgetWatcher<T> watcher) {
        _watchers.remove(watcher);
    }

    //
    // Get a copy of the _watchers Vector.
    //
    private synchronized java.util.List<ListBoxWidgetWatcher<T>> _getWatchers() {
        return Collections.unmodifiableList(_watchers);
    }

    //
    // Notify watchers that an item has been selected.
    //
    private void _notifySelect(int index) {
        java.util.List<ListBoxWidgetWatcher<T>> v = _getWatchers();
        for (ListBoxWidgetWatcher<T> watcher : v) {
            watcher.listBoxSelect(this, index, getSelectedValue());
        }
    }

    //
    // Notify watchers that an item has been double-clicked.
    //
    private void _notifyAction(int index) {
        List<ListBoxWidgetWatcher<T>> v = _getWatchers();
        for (ListBoxWidgetWatcher<T> watcher : v) {
            watcher.listBoxAction(this, index, getSelectedValue());
        }
    }

    /**
     * test main
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame("ListBoxWidget");

        ListBoxWidget<String> list = new ListBoxWidget<>();
        DefaultListModel<String> model = new DefaultListModel<>();
        for (int i = 0; i < 50; i++) {
            model.addElement("row " + i);
        }
        list.setModel(model);
        list.addWatcher(new ListBoxWidgetWatcher<String>() {
            public void listBoxSelect(ListBoxWidget<String> lbwe, int index, Object val) {
                System.out.println("listBoxSelect: " + index);
            }

            public void listBoxAction(ListBoxWidget<String> lbwe, int index, Object val) {
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
    public void setChoices(java.util.List<T> lst) {
        DefaultListModel<T> model = new DefaultListModel<>();
        lst.forEach(model::addElement);
        setModel(model);
    }

    /** Set the contents of the list */
    public void setChoices(T[] ar) {
        DefaultListModel<T> model = new DefaultListModel<>();
        for (T o : ar) model.addElement(o);
        setModel(model);
    }

    /** Clear  out the list */
    public void clear() {
        ((DefaultListModel) getModel()).clear();
    }

}

