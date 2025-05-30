package jsky.app.ot.gemini.gmos;

import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.data.YesNoType;
import edu.gemini.spModel.gemini.gmos.*;
import edu.gemini.spModel.gemini.gmos.GmosCommonType.*;
import edu.gemini.spModel.gemini.gmos.GmosNorthType.DisperserNorth;
import edu.gemini.spModel.gemini.gmos.GmosNorthType.FilterNorth;
import edu.gemini.spModel.gemini.seqcomp.SeqRepeatOffset;
import edu.gemini.spModel.guide.GuideOption;
import edu.gemini.spModel.guide.StandardGuideOptions;
import edu.gemini.spModel.target.TelescopePosWatcher;
import edu.gemini.spModel.target.WatchablePos;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import edu.gemini.spModel.target.offset.OffsetPos;
import edu.gemini.spModel.target.offset.OffsetPosList;
import edu.gemini.spModel.target.offset.OffsetPosListWatcher;
import edu.gemini.spModel.target.offset.OffsetPosSelection;
import edu.gemini.spModel.telescope.IssPort;
import edu.gemini.spModel.type.SpTypeUtil;
import edu.gemini.spModel.util.SPTreeUtil;
import jsky.app.ot.OTOptions;
import jsky.app.ot.StaffBean$;
import jsky.app.ot.editor.type.SpTypeComboBoxModel;
import jsky.app.ot.editor.type.SpTypeComboBoxRenderer;
import jsky.app.ot.gemini.editor.EdCompInstBase;
import jsky.app.ot.nsp.SPTreeEditUtil;
import jsky.app.ot.tpe.TelescopePosEditor;
import jsky.app.ot.tpe.TpeManager;
import jsky.util.gui.*;
import scala.Option;
import scala.Some;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeListener;
import java.util.List;

/**
 * This is the editor for the GMOS instrument component.
 */
