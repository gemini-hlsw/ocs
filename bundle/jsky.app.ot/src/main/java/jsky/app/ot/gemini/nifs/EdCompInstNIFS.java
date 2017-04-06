package jsky.app.ot.gemini.nifs;

import edu.gemini.pot.sp.ISPNode;
import edu.gemini.spModel.gemini.nifs.InstNIFS;
import edu.gemini.spModel.gemini.nifs.NIFSParams.*;
import edu.gemini.spModel.type.SpTypeUtil;
import jsky.app.ot.OT;
import jsky.app.ot.OTOptions;
import jsky.app.ot.gemini.editor.EdCompInstBase;
import jsky.util.gui.DropDownListBoxWidget;
import jsky.util.gui.DropDownListBoxWidgetWatcher;
import jsky.util.gui.TextBoxWidget;

import javax.swing.JPanel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


/**
 * This is the editor for the NIFS instrument component.
 */
public final class EdCompInstNIFS extends EdCompInstBase<InstNIFS>
        implements DropDownListBoxWidgetWatcher, ActionListener {

    /** The GUI layout panel */
    private final NifsForm _w;

    /**
     * The constructor initializes the user interface.
     */
    public EdCompInstNIFS() {
        _w = new NifsForm();

        _w.readModeFaintButton.addActionListener(this);
        _w.readModeMediumButton.addActionListener(this);
        _w.readModeBrightButton.addActionListener(this);

        _w.imagingMirror.setChoices(_getMirrors());

        final String[] filters = _getFilters();
        _w.filter.setChoices(filters);
        _w.filter.setMaximumRowCount(Math.min(filters.length, 20));

        final String[] dispersers = _getDispersers();
        _w.disperser.setChoices(dispersers);
        _w.disperser.setMaximumRowCount(dispersers.length);

        final String[] masks = _getMasks();
        _w.mask.setChoices(masks);
        _w.mask.setMaximumRowCount(masks.length);

        _w.imagingMirror.addWatcher(this);
        _w.mask.addWatcher(this);
        _w.disperser.addWatcher(this);
        _w.filter.addWatcher(this);
        _w.centralWavelength.addWatcher(this);
        _w.maskOffset.addWatcher(this);

        // Arrange to be notified when the OT editable state changes.
        // This is needed to make sure the enabled states are set correctly for the
        // read-mode labels (the ones one right side)
        OT.addEditableStateListener(new OT.EditableStateListener() {
            @Override public ISPNode getEditedNode() { return getNode(); }
            @Override public void updateEditableState() { _updateReadMode(); }
        });
    }

    /** Return an array of imaging mirror names */
    private String[] _getMirrors() {
        return SpTypeUtil.getFormattedDisplayValueAndDescriptions(ImagingMirror.class);
    }


    /** Return an array of disperser names */
    private String[] _getDispersers() {
        return SpTypeUtil.getFormattedDisplayValueAndDescriptions(Disperser.class);
    }


    /** Return an array of mask names */
    private String[] _getMasks() {
        return SpTypeUtil.getFormattedDisplayValueAndDescriptions(Mask.class);
    }

    /**
     * Return an array of filter names
     */
    private String[] _getFilters() {
        return SpTypeUtil.getFormattedDisplayValueAndDescriptions(Filter.class);
    }


    /** Return the window containing the editor */
    public JPanel getWindow() {
        return _w;
    }

    /** Set the data object corresponding to this editor. */
    @Override protected void init() {
        super.init();
        _w.imagingMirror.setValue(getDataObject().getImagingMirror().ordinal());
        _w.disperser.setValue(getDataObject().getDisperser().ordinal());
        _updateFilterWidgets();
        _updateReadMode();
        _updateCentralWavelength();
        _updateMask();
        _w.maskOffset.setValue(getDataObject().getMaskOffsetAsString());
        _updateScienceFOV();
    }

     // Update the filter choice related widgets.
    private void _updateFilterWidgets() {
        _w.filter.setValue(getDataObject().getFilter().ordinal());
    }

    // Update the science field of view based upon the camera and mask
    // settings.
    private void _updateScienceFOV() {
        final double[] scienceArea = getDataObject().getScienceArea();
        _w.scienceFOV.setText(scienceArea[0] + " x " + scienceArea[1] + " arcsec");
    }

    // update the central wavelength display
    private void _updateCentralWavelength() {
        _w.centralWavelength.setValue(getDataObject().getCentralWavelengthAsString());

        // disable if disperser is mirror
        final boolean enabled = OTOptions.areRootAndCurrentObsIfAnyEditable(getProgram(), getContextObservation());
        if (getDataObject().getDisperser() == Disperser.MIRROR ||
            getDataObject().getDisperser() == Disperser.K_LONG ||
            getDataObject().getDisperser() == Disperser.K_SHORT) {
            _w.centralWavelength.setEnabled(false);
        } else {
            _w.centralWavelength.setEnabled(enabled);
        }
    }

    // update the mask and offset displays
    private void _updateMask() {
        _w.mask.setValue(getDataObject().getMask().ordinal());
    }

    // Update the read mode display
    private void _updateReadMode() {
        final boolean enabled = OTOptions.areRootAndCurrentObsIfAnyEditable(getProgram(), getContextObservation());
        final ReadMode readMode = getDataObject().getReadMode();
        if (readMode == ReadMode.FAINT_OBJECT_SPEC) {
            _w.readModeFaintButton.setSelected(true);
            _w.readModeBrightLabel.setEnabled(false);
            _w.readModeMediumLabel.setEnabled(false);
            _w.readModeFaintLabel.setEnabled(enabled);
        } else if (readMode == ReadMode.MEDIUM_OBJECT_SPEC) {
            _w.readModeMediumButton.setSelected(true);
            _w.readModeBrightLabel.setEnabled(false);
            _w.readModeMediumLabel.setEnabled(enabled);
            _w.readModeFaintLabel.setEnabled(false);
        } else if (readMode == ReadMode.BRIGHT_OBJECT_SPEC) {
            _w.readModeBrightButton.setSelected(true);
            _w.readModeBrightLabel.setEnabled(enabled);
            _w.readModeMediumLabel.setEnabled(false);
            _w.readModeFaintLabel.setEnabled(false);
        }
        _w.readModeMinExpTime.setText(readMode.getMinExpAsString());
        _w.readModeNoise.setText(String.format("%.1f e- @ 77K", readMode.getReadNoise()));
        _w.readModeRecMinExpTime.setText(readMode.getRecommendedMinExp());
    }

    /**
     * Must watch table widget actions as part of the TableWidgetWatcher
     * interface, but don't care about them.
     */
    //public void	tableAction(TableWidget twe, int colIndex, int rowIndex) {}

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
     * Handle action events (for checkbuttons).
     */
    public void actionPerformed(ActionEvent evt) {
        final Object w = evt.getSource();

        if (w == _w.readModeFaintButton) {
            getDataObject().setReadMode(ReadMode.FAINT_OBJECT_SPEC);
            _updateReadMode();
        } else if (w == _w.readModeMediumButton) {
            getDataObject().setReadMode(ReadMode.MEDIUM_OBJECT_SPEC);
            _updateReadMode();
        } else if (w == _w.readModeBrightButton) {
            getDataObject().setReadMode(ReadMode.BRIGHT_OBJECT_SPEC);
            _updateReadMode();
        }
    }


    /**
     * Called when an item in a DropDownListBoxWidget is selected.
     */
    public void dropDownListBoxAction(DropDownListBoxWidget ddlbw, int index, String val) {
        if (ddlbw == _w.imagingMirror) {
            final ImagingMirror imagingMirror = ImagingMirror.getImagingMirrorByIndex(index);
            getDataObject().setImagingMirror(imagingMirror);
            if (imagingMirror == ImagingMirror.IN) {
                // set default read mode for imaging mirror
                getDataObject().setReadMode(ReadMode.BRIGHT_OBJECT_SPEC);
                _updateReadMode();
            }
        } else if (ddlbw == _w.mask) {
            getDataObject().setMask(Mask.getMaskByIndex(index));
        } else if (ddlbw == _w.disperser) {
            final Disperser disperser = Disperser.getDisperserByIndex(index);
            getDataObject().setDisperser(disperser);
            if (disperser == Disperser.MIRROR) {
                // OT-314: If the mirror is configured  then the Filter should automatically
                // default to the K-band filter.
                _w.filter.setSelectedIndex(Disperser.MIRROR.defaultFilter().ordinal());
                // OT-318: set default read mode to bright object
                getDataObject().setReadMode(ReadMode.BRIGHT_OBJECT_SPEC);
                _updateReadMode();
            }
            // set default wavelength for disperser
            getDataObject().setCentralWavelength(disperser.getWavelength());
            _updateCentralWavelength();
        } else if (ddlbw == _w.filter) {
            final Disperser disperser = Disperser.getDisperserByIndex(_w.disperser.getSelectedIndex());
            Filter filter = Filter.getFilterByIndex(index);
            if (filter == Filter.SAME_AS_DISPERSER && disperser == Disperser.MIRROR) {
                // OT-314: If the mirror is configured  then the Filter should automatically
                // default to the K-band filter.
                filter = Disperser.MIRROR.defaultFilter();
            }
            getDataObject().setFilter(filter);
        }

        _updateScienceFOV();
    }


    /**
     * A key was pressed in the given TextBoxWidget.
     */
    public void textBoxKeyPress(TextBoxWidget tbwe) {
        super.textBoxKeyPress(tbwe);

        // Note: assume all numeric fields for now
        final double value;
        try {
            value = Double.parseDouble(tbwe.getValue());
        } catch (NumberFormatException e) {
            return;
        }

        if (tbwe == _w.centralWavelength) {
            getDataObject().setCentralWavelength(value);
        } else if (tbwe == _w.maskOffset) {
            getDataObject().setMaskOffset(value);
        }
    }
}
