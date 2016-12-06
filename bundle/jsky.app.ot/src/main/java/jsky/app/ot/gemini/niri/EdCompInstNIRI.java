package jsky.app.ot.gemini.niri;

import edu.gemini.pot.sp.ISPNode;
import edu.gemini.spModel.gemini.niri.InstNIRI;
import edu.gemini.spModel.gemini.niri.Niri;
import edu.gemini.spModel.gemini.niri.Niri.*;
import edu.gemini.spModel.type.SpTypeUtil;
import jsky.app.ot.OT;
import jsky.app.ot.OTOptions;
import jsky.app.ot.gemini.editor.EdCompInstBase;
import jsky.util.gui.DropDownListBoxWidget;
import jsky.util.gui.DropDownListBoxWidgetWatcher;
import jsky.util.gui.TextBoxWidget;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;


/**
 * This is the editor for the NIRI instrument component.
 */
public final class EdCompInstNIRI extends EdCompInstBase<InstNIRI>
        implements DropDownListBoxWidgetWatcher, ActionListener {

    /**
     * The GUI layout panel
     */
    private final NiriForm _w = new NiriForm();

    private final static Component SEPARATOR = new JSeparator(JSeparator.HORIZONTAL);


    /**
     * The constructor initializes the user interface.
     */
    public EdCompInstNIRI() {

        _w.readModeNarrowBandButton.addActionListener(this);
        _w.readMode1To25Button.addActionListener(this);
        _w.readMode3To5Button.addActionListener(this);

        _w.shallowWellDepthButton.addActionListener(this);
        _w.deepWellDepthButton.addActionListener(this);

        _w.roiFullFrameButton.addActionListener(this);
        _w.roiCentral768Button.addActionListener(this);
        _w.roiCentral512Button.addActionListener(this);
        _w.roiCentral256Button.addActionListener(this);
        _w.roiSpec1024x512Button.addActionListener(this);

        _w.camera.setChoices(_getCameras());

        _w.selectedFilter.setModel(new NiriFilterComboBoxModel());
        _w.selectedFilter.setRenderer(new NiriFilterComboBoxRenderer());
        _w.selectedFilter.setMaximumRowCount(Math.min(Filter.values().length, 20));
        _w.selectedFilter.addActionListener(this);

        final String[] dispersers = _getDispersers();
        _w.disperser.setChoices(dispersers);
        _w.disperser.setMaximumRowCount(dispersers.length);

        final String[] masks = _getMasks();
        _w.mask.setChoices(masks);
        _w.mask.setMaximumRowCount(masks.length);

        final String[] beamSplitters = _getBeamSplitters();
        _w.beamSplitter.setChoices(beamSplitters);
        _w.beamSplitter.setMaximumRowCount(beamSplitters.length);

        _w.camera.addWatcher(this);
        _w.mask.addWatcher(this);
        _w.beamSplitter.addWatcher(this);
        _w.disperser.addWatcher(this);
        _w.fastModeExposures.addWatcher(this);

        // Arrange to be notified when the OT editable state changes.
        // This is needed to make sure the enabled states are set correctly for the
        // read-mode labels (the ones one right side)
        OT.addEditableStateListener(new OT.EditableStateListener() {
            @Override public ISPNode getEditedNode() { return getNode(); }
            @Override public void updateEditableState() { _updateReadMode(); }
        });
    }

    /**
     * Return an array of camera names
     */
    private String[] _getCameras() {
        return SpTypeUtil.getFormattedDisplayValueAndDescriptions(Camera.class);
    }


    /**
     * Return an array of disperser names
     */
    private String[] _getDispersers() {
        return SpTypeUtil.getFormattedDisplayValueAndDescriptions(Disperser.class);
    }


    /**
     * Return an array of mask names
     */
    private String[] _getMasks() {
        return SpTypeUtil.getFormattedDisplayValueAndDescriptions(Mask.class);
    }

    /**
     * Return an array of beam splitter names
     */
    private String[] _getBeamSplitters() {
        return SpTypeUtil.getFormattedDisplayValueAndDescriptions(BeamSplitter.class);
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
    @Override protected void init() {
        super.init();
        _w.camera.setValue(getDataObject().getCamera().ordinal());
        _w.disperser.setValue(getDataObject().getDisperser().ordinal());
        _w.mask.setValue(getDataObject().getMask().ordinal());
        _w.beamSplitter.setValue(getDataObject().getBeamSplitter().ordinal());
        _w.fastModeExposures.setValue(getDataObject().getFastModeExposures());
        _updateFilterWidgets();
        _updateScienceFOV();
        _updateReadMode();
        _updateWellDepth();
        _updateROI();
    }


    /**
     * Update the filter choice related widgets.
     */
    private void _updateFilterWidgets() {
        // First fill in the text box.
        final Filter filter = getDataObject().getFilter();
        if (filter != null) {
            _w.selectedFilter.getModel().setSelectedItem(Optional.of(filter));
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

    //
    // Update the read mode display
    //
    private void _updateReadMode() {
        _w.readModeLowBgLabel.setEnabled(false);
        _w.readModeMediumBgLabel.setEnabled(false);
        _w.readModeHighBgLabel.setEnabled(false);

        final boolean enabled = OTOptions.areRootAndCurrentObsIfAnyEditable(getProgram(), getContextObservation());
        final ReadMode readMode = getDataObject().getReadMode();
        final ROIDescription roi = getDataObject().getBuiltinROI().getROIDescription();
        if (readMode == ReadMode.IMAG_SPEC_NB) {
            _w.readModeNarrowBandButton.setSelected(true);
            _w.readModeMinExpTime.setText(readMode.getMinExpAsString(roi));
            _w.readModeNoise.setText(String.format("%.0f e-", readMode.getReadNoise()));
            _w.readModeRecMinExpTime.setText(readMode.getRecommendedMinExp());
            _w.readModeLowBgLabel.setEnabled(enabled);
        } else if (readMode == ReadMode.IMAG_1TO25) {
            _w.readMode1To25Button.setSelected(true);
            _w.readModeMinExpTime.setText(readMode.getMinExpAsString(roi));
            _w.readModeNoise.setText(String.format("%.0f e-", readMode.getReadNoise()));
            _w.readModeRecMinExpTime.setText(readMode.getRecommendedMinExp());
            _w.readModeMediumBgLabel.setEnabled(enabled);
        } else if (readMode == ReadMode.IMAG_SPEC_3TO5) {
            _w.readMode3To5Button.setSelected(true);
            _w.readModeMinExpTime.setText(readMode.getMinExpAsString(roi));
            _w.readModeNoise.setText(String.format("%.0f e-", readMode.getReadNoise()));
            _w.readModeRecMinExpTime.setText(readMode.getRecommendedMinExp());
            _w.readModeHighBgLabel.setEnabled(enabled);
        }
    }

    //
    // Update the ROI display
    //
    private void _updateROI() {
        final BuiltinROI roi = getDataObject().getBuiltinROI();
        if (roi == BuiltinROI.FULL_FRAME) {
            _w.roiFullFrameButton.setSelected(true);
        } else if (roi == BuiltinROI.CENTRAL_768) {
            _w.roiCentral768Button.setSelected(true);
        } else if (roi == BuiltinROI.CENTRAL_512) {
            _w.roiCentral512Button.setSelected(true);
        } else if (roi == BuiltinROI.CENTRAL_256) {
            _w.roiCentral256Button.setSelected(true);
        } else if (roi == BuiltinROI.SPEC_1024_512) {
            _w.roiSpec1024x512Button.setSelected(true);
        }
    }

    // Update the well depth
    private void _updateWellDepth() {

        final WellDepth wd = getDataObject().getWellDepth();
        if (wd == WellDepth.SHALLOW) {
            _w.shallowWellDepthButton.setSelected(true);
        } else if (wd == WellDepth.DEEP) {
            _w.deepWellDepthButton.setSelected(true);
        }
    }

    /**
     * Return the position angle text box
     */
    public TextBoxWidget getPosAngleTextBox() {
        return _w.posAngle;
    }

    /**
     * Return the exposure time text box
     */
    public TextBoxWidget getExposureTimeTextBox() {
        return _w.exposureTime;
    }

    /**
     * Return the coadds text box.
     */
    public TextBoxWidget getCoaddsTextBox() {
        return _w.coadds;
    }

    /**
     * Handle action events (for checkbuttons).
     */
    public void actionPerformed(ActionEvent evt) {
        final Object w = evt.getSource();

        if (w == _w.readModeNarrowBandButton) {
            getDataObject().setReadMode(ReadMode.IMAG_SPEC_NB);
            _updateReadMode();
        } else if (w == _w.readMode1To25Button) {
            getDataObject().setReadMode(ReadMode.IMAG_1TO25);
            _updateReadMode();
        } else if (w == _w.readMode3To5Button) {
            getDataObject().setReadMode(ReadMode.IMAG_SPEC_3TO5);
            _updateReadMode();
        } else if (w == _w.shallowWellDepthButton) {
            getDataObject().setWellDepth(WellDepth.SHALLOW);
            _updateWellDepth();
        } else if (w == _w.deepWellDepthButton) {
            getDataObject().setWellDepth(WellDepth.DEEP);
            _updateWellDepth();
        } else if (w == _w.roiFullFrameButton) {
            getDataObject().setBuiltinROI(BuiltinROI.FULL_FRAME);
            _updateReadMode();
            _updateScienceFOV();
        } else if (w == _w.roiCentral768Button) {
            getDataObject().setBuiltinROI(BuiltinROI.CENTRAL_768);
            _updateReadMode();
            _updateScienceFOV();
        } else if (w == _w.roiCentral512Button) {
            getDataObject().setBuiltinROI(BuiltinROI.CENTRAL_512);
            _updateReadMode();
            _updateScienceFOV();
        } else if (w == _w.roiCentral256Button) {
            getDataObject().setBuiltinROI(BuiltinROI.CENTRAL_256);
            _updateReadMode();
            _updateScienceFOV();
        } else if (w == _w.roiSpec1024x512Button) {
            getDataObject().setBuiltinROI(BuiltinROI.SPEC_1024_512);
            _updateReadMode();
            _updateScienceFOV();
        } else if (w == _w.selectedFilter) {
            @SuppressWarnings("unchecked")
            Optional<Filter> filter = (Optional<Filter>)_w.selectedFilter.getSelectedItem();
            getDataObject().setFilter(filter.orElse(Filter.DEFAULT));
        }
    }


    /**
     * Called when an item in a DropDownListBoxWidget is selected.
     */
    public void dropDownListBoxAction(DropDownListBoxWidget ddlbw, int index, String val) {
        if (ddlbw == _w.camera) {
            getDataObject().setCamera(Camera.getCameraByIndex(index));
            _updateScienceFOV();
        } else if (ddlbw == _w.mask) {
            getDataObject().setMask(Mask.getMaskByIndex(index));
            _updateScienceFOV();
        } else if (ddlbw == _w.beamSplitter) {
            getDataObject().setBeamSplitter(BeamSplitter.getBeamSplitterByIndex(index));
            _updateScienceFOV();
        } else if (ddlbw == _w.disperser) {
            getDataObject().setDisperser(Disperser.getDisperserByIndex(index));
        }
    }

    /**
     * A key was pressed in a given TextBoxWidget.
     */
    public void textBoxKeyPress(TextBoxWidget tbwe) {
        super.textBoxKeyPress(tbwe);

        // Note: assume all numeric fields for now
        final int value;
        try {
            value = Integer.parseInt(tbwe.getValue());
        } catch (NumberFormatException e) {
            return;
        }

        if (tbwe == _w.fastModeExposures) {
            getDataObject().setFastModeExposures(value);
        }
    }


    /**
     * An auxiliary method that returns a Vector with all the original NIRI Filters wrapped as <code>Optional</code>
     * elements, separated by Type. We use an <code>Optional.empty()</code> element to separate them in the final
     * result.
     * This is used for display purposes in the ComboBox.
     * @return a Vector of optional filters.
     */
    private static Vector<Optional<Filter>> getFilterOptions() {
        final java.util.List<Filter> v = new ArrayList<>(SpTypeUtil.getSelectableItems(Niri.Filter.class));

        final Vector<Optional<Niri.Filter>> nv = new Vector<>();
        v.stream().filter(f -> f.type() == Filter.Type.broadband).forEach(f -> nv.add(Optional.of(f)));
        nv.add(Optional.empty());
        v.stream().filter(f -> f.type() == Filter.Type.narrowband).forEach(f -> nv.add(Optional.of(f)));
        return nv;
    }

    /**
     * A ComboBox model for NIRI Filters, such as we can group narrowband and broadband filters together
     * We disable the possibility of selecting the elements in the combo that are used as separators,
     * which are stored as None objects.
     */
    private final class NiriFilterComboBoxModel extends DefaultComboBoxModel {

        @SuppressWarnings("unchecked")
        public NiriFilterComboBoxModel() {
            super(getFilterOptions());
        }

        @Override
        public void setSelectedItem(Object anObject) {

            @SuppressWarnings("unchecked")
            Optional<Niri.Filter> filter = (Optional<Niri.Filter>) anObject;
            if (filter.isPresent()) super.setSelectedItem(anObject);
        }
    }

    /**
     * A Renderer for the Niri Filter combobox. It uses the description for the display (contrary to
     * what other SpTypes use which is the displayable field, add a mark to obsolete filters, and return
     * a <code>JComponent.Separator</code> if the filter is an <code>Optional.empty()</code>.
     */
    private final class NiriFilterComboBoxRenderer extends BasicComboBoxRenderer {

        public Component getListCellRendererComponent(JList jList, Object value, int index, boolean isSelected, boolean hasFocus) {

            @SuppressWarnings("unchecked")
            final Optional<Niri.Filter> opFilter = (Optional<Niri.Filter>) value;

            return opFilter.map(f ->
                            super.getListCellRendererComponent(jList, f.description() + (f.isObsolete() ? "*" : ""), index, isSelected, hasFocus)
            ).orElse(SEPARATOR);

        }
    }


}
