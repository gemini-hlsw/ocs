/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: MultiSelectComboBox.java 6418 2005-06-17 10:04:15Z brighton $
 */

package jsky.util.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.StringBuffer;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.basic.BasicArrowButton;


/**
 * A replacement for JComboBox that supports multiple selections.
 *
 * @version $Revision: 6418 $
 * @author Allan Brighton
 */
public class MultiSelectComboBox extends JComponent {

    private JButton _button;
    private BasicArrowButton _arrowButton;
    private JPopupMenu _popupMenu;
    private JCheckBoxMenuItem[] _menuItems;
    private ListModel _model;
    private DefaultListSelectionModel _selectionModel;
    private ItemListener _itemListener;
    private ListDataListener _listDataListener;
    private boolean _ignoreSelection = false;
    private static final String _ANY = "<Any>";


    /** Create an empty MultiSelectComboBox */
    public MultiSelectComboBox() {
        GridBagUtil layout = new GridBagUtil(this);

        MouseListener l = new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                _popupMenu.show(MultiSelectComboBox.this, 0, getHeight());
            }
        };

        _button = new JButton(_ANY);
        _button.setRequestFocusEnabled(false);
        _button.setBorder(null);
        _button.setHorizontalAlignment(SwingConstants.LEFT);
        _button.addMouseListener(l);

        _arrowButton = new BasicArrowButton(BasicArrowButton.SOUTH);
        _arrowButton.setRequestFocusEnabled(false);
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
        _selectionModel.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                _updateSelection();
            }
        });

        // called when a menubutton is selected
        _itemListener = new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                _updateSelectionModel();
                _fireActionEvent();
            }
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

    /** Create a MultiSelectComboBox based on the given model. */
    public MultiSelectComboBox(ListModel model) {
        this();
        setModel(model);
    }

    /** Create a MultiSelectComboBox containing the given items. */
    public MultiSelectComboBox(Object[] ar) {
        this();

        DefaultListModel model = new DefaultListModel();
        for (int i = 0; i < ar.length; i++)
            model.addElement(ar[i]);
        setModel(model);
    }

    public void setToolTipText(String s) {
        _button.setToolTipText(s);
        _arrowButton.setToolTipText(s);
    }

    public void setEnabled(boolean b) {
        _button.setEnabled(b);
        _arrowButton.setEnabled(b);
    }


    /** Notifies the given listener whenever the list of selected items changes */
    public void addActionListener(ActionListener l) {
        listenerList.add(ActionListener.class, l);
    }

    /** Removes the given listener from the list. */
    public void removeActionListener(ActionListener l) {
        listenerList.remove(ActionListener.class, l);
    }

    /**
     * Notify any listeners of a change.
     */
    private void _fireActionEvent() {
        ActionEvent e = new ActionEvent(this, 0, "");
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ActionListener.class) {
                ((ActionListener) listeners[i + 1]).actionPerformed(e);
            }
        }
    }


    /** Set the model describing the contents of the popup menu */
    public void setModel(ListModel model) {
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

    public ListModel getModel() {
        return _model;
    }


    public ListSelectionModel getSelectionModel() {
        return _selectionModel;
    }

    /** Returns the number of selected items */
    public int getSelectionCount() {
        int result = 0;
        if (_menuItems != null && _menuItems.length != 0) {
            for (int i = 0; i < _menuItems.length; i++) {
                if (_menuItems[i].isSelected()) {
                    result++;
                }
            }
        }
        return result;
    }

    /** Returns an array containing the indexes of the selected items */
    public int[] getSelectedIndexes() {
        int[] result = new int[getSelectionCount()];
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

    /** Set the indexes of the selected items */
    public void setSelectedIndexes(int[] ar) {
        clearSelection();
        if (_menuItems != null && _menuItems.length != 0 && ar.length != 0) {
            for (int i = 0; i < _menuItems.length; i++) {
                Object item = _model.getElementAt(i);
                for (int j = 0; j < ar.length; j++) {
                    if (_model.getElementAt(ar[j]).equals(item)) {
                        _menuItems[i].setSelected(true);
                        break;
                    }
                }
            }
        }
    }

    /** Returns an array containing the selected items */
    public Object[] getSelectedObjects() {
        Object[] result = new Object[getSelectionCount()];
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
        Object[] ar = getSelectedObjects();
        String[] result = new String[ar.length];
        for (int i = 0; i < ar.length; i++)
            result[i] = ar[i].toString();
        return result;
    }

    /** Deselect all items */
    public void clearSelection() {
        if (_menuItems != null && _menuItems.length != 0) {
            for (int i = 0; i < _menuItems.length; i++) {
                if (_menuItems[i].isSelected()) {
                    _menuItems[i].setSelected(false);
                }
            }
        }
    }


    /** Set the selected items */
    public void setSelectedObjects(Object[] ar) {
        clearSelection();
        if (_menuItems != null && _menuItems.length != 0 && ar.length != 0) {
            for (int i = 0; i < _menuItems.length; i++) {
                Object item = _model.getElementAt(i);
                for (int j = 0; j < ar.length; j++) {
                    if (ar[j].equals(item)) {
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
            StringBuffer sb = new StringBuffer();
            String sep = "";
            int count = 0;
            for (int i = 0; i < _menuItems.length; i++) {
                if (_menuItems[i].isSelected()) {
                    sb.append(sep);
                    sep = ", ";
                    sb.append(_menuItems[i].getText());
                    count++;
                }
            }
            if (count == 0) {
                _button.setText(_ANY);
            } else {
                _button.setText(sb.toString());
            }

            // Keep the button's width constant
            Dimension d =_button.getPreferredSize();
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
                    boolean selected = _selectionModel.isSelectedIndex(i);
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
                    boolean selected = _menuItems[i].isSelected();
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


    /** Test main */
    public static void main(String[] args) {
        String[] ar = new String[]{"Test", "First Item", "Second Item", "Third Item", "Fourth Item", "Fifth Item"};
        JComboBox cb = new JComboBox(ar);
        MultiSelectComboBox mscb = new MultiSelectComboBox(ar);

        /*
        mscb.setSelectedObjects(new String[]{"First Item", "Second Item"});
        ListSelectionModel selModel = mscb.getSelectionModel();
        if (selModel.isSelectedIndex(0) || selModel.isSelectedIndex(3))
            System.out.println("Failed test 1");
        if (! (selModel.isSelectedIndex(1) && selModel.isSelectedIndex(2)))
            System.out.println("Failed test 2");
        */

        mscb.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("XXX MultiSelectComboBox.actionPerformed");
            }
        });

        JFrame f = new JFrame("Test MultiSelectComboBox");
        JPanel p = new JPanel();
        p.setLayout(new BorderLayout());
        f.getContentPane().add(p);
        p.add(cb, BorderLayout.NORTH);
        p.add(mscb, BorderLayout.SOUTH);
        f.pack();
        f.setVisible(true);
        f.addWindowListener(new BasicWindowMonitor());
    }
}

