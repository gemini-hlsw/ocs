package jsky.app.ot.gemini.texes;

import edu.gemini.spModel.gemini.texes.InstTexes;
import edu.gemini.spModel.gemini.texes.TexesParams;
import jsky.app.ot.editor.type.SpTypeComboBoxModel;
import jsky.app.ot.editor.type.SpTypeComboBoxRenderer;
import jsky.app.ot.gemini.editor.EdCompInstBase;
import jsky.util.gui.DropDownListBoxWidget;
import jsky.util.gui.DropDownListBoxWidgetWatcher;
import jsky.util.gui.TextBoxWidget;

import javax.swing.ComboBoxModel;
import javax.swing.JPanel;

/**
 * This is the editor for the Phoenix instrument component.
 */
public final class EdCompInstTexes extends EdCompInstBase<InstTexes>
        implements DropDownListBoxWidgetWatcher {

    /** The GUI layout panel */
    private final TexesForm _w;

    /**
     * The constructor initializes the user interface.
     */
    public EdCompInstTexes() {

        _w = new TexesForm();

        final ComboBoxModel<TexesParams.Disperser> disperserModel = new SpTypeComboBoxModel<>(TexesParams.Disperser.class);
        _w.getSelectedDisperser().setModel(disperserModel);
        _w.getSelectedDisperser().setRenderer(new SpTypeComboBoxRenderer());
        _w.getSelectedDisperser().addWatcher(this);

        // see method textBoxKeyPress
        _w.getWavelenght().addWatcher(this);

    }

    /** Return the window containing the editor */
    public JPanel getWindow() {
        return _w;
    }

    /** Set the data object corresponding to this editor. */
    @Override protected void init() {
        super.init();
        _w.getSelectedDisperser().setValue(getDataObject().getDisperser().ordinal());
        _w.getWavelenght().setValue(_roundDouble(getDataObject().getWavelength(), 4));
        _updateScienceFOV();
    }


    /** Return the position angle text box */
    public TextBoxWidget getPosAngleTextBox() {
        return _w.getPosAngle();
    }

    /** Return the exposure time text box */
    public TextBoxWidget getExposureTimeTextBox() {
        return _w.getExposureTime();
    }

    /** Return the coadds text box. */
    public TextBoxWidget getCoaddsTextBox() {
        return _w.getCoadds();
    }

    /**
     * A key was pressed in the given TextBoxWidget.
     */
    public void textBoxKeyPress(TextBoxWidget tbwe) {
        super.textBoxKeyPress(tbwe);

        if (tbwe != _w.getWavelenght()) {
            return;
        }

        final double value;
        try {
            value = Double.parseDouble(tbwe.getValue());
        } catch (NumberFormatException e) {
            return;
        }
        getDataObject().setWavelength(value);
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
        if (ddlbw == _w.getSelectedDisperser()) {
            getDataObject().setDisperser((TexesParams.Disperser)ddlbw.getSelectedItem());
            _updateScienceFOV();
        }
    }

    //
    // Update the science field of view based upon the camera and mask
    // settings.
    //
    private void _updateScienceFOV() {
        final double[] scienceArea = getDataObject().getScienceArea();
        _w.getScienceFOV().setText(scienceArea[0] + " x " + scienceArea[1] + " arcsec");
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
