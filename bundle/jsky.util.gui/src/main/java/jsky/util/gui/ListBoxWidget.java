package jsky.util.gui;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;

public class ListBoxWidget<T> extends JList<T> {

    // Observers
    private final List<ListBoxWidgetWatcher<T>> _watchers = new ArrayList<>();

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
    public synchronized final void addWatcher(final ListBoxWidgetWatcher<T> watcher) {
        if (_watchers.contains(watcher)) {
            return;
        }
        _watchers.add(watcher);
    }

    /**
     * Delete a watcher.
     */
    public synchronized final void deleteWatcher(final ListBoxWidgetWatcher<T> watcher) {
        _watchers.remove(watcher);
    }

    private synchronized List<ListBoxWidgetWatcher<T>> _getWatchers() {
        return new ArrayList<>(_watchers);
    }

    private void _notifySelect(final int index) {
        _getWatchers().forEach(w -> w.listBoxSelect(this, index, getSelectedValue()));
    }

    private void _notifyAction(final int index) {
        _getWatchers().forEach(w -> w.listBoxAction(this, index, getSelectedValue()));
    }

    /** Select the given value */
    public void setValue(final int i) {
        getSelectionModel().clearSelection();
        if (i >= 0)
            getSelectionModel().addSelectionInterval(i, i);
    }

    /** Select the given value */
    public void setValue(final Object s) {
        setValue(((DefaultListModel) getModel()).indexOf(s));
    }

    /** Set the contents of the list */
    public void setChoices(final List<T> lst) {
        final DefaultListModel<T> model = new DefaultListModel<>();
        lst.forEach(model::addElement);
        setModel(model);
    }

    /** Set the contents of the list */
    public void setChoices(final T[] ar) {
        DefaultListModel<T> model = new DefaultListModel<>();
        for (final T o : ar) model.addElement(o);
        setModel(model);
    }

    /** Clear  out the list */
    public void clear() {
        ((DefaultListModel) getModel()).clear();
    }

}

