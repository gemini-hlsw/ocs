package jsky.app.ot.gemini.acqcam;

import edu.gemini.spModel.gemini.acqcam.AcqCamParams.*;
import edu.gemini.spModel.gemini.acqcam.InstAcqCam;
import edu.gemini.spModel.type.SpTypeUtil;
import jsky.app.ot.gemini.editor.EdCompInstBase;
import jsky.util.gui.DropDownListBoxWidget;
import jsky.util.gui.TextBoxWidget;

import javax.swing.JPanel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


/**
 * This is the editor for the AcqCam instrument component.
 */
public final class EdCompInstAcqCam extends EdCompInstBase<InstAcqCam>
        implements jsky.util.gui.DropDownListBoxWidgetWatcher, ActionListener {

    /** The GUI layout panel */
    private final AcqCamForm _w;


    /**
     * The constructor initializes the user interface.
     */
    public EdCompInstAcqCam() {
        _w = new AcqCamForm();

        _w.binningOnButton.addActionListener(this);
        _w.binningOffButton.addActionListener(this);
        _w.windowingOnButton.addActionListener(this);
        _w.windowingOffButton.addActionListener(this);

        _w.cassRotatorFollowingButton.addActionListener(this);
        _w.cassRotatorFixedButton.addActionListener(this);

        _w.colorFilter.setChoices(_getColorFilters());
        _w.ndFilter.setChoices(_getNDFilters());
        _w.lens.setChoices(_getLenses());

        _w.colorFilter.addWatcher(this);
        _w.ndFilter.addWatcher(this);
        _w.lens.addWatcher(this);

        _w.x.addWatcher(this);
        _w.y.addWatcher(this);
        _w.width.addWatcher(this);
        _w.height.addWatcher(this);
    }


    /** Return an array of color filters */
    private String[] _getColorFilters() {
        return SpTypeUtil.getFormattedDisplayValueAndDescriptions(ColorFilter.class);
    }

    /** Return an array of neutral density filters */
    private String[] _getNDFilters() {
        return SpTypeUtil.getFormattedDisplayValueAndDescriptions(NDFilter.class);
    }

    /** Return an array of lens names */
    private String[] _getLenses() {
        return SpTypeUtil.getFormattedDisplayValueAndDescriptions(Lens.class);
    }

    /** Return the window containing the editor */
    public JPanel getWindow() {
        return _w;
    }

    /** Set the data object corresponding to this editor. */
    @Override protected void init() {
        super.init();
        _w.colorFilter.setValue(getDataObject().getColorFilter().ordinal());
        _w.ndFilter.setValue(getDataObject().getNdFilter().ordinal());
        _w.lens.setValue(getDataObject().getLens().ordinal());

        final CassRotator cr = getDataObject().getCassRotator();
        if (cr == CassRotator.FOLLOWING) {
            _w.cassRotatorFollowingButton.setSelected(true);
        } else {
            _w.cassRotatorFixedButton.setSelected(true);
        }

        _updateBinning();
        _updateWindowing();
    }


    // Update the binning widgets
    private void _updateBinning() {
        final boolean b = (getDataObject().getBinning() == Binning.ON);
        _w.binningOnButton.setSelected(b);
        _w.binningOffButton.setSelected(!b);
    }

    // Update the windowing widgets
    private void _updateWindowing() {
        final boolean b = (getDataObject().getWindowing() == Windowing.ON);
        _w.windowingOnButton.setSelected(b);
        _w.windowingOffButton.setSelected(!b);

        _w.x.setText(getDataObject().getXStartAsString());
        _w.y.setText(getDataObject().getYStartAsString());
        _w.width.setText(getDataObject().getXSizeAsString());
        _w.height.setText(getDataObject().getYSizeAsString());

        _updateWindowingState();
    }

    // Update the windowing widget states
    private void _updateWindowingState() {
        final boolean b = (getDataObject().getWindowing() == Windowing.ON);
        _w.x.setEnabled(b);
        _w.xLabel.setEnabled(b);
        _w.xComment.setEnabled(b);
        _w.y.setEnabled(b);
        _w.yLabel.setEnabled(b);
        _w.width.setEnabled(b);
        _w.widthLabel.setEnabled(b);
        _w.widthComment.setEnabled(b);
        _w.height.setEnabled(b);
        _w.heightLabel.setEnabled(b);
    }

    /** Return the position angle text box */
    public TextBoxWidget getPosAngleTextBox() {
        return _w.posAngle;
    }

    /** Return the exposure time text box */
    public TextBoxWidget getExposureTimeTextBox() {
        return _w.exposureTime;
    }

    /** Return null, since there is no coadds text box here. */
    public TextBoxWidget getCoaddsTextBox() {
        return null;
    }

    /**
     * Handle action events.
     */
    public void actionPerformed(ActionEvent evt) {
        final Object w = evt.getSource();

        if (w == _w.binningOnButton) {
            getDataObject().setBinning(Binning.ON);
        } else if (w == _w.binningOffButton) {
            getDataObject().setBinning(Binning.OFF);
        } else if (w == _w.windowingOnButton) {
            getDataObject().setWindowing(Windowing.ON);
            _updateWindowingState();
        } else if (w == _w.windowingOffButton) {
            getDataObject().setWindowing(Windowing.OFF);
            _updateWindowingState();
        } else if (w == _w.cassRotatorFollowingButton) {
            getDataObject().setCassRotator(CassRotator.FOLLOWING);
        } else if (w == _w.cassRotatorFixedButton) {
            getDataObject().setCassRotator(CassRotator.FIXED);
        }
    }


    /**
     * Called when an item in a DropDownListBoxWidget is selected.
     */
    public void dropDownListBoxAction(DropDownListBoxWidget ddlbw, int index, String val) {
        if (ddlbw == _w.colorFilter) {
            getDataObject().setColorFilter(ColorFilter.getColorFilterByIndex(index));
        } else if (ddlbw == _w.ndFilter) {
            getDataObject().setNdFilter(NDFilter.getNDFilterByIndex(index));
        } else if (ddlbw == _w.lens) {
            getDataObject().setLens(Lens.getLensByIndex(index));
        }
    }


    /** Called when a key is pressed in one of the text boxes */
    public void textBoxKeyPress(TextBoxWidget tbw) {
        super.textBoxKeyPress(tbw);

        if (tbw == _w.x)
            getDataObject().setXStart(tbw.getIntegerValue(InstAcqCam.DEF_X));
        else if (tbw == _w.y)
            getDataObject().setYStart(tbw.getIntegerValue(InstAcqCam.DEF_Y));
        else if (tbw == _w.width)
            getDataObject().setXSize(tbw.getIntegerValue(InstAcqCam.DEF_WIDTH));
        else if (tbw == _w.height)
            getDataObject().setYSize(tbw.getIntegerValue(InstAcqCam.DEF_HEIGHT));
    }

    /** Called when enter is pressed in one of the text boxes */
    public void textBoxAction(TextBoxWidget tbwe) {
    } // ignore

}
