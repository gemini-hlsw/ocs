package jsky.plot;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import jsky.coords.TargetDesc;
import jsky.coords.WorldCoords;
import jsky.util.gui.DialogUtil;


/**
 * A panel for editing a list of target positions.
 *
 * @version $Revision: 4784 $
 * @author Allan Brighton
 */
public class TargetListPanel extends JPanel {

    // Table column indexes
    private static final int NAME_COL = 0;
    private static final int RA_COL = 1;
    private static final int DEC_COL = 2;
    private static final int DESCRIPTION_COL = 3;
    private static final int PRIORITY_COL = 4;
    private static final int CATEGORY_COL = 5;

    private Component _parent;
    private TargetListPanelGUI _w = new TargetListPanelGUI();
    private Vector _heading = new Vector(2);
    private TargetDesc[] _targets;
    private EventListenerList _listenerList = new EventListenerList();
    private boolean _edited = false;

    /**
     * Initialize the target list panel.
     * @param parent the parent JFrame or JInternalFrame window
     */
    public TargetListPanel(Component parent) {
        _parent = parent;
        setLayout(new BorderLayout());
        add(_w, BorderLayout.CENTER);

        _heading.add("Name");
        _heading.add("RA");
        _heading.add("Dec");
        _heading.add("Description");
        _heading.add("Priority");
        _heading.add("Category");

        _w.table.setModel(new DefaultTableModel(new Vector(), _heading) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });

        _w.table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                int row = _w.table.getSelectedRow();
                if (row >= 0) {
                    TableModel model = _w.table.getModel();
                    String name = (String) model.getValueAt(row, NAME_COL);
                    String raStr = (String) model.getValueAt(row, RA_COL);
                    String decStr = (String) model.getValueAt(row, DEC_COL);
                    String description = (String) model.getValueAt(row, DESCRIPTION_COL);
                    String priority = (String) model.getValueAt(row, PRIORITY_COL);
                    String category = (String) model.getValueAt(row, CATEGORY_COL);
                    _w.nameField.setText(name);
                    _w.raField.setText(raStr);
                    _w.decField.setText(decStr);
                    _w.descriptionField.setText(description);
                    _w.priorityField.setText(priority);
                    _w.categoryField.setText(category);
                }
            }
        });

        _w.addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    _addNewTarget();
                } catch (Exception ex) {
                    DialogUtil.error("Invalid coordinate: " + ex.getMessage());
                }
            }
        });
        _w.removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                _removeSelectedTarget();
            }
        });
        _w.changeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    _changeTarget();
                } catch (Exception ex) {
                    DialogUtil.error("Invalid coordinate: " + ex.getMessage());
                }
            }
        });
        _w.cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setTargets(_targets);
                _parent.setVisible(false);
            }
        });
        _w.okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                _updateTargets();
                if (_edited) {
                    _edited = false;
                    _fireChange();
                }
                _parent.setVisible(false);
            }
        });
    }

    // Add a new target based on the edited fields
    private void _addNewTarget() {
        String name = _w.nameField.getText();
        String raStr = _w.raField.getText();
        String decStr = _w.decField.getText();
        WorldCoords pos = new WorldCoords(raStr, decStr);
        String description = _w.descriptionField.getText();
        String priority = _w.priorityField.getText();
        String category = _w.categoryField.getText();
        _addRow(new TargetDesc(name, pos, description, priority, category));
    }

    // Change the selected target based on the edited fields
    private void _changeTarget() {
        int row = _w.table.getSelectedRow();
        if (row >= 0) {
            String name = _w.nameField.getText();
            String raStr = _w.raField.getText();
            String decStr = _w.decField.getText();
            WorldCoords pos = new WorldCoords(raStr, decStr);
            String description = _w.descriptionField.getText();
            String priority = _w.priorityField.getText();
            String category = _w.categoryField.getText();
            _changeRow(row, new TargetDesc(name, pos, description, priority, category));
        }
    }

    // Add a new row to the table
    private void _addRow(TargetDesc target) {
        DefaultTableModel model = (DefaultTableModel) _w.table.getModel();
        Vector row = new Vector(5);
        row.add(target.getName());
        WorldCoords pos = target.getCoordinates();
        row.add(pos.getRA().toString());
        row.add(pos.getDec().toString());
        row.add(target.getDescription());
        row.add(target.getPriority());
        row.add(target.getCategory());
        model.addRow(row);
        _edited = true;
    }

    // Change the given row in the table
    private void _changeRow(int row, TargetDesc target) {
        WorldCoords pos = target.getCoordinates();
        DefaultTableModel model = (DefaultTableModel) _w.table.getModel();
        model.setValueAt(target.getName(), row, NAME_COL);
        model.setValueAt(pos.getRA().toString(), row, RA_COL);
        model.setValueAt(pos.getDec().toString(), row, DEC_COL);
        model.setValueAt(target.getDescription(), row, DESCRIPTION_COL);
        model.setValueAt(target.getPriority(), row, PRIORITY_COL);
        model.setValueAt(target.getCategory(), row, CATEGORY_COL);
        _edited = true;
    }

    // Remove the selected target from the table
    private void _removeSelectedTarget() {
        int row = _w.table.getSelectedRow();
        if (row >= 0) {
            DefaultTableModel model = (DefaultTableModel) _w.table.getModel();
            model.removeRow(row);
            _edited = true;
        }
    }


    // Update the _targets array from the table model
    private void _updateTargets() {
        TableModel model = _w.table.getModel();
        int n = model.getRowCount();
        _targets = new TargetDesc[n];
        for (int i = 0; i < n; i++) {
            String name = (String) model.getValueAt(i, NAME_COL);
            String raStr = (String) model.getValueAt(i, RA_COL);
            String decStr = (String) model.getValueAt(i, DEC_COL);
            WorldCoords pos = new WorldCoords(raStr, decStr);
            String description = (String) model.getValueAt(i, DESCRIPTION_COL);
            String priority = (String) model.getValueAt(i, PRIORITY_COL);
            String category = (String) model.getValueAt(i, CATEGORY_COL);
            _targets[i] = new TargetDesc(name, pos, description, priority, category);
        }
    }

    /** Return the array of targets */
    public TargetDesc[] getTargets() {
        return _targets;
    }

    /** Set the array of targets */
    public void setTargets(TargetDesc[] targets) {
        _targets = targets;
        Vector rows = new Vector(targets.length);
        for (int i = 0; i < targets.length; i++) {
            Vector row = new Vector(3);
            row.add(targets[i].getName());
            WorldCoords pos = targets[i].getCoordinates();
            row.add(pos.getRA().toString());
            row.add(pos.getDec().toString());
            row.add(targets[i].getDescription());
            row.add(targets[i].getPriority());
            row.add(targets[i].getCategory());
            rows.add(row);
        }
        _w.table.setModel(new DefaultTableModel(rows, _heading) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
    }


    /**
     * register to receive change events from this object whenever the
     * list of targets changes.
     */
    public void addChangeListener(ChangeListener l) {
        _listenerList.add(ChangeListener.class, l);
    }

    /**
     * Stop receiving change events from this object.
     */
    public void removeChangeListener(ChangeListener l) {
        _listenerList.remove(ChangeListener.class, l);
    }

    /**
     * Notify any listeners of a change in the target list.
     */
    private void _fireChange() {
        ChangeEvent changeEvent = new ChangeEvent(this);
        Object[] listeners = _listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ChangeListener.class) {
                ((ChangeListener) listeners[i + 1]).stateChanged(changeEvent);
            }
        }
    }
}