public abstract class EdCompInstGMOS<T extends InstGmosCommon> extends EdCompInstBase<T>
        implements DropDownListBoxWidgetWatcher, ActionListener,
        TelescopePosWatcher, TableWidgetWatcher, ChangeListener, FocusListener {

    // Allowed central wavelength (lambda_cent) range in nanometers
    protected static final int MIN_LAMBDA_CENT = 350;
    protected static final int MAX_LAMBDA_CENT = 1100;

    /**
     * The OIWFS guide tag.
     */
    private static final String OIWFS = "OIWFS";

    /**
     * Listeners for property changes that affect the parallactic and unbounded angle components.
     */
    final PropertyChangeListener updateParallacticAnglePCL;
    final PropertyChangeListener updateUnboundedAnglePCL;


    @Override
    public void focusGained(FocusEvent focusEvent) {
        //do nothing
    }

    private void reinitializeNodAndShuffleOffset() {
        final int drUi = _w.detectorRows.getIntegerValue(-1);
        final int drModel = getDataObject().getNsDetectorRows();
        if (drUi != drModel) {
            _w.detectorRows.deleteWatcher(this);
            _w.detectorRows.setValue(drModel);
            _w.detectorRows.addWatcher(this);
        }

        final String soUi = _w.shuffleOffset.getValue();
        final String soModel = getDataObject().getShuffleOffsetAsString();
        if (!soUi.equals(soModel)) {
            _w.shuffleOffset.deleteWatcher(this);
            _w.shuffleOffset.setValue(getDataObject().getShuffleOffsetAsString());
            _w.shuffleOffset.addWatcher(this);
        }
    }

    @Override
    public void focusLost(FocusEvent focusEvent) {
        _checkNSValues();
        // When leaving focus of _w.shuffleOffset or detectorRows, show the
        // values that were actually stored in the model (which may differ
        // due to yBin).
        if (focusEvent.getSource() == _w.shuffleOffset) {
            reinitializeNodAndShuffleOffset();
        } else if (focusEvent.getSource() == _w.detectorRows) {
            reinitializeNodAndShuffleOffset();
        }
    }

    /**
     * Local class used to hold a table of information used to check for valid
     * filter, grating and wavelength combinations.
     */
    protected static class GMOSInfo {
        final Enum type;
        final int lambda1;
        final int lambda2;

        public GMOSInfo(Enum type, int lambda1, int lambda2) {
            this.type = type;
            this.lambda1 = lambda1;
            this.lambda2 = lambda2;
        }
    }

    // GMOS filter info for the mask making software
    private static final GMOSInfo[] FILTER_INFO = new GMOSInfo[]{
            //                 filter              lambda1   lambda2
            new GMOSInfo(FilterNorth.g_G0301, 398, 552),
            new GMOSInfo(FilterNorth.r_G0303, 562, 698),
            new GMOSInfo(FilterNorth.i_G0302, 706, 850),
            new GMOSInfo(FilterNorth.z_G0304, 848, 1100),
            new GMOSInfo(FilterNorth.GG455_G0305, 460, 1100),
            new GMOSInfo(FilterNorth.OG515_G0306, 520, 1100),
            new GMOSInfo(FilterNorth.RG610_G0307, 615, 1100)
    };

    // GMOS grating info for the mask making software
    private static final GMOSInfo[] GRATING_INFO = new GMOSInfo[]{
            //                 grating             lambda1   lambda2
            new GMOSInfo(DisperserNorth.B1200_G5301, 300, 1100),
            new GMOSInfo(DisperserNorth.R831_G5302, 498, 1100),
            new GMOSInfo(DisperserNorth.B600_G5303, 320, 1100),
            new GMOSInfo(DisperserNorth.R600_G5304, 530, 1100),
            new GMOSInfo(DisperserNorth.R400_G5305, 520, 1100),
            new GMOSInfo(DisperserNorth.R400_G5310, 520, 1100),
            new GMOSInfo(DisperserNorth.R150_G5306, 430, 1100)
    };


    // The GUI layout panel
    protected final GmosForm<T> _w;
    private final GmosOffsetPosTableWidget<OffsetPos> _offsetTable;

    // Custom ROIs
    private final GmosCustomROITableWidget _customROITable;

    // Current nod & shuffle offset position being edited
    private OffsetPos _curPos;

    // Nod & Shuffle position list being edited
    private OffsetPosList<OffsetPos> _opl;

    private final ActionListener oiwfsBoxListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            final GuideOption opt = (GuideOption) _w.oiwfsBox.getSelectedItem();
            final List<OffsetPos> selList = OffsetPosSelection.apply(getNode()).selectedPositions(_opl);
            for (OffsetPos selPos : selList) {
                selPos.setLink(GmosOiwfsGuideProbe.instance, opt);
            }
            _offsetTable.reinit(getNode(), _opl);
        }
    };

    @Override
    protected double getDefaultExposureTime() {
        return 300.;
    }

    /**
     * The constructor initializes the user interface.
     */
    public EdCompInstGMOS() {
        _w = new GmosForm<>();
        _offsetTable = _w.offsetTable;
        _customROITable = _w.customROITable;

        // JBuilder9 didn't like this line, so put it outside the GUI file
        _w.tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        // OT-493: hide gmmps/mask features when off-site, for now
        // TODO: revisit this
//        if (!OTOptions.isStaffAndHasPreferredSite()) {
        _w.focalPlaneMaskPlotButton.setVisible(false);
        _w.ccdSlowHighButton.setVisible(false);
//        }

        // add action listeners
        _w.focalPlaneBuiltInButton.addActionListener(this);
        _w.focalPlaneMaskButton.addActionListener(this);
        _w.focalPlaneMaskPlotButton.addActionListener(this);
        _w.ccdSlowLowButton.addActionListener(this);
        _w.ccdFastLowButton.addActionListener(this);
        _w.ccdSlowHighButton.addActionListener(this);
        _w.ccdFastHighButton.addActionListener(this);
        _w.ccd3AmpButton.addActionListener(this);
        _w.ccd3AmpButton.setEnabled(false);
        _w.ccd6AmpButton.addActionListener(this);
        _w.ccd6AmpButton.setEnabled(false);
        _w.ccd12AmpButton.addActionListener(this);
        _w.ccd12AmpButton.setEnabled(false);
        _w.transFollowXYButton.addActionListener(this);
        _w.transFollowXYZButton.addActionListener(this);
        _w.transFollowZButton.addActionListener(this);
        _w.transNoFollowButton.addActionListener(this);

        _w.transDtaSpinner.setModel(new SpinnerNumberModel(1, DTAX.getMinimumXOffset().intValue(), DTAX.getMaximumXOffset().intValue(), 1));
        _w.transDtaSpinner.addChangeListener(this);

        _w.noROIButton.addActionListener(this);
        _w.ccd2Button.addActionListener(this);
        _w.centralSpectrumButton.addActionListener(this);
        _w.centralStampButton.addActionListener(this);
        _w.customButton.addActionListener(this);
        _w.customROITable.addWatcher(this);
        _w.xMin.addWatcher(this);
        _w.yMin.addWatcher(this);
        _w.xRange.addWatcher(this);
        _w.yRange.addWatcher(this);
        _w.customROINewButton.addActionListener(this);
        _w.customROIPasteButton.addActionListener(this);
        _w.customROIRemoveButton.addActionListener(this);
        _w.customROIRemoveAllButton.addActionListener(this);

        final String[] customROIColNames = new String[]{"Xmin", "Ymin", "Xrange", "Yrange"};
        _customROITable.setColumnHeaders(customROIColNames);
        _customROITable.setBackground(_w.getBackground());
        _customROITable.getTableHeader().setFont(_customROITable.getFont().deriveFont(Font.BOLD));


        _w.upLookingButton.addActionListener(this);
        _w.sideLookingButton.addActionListener(this);

        _w.newButton.addActionListener(this);
        _w.removeAllButton.addActionListener(this);
        _w.removeButton.addActionListener(this);

        // initialize the combo boxes
        initListBox(_w.disperserComboBox, getDisperserClass(), evt -> {
            getDataObject().setDisperser((Enum) _w.disperserComboBox.getSelectedItem());
            _updateDisperser();
            _checkDisperserValue();
        });


        initListBox(_w.filterComboBox, getFilterClass(), e -> {
            getDataObject().setFilter((Enum) _w.filterComboBox.getSelectedItem());
            _checkDisperserValue();

        });

        initListBox(_w.detectorManufacturerComboBox, getDetectorManufacturerClass(), e -> {
            final DetectorManufacturer selectedDetectorManufacturer =
                    (DetectorManufacturer) _w.detectorManufacturerComboBox.getSelectedItem();
            if (selectedDetectorManufacturer != getDataObject().getDetectorManufacturer()) {
                getDataObject().setDetectorManufacturer((DetectorManufacturer) _w.detectorManufacturerComboBox.getSelectedItem());
                _updateControlVisibility();
                _updateReadoutCharacteristics();
                _updateNodAndShuffle();
                _updateCCD();
            }
        });

        final String[] fpUnits = _getFPUnits();
        _w.builtinComboBox.setChoices(fpUnits);
        _w.builtinComboBox.setMaximumRowCount(fpUnits.length);

        final String[] slitWidths = _getCustomMaskSlitWidths();
        _w.customSlitWidthComboBox.setChoices(slitWidths);
        _w.customSlitWidthComboBox.setMaximumRowCount(slitWidths.length);

        final String[] binChoices = _getBinnings();
        _w.xBinComboBox.setChoices(binChoices);
        _w.yBinComboBox.setChoices(binChoices);

        _w.orderComboBox.setChoices(_getOrders());

        _w.builtinComboBox.addWatcher(this);
        _w.customSlitWidthComboBox.addWatcher(this);
        _w.xBinComboBox.addWatcher(this);
        _w.yBinComboBox.addWatcher(this);
        _w.orderComboBox.addWatcher(this);

        _w.centralWavelength.addWatcher(this);
        _w.focalPlaneMask.addWatcher(this);

        _w.xOffset.addWatcher(this);
        _w.yOffset.addWatcher(this);
        _w.offsetTable.addWatcher(this);

        final String[] colNames = new String[]{"#", "p", "q", OIWFS};
        _offsetTable.setColumnHeaders(colNames);
        _offsetTable.setBackground(_w.getBackground());
        _offsetTable.getTableHeader().setFont(_offsetTable.getFont().deriveFont(Font.BOLD));
        _w.oiwfsBox.setChoices(StandardGuideOptions.instance.getAll());
        _w.oiwfsBox.addActionListener(oiwfsBoxListener);

        _w.nsCheckButton.addActionListener(this);

        _w.electronicOffsetCheckBox.addActionListener(this);
        _w.shuffleOffset.addWatcher(this);
        _w.shuffleOffset.addFocusListener(this);
        _w.detectorRows.addFocusListener(this);
        _w.detectorRows.addWatcher(this);
        _w.numNSCycles.addWatcher(this);

        _w.preImgCheckButton.addActionListener(this);

        _clearWarning(_w.warning1);
        _clearWarning(_w.warning2);
        _clearWarning(_w.warning3);
        _clearWarning(_w.warning4);
        _clearWarning(_w.warningCustomROI);

        _w.detectorManufacturerComboBox.setEnabled(OTOptions.isStaffGlobally()); // REL-1194
        StaffBean$.MODULE$.addPropertyChangeListener(evt -> {
            _w.detectorManufacturerComboBox.setEnabled(isEnabled() && OTOptions.isStaffGlobally()); // REL-1194
        });

        addCustomRoiPasteKeyBinding();

        // Create the property change listeners for the parallactic angle panel.
        updateParallacticAnglePCL = evt -> _w.posAnglePanel.updateParallacticControls();
        updateUnboundedAnglePCL = evt -> _w.posAnglePanel.updateUnboundedControls();
    }

    private Site getSite() {
        // each GMOS is associated with a single site.
        return getDataObject().getSite().iterator().next();
    }

    protected Class getFilterClass() {
        return FilterNorth.class;
    }

    protected Class getDisperserClass() {
        return DisperserNorth.class;
    }

    protected Class getDetectorManufacturerClass() {
        return GmosCommonType.DetectorManufacturer.class;
    }

    //This method will initialize the DropDownListBox with the elements
    //indicated in Enum class, and will configure the widget to show all the
    //available options.
    @SuppressWarnings("unchecked")
    private <E extends Enum<E>> void initListBox(JComboBox<E> widget, Class<E> c, ActionListener l) {
        final SpTypeComboBoxModel<E> model = new SpTypeComboBoxModel<>(c);
        widget.setModel(model);
        widget.setRenderer(new SpTypeComboBoxRenderer());
        widget.setMaximumRowCount(c.getEnumConstants().length);
        if (l != null) widget.addActionListener(l);
    }

    /**
     * Return an array of mask names
     */
    protected String[] _getFPUnits() {
        return SpTypeUtil.getFormattedDisplayValueAndDescriptions(GmosNorthType.FPUnitNorth.class);
    }

    /**
     * Return an array of order names
     */
    private String[] _getOrders() {
        return SpTypeUtil.getFormattedDisplayValueAndDescriptions(Order.class);
    }

    /**
     * Return an array of binning names
     */
    private String[] _getBinnings() {
        return SpTypeUtil.getFormattedDisplayValueAndDescriptions(Binning.class);
    }

    /**
     * Return an array of possible slit widths for custom masks
     */
    private String[] _getCustomMaskSlitWidths() {
        return SpTypeUtil.getFormattedDisplayValueAndDescriptions(CustomSlitWidth.class);
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
    protected void init() {
        super.init();
        _curPos = null;
        _opl = null;

        _updateFilter();
        _updateExposureTime();
        _updateDisperser();
        _updateFPU();
        _updateCCD();
        _updateStageDetails();
        _updateBuiltinROIDetails();
        _updatePort();
        _updateNodAndShuffle();
        _updatePreimaging();
        _checkDisperserValue();

        _updateControlVisibility();
        _updateReadoutCharacteristics();

        _updateCustomSlitWidth();

        _w.posAnglePanel.init(this, getSite());


        // If the position angle mode or FPU mode properties change, force an update on the parallactic angle mode.
        final T inst = getDataObject();
        inst.addPropertyChangeListener(InstGmosCommon.FPU_MODE_PROP.getName(), updateParallacticAnglePCL);
        inst.addPropertyChangeListener(InstGmosCommon.DISPERSER_PROP_NAME, updateParallacticAnglePCL);

        // If the MOS preimaging or FPU mode properties change, force an update on the unbounded angle mode.
        inst.addPropertyChangeListener(InstGmosCommon.IS_MOS_PREIMAGING_PROP.getName(), updateUnboundedAnglePCL);
        inst.addPropertyChangeListener(InstGmosCommon.FPU_MODE_PROP.getName(),          updateUnboundedAnglePCL);
    }

    @Override
    protected void cleanup() {
        super.cleanup();

        final T inst = getDataObject();
        inst.removePropertyChangeListener(InstGmosCommon.FPU_MODE_PROP.getName(), updateParallacticAnglePCL);
        inst.removePropertyChangeListener(InstGmosCommon.DISPERSER_PROP_NAME, updateParallacticAnglePCL);
    }

    // Initialize controls based on gmos specific information.
    protected void _updateControlVisibility() {
        _w.transFollowXYZButton.setVisible(false);
        _w.transFollowZButton.setVisible(false);

        DetectorManufacturer det = getDataObject().getDetectorManufacturer();
        if (det == GmosCommonType.DetectorManufacturer.E2V) {
            _w.ccd3AmpButton.setVisible(true);
            _w.ccd6AmpButton.setVisible(true);
            _w.ccd12AmpButton.setVisible(false);
            _w.noROIButton.setText("Full Frame Readout");
            _w.ccd2Button.setText("CCD2: 2.5' x 5.5' imaging FOV");
            _w.centralSpectrumButton.setText("Central Spectrum: spectroscopy of central 75\" FOV");
            _w.centralStampButton.setText("Central Stamp: 22\" x 22\" imaging FOV");
            _w.customButton.setText("Custom ROI");
            // Only allow to ever change from E2V to Hammamatsu
            // for  incomplete observations. Non staff can change it according to REL-4509
            _w.detectorManufacturerComboBox.setEnabled(isEnabled());
        } else {// HAMAMATSU:
            if (OTOptions.isStaff(getProgram().getProgramID())) {
                _w.ccd3AmpButton.setVisible(false);
                _w.ccd6AmpButton.setVisible(true);
                _w.ccd12AmpButton.setVisible(true);
            } else {
                _w.ccd3AmpButton.setVisible(false);
                _w.ccd6AmpButton.setVisible(false);
                _w.ccd12AmpButton.setVisible(false);
            }
            _w.detectorManufacturerComboBox.setEnabled(false); // Never allow to change from Hammamatsu to E2V
            _w.noROIButton.setText("Full Frame Readout");
            _w.ccd2Button.setText("CCD2: 2.75' x 5.5' imaging FOV");
            _w.centralSpectrumButton.setText("Central Spectrum: spectroscopy of central 80\" FOV");
            _w.centralStampButton.setText("Central Stamp: 24\" x 24\" imaging FOV");
            _w.customButton.setText("Custom ROI");
        }
        if (OTOptions.isStaff(getProgram().getProgramID())) {
            _w.customButton.setEnabled(true);
            _w.customROINewButton.setEnabled(true);
            _w.customROIPasteButton.setEnabled(true);
            _w.customROIRemoveButton.setEnabled(true);
            _w.customROIRemoveAllButton.setEnabled(true);
            _w.xMin.setEditable(true);
            _w.yMin.setEditable(true);
            _w.xRange.setEditable(true);
            _w.yRange.setEditable(true);
        } else {
            _w.customButton.setEnabled(false);
            _w.customROINewButton.setEnabled(false);
            _w.customROIPasteButton.setEnabled(false);
            _w.customROIRemoveButton.setEnabled(false);
            _w.customROIRemoveAllButton.setEnabled(false);
            _w.xMin.setEditable(false);
            _w.yMin.setEditable(false);
            _w.xRange.setEditable(false);
            _w.yRange.setEditable(false);
        }
        // Make sure for the nth time nobody can edit these
        _w.ccd3AmpButton.setEnabled(false);
        _w.ccd6AmpButton.setEnabled(false);
        _w.ccd12AmpButton.setEnabled(false);

    }

    // Display the current filter settings
    private void _updateFilter() {
        _w.filterComboBox.getModel().setSelectedItem(getDataObject().getFilter());
    }

    // Display the current exposure time settings
    private void _updateExposureTime() {
        _w.exposureTime.setValue(getDataObject().getExposureTimeAsString());
    }


    // Display the current CCD readout details settings
    private void _updateCCD() {
        _w.xBinComboBox.setValue(_getBinningIndex(getDataObject().getCcdXBinning()));
        _w.yBinComboBox.setValue(_getBinningIndex(getDataObject().getCcdYBinning()));

        final AmpReadMode s = getDataObject().getAmpReadMode();
        final AmpGain g = getDataObject().getGainChoice();
        final DetectorManufacturer detectorManufacturer = getDataObject().getDetectorManufacturer();

        boolean slowHighVisible = OTOptions.isStaff(getProgram().getProgramID());
        if ((s == AmpReadMode.SLOW) && (g == AmpGain.LOW)) {
            _w.ccdSlowLowButton.setSelected(true);
        } else if ((s == AmpReadMode.SLOW) && (g == AmpGain.HIGH)) {
            slowHighVisible = true;
            _w.ccdSlowHighButton.setSelected(true);
        } else if ((s == AmpReadMode.FAST) && (g == AmpGain.LOW)) {
            _w.ccdFastLowButton.setSelected(true);
        } else if ((s == AmpReadMode.FAST) && (g == AmpGain.HIGH)) {
            _w.ccdFastHighButton.setSelected(true);
        }
        _w.ccdSlowHighButton.setVisible(slowHighVisible);
        _w.ccdSlowHighButton.setEnabled(isEnabled() && OTOptions.isStaff(getProgram().getProgramID()));

        _w.detectorManufacturerComboBox.setSelectedItem(detectorManufacturer);

        _updateReadoutCharacteristics();

        final Enum c = getDataObject().getAmpCount();
        if ((c == GmosCommonType.AmpCount.THREE))
            _w.ccd3AmpButton.setSelected(true);
        else if ((c == GmosCommonType.AmpCount.SIX))
            _w.ccd6AmpButton.setSelected(true);
        else if (c == GmosCommonType.AmpCount.TWELVE)
            _w.ccd12AmpButton.setSelected(true);

        // changing the CCD can render ROIs invalid
        validateROIs();

    }

    // Display the current translation stage details settings
    private void _updateStageDetails() {
        final StageMode m = (StageMode) getDataObject().getStageMode();
        if ((m == GmosNorthType.StageModeNorth.FOLLOW_XY) || (m == GmosSouthType.StageModeSouth.FOLLOW_XY)) {
            _w.transFollowXYButton.setSelected(true);
        } else if ((m == GmosNorthType.StageModeNorth.FOLLOW_XYZ) || (m == GmosSouthType.StageModeSouth.FOLLOW_XYZ)) {
            _w.transFollowXYZButton.setSelected(true);
        } else if ((m == GmosNorthType.StageModeNorth.FOLLOW_Z_ONLY) || (m == GmosSouthType.StageModeSouth.FOLLOW_Z_ONLY)) {
            _w.transFollowZButton.setSelected(true);
        } else if ((m == GmosNorthType.StageModeNorth.NO_FOLLOW) || (m == GmosSouthType.StageModeSouth.NO_FOLLOW)) {
            _w.transNoFollowButton.setSelected(true);
        }

        _w.transDtaSpinner.setValue(getDataObject().getDtaXOffset().intValue());
    }

    // Display the current value for any builtin ROI
    private void _updateBuiltinROIDetails() {
        final BuiltinROI roi = getDataObject().getBuiltinROI();
        if (roi == BuiltinROI.FULL_FRAME) {
            _w.noROIButton.setSelected(true);
        } else if (roi == BuiltinROI.CCD2) {
            _w.ccd2Button.setSelected(true);
        } else if (roi == BuiltinROI.CENTRAL_SPECTRUM) {
            _w.centralSpectrumButton.setSelected(true);
        } else if (roi == BuiltinROI.CENTRAL_STAMP) {
            _w.centralStampButton.setSelected(true);
        } else if (roi == BuiltinROI.CUSTOM) {
            _w.customButton.setSelected(true);
        }

        _customROITable.reinit(
                getDataObject().getCustomROIs(),
                getDataObject().getCcdXBinning(),
                getDataObject().getCcdYBinning());

        _w.xMin.setValue(1);
        _w.yMin.setValue(1);
        _w.xRange.setValue(1);
        _w.yRange.setValue(1);

        // changing any of these settings might turn ROIs invalid
        validateROIs();
    }

    // Display the current ISS port settings
    private void _updatePort() {
        final IssPort port = getDataObject().getIssPort();
        if (port == IssPort.SIDE_LOOKING) {
            _w.sideLookingButton.setSelected(true);
        } else if (port == IssPort.UP_LOOKING) {
            _w.upLookingButton.setSelected(true);
        }
    }


    // Display the current FPU settings
    private void _updateFPU() {
        _w.builtinComboBox.setValue(getDataObject().getFPUnit().ordinal());

        final FPUnitMode fpUnitMode = getDataObject().getFPUnitMode();
        _w.focalPlaneMask.setText(getDataObject().getFPUnitCustomMask());

        final boolean enabled = OTOptions.areRootAndCurrentObsIfAnyEditable(getProgram(), getContextObservation());

        if (fpUnitMode == FPUnitMode.CUSTOM_MASK) {
            _w.focalPlaneMask.setEnabled(enabled);
            _w.builtinComboBox.setEnabled(false);

            _w.focalPlaneMaskButton.setSelected(true);
            _w.focalPlaneMaskPlotButton.setEnabled(enabled);
            _w.customSlitWidthComboBox.setEnabled(true);
        } else {
            _w.focalPlaneMask.setEnabled(false);
            _w.builtinComboBox.setEnabled(enabled);

            _w.focalPlaneBuiltInButton.setSelected(true);
            _w.focalPlaneMaskPlotButton.setEnabled(false);
            _w.customSlitWidthComboBox.setEnabled(false);
        }
    }

    /**
     * Display the current Disperser settings
     */
    protected void _updateDisperser() {
        _w.disperserComboBox.getModel().setSelectedItem(getDataObject().getDisperser());
        _w.centralWavelength.setValue(String.valueOf(getDataObject().getDisperserLambda()));
        _w.orderComboBox.setValue(getDataObject().getDisperserOrder().ordinal());
        if (isMirror()) {
            _w.orderComboBox.setEnabled(false);
            _w.orderLabel.setEnabled(false);
            _w.centralWavelength.setEnabled(false);
            _w.centralWavelengthLabel.setEnabled(false);
            _w.preImgCheckButton.setEnabled(true);
        } else {
            final boolean enabled = OTOptions.areRootAndCurrentObsIfAnyEditable(getProgram(), getContextObservation());
            _w.orderComboBox.setEnabled(enabled);
            _w.orderLabel.setEnabled(enabled);
            _w.centralWavelength.setEnabled(enabled);
            _w.centralWavelengthLabel.setEnabled(enabled);
            _w.preImgCheckButton.setEnabled(false);
            getDataObject().setMosPreimaging(YesNoType.NO);
            _updatePreimaging();
        }
    }

    private boolean isMirror() {
        return ((GmosCommonType.Disperser) getDataObject().getDisperser()).isMirror();
    }

    @Override
    protected void updateEnabledState(boolean enabled) {
        super.updateEnabledState(enabled);
        _w.preImgCheckButton.setEnabled(enabled && isMirror());

        final boolean customMask = (getDataObject().getFPUnitMode() == FPUnitMode.CUSTOM_MASK);
        _w.builtinComboBox.setEnabled(enabled && !customMask);
        _w.focalPlaneMask.setEnabled(enabled && customMask);
        _w.customSlitWidthComboBox.setEnabled(enabled && customMask);
        _w.posAnglePanel.updateEnabledState(enabled);
    }

    protected void _updateCustomSlitWidth() {
        _w.customSlitWidthComboBox.deleteWatcher(this);
        CustomSlitWidth csw = getDataObject().getCustomSlitWidth();
        if (csw == null) csw = CustomSlitWidth.OTHER;
        _w.customSlitWidthComboBox.setSelectedIndex(csw.ordinal());
        _w.customSlitWidthComboBox.addWatcher(this);
    }

    // Return the combo box index for the given Binning value
    private int _getBinningIndex(Binning b) {
        if (b == Binning.ONE)
            return 0;
        if (b == Binning.TWO)
            return 1;
        if (b == Binning.FOUR)
            return 2;
        return 0;
    }


    // Update the nod & shuffle tab with the current values from the data object
    private void _updateNodAndShuffle() {
        if (getDataObject().useNS()) {
            // Nod & Shuffle enabled
            _w.nsCheckButton.setSelected(true);

            _w.tabbedPane.setEnabledAt(_w.tabbedPane.indexOfComponent(_w.nsPanel), true);

            // Get the current offset list and fill in the table widget
            _opl = getDataObject().getPosList();

            _opl.addWatcher(offsetListWatcher);
            _offsetTable.reinit(getNode(), _opl);

            // add default offset positions at (0,0) and (0,10), if the table is empty
            if (_opl.size() == 0) {
                _opl.addPosition(0., 0.);
                _opl.addPosition(0., 10.);
            }

            // only allow 2 nod offset positions for now
            _w.newButton.setEnabled(_opl.size() < 2);

            // update the WFS menus when the target list changes
            final TargetObsComp obsComp = getContextTargetObsCompDataObject();
            if (obsComp != null) {
                obsComp.addPropertyChangeListener(TargetObsComp.TARGET_ENV_PROP, targetListWatcher);
            }

            _w.shuffleOffset.setValue(getDataObject().getShuffleOffsetAsString());
            _w.detectorRows.setValue(Integer.toString(getDataObject().getNsDetectorRows()));
            _w.numNSCycles.setValue(Integer.toString(getDataObject().getNsNumCycles()));
            _w.totalTime.setText(Double.toString(getDataObject().getTotalIntegrationTime()));

            _checkNSValues();

            _offsetTable.selectPos(_opl.getPositionAt(0));
        } else {
            // Nod & Shuffle disabled
            _w.nsCheckButton.setSelected(false);

            _w.tabbedPane.setEnabledAt(_w.tabbedPane.indexOfComponent(_w.nsPanel), false);
            if (_w.tabbedPane.getSelectedComponent() == _w.nsPanel)
                _w.tabbedPane.setSelectedIndex(0);
        }
    }


    private void _updatePreimaging() {
        _w.preImgCheckButton.setSelected(getDataObject().isMosPreimaging() == YesNoType.YES);
    }


    /**
     * Check if the offsets are all "sufficiently small" to not require a target, i.e. sqrt(p^2+q^2) <= 1.
     * See REL-1308 for more information.
     */
    private boolean _checkOffsetsSmall() {
        boolean sufficientlySmall = true;
        for (OffsetPos op : _opl.getAllPositions()) {
            double p = op.getXaxis();
            double q = op.getYaxis();
            if (Math.sqrt(p * p + q * q) > 1) {
                sufficientlySmall = false;
                break;
            }
        }
        return sufficientlySmall;
    }

    /**
     * Check the selected nod & shuffle settings and display a warning if needed.
     */
    private void _checkNSValues() {
        _clearWarning(_w.warning2);
        _clearWarning(_w.warning4);

        String msg = null;
        if (_opl != null) {
            boolean hasOiwfsGuideStar = primaryOiwfsStarExists();

            final InstGmosCommon.UseElectronicOffsettingRuling ruling;
            if (hasOiwfsGuideStar) {
                ruling = InstGmosCommon.checkUseElectronicOffsetting(getDataObject(), _opl);
                if (!ruling.allow) msg = ruling.message;
            } else if (!_checkOffsetsSmall())
                msg = "Electronic offsetting requires a GMOS OIWFS to be defined unless the offsets are sufficiently small.";
        }
        if (getDataObject().isUseElectronicOffsetting()) {
            _w.electronicOffsetCheckBox.setSelected(true);
            _w.electronicOffsetCheckBox.setEnabled(true);
            if (msg != null) {
                _setWarning(_w.warning2, msg);
            }
        } else {
            _w.electronicOffsetCheckBox.setSelected(false);
            _w.electronicOffsetCheckBox.setEnabled(msg == null);
        }

        final int ybin = getDataObject().getCcdYBinning().getValue();
        if (ybin != 1) {
            final int rows = getDataObject().getNsDetectorRows();
            if (rows % ybin != 0) {
                _setWarning(_w.warning2, "The number of shuffled rows should be a multiple of the Y-binning (" + ybin + ")");
                _setWarning(_w.warning4, "The number of shuffled rows should be a multiple of the Y-binning (" + ybin + ")");
            }
        }
    }

    /**
     * Check that the dta x offset and binning are agreeable.  Not sure where to put this error.
     */
    private void _checkDTAXAndBinning() {
        _clearWarning(_w.warning3);
        final int ybin = getDataObject().getCcdYBinning().getValue();
        if (ybin != 1) {
            final int dtxoffset = getDataObject().getDtaXOffset().intValue();
            if (dtxoffset % ybin != 0)
                _setWarning(_w.warning3, "The DTA X offset should be a multiple of the Y-binning (" + ybin + ")");
        }
    }

    private boolean primaryOiwfsStarExists() {
        final TargetEnvironment env = getContextTargetEnv();
        return env != null && env.getPrimaryGuideProbeTargets(GmosOiwfsGuideProbe.instance).exists(gt -> gt.getPrimary().isDefined());
    }

    /**
     * Show the given OffsetPos
     */
    protected void showPos(OffsetPos op) {
        _w.xOffset.setValue(op.getXAxisAsString());
        _w.yOffset.setValue(op.getYAxisAsString());
        _w.oiwfsBox.removeActionListener(oiwfsBoxListener);
        _w.oiwfsBox.setValue(_offsetTable.getGuideOption(op));
        _w.oiwfsBox.addActionListener(oiwfsBoxListener);
    }

    /**
     * A table row was selected, so show the selected position.
     *
     * @see TableWidgetWatcher
     */
    public void tableRowSelected(final TableWidget twe, final int rowIndex) {
        if (twe == _offsetTable) {
            if (_curPos != null)
                _curPos.deleteWatcher(this);
            _curPos = _offsetTable.getSelectedPos();
            _curPos.addWatcher(this);
            showPos(_curPos);
        } else if (twe == _customROITable) {
            showROI(_customROITable.getSelectedROI());
        }
    }

    private void showROI(final ROIDescription rDesc) {
        _w.xMin.setValue(rDesc.getXStart());
        _w.yMin.setValue(rDesc.getYStart());
        _w.xRange.setValue(rDesc.getXSize(getDataObject().getCcdXBinning()));
        _w.yRange.setValue(rDesc.getYSize(getDataObject().getCcdYBinning()));
    }

    /**
     * Shows a warning for the first invalid ROI it finds, for too many ROIs or overlapping ROIs.
     * Clears all warnings if there are none.
     */
    private void validateROIs() {
        final DetectorManufacturer ccd = getDataObject().getDetectorManufacturer();
        for (ROIDescription rDesc : _customROITable.getCustomROIs().get()) {
            if (!rDesc.validate(ccd.getXsize(), ccd.getYsize())) {
                _setWarning(_w.warningCustomROI, "ROI is not within valid ranges: " + rDesc);
                return;
            }
        }
        if (_customROITable.getCustomROIs().size() > ccd.getMaxROIs()) {
            _setWarning(_w.warningCustomROI, "There must not be more than " + ccd.getMaxROIs() + " custom ROIs for this detector");
        } else if (ccd == DetectorManufacturer.E2V && _customROITable.getCustomROIs().rowOverlap()) {
            _setWarning(_w.warningCustomROI, "The custom ROIs must not overlap");
        } else if (ccd == DetectorManufacturer.HAMAMATSU && _customROITable.getCustomROIs().pixelOverlap()) {
            _setWarning(_w.warningCustomROI, "The custom ROIs must not overlap");
        } else {
            _clearWarning(_w.warningCustomROI);
        }
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
        return null;
    }

    /**
     * Handle action events (for checkbuttons).
     */
    public void actionPerformed(ActionEvent evt) {
        final Object w = evt.getSource();
        final boolean enabled = OTOptions.areRootAndCurrentObsIfAnyEditable(getProgram(), getContextObservation());

        if (w == _w.focalPlaneBuiltInButton) {
            getDataObject().setFPUnitMode(FPUnitMode.BUILTIN);
            _w.builtinComboBox.setEnabled(enabled);
            _w.focalPlaneMask.setEnabled(false);
            _w.focalPlaneMaskPlotButton.setEnabled(false);
            _updateFPU();
        } else if (w == _w.focalPlaneMaskButton) {
            getDataObject().setFPUnitMode(FPUnitMode.CUSTOM_MASK);
            getDataObject().setFPUnitCustomMask(_w.focalPlaneMask.getText());
            _w.builtinComboBox.setEnabled(false);
            _w.focalPlaneMask.setEnabled(enabled);
            _w.focalPlaneMaskPlotButton.setEnabled(enabled);
            _updateFPU();
        } else if (w == _w.ccdSlowLowButton) {
            getDataObject().setAmpReadMode(AmpReadMode.SLOW);
            getDataObject().setGainChoice(AmpGain.LOW);
            _updateReadoutCharacteristics();
        } else if (w == _w.ccdSlowHighButton) {
            getDataObject().setAmpReadMode(AmpReadMode.SLOW);
            getDataObject().setGainChoice(AmpGain.HIGH);
            _updateReadoutCharacteristics();
        } else if (w == _w.ccdFastLowButton) {
            getDataObject().setAmpReadMode(AmpReadMode.FAST);
            getDataObject().setGainChoice(AmpGain.LOW);
            _updateReadoutCharacteristics();
        } else if (w == _w.ccdFastHighButton) {
            getDataObject().setAmpReadMode(AmpReadMode.FAST);
            getDataObject().setGainChoice(AmpGain.HIGH);
            _updateReadoutCharacteristics();
        } else if (w == _w.ccd3AmpButton) {
            getDataObject().setAmpCount("Three");
            _updateReadoutCharacteristics();
        } else if (w == _w.ccd6AmpButton) {
            getDataObject().setAmpCount("Six");
            _updateReadoutCharacteristics();
        } else if (w == _w.ccd12AmpButton) {
            getDataObject().setAmpCount("Twelve");
            _updateReadoutCharacteristics();
        } else if (w == _w.transFollowXYButton) {
            getDataObject().setStageMode("Follow in XY");
        } else if (w == _w.transFollowXYZButton) {
            getDataObject().setStageMode("Follow in XYZ(focus)");
        } else if (w == _w.transFollowZButton) {
            getDataObject().setStageMode("Follow in Z Only");
        } else if (w == _w.transNoFollowButton) {
            getDataObject().setStageMode("Do Not Follow");
        } else if (w == _w.noROIButton) {
            getDataObject().setBuiltinROI(BuiltinROI.FULL_FRAME);
        } else if (w == _w.ccd2Button) {
            getDataObject().setBuiltinROI(BuiltinROI.CCD2);
        } else if (w == _w.centralSpectrumButton) {
            getDataObject().setBuiltinROI(BuiltinROI.CENTRAL_SPECTRUM);
        } else if (w == _w.centralStampButton) {
            getDataObject().setBuiltinROI(BuiltinROI.CENTRAL_STAMP);
        } else if (w == _w.customButton) {
            getDataObject().setBuiltinROI(BuiltinROI.CUSTOM);
        } else if (w == _w.upLookingButton) {
            getDataObject().setIssPort(IssPort.UP_LOOKING);
        } else if (w == _w.sideLookingButton) {
            getDataObject().setIssPort(IssPort.SIDE_LOOKING);
        } else if (w == _w.newButton) {
            if (_curPos == null)
                _opl.addPosition();
            else
                _opl.addPosition(_opl.getPositionIndex(_curPos) + 1);
        } else if (w == _w.nsCheckButton) {
            if (_w.nsCheckButton.isSelected()) {
                getDataObject().setUseNS(true);
                _removeOffsetNodes();
                _updateNodAndShuffle();
                if (_w.tabbedPane.getSelectedComponent() != _w.nsPanel) {
                    _w.tabbedPane.setSelectedComponent(_w.nsPanel);
                }
            } else {
                getDataObject().setUseNS(false);
                _updateNodAndShuffle();
                if (_w.tabbedPane.getSelectedComponent() == _w.nsPanel) {
                    _w.tabbedPane.setSelectedIndex(0);
                }
            }
        } else if (w == _w.electronicOffsetCheckBox) {
            final boolean useEO = _w.electronicOffsetCheckBox.isSelected();
            getDataObject().setUseElectronicOffsetting(useEO);
            _checkNSValues();
        } else if (w == _w.removeAllButton) {
            _opl.removeAllPositions();
        } else if (w == _w.removeButton) {
            if (_curPos != null)
                _opl.removePosition(_curPos);
        } else if (w == _w.preImgCheckButton) {
            final boolean isPreimg = _w.preImgCheckButton.isSelected();
            getDataObject().setMosPreimaging(isPreimg ? YesNoType.YES : YesNoType.NO);
        } else if (w == _w.customROINewButton) {
            if (_customROITable.getCustomROIs().size() >= getDataObject().getDetectorManufacturer().getMaxROIs()) {
                //raise popup and do NOT add a new custom ROI
                DialogUtil.error("You cannot declare more than 5 custom ROIs for HAMAMATSU CCDs or 4 for E2V CCDs");
            } else {
                final Option<ROIDescription> r = editedROI();
                if (r.nonEmpty()) {
                    _customROITable.addROI(r.get());
                    getDataObject().setCustomROIs(_customROITable.getCustomROIs());
                    validateROIs();
                }
            }
        } else if (w == _w.customROIPasteButton) {
            final DetectorManufacturer ccd = getDataObject().getDetectorManufacturer();
            if (_customROITable.paste(ccd)) {
                getDataObject().setCustomROIs(_customROITable.getCustomROIs());
            }
            validateROIs();
        } else if (w == _w.customROIRemoveButton) {
            _customROITable.removeSelectedROI();
            getDataObject().setCustomROIs(_customROITable.getCustomROIs());
            validateROIs();
        } else if (w == _w.customROIRemoveAllButton) {
            _customROITable.removeAllROIs();
            getDataObject().setCustomROIs(_customROITable.getCustomROIs());
            validateROIs();
        }
    }

    // REL-1056: enable paste of table data
    private void addCustomRoiPasteKeyBinding() {
        KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        _customROITable.getInputMap().put(stroke, "paste");
        AbstractAction action = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                _customROITable.paste(getDataObject().getDetectorManufacturer());
            }
        };
        _customROITable.registerKeyboardAction(action, "Paste", stroke, JComponent.WHEN_FOCUSED);
    }

    private void _updateReadoutCharacteristics() {
        _w.ccdGainLabel.setText(String.format("%.1f", getDataObject().getMeanGain()));
        _w.meanReadNoiseLabel.setText(String.format("%.1f", getDataObject().getMeanReadNoise()));
        _w.ampCountLabel.setText(getDataObject().getAmpCount().toString().toLowerCase());
    }


    /**
     * Called when the value in the DTA-X spinner is changed.
     */
    public void stateChanged(ChangeEvent evt) {
        final int i = (Integer) (_w.transDtaSpinner.getValue());
        getDataObject().setDtaXOffset(DTAX.valueOf(i));
        _checkDTAXAndBinning();
    }


    // Remove any offset nodes in the SP tree (offset nodes are not allowed when using nod & shuffle)
    private void _removeOffsetNodes() {
        try {
            final ISPObservation obsNode = getContextObservation();
            final List l = SPTreeUtil.findSeqComponents(obsNode, SeqRepeatOffset.SP_TYPE);
            for (Object aL : l) {
                final ISPNode sc = (ISPSeqComponent) aL;
                SPTreeEditUtil.removeNode(sc);
            }
        } catch (Exception e) {
            DialogUtil.error(e);
        }
    }

    /**
     * Return the FPUnit for the given index
     */
    protected FPUnit _getFPUnitByIndex(int index) {
        return GmosNorthType.FPUnitNorth.getFPUnitByIndex(index);
    }

    /**
     * Return the Order for the given index
     */
    protected Order _getOrderByIndex(int index) {
        return Order.getOrderByIndex(index);
    }

    /**
     * Return the Binning for the given index
     */
    protected Binning _getBinningByIndex(int index) {
        return Binning.getBinningByIndex(index);
    }


    /**
     * Called when an item in a DropDownListBoxWidget is selected.
     */
    public void dropDownListBoxAction(DropDownListBoxWidget ddlbw, int index, String val) {
        if (ddlbw == _w.builtinComboBox) {
            getDataObject().setFPUnit((Enum) _getFPUnitByIndex(index));
        } else if (ddlbw == _w.xBinComboBox) {
            getDataObject().setCcdXBinning(_getBinningByIndex(index));
        } else if (ddlbw == _w.yBinComboBox) {
            getDataObject().setCcdYBinning(_getBinningByIndex(index));
            // Also check that the dta x offset and binning are agreeable
            _checkNSValues();
            _checkDTAXAndBinning();
        } else if (ddlbw == _w.orderComboBox) {
            getDataObject().setDisperserOrder(_getOrderByIndex(index));
        } else if (ddlbw == _w.customSlitWidthComboBox) {
            getDataObject().setCustomSlitWidth(CustomSlitWidth.getByIndex(index));
        }
    }


    /**
     * A key was pressed in the given TextBoxWidget.
     */
    public void textBoxKeyPress(TextBoxWidget tbwe) {
        super.textBoxKeyPress(tbwe);

        if (tbwe == _w.exposureTime) {
            // parent class sets the exposure time, we just need to
            // update the total integration time
            _w.totalTime.setText(Double.toString(getDataObject().getTotalIntegrationTime()));
        } else if (tbwe == _w.centralWavelength) {
            final double lambda_cent;
            try {
                lambda_cent = Double.parseDouble(tbwe.getValue());
            } catch (NumberFormatException e) {
                return;
            }
            if (_checkWavelength(lambda_cent)) {
                getDataObject().setDisperserLambda(lambda_cent);
            }
        } else if (tbwe == _w.focalPlaneMask) {
            getDataObject().setFPUnitCustomMask(_w.focalPlaneMask.getText());
        } else if (tbwe == _w.xOffset) {
            final double nVal = _w.xOffset.getDoubleValue(0.0);
            if (_curPos == null)
                return;
            _curPos.deleteWatcher(this);
            _curPos.setXY(nVal, _curPos.getYaxis(), getContextIssPort());
            _curPos.addWatcher(this);
            _checkNSValues();
        } else if (tbwe == _w.yOffset) {
            final double nVal = _w.yOffset.getDoubleValue(0.0);
            if (_curPos == null)
                return;
            _curPos.deleteWatcher(this);
            _curPos.setXY(_curPos.getXaxis(), nVal, getContextIssPort());
            _curPos.addWatcher(this);
            _checkNSValues();
        } else if (tbwe == _w.shuffleOffset) {
            final double value;
            try {
                value = Double.parseDouble(tbwe.getValue());
            } catch (NumberFormatException e) {
                return;
            }
            getDataObject().setShuffleOffset(value);
            // the detector rows value was calculated from the shuffle offset value
            _w.detectorRows.setValue(Integer.toString(getDataObject().getNsDetectorRows()));
            _checkNSValues();
        } else if (tbwe == _w.detectorRows) {
            final int value;
            try {
                value = Integer.parseInt(tbwe.getValue());
            } catch (NumberFormatException e) {
                return;
            }
            getDataObject().setNsDetectorRows(value);
            // The shuffle offset value was calculated from the detector rows value
            _w.shuffleOffset.setValue(getDataObject().getShuffleOffsetAsString());
            _checkNSValues();
        } else if (tbwe == _w.numNSCycles) {
            final int value;
            try {
                value = Integer.parseInt(tbwe.getValue());
            } catch (NumberFormatException e) {
                return;
            }
            getDataObject().setNsNumCycles(value);
            // The total integration time  value was calculated from the Nod & Shuffle cycles value
            _w.totalTime.setText(Double.toString(getDataObject().getTotalIntegrationTime()));
        } else if (tbwe == _w.xMin || tbwe == _w.yMin || tbwe == _w.xRange || tbwe == _w.yRange) {
            final int sel = _customROITable.getSelectedRow();
            if (sel >= 0 && sel < _customROITable.getRowCount()) {
                final Option<ROIDescription> newROI = editedROI();
                if (newROI.nonEmpty()) {
                    _customROITable.deleteWatcher(this);
                    _customROITable.updateSelectedROI(newROI.get());
                    getDataObject().setCustomROIs(_customROITable.getCustomROIs());
                    _customROITable.selectRowAt(sel);
                    _customROITable.addWatcher(this);
                    validateROIs();
                }
            }
        }
    }

    /**
     * Gets a new validated ROI based on the values in the text edit components if those values are valid numbers.
     *
     * @return a new ROI based on the parsed numbers if the ROI is valid for the current configuration, None otherwise
     */
    private Option<ROIDescription> editedROI() {
        try {
            final int x = Integer.parseInt(_w.xMin.getValue());
            final int y = Integer.parseInt(_w.yMin.getValue());
            final int w = Integer.parseInt(_w.xRange.getValue()) * getDataObject().getCcdXBinning().getValue();
            final int h = Integer.parseInt(_w.yRange.getValue()) * getDataObject().getCcdYBinning().getValue();

            final ROIDescription newROI = new ROIDescription(x, y, w, h);
            final DetectorManufacturer ccd = getDataObject().getDetectorManufacturer();
            if (newROI.validate(ccd.getXsize(), ccd.getYsize())) {
                return new Some<>(newROI);
            } else {
                _setWarning(_w.warningCustomROI, "ROI is not within valid ranges: " + newROI);
                return Option.empty();
            }

        } catch (NumberFormatException e) {
            _setWarning(_w.warningCustomROI, "Cannot parse number, " + e.getMessage());
            return Option.empty();
        }
    }

    /**
     * A return key was pressed in the given TextBoxWidget.
     */
    public void textBoxAction(TextBoxWidget tbwe) {
    }


    // -- Implement the TelescopePosWatcher interface --

    public void telescopePosUpdate(WatchablePos tp) {
        if (tp != _curPos) {
            // This shouldn't happen ...
            System.out.println(getClass().getName() + ": received a position " +
                    " update for a position other than the current one: " + tp);
            return;
        }
        showPos(_curPos);
    }

    private final OffsetPosListWatcher<OffsetPos> offsetListWatcher = new OffsetPosListWatcher<OffsetPos>() {
        /**
         * The position list has been reset, or changed so much that the client should
         * start from scratch.
         */
        public void posListReset(OffsetPosList<OffsetPos> tpl) {
            // only allow 2 nod offset positions for now
            _w.newButton.setEnabled(tpl.size() < 2);
            _checkNSValues();
        }

        /**
         * A position has been added to the position list.
         */
        public void posListAddedPosition(OffsetPosList<OffsetPos> tpl, List<OffsetPos> newPosList) {
            // only allow 2 nod offset positions for now
            final int n = tpl.size();
            _w.newButton.setEnabled(n < 2);

            // if a third position is added in the TPE, delete it and use the x,y for the second position
            if (n == 3) {
                final OffsetPos opNew = tpl.getPositionAt(2);
                final OffsetPos opOld = tpl.getPositionAt(1);
                tpl.removePosition(opNew);
                if (opOld != null && opNew != null)
                    opOld.setXY(opNew.getXaxis(), opNew.getYaxis(), getContextIssPort());

                // TPE REFACTOR -- this won't work unless we commit first
                final TelescopePosEditor tpe = TpeManager.get();
                if (tpe != null) tpe.reset(getNode());
            }
        }

        public void posListRemovedPosition(OffsetPosList<OffsetPos> tpl, List<OffsetPos> rmPosList) {
            // only allow 2 nod offset positions for now
            _w.newButton.setEnabled(tpl.size() < 2);
            _checkNSValues();
        }

        public void posListPropertyUpdated(OffsetPosList<OffsetPos> tpl, String propertyName, Object oldValue, Object newValue) {
            // ignore, irrelevant
        }

    };

    private final PropertyChangeListener targetListWatcher = evt -> _checkNSValues();


    protected void _setWarning(JLabel label, String s) {
        if (label == null) return;
        label.setText("Warning: " + s);
        label.setVisible(true);
    }

    protected void _clearWarning(JLabel label) {
        if (label == null) return;
        label.setVisible(false);
        label.setText(" ");
    }

    /**
     * Return true if the given value is valid for the central wavelength
     * and display an error or warning message if needed.
     */
    protected boolean _checkWavelength(double lambda_cent) {
        _clearWarning(_w.warning1);
        if (lambda_cent < MIN_LAMBDA_CENT || lambda_cent > MAX_LAMBDA_CENT) {
            _setWarning(_w.warning1, "Central wavelength outside useful range for GMOS ("
                    + MIN_LAMBDA_CENT + ", " + MAX_LAMBDA_CENT + ")");
            return false;
        }

        final Disperser disperser = (GmosCommonType.Disperser) getDataObject().getDisperser();
        if (disperser == DisperserNorth.B1200_G5301 && lambda_cent > 595.) {
            _setWarning(_w.warning1, "Central wavelength too large (max 595), GMOS camera overfilled");
            return true;
        }
        if (disperser == DisperserNorth.R831_G5302 && lambda_cent > 860.) {
            _setWarning(_w.warning1, "Central wavelength too large (max 860), GMOS camera overfilled");
            return true;
        }

        final Filter filter = (GmosCommonType.Filter) getDataObject().getFilter();
        double lambda1_filter = 0., lambda2_filter = 0.;
        boolean found = false;
        for (GMOSInfo aFILTER_INFO : FILTER_INFO) {
            if (filter == aFILTER_INFO.type) {
                lambda1_filter = aFILTER_INFO.lambda1;
                lambda2_filter = aFILTER_INFO.lambda2;
                found = true;
            }
        }

        if (found) {
            for (GMOSInfo aGRATING_INFO : GRATING_INFO) {
                if (disperser == aGRATING_INFO.type) {
                    final double lambda1_grating = aGRATING_INFO.lambda1;
                    final double lambda2_grating = aGRATING_INFO.lambda2;
                    final double minVal = Math.max(lambda1_filter, lambda1_grating);
                    final double maxVal = Math.min(lambda2_filter, lambda2_grating);
                    if (lambda_cent < minVal || lambda_cent > maxVal) {
                        _setWarning(_w.warning1, "Central wavelength outside wavelength range ("
                                + minVal + ", " + maxVal + ")");
                    }
                    break;
                }
            }
        }

        return true;
    }


    /**
     * Check the selected disperser value and display a warning if needed.
     */
    protected void _checkDisperserValue() {
        _clearWarning(_w.warning1);
        final Disperser disperser = (Disperser) getDataObject().getDisperser();
        final Filter filter = (Filter) getDataObject().getFilter();

        if (filter == FilterNorth.g_G0301
                && (disperser == DisperserNorth.R831_G5302
                || disperser == DisperserNorth.R600_G5304
                || disperser == DisperserNorth.R400_G5305
                || disperser == DisperserNorth.R400_G5310)) {
            _setWarning(_w.warning1, "Grating-filter combination gives very small wavelength coverage");
            return;
        }

        _checkWavelength(getDataObject().getDisperserLambda());
    }

}

