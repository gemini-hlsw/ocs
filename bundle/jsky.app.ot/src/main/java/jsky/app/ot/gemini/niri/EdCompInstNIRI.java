// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: EdCompInstNIRI.java 8275 2007-11-22 20:15:41Z gillies $
//
package jsky.app.ot.gemini.niri;

import edu.gemini.pot.sp.ISPNode;
import edu.gemini.spModel.gemini.niri.InstNIRI;
import edu.gemini.spModel.gemini.niri.Niri;
import edu.gemini.spModel.gemini.niri.Niri.*;
import edu.gemini.spModel.type.SpTypeUtil;
import jsky.app.ot.OT;
import jsky.app.ot.OTOptions;
import jsky.app.ot.editor.type.SpTypeComboBoxModel;
import jsky.app.ot.editor.type.SpTypeComboBoxRenderer;
import jsky.app.ot.gemini.editor.EdCompInstBase;
import jsky.util.gui.DropDownListBoxWidget;
import jsky.util.gui.DropDownListBoxWidgetWatcher;
import jsky.util.gui.TextBoxWidget;

import javax.swing.JPanel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;


/**
 * This is the editor for the NIRI instrument component.
 */
public final class EdCompInstNIRI extends EdCompInstBase<InstNIRI>
        implements DropDownListBoxWidgetWatcher, ActionListener {

    /**
     * The GUI layout panel
     */
    private final NiriForm _w;


    /**
    /**
     * The constructor initializes the user interface.
     */
    public EdCompInstNIRI() {
        _w = new NiriForm();

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

//        final String[] filters = _getFilters();
//        _w.selectedFilter.setChoices(filters);
//        _w.selectedFilter.setMaximumRowCount(Math.min(filters.length, 20));

        final SpTypeComboBoxModel<Filter> filterModel = new SpTypeComboBoxModel<>(Filter.class);
        _w.selectedFilter.setModel(filterModel);
        _w.selectedFilter.setRenderer(new SpTypeComboBoxRenderer());
        _w.selectedFilter.setMaximumRowCount(Filter.values().length);
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
//        _w.selectedFilter.addWatcher(this);
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
     * Return an array of filter names (combined broad band and narrow
     * band, separated by an empty item).
     */
    private String[] _getFilters() {
        // SW: this is all gross.  We should be storing actual Filter objects
        // in the combo boxes, not strings.
        final Niri.Filter[] filters = Niri.Filter.values();
        final List<String> descriptions = new ArrayList<String>(filters.length + 1);

        Niri.Filter.Type type = Niri.Filter.Type.broadband;
        for (Filter f : filters) {
            if (f.type() != type) {
                type = f.type();
                descriptions.add("--------------");
            }
            descriptions.add(f.description());
        }
        return descriptions.toArray(new String[descriptions.size()]);
    }


    /**
     * Return the Filter object, given the description String
     */
    private Filter _getFilterFromDesc(String desc) {
        final Niri.Filter[] filters = Niri.Filter.values();
        for (Filter f : filters) {
            if (desc.equals(f.description())) return f;
        }
        return Niri.Filter.DEFAULT;
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
     * Get the index of the filter in the given array, or -1 if the filter
     * isn't in the array.
     */
//    private int _getFilterIndex(Filter filter, SPTypeBaseList blist) {
//        return blist.getIndexByType(filter);
//    }

    /**
     * Update the filter choice related widgets.
     */
    private void _updateFilterWidgets() {
        // First fill in the text box.
        final Filter filter = getDataObject().getFilter();
        if (filter != null) {
            _w.selectedFilter.getModel().setSelectedItem(filter);
        }

        /*
        // See which type of filter the selected filter is, if any.
        if (filter != null) {
            int index = _getFilterIndex(filter, BroadBandFilter.TYPES);
            if (index != -1) {
                _w.selectedFilter.setValue(index);
            } else {
                index = _getFilterIndex(filter, NarrowBandFilter.TYPES);
                if (index != -1) {
                    // leave room for space between broad and narrow band listings
                    _w.selectedFilter.setValue(NIRIParams.BROADBANDFILTERS.length + 1 + index);
                }
            }
            // XXX _w.filterWavelength.setText(filter.getDescription());
        }
        */
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
            _w.readModeNoise.setText(readMode.getReadNoise());
            _w.readModeRecMinExpTime.setText(readMode.getRecommendedMinExp());
            _w.readModeLowBgLabel.setEnabled(enabled);
        } else if (readMode == ReadMode.IMAG_1TO25) {
            _w.readMode1To25Button.setSelected(true);
            _w.readModeMinExpTime.setText(readMode.getMinExpAsString(roi));
            _w.readModeNoise.setText(readMode.getReadNoise());
            _w.readModeRecMinExpTime.setText(readMode.getRecommendedMinExp());
            _w.readModeMediumBgLabel.setEnabled(enabled);
        } else if (readMode == ReadMode.IMAG_SPEC_3TO5) {
            _w.readMode3To5Button.setSelected(true);
            _w.readModeMinExpTime.setText(readMode.getMinExpAsString(roi));
            _w.readModeNoise.setText(readMode.getReadNoise());
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
     * Must watch table widget actions as part of the TableWidgetWatcher
     * interface, but don't care about them.
     */
    //public void	tableAction(TableWidget twe, int colIndex, int rowIndex) {}

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
            getDataObject().setFilter((Filter) _w.selectedFilter.getSelectedItem());
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
}
