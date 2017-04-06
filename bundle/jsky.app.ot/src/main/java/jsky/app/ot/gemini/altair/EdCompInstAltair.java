package jsky.app.ot.gemini.altair;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.spModel.gemini.altair.AltairParams.*;
import edu.gemini.spModel.gemini.altair.InstAltair;
import jsky.app.ot.editor.OtItemEditor;

import javax.swing.JPanel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


/**
 * This is the editor for the Altair instrument component.
 */
public final class EdCompInstAltair extends OtItemEditor<ISPObsComponent, InstAltair>
        implements ActionListener {

    /**
     * The GUI layout panel
     */
    private final AltairForm _w;

    /**
     * The constructor initializes the user interface.
     */
    public EdCompInstAltair() {
        _w = new AltairForm();
        _w.wavelength850_2500_Button.addActionListener(this);
        _w.wavelength850_5000_Button.addActionListener(this);
        _w.wavelength589_Button.addActionListener(this);
        _w.adcCheck.addActionListener(this);
        _w.cassRotatorFollowingButton.addActionListener(this);
        _w.cassRotatorFixedButton.addActionListener(this);
        _w.ndFilterInButton.addActionListener(this);
        _w.ndFilterOutButton.addActionListener(this);
        _w.ngsRadioButton.addActionListener(this);
        _w.ngsWithFieldLensRadioButton.addActionListener(this);
        _w.lgsRadioButton.addActionListener(this);
        _w.lgsP1RadioButton.addActionListener(this);
        _w.lgsOiRadioButton.addActionListener(this);
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

        final InstAltair inst = getDataObject();

        _w.adcCheck.setSelected(inst.getAdc() == ADC.ON);

        final Wavelength wl = inst.getWavelength();
        if (wl == Wavelength.BS_850_2500) {
            _w.wavelength850_2500_Button.setSelected(true);
        } else if (wl == Wavelength.BS_850_5000) {
            _w.wavelength850_5000_Button.setSelected(true);
        } else {
            _w.wavelength589_Button.setSelected(true);
        }

        final CassRotator cr = inst.getCassRotator();
        if (cr == CassRotator.FOLLOWING) {
            _w.cassRotatorFollowingButton.setSelected(true);
        } else {
            _w.cassRotatorFixedButton.setSelected(true);
        }

        final NdFilter nd = inst.getNdFilter();
        if (nd == NdFilter.IN) {
            _w.ndFilterInButton.setSelected(true);
        } else {
            _w.ndFilterOutButton.setSelected(true);
        }

        switch (inst.getMode()) {
            case NGS:
                _w.ngsRadioButton.setSelected(true);
                break;
            case NGS_FL:
                _w.ngsWithFieldLensRadioButton.setSelected(true);
                break;
            case LGS:
                _w.lgsRadioButton.setSelected(true);
                break;
            case LGS_P1:
                _w.lgsP1RadioButton.setSelected(true);
                break;
            case LGS_OI:
                _w.lgsOiRadioButton.setSelected(true);
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    /**
     * Handle action events (for checkbuttons).
     */
    public void actionPerformed(ActionEvent evt) {
        final Object w = evt.getSource();
        if (w == _w.adcCheck) {
            getDataObject().setAdc(_w.adcCheck.isSelected() ? ADC.ON : ADC.OFF);
        } else if (w == _w.wavelength850_5000_Button) {
            getDataObject().setWavelength(Wavelength.BS_850_5000);
        } else if (w == _w.wavelength850_2500_Button) {
            getDataObject().setWavelength(Wavelength.BS_850_2500);
        } else if (w == _w.wavelength589_Button) {
            getDataObject().setWavelength(Wavelength.BS_589);
        } else if (w == _w.cassRotatorFollowingButton) {
            getDataObject().setCassRotator(CassRotator.FOLLOWING);
        } else if (w == _w.cassRotatorFixedButton) {
            getDataObject().setCassRotator(CassRotator.FIXED);
        } else if (w == _w.ndFilterInButton) {
            getDataObject().setNdFilter(NdFilter.IN);
        } else if (w == _w.ndFilterOutButton) {
            getDataObject().setNdFilter(NdFilter.OUT);
        } else if (w == _w.lgsRadioButton) {
            getDataObject().setMode(Mode.LGS);
        } else if (w == _w.lgsP1RadioButton) {
            getDataObject().setMode(Mode.LGS_P1);
        } else if (w == _w.lgsOiRadioButton) {
            getDataObject().setMode(Mode.LGS_OI);
        } else if (w == _w.ngsRadioButton) {
            getDataObject().setMode(Mode.NGS);
        } else if (w == _w.ngsWithFieldLensRadioButton) {
            getDataObject().setMode(Mode.NGS_FL);
        }
    }
}
