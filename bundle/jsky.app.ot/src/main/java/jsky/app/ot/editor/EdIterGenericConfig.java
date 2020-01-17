package jsky.app.ot.editor;

import edu.gemini.pot.sp.*;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.spModel.data.AbstractDataObject;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.data.OptionTypeUtil;
import edu.gemini.spModel.data.SuggestibleString;
import edu.gemini.spModel.data.config.IConfigProvider;
import edu.gemini.spModel.data.config.ISysConfig;
import edu.gemini.spModel.data.property.PropertyFilter;
import edu.gemini.spModel.data.property.PropertyProvider;
import edu.gemini.spModel.data.property.PropertySupport;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.seqcomp.InstrumentSequenceSync;
import edu.gemini.spModel.seqcomp.SeqConfigComp;
import edu.gemini.spModel.type.DisplayableSpType;
import edu.gemini.spModel.type.ObsoletableSpType;
import edu.gemini.spModel.type.PartiallyEngineeringSpType;
import edu.gemini.spModel.type.SpTypeUtil;
import edu.gemini.spModel.util.SPTreeUtil;
import jsky.app.ot.OTOptions;
import jsky.app.ot.nsp.SPTreeEditUtil;
import jsky.util.gui.Resources;
import jsky.util.gui.*;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.text.Document;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * This class implements an editor for "configuration editor" subclasses.
 * The configuration is edited in a table whose rows are iteration steps,
 * and whose columns are the attributes being iterated over.  In a single
 * step, all the attributes in the table columns are set with the values
 * in the table row when the iterator is executed.
 * <p/>
 * <p> The attributes available for iteration (i.e., potential columns in
 * the table) are displayed in a list box in the upper right hand corner.
 * Selecting an item in the list box makes it a column in the table.  When
 * a cell in the table is selected, an editor for its value is displayed
 * in the upper left hand corner.
 * <p/>
 * <p/>
 * For each item being iterated over, there is a attribute with a value
 * for each of its steps.  For instance:
 * <pre>
 *     &lt;av name=filterIter descr="No Description"&gt;
 *        &lt;val value="x300 NDF + CBF"&gt;
 *        &lt;val value="CBF"&gt;
 *     &lt;/av&gt;
 * </pre>
 * <p/>
 * <p/>
 * shows that the values for the two steps of the filter iteration are
 * "x300 NDF + CBF" and "CBF".  The "diffuserIter" would also have two
 * values for the two steps.
 */
