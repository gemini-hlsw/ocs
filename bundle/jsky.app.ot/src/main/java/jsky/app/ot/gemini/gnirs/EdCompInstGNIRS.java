package jsky.app.ot.gemini.gnirs;

import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.gemini.gnirs.GNIRSConstants;
import edu.gemini.spModel.gemini.gnirs.GNIRSParams.*;
import edu.gemini.spModel.gemini.gnirs.InstGNIRS;
import edu.gemini.spModel.telescope.IssPort;
import edu.gemini.spModel.type.SpTypeUtil;
import jsky.app.ot.OTOptions;
import jsky.app.ot.editor.type.SpTypeComboBoxModel;
import jsky.app.ot.editor.type.SpTypeComboBoxRenderer;
import jsky.app.ot.gemini.editor.EdCompInstBase;
import jsky.util.gui.DropDownListBoxWidget;
import jsky.util.gui.DropDownListBoxWidgetWatcher;
import jsky.util.gui.TextBoxWidget;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;
import java.util.*;

/**
 * This is the editor for the GNIRS South instrument component.
 */
public class EdCompInstGNIRS extends EdCompInstBase<InstGNIRS> implements ActionListener, DropDownListBoxWidgetWatcher {

    // The GUI layout panel
    private final GnirsForm _w;

    private JRadioButton _sidePortButton;
    private JRadioButton _upPortButton;

    // Set to true while updating some widgets, to ignore
    private boolean _ignoreEvents;

    // These are some indexes of menu items that need to be enabled/disabled
//    private static final int _SLIT_WIDTH_IFU_INDEX = SlitWidth.IFU.ordinal();

    // Used to format wavelength values
    private static final NumberFormat _nf = NumberFormat.getInstance(Locale.US);

    static {
        _nf.setMaximumFractionDigits(3);
    }

    /**
     * Listeners for property changes that affect the parallactic angle components.
     */
    final PropertyChangeListener updateParallacticAnglePCL;

