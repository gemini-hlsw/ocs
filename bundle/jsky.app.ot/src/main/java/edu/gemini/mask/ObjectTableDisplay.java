package edu.gemini.mask;

import edu.gemini.catalog.ui.tpe.CatalogDisplay;
import jsky.catalog.gui.TableDisplayTool;
import jsky.catalog.gui.QueryResultDisplay;
import jsky.catalog.TableQueryResult;
import jsky.catalog.QueryResult;
import jsky.navigator.Navigator;
import jsky.navigator.NavigatorManager;
import jsky.util.gui.DialogUtil;
import jsky.util.gui.SortedJTable;
import jsky.util.gui.TextBoxWidget;
import jsky.image.gui.MainImageDisplay;

import javax.swing.JPanel;
import javax.swing.JCheckBox;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.BorderLayout;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

/**
 * Defines the widget used to display {@link ObjectTable}s.
 * This is the usual widget used to display table query results,
 * but with some added features.
 */
public class ObjectTableDisplay extends TableDisplayTool
        implements jsky.util.gui.TextBoxWidgetWatcher, ActionListener, PropertyChangeListener {

    // Extra edit panel added below object table display
    private MaskButtonPanelGUI _maskButtonPanel;

    // Popup dialog for generating mask files
    private MaskDialog _maskDialog;

    // Reference to the catalog window
    private Navigator _navigator;

    // Reference to main image display widget
    private CatalogDisplay _imageDisplay = NavigatorManager.get().getImageDisplay();

    // Used to visualize the mask items on the image
    private MaskDisplay _maskDisplay = new MaskDisplay(_imageDisplay);

    private JCheckBox _plotSlitsButton;
    private JCheckBox _plotGapsButton;
    private JCheckBox _plotBandsButton;

    /**
     * Constructor
     */
    public ObjectTableDisplay(TableQueryResult tableQueryResult,
                              QueryResultDisplay display) {
        super(tableQueryResult, display, ((Navigator)display).getPlotter());

        // update GUI when the table data changes
        _navigator = (Navigator)display;
        _navigator.addChangeListener(e -> {
            _tableChanged(getTable());
            _maskDisplay.setEnabled(ObjectTableDisplay.this == _navigator.getResultComponent());
        });
        _tableChanged(tableQueryResult);
    }


    /**
     * Override parent method to find out when the table data changes.
     */
    public void setQueryResult(QueryResult queryResult) {
        super.setQueryResult(queryResult);
        _tableChanged((TableQueryResult)queryResult);
    }

     // Check if this is an ODF (output, object definition table)
     // or an OT (input, object table) and display the corresponding widgets.
    private void _tableChanged(TableQueryResult tableQueryResult) {
         if (tableQueryResult instanceof ObjectTable) {
             ObjectTable table = (ObjectTable)tableQueryResult;
             BandDef bandDef = table.getMaskParams().getBandDef();
             if (table.isODF()) {
                 _maskButtonPanel.setVisible(false);
                 _plotSlitsButton.setVisible(true);
                 getTableDisplay().setShow(null);
             } else {
                 _maskButtonPanel.setVisible(true);
                 _plotSlitsButton.setVisible(false);
                 getTableDisplay().setShow(ObjectTable.OT_TABLE_COLUMNS);

                 // arrange to update the bands display if something changes
                 bandDef.removePropertyChangeListener(this);
                 bandDef.addPropertyChangeListener(this);
             }

             _plotBandsButton.setVisible(bandDef.getShuffleMode() == BandDef.BAND_SHUFFLE
                && bandDef.getBands().length > 0);

             _maskDisplay.setTable(table);
             _maskDisplay.repaintImage();
         }
     }

    // Called when the table's maskParams change. Check if we need to update
    // the band related display items.
    public void propertyChange(PropertyChangeEvent evt) {
        String prop = evt.getPropertyName();
        if (prop.equals(BandDef.SHUFFLE_MODE) || prop.equals(BandDef.BANDS)) {
            ObjectTable table = (ObjectTable)getTable();
            BandDef bandDef = table.getMaskParams().getBandDef();
            _plotBandsButton.setVisible(bandDef.getShuffleMode() == BandDef.BAND_SHUFFLE
                    && bandDef.getBands().length > 0);
            _maskDisplay.updateBands();
            _maskDisplay.repaintImage();
        }
    }

    /**
     * make and return the button panel
     */
    protected JPanel makeButtonPanel() {
        JPanel origPanel = super.makeButtonPanel();
        _maskButtonPanel = new MaskButtonPanelGUI();

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        JPanel checkBoxPanel = new JPanel();
        _addPlotSlitsButton(checkBoxPanel);
        _addPlotGapsButton(checkBoxPanel);
        _addPlotBandsButton(checkBoxPanel);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BorderLayout());
        buttonPanel.add(BorderLayout.WEST, checkBoxPanel);
        buttonPanel.add(BorderLayout.EAST, origPanel);

        panel.add(BorderLayout.NORTH, _maskButtonPanel);
        panel.add(BorderLayout.SOUTH, buttonPanel);

        // update table when user enters values
        _maskButtonPanel.slitPosX.addWatcher(this);
        _maskButtonPanel.slitPosY.addWatcher(this);
        _maskButtonPanel.slitSizeX.addWatcher(this);
        _maskButtonPanel.slitSizeY.addWatcher(this);
        _maskButtonPanel.slitTilt.addWatcher(this);

        // clear input fields when table selection changes, and disable if there is no
        // selection
        getSortedJTable().getSelectionModel().addListSelectionListener(e -> {
            _maskButtonPanel.slitPosX.setText("");
            _maskButtonPanel.slitPosY.setText("");
            _maskButtonPanel.slitSizeX.setText("");
            _maskButtonPanel.slitSizeY.setText("");
            _maskButtonPanel.slitTilt.setText("");

            boolean enabled1 = (getSortedJTable().getSelectedRowCount() != 0);
            _maskButtonPanel.slitPosX.setEnabled(enabled1);
            _maskButtonPanel.slitPosY.setEnabled(enabled1);
            _maskButtonPanel.slitSizeX.setEnabled(enabled1);
            _maskButtonPanel.slitSizeY.setEnabled(enabled1);
            _maskButtonPanel.slitTilt.setEnabled(enabled1);
        });

        _maskButtonPanel.p0Button.addActionListener(this);
        _maskButtonPanel.p1Button.addActionListener(this);
        _maskButtonPanel.p2Button.addActionListener(this);
        _maskButtonPanel.p3Button.addActionListener(this);
        _maskButtonPanel.pXButton.addActionListener(this);

        _maskButtonPanel.designMaskButton.addActionListener(ev -> {
            try {
                _designMask();
            } catch (Exception e) {
                DialogUtil.error(getParent(), e);
            }
        });

        return panel;
    }

    private void _addPlotGapsButton(JPanel panel) {
        _plotGapsButton = new JCheckBox("Show Gaps");
        _plotGapsButton.setToolTipText("Toggle the visualization of the gaps on the detector chips");
        panel.add(_plotGapsButton);
        _plotGapsButton.addActionListener(ev -> {
            _maskDisplay.setShowGaps(_plotGapsButton.isSelected());
            _maskDisplay.repaintImage();
        });
    }

    private void _addPlotSlitsButton(JPanel panel) {
        _plotSlitsButton = new JCheckBox("Show Slits");
        _plotSlitsButton.setToolTipText("Toggle the visualization of the slits and spectrum overlay");
        panel.add(_plotSlitsButton);
        _plotSlitsButton.addActionListener(ev -> {
            _maskDisplay.setShowSlits(_plotSlitsButton.isSelected());
            _maskDisplay.repaintImage();
        });
    }


    private void _addPlotBandsButton(JPanel panel) {
        _plotBandsButton = new JCheckBox("Show N&S Bands");
        _plotBandsButton.setToolTipText("Toggle the visualization of the nod & shuffle bands");
        panel.add(_plotBandsButton);
        _plotBandsButton.addActionListener(ev -> {
            _maskDisplay.setShowBands(_plotBandsButton.isSelected());
            _maskDisplay.repaintImage();
        });
    }

    private void _designMask() {
        if (_maskDialog == null) {
            _maskDialog = new MaskDialog((ObjectTable)getTable(), _imageDisplay);
        }

        _maskDialog.setVisible(true);
        _maskDialog.setTable((ObjectTable)getTable());
    }

    public void textBoxKeyPress(TextBoxWidget tbw) {
        Double d;
        try {
            d = (Double.valueOf(tbw.getValue()));
        } catch (NumberFormatException ex) {
            return;
        }

        if (tbw == _maskButtonPanel.slitPosX) {
            _updateTable(ObjectTable.SLITPOS_X_COL, d);
        } else if (tbw == _maskButtonPanel.slitPosY) {
            _updateTable(ObjectTable.SLITPOS_Y_COL, d);
        } else if (tbw == _maskButtonPanel.slitSizeX) {
            _updateTable(ObjectTable.SLITSIZE_X_COL, d);
        } else if (tbw == _maskButtonPanel.slitSizeY) {
            _updateTable(ObjectTable.SLITSIZE_Y_COL, d);
        } else if (tbw == _maskButtonPanel.slitTilt) {
            _updateTable(ObjectTable.SLITTILT_COL, d);
        }
    }

    public void textBoxAction(TextBoxWidget tbw) {
        // See textBoxKeyPress()
    }

    // Called when one of the priority buttons is pressed to change the priority
    // for the selected rows
    public void actionPerformed(ActionEvent e) {
        Object o = e.getSource();
        String priority = null;
        if (o == _maskButtonPanel.p0Button) {
            priority = "0";
        } else if (o == _maskButtonPanel.p1Button) {
            priority = "1";
        } else if (o == _maskButtonPanel.p2Button) {
            priority = "2";
        } else if (o == _maskButtonPanel.p3Button) {
            priority = "3";
        } else if (o == _maskButtonPanel.pXButton) {
            priority = "X";
        }
        if (priority != null) {
            _updateTable(ObjectTable.PRIORITY_COL, priority);
        }
    }

    // Update the column values for the selected table rows to the given value.
    private void _updateTable(int col, Object value) {
        ObjectTable table = (ObjectTable)getTable();
        SortedJTable t = getSortedJTable();
        int[] rows = t.getSelectedRows();
        for (int row : rows) {
            table.setValueAt(value, row, col);
        }
    }
}
