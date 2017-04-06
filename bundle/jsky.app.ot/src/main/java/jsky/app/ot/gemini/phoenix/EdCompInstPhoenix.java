package jsky.app.ot.gemini.phoenix;

import edu.gemini.spModel.gemini.phoenix.InstPhoenix;
import edu.gemini.spModel.gemini.phoenix.PhoenixParams;
import edu.gemini.spModel.gemini.phoenix.PhoenixParams.Mask;
import edu.gemini.spModel.type.SpTypeUtil;
import jsky.app.ot.editor.type.SpTypeComboBoxModel;
import jsky.app.ot.editor.type.SpTypeComboBoxRenderer;
import jsky.app.ot.gemini.editor.EdCompInstBase;
import jsky.util.gui.DropDownListBoxWidget;
import jsky.util.gui.DropDownListBoxWidgetWatcher;
import jsky.util.gui.TextBoxWidget;

import javax.swing.JPanel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * This is the editor for the Phoenix instrument component.
 */
public final class EdCompInstPhoenix extends EdCompInstBase<InstPhoenix>
        implements DropDownListBoxWidgetWatcher,
        ActionListener {

    /** The GUI layout panel */
    private final PhoenixForm _w;

    /**
     * The constructor initializes the user interface.
     */
    public EdCompInstPhoenix() {

        _w = new PhoenixForm();

        _w.mask.setChoices(_getMasks());

        final SpTypeComboBoxModel<PhoenixParams.Filter> model;
        model = new SpTypeComboBoxModel<>(PhoenixParams.Filter.class);
        _w.selectedFilter.setModel(model);
        _w.selectedFilter.setRenderer(new SpTypeComboBoxRenderer());
        _w.selectedFilter.setMaximumRowCount(PhoenixParams.Filter.values().length);
        _w.selectedFilter.addActionListener(evt -> {
            final PhoenixParams.Filter filter;
            filter = (PhoenixParams.Filter) _w.selectedFilter.getSelectedItem();
            getDataObject().setFilter(filter);
        });

        _w.mask.addWatcher(this);
//        _w.selectedFilter.addWatcher(this);

        // see method textBoxKeyPress
        _w.gratingWavelength.addWatcher(this);
        _w.gratingWavenumber.addWatcher(this);

        _w.wavelengthRadioButton.addActionListener(this);
        _w.wavenumberRadioButton.addActionListener(this);

        // initial state
        _w.gratingWavelength.setEnabled(true);
        _w.wavelengthUnits.setEnabled(true);
        _w.gratingWavenumber.setEnabled(false);
        _w.wavenumberUnits.setEnabled(false);

    }

    /** Return an array of mask names */
    private String[] _getMasks() {
        return SpTypeUtil.getFormattedDisplayValueAndDescriptions(Mask.class);
    }

    /** Return the window containing the editor */
    public JPanel getWindow() {
        return _w;
    }

    /** Set the data object corresponding to this editor. */
    @Override protected void init() {
        super.init();
        _w.selectedFilter.getModel().setSelectedItem(getDataObject().getFilter());
//        _w.selectedFilter.setValue(getDataObject().getFilter());
        _w.mask.setValue(getDataObject().getMask().ordinal());
        _w.gratingWavelength.setValue(getDataObject().getGratingWavelength());
        _w.gratingWavenumber.setValue(getDataObject().getGratingWavenumber());
        final boolean wavelengthIsEnabled = _w.gratingWavelength.isEnabled();
        final boolean wavenumberIsEnabled = _w.gratingWavenumber.isEnabled();
        // XXX a workaround for now.  The gratingWavenumber and wavenumberUnits
        // fields are disabled in the constructor, but both show up enabled
        // when you create a new Science Program with a Phoenix instrument.
        if (wavelengthIsEnabled && wavenumberIsEnabled) {
            _w.gratingWavenumber.setEnabled(false);
            _w.wavenumberUnits.setEnabled(false);
        }
        _updateScienceFOV();
    }


    /** Return the position angle text box */
    public TextBoxWidget getPosAngleTextBox() {
        return _w.posAngle;
    }

    /** Return the exposure time text box */
    public TextBoxWidget getExposureTimeTextBox() {
        return _w.exposureTime;
    }

    /** Return the coadds text box. */
    public TextBoxWidget getCoaddsTextBox() {
        return _w.coadds;
    }

    /**
     * Handle action events
     */
    public void actionPerformed(ActionEvent evt) {
        final Object w = evt.getSource();

        if (w == _w.wavelengthRadioButton) {
            _w.gratingWavelength.setEnabled(true);
            _w.wavelengthUnits.setEnabled(true);
            _w.gratingWavenumber.setEnabled(false);
            _w.wavenumberUnits.setEnabled(false);
        } else if (w == _w.wavenumberRadioButton) {
            _w.gratingWavenumber.setEnabled(true);
            _w.wavenumberUnits.setEnabled(true);
            _w.gratingWavelength.setEnabled(false);
            _w.wavelengthUnits.setEnabled(false);
        }
    }

    /**
     * A key was pressed in the given TextBoxWidget.
     */
    public void textBoxKeyPress(TextBoxWidget tbwe) {
        super.textBoxKeyPress(tbwe);

        if (tbwe != _w.gratingWavelength && tbwe != _w.gratingWavenumber) {
            return;
        }

        double value;
        try {
            value = Double.parseDouble(tbwe.getValue());
        } catch (NumberFormatException e) {
            return;
        }

        if (tbwe == _w.gratingWavelength) {
            value = _checkWavelength(value);
            getDataObject().setGratingWavelength(value);
            _updateWavenumber(value);
        } else if (tbwe == _w.gratingWavenumber) {
            value = _checkWavenumber(value);
            getDataObject().setGratingWavenumber(value);
            _updateWavelength(value);
        }

    }

    /**
     * A return key was pressed in the given TextBoxWidget.
     */
    public void textBoxAction(TextBoxWidget tbwe) {
    }

    /**
     * Called when an item in a DropDownListBoxWidget is selected.
     */
    public void dropDownListBoxAction(DropDownListBoxWidget ddlbw, int index, String val) {
        if (ddlbw == _w.mask) {
            getDataObject().setMask(Mask.getMaskByIndex(index));
            _updateScienceFOV();
        }
    }

    //
    // Update the science field of view based upon the camera and mask
    // settings.
    //
    private void _updateScienceFOV() {
        final double[] scienceArea = getDataObject().getScienceArea();
        _w.scienceFOV.setText(scienceArea[0] + " x " + scienceArea[1] + " arcsec");
    }

    // XXX
    private double _checkWavelength(double lambda) {
        return lambda;
    }

    // XXX
    private double _checkWavenumber(double number) {
        return number;
    }

    private void _updateWavenumber(double wavelength) {
        if (wavelength <= 0.0) {
            return;
        }
        final double wavenumber = InstPhoenix.CONVERSION_CONSTANT / wavelength;
        final double rWavenumber = _roundDouble(wavenumber, 2);
        getDataObject().setGratingWavenumber(rWavenumber);
        _w.gratingWavenumber.setText(Double.toString(rWavenumber));
    }

    private void _updateWavelength(double wavenumber) {
        if (wavenumber <= 0.0) {
            return;
        }
        final double wavelength = InstPhoenix.CONVERSION_CONSTANT / wavenumber;
        final double rWavelength = _roundDouble(wavelength, 4);
        getDataObject().setGratingWavelength(rWavelength);
        _w.gratingWavelength.setText(Double.toString(rWavelength));
    }

    private double _roundDouble(double value, int n) {
        if (n <= 0) {
            return value;
        }

        // shift decimal point to right before rounding
        final double d = Math.pow(10.0, (double) n);
        final int iValue = (int) (Math.round(value * d));
        return (double) iValue / d;
    }


}
