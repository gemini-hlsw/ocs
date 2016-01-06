package jsky.util.gui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.plaf.basic.BasicArrowButton;

/**
 * A replacement for JComboBox that supports multiple selections.
 */
public class MultiSelectComboBox<T> extends JComponent {

    private final JButton _button;
    private final BasicArrowButton _arrowButton;
    private JPopupMenu _popupMenu;
    private JCheckBoxMenuItem[] _menuItems;
    private ListModel<T> _model;
    private final DefaultListSelectionModel _selectionModel;
    private final ItemListener _itemListener;
    private final ListDataListener _listDataListener;
    private boolean _ignoreSelection = false;
    private static final String _ANY = "<Any>";


    /** Create an empty MultiSelectComboBox */
    private MultiSelectComboBox() {
        final GridBagUtil layout = new GridBagUtil(this);

        final MouseListener l = new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                _popupMenu.show(MultiSelectComboBox.this, 0, getHeight());
            }
        };

        _button = new JButton(_ANY);
        _button.setRequestFocusEnabled(false);
        _button.setBorder(null);
        _button.setHorizontalAlignment(SwingConstants.LEFT);
        _button.addMouseListener(l);
        _button.setOpaque(false);

        _arrowButton = new BasicArrowButton(BasicArrowButton.SOUTH);
        _arrowButton.setRequestFocusEnabled(false);
        _arrowButton.setForeground(Color.black);
        _arrowButton.setBorder(null);
        _arrowButton.addMouseListener(l);

        _popupMenu = new JPopupMenu();
        setRequestFocusEnabled(true);
        setBorder(BorderFactory.createEtchedBorder());

        layout.add(_button, 0, 0, 1, 1, 1.0, 1.0,
                   GridBagConstraints.BOTH, GridBagConstraints.CENTER,
                   new Insets(2, 2, 2, 0), 0, 0);
        layout.add(_arrowButton, 1, 0, 1, 1, 0.0, 0.0,
                   GridBagConstraints.BOTH, GridBagConstraints.EAST,
                   new Insets(2, 0, 2, 2), 0, 0);

        _selectionModel = new DefaultListSelectionModel();
        _selectionModel.addListSelectionListener(e -> _updateSelection());

        // called when a menubutton is selected
        _itemListener = e -> {
            _updateSelectionModel();
            _fireActionEvent();
        };

        // called when the model changes
        _listDataListener = new ListDataListener() {
            public void intervalAdded(ListDataEvent e) {
                setModel(_model);
            }

            public void intervalRemoved(ListDataEvent e) {
                setModel(_model);
            }

            public void contentsChanged(ListDataEvent e) {
                setModel(_model);
            }
        };
    }


    /** Create a MultiSelectComboBox containing the given items. */
    public MultiSelectComboBox(final T[] ar) {
        this();

        final DefaultListModel<T> model = new DefaultListModel<>();
        for (final T anAr : ar) model.addElement(anAr);
        setModel(model);
    }

    public void setToolTipText(final String s) {
        _button.setToolTipText(s);
        _arrowButton.setToolTipText(s);
    }

    public void setEnabled(final boolean b) {
        _button.setEnabled(b);
        _arrowButton.setEnabled(b);
    }


    /** Notifies the given listener whenever the list of selected items changes */
    public void addActionListener(final ActionListener l) {
        listenerList.add(ActionListener.class, l);
    }

    /** Removes the given listener from the list. */
    public void removeActionListener(final ActionListener l) {
        listenerList.remove(ActionListener.class, l);
    }

    /**
     * Notify any listeners of a change.
     */
    private void _fireActionEvent() {
        final ActionEvent e = new ActionEvent(this, 0, "");
        final Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ActionListener.class) {
                ((ActionListener) listeners[i + 1]).actionPerformed(e);
            }
        }
    }


    /** Set the model describing the contents of the popup menu */
    public void setModel(final ListModel<T> model) {
        clearSelection();
        _popupMenu = new JPopupMenu();

        _model = model;
        int n = model.getSize();
        _menuItems = new JCheckBoxMenuItem[n];
        for (int i = 0; i < n; i++) {
            _menuItems[i] = new JCheckBoxMenuItem(model.getElementAt(i).toString());
            _menuItems[i].removeItemListener(_itemListener);
            _menuItems[i].addItemListener(_itemListener);
            _popupMenu.add(_menuItems[i]);
        }

        model.addListDataListener(_listDataListener);
    }

    public ListModel<T> getModel() {
        return _model;
    }

    public ListSelectionModel getSelectionModel() {
        return _selectionModel;
    }

    /** Returns the number of selected items */
    public int getSelectionCount() {
        int result = 0;
        if (_menuItems != null && _menuItems.length != 0) {
            for (final JCheckBoxMenuItem _menuItem : _menuItems) {
                if (_menuItem.isSelected()) {
                    result++;
                }
            }
        }
        return result;
    }

    /** Returns an array containing the indexes of the selected items */
    public int[] getSelectedIndexes() {
        final int[] result = new int[getSelectionCount()];
        int index = 0;
        if (_menuItems != null && _menuItems.length != 0 && result.length != 0) {
            for (int i = 0; i < _menuItems.length; i++) {
                if (_menuItems[i].isSelected()) {
                    result[index++] = i;
                }
            }
        }
        return result;
    }

    /** Returns an array containing the selected items */
    public Object[] getSelectedObjects() {
        final Object[] result = new Object[getSelectionCount()];
        int index = 0;
        if (_menuItems != null && _menuItems.length != 0 && result.length != 0) {
            for (int i = 0; i < _menuItems.length; i++) {
                if (_menuItems[i].isSelected()) {
                    result[index++] = _model.getElementAt(i);
                }
            }
        }
        return result;
    }

    /** Returns an array containing the selected items as Strings */
    public String[] getSelected() {
        final Object[] ar = getSelectedObjects();
        final String[] result = new String[ar.length];
        for (int i = 0; i < ar.length; i++)
            result[i] = ar[i].toString();
        return result;
    }

    /** Deselect all items */
    public void clearSelection() {
        if (_menuItems != null && _menuItems.length != 0) {
            for (final JCheckBoxMenuItem _menuItem : _menuItems) {
                if (_menuItem.isSelected()) {
                    _menuItem.setSelected(false);
                }
            }
        }
    }


    /** Set the selected items */
    public void setSelectedObjects(final Object[] ar) {
        clearSelection();
        if (_menuItems != null && _menuItems.length != 0 && ar.length != 0) {
            for (int i = 0; i < _menuItems.length; i++) {
                final Object item = _model.getElementAt(i);
                for (final Object anAr : ar) {
                    if (anAr.equals(item)) {
                        _menuItems[i].setSelected(true);
                        break;
                    }
                }
            }
        }
    }


    // Update the button to display the text of the selected items
    private void _updateButton() {
        if (_menuItems != null && _menuItems.length != 0) {
            final StringBuilder sb = new StringBuilder();
            String sep = "";
            int count = 0;
            for (final JCheckBoxMenuItem _menuItem : _menuItems) {
                if (_menuItem.isSelected()) {
                    sb.append(sep);
                    sep = ", ";
                    sb.append(_menuItem.getText());
                    count++;
                }
            }
            if (count == 0) {
                _button.setText(_ANY);
            } else {
                _button.setText(sb.toString());
            }

            // Keep the button's width constant
            final Dimension d =_button.getPreferredSize();
            d.width = 0;
            _button.setPreferredSize(d);
            _button.setMinimumSize(d);
        }
    }

    // Update the GUI selection to match the selection model
    private void _updateSelection() {
        if (!_ignoreSelection) {
            _ignoreSelection = true;
            try {
                for (int i = 0; i < _menuItems.length; i++) {
                    final boolean selected = _selectionModel.isSelectedIndex(i);
                    if (selected != _menuItems[i].isSelected())
                        _menuItems[i].setSelected(selected);
                }
                _updateButton();
            } finally {
                _ignoreSelection = false;
            }
        }
    }

    // Update the selection model to match the GUI selection
    private void _updateSelectionModel() {
        if (!_ignoreSelection) {
            _ignoreSelection = true;
            try {
                for (int i = 0; i < _menuItems.length; i++) {
                    final boolean selected = _menuItems[i].isSelected();
                    if (selected != _selectionModel.isSelectedIndex(i)) {
                        if (selected)
                            _selectionModel.setSelectionInterval(i, i);
                        else
                            _selectionModel.removeSelectionInterval(i, i);
                    }
                }
                _updateButton();
            } finally {
                _ignoreSelection = false;
            }
        }
    }
}