public class EdIterGenericConfig<T extends SeqConfigComp> extends OtItemEditor<ISPSeqComponent, T> implements CellSelectTableWatcher,
        ListBoxWidgetWatcher<PropertyDescriptor>, ActionListener, TextBoxWidgetWatcher {

    private static final Logger LOG = Logger.getLogger(EdIterGenericConfig.class.getName());

    // The iteration table widget.
    private CellSelectTableWidget _iterTab;
    private EdIterGenericConfigTableModel _tableModel;

    // all available properties
    private Map<String, PropertyDescriptor> _allPropertyMap;

    // A ref to _listBoxVE, _textBoxVE, ..., depending upon the type of
    // attribute represented by the selected cell.
    private ICValueEditor _valueEditor;

    private ICListBoxValueEditor _listBoxVE;
    private ICTextBoxValueEditor _textBoxVE;
    private ICComboBoxValueEditor _comboBoxVE;
    private ICTextBoxValueEditor _numberBoxVE;

    // The list box that contains the available items.
    private ListBoxWidget<PropertyDescriptor> _itemsLBW;

    // Used to ignore events when adding items
    private boolean _ignoreEvents;

    /**
     * The GUI layout
     */
    private MiniConfigIterForm _w;

    /**
     * Describes the current parameters and their values
     */
//    private ISysConfig _currentSysConfig;

    // Set to true if this is the first instrument iterator in the sequence,
    // which means that the values in the first row override the values in the
    // instrument data object.
    private boolean _isFirstInstIterator;

    /**
     * Default constructor
     */
    public EdIterGenericConfig() {
        _w = new MiniConfigIterForm();

        _w.title.addWatcher(this);

        // add button action listeners
        _w.deleteItem.addActionListener(this);
        _w.addStep.addActionListener(this);
        _w.deleteStep.addActionListener(this);
        _w.top.addActionListener(this);
        _w.up.addActionListener(this);
        _w.down.addActionListener(this);
        _w.bottom.addActionListener(this);

        // JBuilder has some problems with image buttons...
        _w.top.setIcon(Resources.getIcon("top.gif"));
        _w.up.setIcon(Resources.getIcon("up.gif"));
        _w.bottom.setIcon(Resources.getIcon("bottom.gif"));
        _w.down.setIcon(Resources.getIcon("down.gif"));

        // Watch for selection of cells in the iterator table.
        _iterTab = _w.iterStepsTable;
        _iterTab.addWatcher(this);

        _iterTab.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable jTable, Object object, boolean b, boolean b1, int row, int column) {
                String strVal;
                if (object == null) {
                    strVal = "";
                } else {
                    if (object instanceof DisplayableSpType) {
                        strVal = ((DisplayableSpType) object).displayValue();
                    } else if (object instanceof Option) {
                        strVal = OptionTypeUtil.toDisplayString((Option) object);
                    } else {
                        strVal = object.toString();
                    }
                    if ((object instanceof ObsoletableSpType) && ((ObsoletableSpType) object).isObsolete()) {
                        strVal += "*";
                    }
                }

                PropertyDescriptor pd = _tableModel.getPropertyDescriptor(column);
                setEnabled(isEditableProperty(pd));
                return super.getTableCellRendererComponent(jTable, strVal, b, b1, row, column);
            }
        });

        _iterTab.getSelectionModel().addListSelectionListener(e -> {
            _w.addStep.setEnabled(isEnabled() && _iterTab.getColumnCount() > 0);
            _w.deleteStep.setEnabled(isEnabled() && _iterTab.getSelectedRow() >= 0);
        });

        // Watch for selection of available items.
        _itemsLBW = _w.availableItems;
        _itemsLBW.addWatcher(this);

        // Initialize the ListBox value editor
        //noinspection unchecked
        _listBoxVE =
                new ICListBoxValueEditor(this, _w.listBoxGroup, _w.listBoxTitle,
                        _w.availableChoices);

        // Initialize the TextBox value editor
        _textBoxVE =
                new ICTextBoxValueEditor(this, _w.textBoxGroup, _w.textBoxTitle,
                        _w.textBox);
        _numberBoxVE =
                new ICTextBoxValueEditor(this, _w.numberBoxGroup, _w.numberBoxTitle,
                        _w.numberBox);

        // Initialize the ComboBox value editor
        //noinspection unchecked
        _comboBoxVE =
                new ICComboBoxValueEditor(this, _w.comboBoxGroup, _w.comboBoxTitle,
                        _w.comboBox);

        _valueEditor = _listBoxVE;
    }

    private boolean isEditableProperty(final PropertyDescriptor pd) {
        if (pd == null) return true;
        if (OTOptions.isStaff(getProgram().getProgramID())) return true;
        return !pd.isExpert() && !PropertySupport.isEngineering(pd);
    }

    /**
     * Update the enabled (editable) state of this editor.
     * The default implementation just enables or disables all components in
     * the editor window.
     * Here we need to handle the components that are not currently in the
     * component hierarchy.
     */
    protected void updateEnabledState(final boolean enabled) {
        if (enabled != isEnabled()) {
            setEnabled(enabled);
            updateEnabledState(getWindow().getComponents(), enabled);

            // 3 out of 4 of these will not be in the component hierarchy
            final List<Component> l = new ArrayList<>(3);
            if (_valueEditor != _listBoxVE) {
                l.add(_w.availableChoices);
            }
            if (_valueEditor != _textBoxVE) {
                l.add(_w.textBox);
            }
            if (_valueEditor != _numberBoxVE) {
                l.add(_w.numberBox);
            }
            if (_valueEditor != _comboBoxVE) {
                l.add(_w.comboBox);
            }
            final Component[] ar = new Component[l.size()];
            l.toArray(ar);
            updateEnabledState(ar, enabled);
        }

        final int rowIndex = _iterTab.getSelectedRow();
        final int colIndex = _iterTab.getSelectedColumn();

        if (enabled && (colIndex >= 0)) {
            final PropertyDescriptor pd = _tableModel.getPropertyDescriptor(colIndex);
            _w.deleteItem.setEnabled(isEditableProperty(pd));
        } else {
            _w.deleteItem.setEnabled(false);
        }

        _w.deleteStep.setEnabled(enabled && (rowIndex >= 0));
        _w.addStep.setEnabled(enabled && (_tableModel.getColumnCount() > 0));
    }

    /**
     * Return the window containing the editor
     */
    public JPanel getWindow() {
        return _w;
    }

    /**
     * Set the data object corresponding to this editor.
     */
    public void init() {

        final T o = getDataObject();
        String title = o.getEditableTitle();
        _w.title.setText(title);

        _checkIfFirstInstIterator();

        _allPropertyMap = getAllPropertyMap(o);
        _initFromDataObject(_allPropertyMap, o);
    }

    // (Re)Initialize the GUI from the data object
    private void _initFromDataObject(Map<String, PropertyDescriptor> props, ISPDataObject obj) {
        IConfigProvider iIterConfigProvider = (IConfigProvider) obj;
        ISysConfig currentSysConfig = iIterConfigProvider.getSysConfig();

        _tableModel = new EdIterGenericConfigTableModel(currentSysConfig, props);

        _listBoxVE.setVisible(true);
        _textBoxVE.setVisible(false);
        _comboBoxVE.setVisible(false);
        _numberBoxVE.setVisible(false);

        _valueEditor = _listBoxVE;
        _valueEditor.clear();

        _initAvailableItems();

        // If this is the first instrument iterator, make sure there are no
        // nulls in the first row.
        _syncFirstRowNulls();

        _iterTab.setModel(_tableModel);
        if (_tableModel.getRowCount() > 0) {
            _iterTab.selectCell(0, 0);
        }
        _updateTableInfo();
    }


    // Check if this is the first instrument iterator in the sequence and set a flag
    private void _checkIfFirstInstIterator() {
        _isFirstInstIterator = false;
        final ISPObservation obsNode = getContextObservation();
        if (obsNode != null) {
            final ISPSeqComponent seq = obsNode.getSeqComponent();
            if (seq != null) {
                final ISPSeqComponent first = InstrumentSequenceSync.firstInstrumentIterator(seq);
                _isFirstInstIterator = (first != null) && (first == getNode());
            }
        }
    }


    // Return the "Delete Item" button
    public JButton getDeleteItem() {
        return _w.deleteItem;
    }


    /**
     * This makes sure that the GUI knows this item was edited.
     */
    private void _updateSysConfig() {
        getDataObject().setSysConfig(_tableModel.getSysConfig());
    }

    //
    // Update table info text box that shows the number of items and steps
    // in the table.
    //
    private void _updateTableInfo() {
        JLabel stw = _w.tableInfo;
        int items = _tableModel.getColumnCount();
        int steps = _tableModel.getRowCount();

        String message = "(" + items;
        if (items == 1) {
            message += " Item, ";
        } else {
            message += " Items, ";
        }
        message += steps;
        if (steps == 1) {
            message += " Step)";
        } else {
            message += " Steps)";
        }

        stw.setText(message);
    }


    private PropertyDescriptor _getCurPropertyDescriptor() {
        // Get the column index of the selected cell.
        int[] coord = _iterTab.getSelectedCoordinates();
        int colIndex = coord[0];
        if (colIndex == -1) return null;
        return _tableModel.getPropertyDescriptor(colIndex);
    }

    /**
     * Select the cell on the current row in the given column.
     */
    public void selectColumnCell(int colIndex) {
        int[] coord = _iterTab.getSelectedCoordinates();
        int rowIndex = coord[1];
        _iterTab.selectCell(colIndex, rowIndex);
        _iterTab.focusAtCell(colIndex, rowIndex);
    }

    private void addConfigItem(PropertyDescriptor pd) {
        Object defaultValue = _getDefaultParamValue(pd.getName());
        _tableModel.addColumn(pd, defaultValue);
        if (_tableModel.getRowCount() == 0) _tableModel.addRow(0);
        _tableModel.setValueAt(defaultValue, 0, _tableModel.getColumnIndex(pd));
        _updateSysConfig();
    }

    // Figures out the iterable properties that should be displayed.
    private Map<String, PropertyDescriptor> getAllPropertyMap(Object dataObj) {
        if (!(dataObj instanceof PropertyProvider)) {
            return Collections.emptyMap();
        }
        return ((PropertyProvider) dataObj).getProperties();
    }

    // Figures out the iterable properties that should be displayed.
    private Set<PropertyDescriptor> getEditablePropertySet(Map<String, PropertyDescriptor> allPropertyMap) {

        if (OTOptions.isStaff(getProgram().getProgramID())) {
            return new HashSet<>(allPropertyMap.values());
        }

        // Not onsite so get rid of expert properties.
        PropertyFilter filter = new PropertyFilter.Not(PropertyFilter.EXPERT_FILTER);
        filter = new PropertyFilter.And(filter,
                               new PropertyFilter.Not(PropertyFilter.ENGINEERING_FILTER));
        return new HashSet<>(PropertySupport.filter(filter, allPropertyMap).values());
    }

    // If this is the first instrument iterator in the sequence, return the current parameter
    // value from the instrument data object, otherwise  return the default parameter value (null).
    private Object _getDefaultParamValue(String attribute) {
        if (_isFirstInstIterator) {
            // check the static instrument for the value
            SPInstObsComp obsComp = getContextInstrumentDataObject();
            if (obsComp instanceof PropertyProvider) {
                Map<String, PropertyDescriptor> map;
                map = ((PropertyProvider) obsComp).getProperties();
                PropertyDescriptor pd = map.get(attribute);
                if (pd != null) {
                    Object o;
                    o = SPTreeEditUtil.getDefaultParamValue(obsComp, pd);
                    if (o != null) return o;
                }
            }

            // If there is an engineering component, check it too
            ISPObservation obs = getContextObservation();
            ISPObsComponent eng = SPTreeUtil.findObsComponentByBroadType(obs, SPComponentBroadType.ENGINEERING); //"Engineering");
            if (eng != null) {
                AbstractDataObject dataObject;
                // LORD OF DESTRUCTION: DataObjectManager get without set
                dataObject = (AbstractDataObject) eng.getDataObject();
                if (dataObject instanceof PropertyProvider) {
                    Map<String, PropertyDescriptor> map;
                    map = ((PropertyProvider) dataObject).getProperties();
                    PropertyDescriptor pd = map.get(attribute);
                    if (pd != null) {
                        return SPTreeEditUtil.getDefaultParamValue(dataObject, pd);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Delete the column of the currently selected cell from the table.  This
     * will result in removing the associated attribute from the set of
     * attributes being iterated over.
     */
    public void deleteSelectedColumn() {
        PropertyDescriptor pd = _getCurPropertyDescriptor();
        if (pd == null) return;
        deleteColumn(pd);
    }

    /**
     * Remove an attribute from the set being iterated over.
     */
    public void deleteColumn(PropertyDescriptor pd) {
        int col = _tableModel.getColumnIndex(pd);
        if (col < 0) return;

        // Remember which cell was selected.
        int[] coord = _iterTab.getSelectedCoordinates();
        int colIndex = coord[0];
        int rowIndex = coord[1];

        _tableModel.removeColumn(col);

        // Make sure the colIndex is still valid
        if (_tableModel.getColumnCount() <= colIndex) {
            colIndex = _tableModel.getColumnCount() - 1;
        }

        // Reselect the old col,row if there's anything left in the table,
        // otherwise, remove all the rows and all the available choices.
        if (colIndex >= 0) {
            _iterTab.selectCell(colIndex, rowIndex);
            _iterTab.focusAtRow(rowIndex);
        } else {
            _valueEditor.clear();
            _w.addStep.setEnabled(false);
            _w.deleteItem.setEnabled(false);
        }

        // In Bongo1.0, used to just reinit the list of available items, removing
        // the just added value.  That no longer works, so now just select nothing.
        //_initAvailableItems();
        _unselectAvailableItems();

        _updateTableInfo();
        _updateSysConfig();
    }


    /**
     * Add an iteration step.
     */
    public void addStep() {
        if (_tableModel.getColumnCount() == 0) return;

        // Figure out the coordinates of the currently selected cell, if any
        int[] coord = _iterTab.getSelectedCoordinates();
        int colIndex = coord[0];
        int rowIndex = coord[1];
        if (rowIndex < 0) rowIndex = _tableModel.getRowCount()-1;

        _tableModel.addRow(++rowIndex);
        if (colIndex < 0) colIndex = 0;

        // Select the cell in the current column, newly added row
        _iterTab.selectCell(colIndex, rowIndex);
        _iterTab.focusAtRow(rowIndex);

        _updateTableInfo();

        if (_tableModel.getRowCount() == 1) _syncFirstRowNulls();
        _updateSysConfig();
    }


    /**
     * Delete an iteration step.
     */
    public void deleteStep() {
        if ((_tableModel.getColumnCount() == 0) || (_tableModel.getRowCount() == 0)) {
            return;
        }

        int[] coord = _iterTab.getSelectedCoordinates();
        if ((coord[0] == -1) || (coord[1] == -1)) return;

        int rowIndex = coord[1];
        _tableModel.removeRow(rowIndex);

        // Select the next cell in the next step (or previous step if this was
        // the last element.)
        if (_iterTab.getRowCount() <= rowIndex) {
            rowIndex = _iterTab.getRowCount() - 1;
        }

        if (rowIndex >= 0) {
            _syncFirstRowNulls();
            _iterTab.selectCell(coord[0], rowIndex);
            _iterTab.focusAtRow(rowIndex);
        } else {
            _valueEditor.clear();
            _unselectAvailableItems();
            _w.addStep.setEnabled(false);
            _w.deleteItem.setEnabled(false);
        }
        _updateTableInfo();
        _updateSysConfig();
    }

    private void _syncFirstRowNulls() {
        if (!_isFirstInstIterator) return;
        if (_tableModel.getRowCount() <= 0) return;

        boolean updatedSysConfig = false;
        for (int col = 0; col<_tableModel.getColumnCount(); ++col) {
            Object val = _tableModel.getValueAt(0, col);
            if (val != null) continue;

            PropertyDescriptor pd = _tableModel.getPropertyDescriptor(col);
            val = _getDefaultParamValue(pd.getName());
            if (val != null) {
                updatedSysConfig = true;
                _tableModel.setValueAt(val, 0, col);
            }
        }
        if (updatedSysConfig) _updateSysConfig();
    }

    private void moveStep(int to) {
        int[] coord = _iterTab.getSelectedCoordinates();
        int row = coord[1];
        int col = coord[0];
        if ((row == -1) || (col == -1)) return;

        int rowCount = _tableModel.getRowCount();
        if ((row == to) || (to < 0) || (to >= rowCount)) return; // nothing to do

        _tableModel.moveRow(row, to);
        if ((row == 0) || (to == 0)) _syncFirstRowNulls();

        _updateSysConfig();
        _iterTab.selectCell(col, to);
        _iterTab.focusAtRow(to);
    }

    /**
     * Move the current step to be the first step.
     */
    public void stepToFirst() {
        moveStep(0);
    }

    /**
     * Move the current step up one.
     */
    public void decrementStep() {
        int[] coord = _iterTab.getSelectedCoordinates();
        moveStep(coord[1]-1);
    }

    /**
     * Move the current step down one.
     */
    public void incrementStep() {
        int[] coord = _iterTab.getSelectedCoordinates();
        moveStep(coord[1]+1);
    }

    /**
     * Move the current step to the end.
     */
    public void stepToLast() {
        moveStep(_tableModel.getRowCount()-1);
    }

    /**
     * Change the _valueEditor reference to the given value editor.  The
     * old value editor will be hidden, and the new one shown.
     */
    private void _setEditor(ICValueEditor ve) {
        if (_valueEditor == ve) {
            return;
        }

        if (_valueEditor != null) {
            _valueEditor.setVisible(false);
        }

        _valueEditor = ve;
        _valueEditor.setVisible(true);
    }

    /**
     * Called when a table cell is selected.  The value editor is reconfigured
     * to display the appropriate editor for the attribute in the selected
     * cell.
     *
     * @see CellSelectTableWidget
     */
    public void cellSelected(CellSelectTableWidget w, int colIndex, int rowIndex) {
        if (_ignoreEvents) return;

        Object cellValue = _tableModel.getValueAt(rowIndex, colIndex);

        PropertyDescriptor pd = _tableModel.getPropertyDescriptor(colIndex);
        _setEditor(getValueEditor(pd));

        _valueEditor.editValue(pd, cellValue);
        boolean enabled = isEnabled() && isEditableProperty(pd);
        _valueEditor.setEnabled(enabled);
        _w.deleteItem.setEnabled(enabled);
    }

    private ICValueEditor getValueEditor(PropertyDescriptor pd) {
        if (pd == null) return _textBoxVE;

        Class<?> propertyType = pd.getPropertyType();
        if (Option.class.isAssignableFrom(propertyType)) {
            propertyType = PropertySupport.getWrappedType(pd);
        }

        return getValueEditor(propertyType);
    }

    private ICValueEditor getValueEditor(Class<?> propertyType) {
        ICValueEditor res;
        if (propertyType.isEnum()) {
            res = _listBoxVE;

        } else if (SuggestibleString.class.isAssignableFrom(propertyType)) {
            res = _comboBoxVE;

        } else {
            if (Number.class.isAssignableFrom(propertyType)) {
                res = _numberBoxVE;
            } else {
                res = _textBoxVE;
            }
        }
        return res;
    }

    //
    // Turn off the selection in the list box containing the available
    // items.
    //
    private void _unselectAvailableItems() {
        _itemsLBW.deleteWatcher(this);
        _itemsLBW.setValue(-1);
        _itemsLBW.addWatcher(this);
    }

    //
    // Initialize the list box containing the available items.
    //
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void _initAvailableItems() {
        Vector<PropertyDescriptor> v = new Vector<>(getEditablePropertySet(_allPropertyMap));

        v.sort(new Comparator<PropertyDescriptor>() {
            private boolean isEngineering(PropertyDescriptor pd) {
                return pd.isExpert() || PropertySupport.isEngineering(pd);
            }

            public int compare(PropertyDescriptor pd1, PropertyDescriptor pd2) {
                // Sort engineering items to the bottom.
                boolean e1 = isEngineering(pd1);
                boolean e2 = isEngineering(pd2);
                if (e1) {
                    if (!e2) return 1;
                } else if (e2) {
                    return -1;
                }

                // Sort by name otherwise.
                return pd1.getDisplayName().compareToIgnoreCase(pd2.getDisplayName());
            }
        });

        ListCellRenderer rend = new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list,
                                                          Object value, int index, boolean isSelected, boolean hasFocus) {
                PropertyDescriptor pd = (PropertyDescriptor) value;
                JLabel lab = (JLabel) super.getListCellRendererComponent(list, pd.getDisplayName(), index, isSelected, hasFocus);

                if (pd.isExpert() || PropertySupport.isEngineering(pd)) {
                    lab.setIcon(Resources.getIcon("eclipse/engineering.gif"));
                } else {
                    lab.setIcon(null);
                }

                return lab;
            }

        };

        _itemsLBW.deleteWatcher(this);
        _itemsLBW.setValue(-1);
        _itemsLBW.setChoices(v);
        _itemsLBW.setCellRenderer(rend);
        _itemsLBW.addWatcher(this);
    }

    /**
     * Called when an item is selected from the list of available items.  The
     * corresponding attribute is added as a table column and the selection
     * is moved to the new column.
     *
     * @see jsky.util.gui.ListBoxWidgetWatcher
     */
    public void listBoxSelect(ListBoxWidget<PropertyDescriptor> w, int index, Object val) {
        if (w != _itemsLBW) {
            // Something odd happened
            throw new RuntimeException("weird listBoxSelect error: " + w);
        }

        PropertyDescriptor pd = (PropertyDescriptor) val;

        // Find the index of the column, if it is already in the table.  If not,
        // add it.  If so, select it.
        int colIndex = _tableModel.getColumnIndex(pd);
        if (colIndex == -1) {
            // Workaround for problem with multiple selection/cell addition events
            _ignoreEvents = true;
            try {
                addConfigItem(pd);
                _iterTab.clearSelection();
            } finally {
                _ignoreEvents = false;
            }
            colIndex = _tableModel.getColumnIndex(pd);
            if (colIndex != -1) {
                _iterTab.selectCell(colIndex, 0);
                _iterTab.focusAtCell(colIndex, 0);
            }
        } else {
            // select cell in existing column
            selectColumnCell(colIndex);
        }
        _updateTableInfo();
    }

    /**
     * Ignore list box actions.
     *
     * @see jsky.util.gui.ListBoxWidgetWatcher
     */
    public void listBoxAction(ListBoxWidget<PropertyDescriptor> w, int index, Object val) {
        // Don't care ...
    }

    /**
     * This method is called when the value of the selected step and attribute
     * is changed.
     */
    public void cellValueChanged(Object newVal, boolean finishedEditing) {
        // Get the selected cell's coordinates
        int[] coord = _iterTab.getSelectedCoordinates();
        if ((coord[0] == -1) || (coord[1] == -1)) return;
        int colIndex = coord[0];
        int rowIndex = coord[1];

        // Figure out the IterConfigItem that goes with the selected cell
        PropertyDescriptor pd = _tableModel.getPropertyDescriptor(colIndex);
        if (pd == null) {
            throw new RuntimeException(
                    "couldn't find the IterConfigItem associated with column: " +
                    colIndex);
        }
//        _iterTab2.setSelectedCell(newVal);

        // Set the value in the selected cell
        if (newVal == null) {
            if (rowIndex == 0) return;
            newVal = _tableModel.getValueAt(rowIndex - 1, colIndex);
        }
        _tableModel.setValueAt(newVal, rowIndex, colIndex);

        ++rowIndex;

        if ((finishedEditing) && (rowIndex < _tableModel.getRowCount())) {
            // Move to the next cell down in the column
            _iterTab.selectCell(colIndex, rowIndex);
            _iterTab.focusAtCell(colIndex, rowIndex);
        }
        _updateSysConfig();
    }

    //
    // Handle action events on the buttons in the editor.
    //
    public void actionPerformed(ActionEvent e) {
        Object w = e.getSource();

        // Delete the selected column
        if (w == _w.deleteItem) {
            deleteSelectedColumn();
            return;
        }

        // Add a row (iter step) to the table
        if (w == _w.addStep) {
            addStep();
            return;
        }

        // Delete a row (iter step) from the table
        if (w == _w.deleteStep) {
            deleteStep();
            return;
        }

        // Move a row (iter step) to the end
        if (w == _w.top) {
            stepToFirst();
            return;
        }

        // Move a row (iter step) up
        if (w == _w.up) {
            decrementStep();
            return;
        }

        // Move a row (iter step) down
        if (w == _w.down) {
            incrementStep();
            return;
        }

        // Move a row (iter step) to the end
        if (w == _w.bottom) {
            stepToLast();
        }
    }


    /**
     * Show the given group of widgets (in upper left choice panel)
     */
    public void showGroup(JPanel panel) {
        _w.choicePanel.remove(_w.listBoxGroup);
        _w.choicePanel.remove(_w.textBoxGroup);
        _w.choicePanel.remove(_w.numberBoxGroup);
        _w.choicePanel.remove(_w.comboBoxGroup);

        _w.choicePanel.add(panel, BorderLayout.CENTER);
        _w.choicePanel.revalidate();
        _w.repaint();
    }

   /**
     * Watch changes to the title text box.
     * @see TextBoxWidgetWatcher
     */
    public void textBoxKeyPress(TextBoxWidget tbwe) {
        getDataObject().setTitle(tbwe.getText().trim());
    }

    /**
     * Text box action, ignore.
     * @see TextBoxWidgetWatcher
     */
    public void textBoxAction(TextBoxWidget tbwe) {
    }

    //
    // Helper class for EdIterGenericConfig.  It is the base class for editing
    // various attribute types (currently "list of choices" and "text box").
    // The ICValueEditor is concerned with displaying the control needed to
    // edit a particular attribute, as described by a
    // jsky.app.ot.sp.iter.IterConfigItem.  When the user changes the value in the
    // editor, the EdIterGenericConfig instance is informed of the new value.
    // Each ICValueEditor subclass appears in its own JPanel.
    abstract class ICValueEditor {

        protected EdIterGenericConfig<T> _ci;
        protected JPanel _container;
        protected JLabel _title;

        ICValueEditor(EdIterGenericConfig<T> ci, JPanel panel, JLabel title) {
            _ci = ci;
            _container = panel;
            _title = title;
        }

        // Show the JPanel that contains the editor's widgets.
        void setVisible(boolean visible) {
            if (visible) {
                _ci.showGroup(_container);
            }
        }

        // Update the editor's widgets to reflect the given IterConfigItem.
        abstract void editValue(PropertyDescriptor property, Object curValue);

        // Update the editor's widgets to show no value.
        abstract void clear();

        abstract void setEnabled(boolean enabled);
    }

    private static final ListCellRenderer<Object> LIST_BOX_OPTION_RENDERER = new DefaultListCellRenderer() {
        @Override
        public Component getListCellRendererComponent(JList<?> jList, Object obj, int i, boolean b, boolean b1) {
            if (obj == null) {
                return super.getListCellRendererComponent(jList, null, i, b, b1);
            }

            int style = Font.PLAIN;

            String strVal;
            if (obj instanceof DisplayableSpType) {
                strVal = ((DisplayableSpType) obj).displayValue();
            } else if (obj instanceof Option) {
                strVal = OptionTypeUtil.toDisplayString((Option) obj);
                if (None.instance().equals(obj)) {
                    style = Font.ITALIC;
                }
            } else {
                strVal = obj.toString();
            }

            JLabel lab = (JLabel)super.getListCellRendererComponent(jList, strVal, i, b, b1);
            lab.setFont(lab.getFont().deriveFont(style));

            // OT-50: display icon for engineering choices
            if (obj instanceof PartiallyEngineeringSpType && ((PartiallyEngineeringSpType)obj).isEngineering()) {
                lab.setIcon(Resources.getIcon("eclipse/engineering.gif"));
            } else {
                lab.setIcon(null);
            }

            return lab;
        }
    };

    //
    // An ICValueEditor for ListBoxes.
    //
    private class ICListBoxValueEditor extends ICValueEditor implements ListBoxWidgetWatcher<Object> {

        ListBoxWidget<Object> _choicesLBW;
        PropertyDescriptor _lastProp;

        ICListBoxValueEditor(EdIterGenericConfig<T> ci, JPanel panel, JLabel label,
                             ListBoxWidget<Object> lbw) {
            super(ci, panel, label);
            _choicesLBW = lbw;
            _choicesLBW.addWatcher(this);
        }

        // Show the list of choices defined in the IterConfigItem, and select the
        // curValue.
        void editValue(PropertyDescriptor pd, Object curValue) {
            _title.setText("Select " + pd.getDisplayName());

            _choicesLBW.deleteWatcher(this);

            if (pd != _lastProp) {
                _choicesLBW.clear();
                _choicesLBW.setCellRenderer(LIST_BOX_OPTION_RENDERER);
                _choicesLBW.setChoices(getChoices(pd));
                _lastProp = pd;
            }

            if ((curValue == null) || curValue.equals("")) {
                _choicesLBW.setValue(-1);
            } else {
                _choicesLBW.setValue(curValue);
            }
            _choicesLBW.addWatcher(this);
        }

        private List<Object> getChoices(PropertyDescriptor pd) {
            Class<?> propertyType = pd.getPropertyType();
            if (Option.class.isAssignableFrom(propertyType)) {
                propertyType = PropertySupport.getWrappedType(pd);
                return getOptionActiveElements(propertyType);
            }
            return getActiveElements(propertyType);
        }

        private List<Object> getOptionActiveElements(Class<?> c) {
            List<Object> underlyingChoices = getActiveElements(c);

            List<Object> res = new ArrayList<>(underlyingChoices.size() + 1);
            res.add(None.instance());
            res.addAll(underlyingChoices.stream().map(Some::new).collect(Collectors.toList()));
            return res;
        }

        @SuppressWarnings("rawtypes")
        private List<Object> getActiveElements(Class c) {
            //noinspection unchecked
            return new ArrayList<>(engineeringFilter(SpTypeUtil.getSelectableItems(c)));
        }

        void setEnabled(boolean enabled) {
            _choicesLBW.setEnabled(enabled);
        }

        void clear() {
            _title.setText("Nothing Selected");
            _choicesLBW.deleteWatcher(this);
            _choicesLBW.clear();
            _choicesLBW.addWatcher(this);
            _lastProp = null;
        }

        // Called when the user selects an option from the list box.  The
        // EdIterGenericConfig instance is informed of the new value.
        @Override
        public void listBoxSelect(ListBoxWidget<Object> w, int index, Object val) {
            // There is a new value for the current attribute, but the user
            // may not be finished editing.
            _ci.cellValueChanged(val, false);
        }

        // Called when the user double-clicks an option from the list box.
        // The EdIterGenericConfig instance is informed of the new value.
        @Override
        public void listBoxAction(ListBoxWidget<Object> w, int index, Object val) {
            // There is a new value for the current attribute, and the user
            // is finished editing.
            _ci.cellValueChanged(val, true);
        }
    }

    //
    // An ICValueEditor for ComboBoxes.
    //
    class ICComboBoxValueEditor extends ICValueEditor {

        DropDownListBoxWidget<Object> _comboBox;
        PropertyDescriptor _lastProp;
        // Flag used to ignore events in combobox
        private boolean _ignoreUpdate = false;

        private String _lastValidValue;

        ICComboBoxValueEditor(EdIterGenericConfig<T> ci, JPanel panel, JLabel label,
                              DropDownListBoxWidget<Object> comboBox) {
            super(ci, panel, label);
            _comboBox = comboBox;
            // Also need to update on keystrokes in the editor
            final Object o = comboBox.getEditor().getEditorComponent();
            if (o instanceof JTextField) {
                ((JTextField) o).getDocument().addDocumentListener(new DocumentListener() {
                    public void changedUpdate(DocumentEvent e) {
                        _update(e.getDocument());
                    }

                    public void insertUpdate(DocumentEvent e) {
                        _update(e.getDocument());
                    }

                    public void removeUpdate(DocumentEvent e) {
                        _update(e.getDocument());
                    }
                });

                // When the focus leaves, restore the value to the last valid
                // one.  If the field is a double for example, and the user
                // has entered something that doesn't parse as a double, we
                // will reset the field here to the last valid value.
                ((JTextField) o).addFocusListener(new FocusListener() {
                    @Override public void focusGained(FocusEvent e) { }
                    @Override public void focusLost(FocusEvent e) {
                        if (_lastValidValue == null) return;
                        boolean ignore = _ignoreUpdate;
                        _ignoreUpdate = true;
                        try {
                            ((JTextField)o).setText(_lastValidValue);
                        } finally {
                            _ignoreUpdate = ignore;
                        }
                    }
                });
            }
        }

        void setEnabled(boolean enabled) {
            _comboBox.setEnabled(enabled);
        }

        // Called when the user types a value in the combobox editor
        private void _update(final Document doc) {
            if (_ignoreUpdate) {
                return;
            }

            _ignoreUpdate = true;
            try {
                final String val = doc.getText(0, doc.getLength());
                final PropertyEditor ed = PropertyEditorManager.findEditor(_lastProp.getPropertyType());
                if (ed != null) {
                    ed.setAsText(val);
                    _ci.cellValueChanged(ed.getValue(), false);
                    _lastValidValue = val;  // record the update
                }
            } catch (final Exception e) {
                // Value wasn't valid for this property.
            }
            _ignoreUpdate = false;
        }

        // Show the list of choices defined in the IterConfigItem, and select the
        // curValue.
        void editValue(PropertyDescriptor pd, Object curValue) {
            _title.setText("Select " + pd.getDisplayName());

            _ignoreUpdate = true;
            try {
                if (pd != _lastProp) {
                    _comboBox.setChoices(getSuggestions(pd, curValue));
                    _lastProp = pd;
                }
                if ((curValue == null) || curValue.equals("")) {
                    _comboBox.setValue(-1);
                } else {
                    _comboBox.setValue(curValue);
                }
            } finally {
                _ignoreUpdate = false;
            }

            // Remember this value as the last valid value.
            if (curValue != null) {
                _lastValidValue = ((SuggestibleString) curValue).getStringValue();
            } else {
                _lastValidValue = null;
            }
        }

        private Object[] getSuggestions(PropertyDescriptor pd, Object curValue) {
            if (curValue != null) {
                return ((SuggestibleString) curValue).getEnumConstants();
            }

            Class<?> c = pd.getPropertyType();
            try {
                SuggestibleString ss = (SuggestibleString) c.newInstance();
                return ss.getEnumConstants();
            } catch (Exception ex) {
                LOG.log(Level.INFO, "Could not create an instance of: " + c.getName());
            }
            return new Object[0];
        }

        void clear() {
            _title.setText("Nothing Selected");
            _comboBox.clear();
            _lastProp = null;
        }
    }

    //
    // An ICValueEditor for TextBoxes.
    //
    class ICTextBoxValueEditor extends ICValueEditor implements TextBoxWidgetWatcher {

        TextBoxWidget _textBox;
        private PropertyDescriptor _pd;

        ICTextBoxValueEditor(EdIterGenericConfig<T> ci, JPanel panel, JLabel label,
                             TextBoxWidget tbw) {
            super(ci, panel, label);
            _textBox = tbw;
            _textBox.addWatcher(this);
        }

        //
        // Put the curValue in the text box.
        //
        void editValue(PropertyDescriptor pd, Object curValue) {
            _title.setText("Enter " + pd.getDisplayName());
            if (curValue == null) {
                _textBox.setText("");
            } else {
                _textBox.setText(curValue.toString());
            }
            _pd = pd;
        }

        void setEnabled(boolean enabled) {
            _textBox.setEnabled(enabled);
        }

        void clear() {
            _title.setText("Nothing Selected");
            _textBox.setText("");
        }

        private Object _getValueObject(String text) {
            return PropertySupport.stringToValue(text, _pd);
        }

        // Called when the user types a key in the text box.  The
        // EdIterGenericConfig instance is informed of the new value.
        public void textBoxKeyPress(TextBoxWidget tbw) {
            _ci.cellValueChanged(_getValueObject(tbw.getText()), false);
        }

        // Called when the user types a return key in the text box.  The
        // EdIterGenericConfig instance is informed of the new value.
        public void textBoxAction(TextBoxWidget tbw) {
            _ci.cellValueChanged(_getValueObject(tbw.getText()), true);
        }
    }

    // OT-50: If the OT is not running on-site, remove any expert/engineering options from the returned array
    private <A extends Enum<A>> List<A> engineeringFilter(List<A> list) {
        if (OTOptions.isStaff(getProgram().getProgramID())) {
            return list;
        }
        List<A> result = new ArrayList<>(list.size());
        for (A o : list) {
            if (o instanceof PartiallyEngineeringSpType) {
                if (!((PartiallyEngineeringSpType) o).isEngineering()) {
                    result.add(o);
                }
            } else {
                result.add(o);
            }
        }
        return result;
    }
}