    /**
     * The constructor initializes the user interface.
     */
    public EdCompInstGNIRS() {
        _w = new GnirsForm();

        _w.pixelScale.setChoices(SpTypeUtil.getFormattedDisplayValueAndDescriptions(PixelScale.class));
        _w.disperser.setChoices(SpTypeUtil.getFormattedDisplayValueAndDescriptions(Disperser.class));
        _w.centralWavelength.setChoices(_getDefaultWavelengths());

        //TODO: Add filter and acqMirror to GNIRS instrument component
       /* final SpTypeComboBoxModel<Filter> fModel = new SpTypeComboBoxModel<Filter>(Filter.class);
        _w.filter.setModel(fModel);
        _w.filter.setRenderer(new SpTypeComboBoxRenderer());
        _w.filter.setMaximumRowCount(Filter.class.getEnumConstants().length);
        _w.filter.addActionListener(this);

        final SpTypeComboBoxModel<AcquisitionMirror> amModel = new SpTypeComboBoxModel<AcquisitionMirror>(AcquisitionMirror.class);
        _w.acqMirror.setModel(amModel);
        _w.acqMirror.setRenderer(new SpTypeComboBoxRenderer());
        _w.acqMirror.setMaximumRowCount(AcquisitionMirror.class.getEnumConstants().length);
        _w.acqMirror.addActionListener(this);  */

        final SpTypeComboBoxModel<WellDepth> wdModel = new SpTypeComboBoxModel<WellDepth>(WellDepth.class);
        _w.well.setModel(wdModel);
        _w.well.setRenderer(new SpTypeComboBoxRenderer());
        _w.well.setMaximumRowCount(WellDepth.values().length);
        _w.well.addActionListener(this);

        _w.pixelScale.addActionListener(this);
        _w.disperser.addActionListener(this);

        final SpTypeComboBoxModel<SlitWidth> swModel = new SpTypeComboBoxModel<SlitWidth>(SlitWidth.class);
        _w.slitWidth.setModel(swModel);
        _w.slitWidth.setRenderer(new SpTypeComboBoxRenderer());
        _w.slitWidth.setMaximumRowCount(SlitWidth.class.getEnumConstants().length);
        _w.slitWidth.addActionListener(this);

        _w.centralWavelength.addWatcher(this);
        // Also need to update on keystrokes in the editor
        final Object o = _w.centralWavelength.getEditor().getEditorComponent();
        if (o instanceof JTextField) {
            ((JTextComponent) o).getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) {
                    _centralWavelengthChanged(e.getDocument());
                }

                public void removeUpdate(DocumentEvent e) {
                    _centralWavelengthChanged(e.getDocument());
                }

                public void changedUpdate(DocumentEvent e) {
                    _centralWavelengthChanged(e.getDocument());
                }
            });
        }

        _w.crossDispersed.setModel(new XdModel());
        _w.crossDispersed.setRenderer(new XdRenderer());
        _w.crossDispersed.setMaximumRowCount(CrossDispersed.class.getEnumConstants().length);
        _w.crossDispersed.addActionListener(this);

        _w.readModeBrightRadioButton.addActionListener(this);
        _w.readModeFaintRadioButton.addActionListener(this);
        _w.readModeVeryBrightRadioButton.addActionListener(this);
        _w.readModeVeryFaintRadioButton.addActionListener(this);

        _w.orderTable.setBackground(_w.getBackground());

        // Create the property change listeners for the parallactic angle panel.
        updateParallacticAnglePCL = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                _w.posAnglePanel.updateParallacticControls();
            }
        };

        initPortTab(_w);
    }

    // CrossDisperser combo box model that doesn't allow an invalid option
    // to be selected.
    private final class XdModel extends SpTypeComboBoxModel<CrossDispersed> {
        private Set<CrossDispersed> xdOptions;

        XdModel() {
            super(CrossDispersed.class);
            xdOptions = new HashSet<CrossDispersed>(SpTypeUtil.getSelectableItems(CrossDispersed.class));
        }

        void setValidOptions(PixelScale ps) {
            xdOptions = ps.getXdOptions();
        }

        @Override
        public void setSelectedItem(Object item) {
            if (xdOptions.contains(item)) super.setSelectedItem(item);
        }
    }

    // CrossDisperser renderer that shows invalid options as disabled.
    private final class XdRenderer extends SpTypeComboBoxRenderer {
        private Set<CrossDispersed> xdOptions = Collections.emptySet();

        void setValidOptions(PixelScale ps) {
            xdOptions = ps.getXdOptions();
        }

        @Override
        public Component getListCellRendererComponent(JList jList, Object value, int index, boolean isSelected, boolean hasFocus) {
            final Component comp = super.getListCellRendererComponent(jList, value, index, isSelected, hasFocus);    //To change body of overridden methods use File | Settings | File Templates.
            final boolean visible = (value != null) && xdOptions.contains(value);
            comp.setEnabled(visible);
            return comp;
        }
    }

    private class PortButtonListener implements ActionListener {
        private final IssPort port;

        PortButtonListener(IssPort port) {
            this.port = port;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (getDataObject() == null) return;
            getDataObject().setIssPort(port);
        }
    }

    private void initPortTab(GnirsForm f) {
        f.portTab.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        _sidePortButton = new JRadioButton("Side-looking") {{
            addActionListener(new PortButtonListener(IssPort.SIDE_LOOKING));
        }};
        _upPortButton = new JRadioButton("Up-looking") {{
            addActionListener(new PortButtonListener(IssPort.UP_LOOKING));
        }};

        new ButtonGroup() {{
            add(_sidePortButton);
            add(_upPortButton);
        }};

        f.portTab.add(_sidePortButton, new GridBagConstraints() {{
            gridx = 0;
            gridy = 0;
            anchor = WEST;
            insets = new Insets(0, 0, 10, 0);
        }});
        f.portTab.add(_upPortButton, new GridBagConstraints() {{
            gridx = 0;
            gridy = 1;
            anchor = WEST;
        }});
        f.portTab.add(new JPanel(), new GridBagConstraints() {{
            gridx = 1;
            gridy = 2;
            fill = BOTH;
            weightx = 1.0;
            weighty = 1.0;
        }});
    }

    /**
     * Return the window containing the editor
     */
    public JPanel getWindow() {
        return _w;
    }

    private final PropertyChangeListener xdListener = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            watchCrossDispersed(false);
            final CrossDispersed xd = getDataObject().getCrossDispersed();
            _w.crossDispersed.setSelectedItem(xd);
            watchCrossDispersed(true);
        }
    };

    @Override
    protected void cleanup() {
        final InstGNIRS inst = getDataObject();
        inst.removePropertyChangeListener(xdListener);
        inst.removePropertyChangeListener(InstGNIRS.SLIT_WIDTH_PROP.getName(), updateParallacticAnglePCL);
        inst.removePropertyChangeListener(InstGNIRS.DISPERSER_PROP.getName(),  updateParallacticAnglePCL);
    }

    /**
     * Set the data object corresponding to this editor.
     */
    @Override protected void init() {
        super.init();

        //TODO: Add a PropertyChangeListener for all the properties relevant in InstGnirs
        watchCrossDispersed(true);
        _updatePixelScale();
        _updateDisperser();
        _updateSlitWidth();
//        _updateFilter();
//        _updateAcqMirror();
        _updateCentralWavelength();
        _updateCrossDispersed();
        _updateWeelDepth();
        _updateReadMode();
        _updateOrderTable();

        _updateScienceFOV();
        _updateMinExpTime();

        _updateEnabledStates();
        _updatePort();

        _w.posAnglePanel.init(this, Site.GN);

        // If the position angle mode or FPU mode properties change, force an update on the parallactic angle mode.
        final InstGNIRS inst = getDataObject();
        inst.addPropertyChangeListener(InstGNIRS.SLIT_WIDTH_PROP.getName(), updateParallacticAnglePCL);
        inst.addPropertyChangeListener(InstGNIRS.DISPERSER_PROP.getName(),  updateParallacticAnglePCL);
    }

    protected void updateEnabledState(final boolean enabled) {
        super.updateEnabledState(enabled);
        _w.posAnglePanel.updateEnabledState(enabled);
    }

    // Return an array of default wavelength description strings (wavelength (order n))
    private String[] _getDefaultWavelengths() {
        final int n = Order.values().length;
        final String[] ar = new String[n - 2]; // skip orders 7 and 8
        int index = 0;
        for (int i = 0; i < n; i++) {
            final Order order = Order.values()[i];
            if (order == Order.SEVEN || order == Order.EIGHT) {
                //do nothing, just continue
            } else if (order == Order.XD) {
                ar[index++] = GNIRSConstants.CROSS_DISPERSED_NAME;
            } else {
                ar[index++] = order.getBand() + "  (order " + order.getOrder() + ") = " +
                        _nf.format(order.getDefaultWavelength());
            }
        }
        return ar;
    }

    // Updates (only) the enabled states of the components based on the current settings.
    private void _updateEnabledStates() {
        if (!OTOptions.isEditable(getProgram(), getContextObservation())) {
            return;
        }

        // -Pixel scale=0.05"/pix: IFU and Res=700 grayed out, limited slit widths?
        // -Pixel scale=0.15"/pix: Res=18000 grayed out, limited slit widths?
        final PixelScale ps = getDataObject().getPixelScale();
        final boolean ps_005 = (ps == PixelScale.PS_005);
        _w.slitWidth.setEnabledObject(SlitWidth.IFU, !ps_005);

        // -XD=yes: Wollaston prism grayed out
        final boolean isXD = getDataObject().checkCrossDispersed();

        ((XdRenderer) _w.crossDispersed.getRenderer()).setValidOptions(ps);
        ((XdModel) _w.crossDispersed.getModel()).setValidOptions(ps);

        _w.tabbedPane.setEnabledAt(1, isXD);
        if (!isXD) {
            _w.tabbedPane.setSelectedComponent(_w.readModeTab);
        }

        //Min exposure time will be visible on site only
        //_w.minExpTime.setVisible(OTOptions.isStaff(getProgram().getProgramID()));
        //_w.minExpTimeLabel.setVisible(OTOptions.isStaff(getProgram().getProgramID()));

        //SCI-0418 read mode tab is now usable in regular OT
        //Read mode tab will be "for display only" in the public version
        /*_w.readModeBrightRadioButton.setEnabled(OT.isStaffAndHasPreferredSite());
        _w.readModeFaintRadioButton.setEnabled(OT.isStaffAndHasPreferredSite());
        _w.readModeVeryBrightRadioButton.setEnabled(OT.isStaffAndHasPreferredSite());
        _w.readModeVeryFaintRadioButton.setEnabled(OT.isStaffAndHasPreferredSite());
        */
    }


    // Apply a set of constraints, as specified in the GNIRS specs (see comments below)
    private void _applyConstraints() {
        // -IFU: sets pixel scale=0.15"/pix, XD=no
        if (getDataObject().getSlitWidth() == SlitWidth.IFU) {
            getDataObject().setPixelScale(PixelScale.PS_015);
            getDataObject().setCrossDispersed(CrossDispersed.NO);
            _updatePixelScale();
            _updateCrossDispersed();
        }

        if (getDataObject().checkCrossDispersed()) {
            _updateOrderTable();
        }
        _updateReadMode();
        _updateScienceFOV();
        _updateEnabledStates();
        _updateWeelDepth();
    }

    // Update the calculated science field of view display
    private void _updateScienceFOV() {
        final double[] scienceArea = getDataObject().getScienceArea();
        _w.scienceFOV.setText(scienceArea[0] + " x " + scienceArea[1] + " arcsec");
    }

    // Update the display of the minimum exposure time
    private void _updateMinExpTime() {
        _w.minExpTime.setText(getDataObject().getMinExpTime() + " secs");
    }

    // Update the table of orders and wavelengths. If updateTextBox is true,
    // also update the textbox to match the selection.
    private void _updateOrderTable() {
        int selectedRow = _w.orderTable.getSelectedRow();
        if (selectedRow == -1) {
            selectedRow = 0;
        }
        final Vector<String> columnNames = new Vector<String>(2);
        columnNames.add("Wavelength (um)");
        columnNames.add("Order");
        columnNames.add("Start Wavelength");
        columnNames.add("End Wavelength");
        final int n = Order.values().length;
        final Vector<Vector<String>> data = new Vector<Vector<String>>(n - 3); // skip orders 1, 2, and XP here
        final Disperser disperser = getDataObject().getDisperser();
        final PixelScale pixelScale = getDataObject().getPixelScale();
        for (int i = 0; i < n; i++) {
            final Order order = Order.getOrderByIndex(i);
            if (order == Order.ONE || order == Order.TWO || order == Order.XD) {
                continue; // skip orders 1, 2, and XP here
            }
            final Vector<String> row = new Vector<String>(2);
            row.add(_nf.format(getDataObject().getCentralWavelength(order)));
            row.add(order.displayValue());
            final double wavelength = getDataObject().getCentralWavelength(order);
            row.add(_nf.format(order.getStartWavelength(wavelength, disperser, pixelScale)));
            row.add(_nf.format(order.getEndWavelength(wavelength, disperser, pixelScale)));
            data.add(row);
        }
        final DefaultTableModel model = new DefaultTableModel(data, columnNames) {
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        _ignoreEvents = true;
        try {
            _w.orderTable.setModel(model);
            // restore the selection
            _w.orderTable.getSelectionModel().setSelectionInterval(selectedRow, selectedRow);
        } finally {
            _ignoreEvents = false;
        }

    }

    // Update the pixel scale display from the data object.
    private void _updatePixelScale() {
        _w.pixelScale.removeActionListener(this);
        try {
            _w.pixelScale.setSelectedIndex(getDataObject().getPixelScale().ordinal());
        } finally {
            _w.pixelScale.addActionListener(this);
        }
    }

    // Update the disperser display from the data object.
    private void _updateDisperser() {
        _w.disperser.setSelectedIndex(getDataObject().getDisperser().ordinal());
    }

    // Update the slit width display from the data object.
    private void _updateSlitWidth() {
        _w.slitWidth.getModel().setSelectedItem(getDataObject().getSlitWidth());
    }

    // Update the filter display from the data object.
//    private void _updateFilter() { _w.filter.getModel().setSelectedItem(getDataObject().getFilter()); }

    // Update the acquisition mirror display from the data object.
//    private void _updateAcqMirror() { _w.acqMirror.getModel().setSelectedItem(getDataObject().getAcquisitionMirror()); }

    // Update the Weel Depth display from the data object.
    private void _updateWeelDepth() {
        _w.well.getModel().setSelectedItem(getDataObject().getWellDepth());
        final StringBuilder sb = new StringBuilder();
        sb.append(getDataObject().getWellDepth().getBias());
        sb.append("mV");
        _w.biasLevel.setText(sb.toString());
    }

    private void _updateCentralWavelength() {
        _ignoreEvents = true;
        _w.centralWavelength.deleteWatcher(this);
        try {
            _w.centralWavelength.setValue(getDataObject().getCentralWavelength());
        } catch (IllegalStateException e) {
            // ignore if user is typing in value
        } finally {
            _w.centralWavelength.addWatcher(this);
            _ignoreEvents = false;
        }

        // This puts the order in parens in the label for central wavelength
        final Order o = Order.getOrder(getDataObject().getCentralWavelength().doubleValue(), Order.DEFAULT);
        _w.centralWavelengthLabel.setText("Central Wavelength (" + o.displayValue() + "):");
    }

    // Update from the data object.
    private void _updateCrossDispersed() {
        _w.crossDispersed.removeActionListener(this);
        _w.crossDispersed.getModel().setSelectedItem(getDataObject().getCrossDispersed());
        _w.crossDispersed.addActionListener(this);
    }

    private void _updateReadMode() {
        final ReadMode readMode = getDataObject().getReadMode();
        if (readMode == ReadMode.BRIGHT) {
            _w.readModeBrightRadioButton.setSelected(true);
        } else if (readMode == ReadMode.FAINT) {
            _w.readModeFaintRadioButton.setSelected(true);
        } else if (readMode == ReadMode.VERY_BRIGHT) {
            _w.readModeVeryBrightRadioButton.setSelected(true);
        } else if (readMode == ReadMode.VERY_FAINT) {
            _w.readModeVeryFaintRadioButton.setSelected(true);
        }

        _w.lowNoiseReads.setText(String.valueOf(readMode.getLowNoiseReads()));
        _w.readNoise.setText(String.format("%.0fe-", readMode.getReadNoise()));
        _w.minExpTime.setText(readMode.getMinExpAsString());
    }

    private void _updatePort() {
        switch (getDataObject().getIssPort()) {
            case SIDE_LOOKING:
                _sidePortButton.setSelected(true);
                break;
            case UP_LOOKING:
                _upPortButton.setSelected(true);
                break;
        }
    }


    // Called when the user types in the central wavelength value in the combo box
    // editor field in the main panel
    private void _centralWavelengthChanged(Document doc) {
        if (_ignoreEvents) {
            return;
        }
        try {
            final String s = doc.getText(0, doc.getLength());
            final double d = Double.parseDouble(s);
            _setCentralWavelength(d);
        } catch (Exception ex) {
            //do nothing here
        }
    }

    // Called when the user changes the central wavelength value in the combo box
    // in the main panel
    private void _centralWavelengthChanged() {
        final int i = _w.centralWavelength.getSelectedIndex();
        final double d;
        if (i != -1) {
            final Order order = Order.getOrderByIndex(i);
            d = order.getDefaultWavelength();
        } else {
            try {
                d = Double.parseDouble(_w.centralWavelength.getValue().toString());
            } catch (NumberFormatException e) {
                Toolkit.getDefaultToolkit().beep();
                return;
            }
        }
        _setCentralWavelength(d);
    }

    // Called when the user changes the central wavelength value in the combo box
    // in the main panel
    private void _setCentralWavelength(double wavelength) {
        getDataObject().setCentralWavelength(wavelength);
        _updateCentralWavelength();
        _updateOrderTable();
    }

    /**
     * Return the coadds text box.
     */
    public TextBoxWidget getCoaddsTextBox() {
        return _w.coadds;
    }


    /**
     * Return the exposure time text box.
     */
    public TextBoxWidget getExposureTimeTextBox() {
        return _w.exposureTime;
    }

    private void watchCrossDispersed(boolean watch) {
        final String prop = InstGNIRS.CROSS_DISPERSED_PROP.getName();
        if (watch) {
            getDataObject().addPropertyChangeListener(prop, xdListener);
        } else {
            getDataObject().removePropertyChangeListener(prop, xdListener);
        }
    }

    /**
     * Handle action events (for checkbuttons).
     */
    public void actionPerformed(ActionEvent evt) {
        if (_ignoreEvents) {
            return;
        }
        final Object w = evt.getSource();

        if (w == _w.crossDispersed) {
            final CrossDispersed xd = (CrossDispersed) _w.crossDispersed.getModel().getSelectedItem();
            if (xd == null) return;

            watchCrossDispersed(false);
            getDataObject().setCrossDispersed(xd);
            watchCrossDispersed(true);
            if (getDataObject().checkCrossDispersed()) {
                // set wavelength to default for cross-dispersed
                getDataObject().setCentralWavelength(Order.XD.getDefaultWavelength(), Order.XD);
                _updateCentralWavelength();
            }

        } else if (w == _w.readModeBrightRadioButton) {
            getDataObject().setReadMode(ReadMode.BRIGHT);
        } else if (w == _w.readModeFaintRadioButton) {
            getDataObject().setReadMode(ReadMode.FAINT);
        } else if (w == _w.readModeVeryBrightRadioButton) {
            getDataObject().setReadMode(ReadMode.VERY_BRIGHT);
        } else if (w == _w.readModeVeryFaintRadioButton) {
            getDataObject().setReadMode(ReadMode.VERY_FAINT);

        } else if (w == _w.pixelScale) {
            getDataObject().setPixelScale(PixelScale.getPixelScaleByIndex(_w.pixelScale.getSelectedIndex()));
        } else if (w == _w.disperser) {
            getDataObject().setDisperser(Disperser.getDisperserByIndex(_w.disperser.getSelectedIndex()));
//        } else if (w == _w.acqMirror) {
//            getDataObject().setAcquisitionMirror((AcquisitionMirror) _w.acqMirror.getModel().getSelectedItem());
//        } else if (w == _w.filter) {
//            getDataObject().setFilter((Filter) _w.filter.getModel().getSelectedItem());
        } else if (w == _w.slitWidth) {
            getDataObject().setSlitWidth((SlitWidth) _w.slitWidth.getModel().getSelectedItem());
        } else if (w == _w.well) {
            getDataObject().setWellDepth((WellDepth) _w.well.getSelectedItem());
        }

        _applyConstraints();
    }

    /**
     * Called when an item in a DropDownListBoxWidget is selected.
     */
    public void dropDownListBoxAction(DropDownListBoxWidget ddlbwe, int index, String val) {
        if (_ignoreEvents) {
            return;
        }
        if (ddlbwe == _w.centralWavelength) {
            _centralWavelengthChanged();
        }
        _applyConstraints();
    }

    public void textBoxKeyPress(TextBoxWidget tbwe) {
        if (tbwe == getExposureTimeTextBox()) {
            final double exposure = tbwe.getDoubleValue(-1);
            _updateReadModeBasedOnExpTime(exposure);
        }
        super.textBoxKeyPress(tbwe);
    }

    private void _updateReadModeBasedOnExpTime(double exposure) {
        if (exposure >= 0) {
            getDataObject().setReadModeNoProp(InstGNIRS.selectReadMode(exposure));
            _updateReadMode();
        }
    }
}

