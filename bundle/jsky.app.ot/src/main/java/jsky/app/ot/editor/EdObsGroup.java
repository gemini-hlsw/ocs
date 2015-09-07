// Copyright 2001 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: EdObsGroup.java 47001 2012-07-26 19:40:02Z swalker $
//
package jsky.app.ot.editor;

import edu.gemini.pot.sp.ISPGroup;
import edu.gemini.shared.gui.text.AbstractDocumentListener;
import edu.gemini.spModel.obs.ObsTimesService;
import edu.gemini.spModel.obs.plannedtime.PlannedTimeSummary;
import edu.gemini.spModel.obs.plannedtime.PlannedTimeSummaryService;
import edu.gemini.spModel.obscomp.SPGroup;
import edu.gemini.spModel.obscomp.SPGroup.GroupType;
import edu.gemini.spModel.time.ChargeClass;
import edu.gemini.spModel.time.ObsTimeCharges;
import edu.gemini.spModel.time.ObsTimes;
import edu.gemini.spModel.time.TimeAmountFormatter;
import jsky.app.ot.OTOptions;
import jsky.app.ot.editor.type.SpTypeComboBoxModel;
import jsky.app.ot.editor.type.SpTypeComboBoxRenderer;
import jsky.util.gui.DialogUtil;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * This is the editor for the Observation Group item.
 * <p/>
 * Note that the group item is a dummy item that is not really part of the science
 * program tree. The only group related information actually saved in the SP tree
 * is the name of the group for each observation, if it belongs to a group.
 */
public final class EdObsGroup extends OtItemEditor<ISPGroup, SPGroup> {

    private static final Logger LOGGER = Logger.getLogger(EdObsGroup.class.getName());

    /**
     * the GUI layout panel
     */
    private final ObsGroupForm _w;

    /**
     * Constructor
     */
    @SuppressWarnings("unchecked")
    public EdObsGroup() {
        _w = new ObsGroupForm();
        _w.obsGroupName.getDocument().addDocumentListener(new AbstractDocumentListener() {
            public void textChanged(DocumentEvent e, String newText) {
                final SPGroup group = getDataObject();
                if (group != null) group.setGroup(newText.trim());
            }
        });
        _w.libraryIdTextField.getDocument().addDocumentListener(new AbstractDocumentListener() {
            public void textChanged(DocumentEvent e, String newText) {
                final SPGroup group = getDataObject();
                if (group != null) group.setLibraryId(newText.trim());
            }
        });

        // Initialize the combo box.
        final SpTypeComboBoxModel model = new SpTypeComboBoxModel(GroupType.class);
        _w.groupType.setModel(model);
        _w.groupType.setRenderer(new SpTypeComboBoxRenderer());
        _w.groupType.setMaximumRowCount(GroupType.values().length);
        _w.groupType.setSelectedIndex(0);

        _w.groupType.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final SPGroup group = getDataObject(); // copy ref to avoid race condition
                if (group != null) {
                    group.setGroupType((GroupType) _w.groupType.getSelectedItem());
                }
            }
        });

        // override green theme for value labels
        _w.totalPiTime.setForeground(Color.black);
        _w.totalExecTime.setForeground(Color.black);

        if (!OTOptions.isStaffGlobally()) {
            // The total planned time layout is different when off site
            _w.totalPiTimeLabel.setText("Planned");
            _w.plannedLabel.setVisible(false);
            _w.totalExecTimeLabel.setVisible(false);
            _w.totalExecTime.setVisible(false);
            _w.usedLabel.setVisible(false);
        }
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

        final boolean isLibrary = isLibraryProgram();
        _w.libraryIdLabel.setVisible(isLibrary);
        _w.libraryIdTextField.setVisible(isLibrary);

        // Show the library id
        final String libraryId = getDataObject().getLibraryId();
        _w.libraryIdTextField.setText(libraryId);

        // Show the group name
        final String name = getDataObject().getGroup();
        _w.obsGroupName.setText(name);

        // And the group type.
        _w.groupType.getModel().setSelectedItem(getDataObject().getGroupType());

        // The total planed time is updated whenever the sequence or instrument
        // is changed. If the user didn't press Apply before selecting the group
        // node, we may need to give give the events a chance to be handled, to make
        // sure this value is updated before it is displayed.
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                _updateTotalPlannedTime();
                _updateTotalUsedTime();
            }
        });
    }

    // update the total planned time display
    private void _updateTotalPlannedTime() {

        long piTime = 0, execTime = 0;
        try {
            final PlannedTimeSummary pt = PlannedTimeSummaryService.getTotalTime(getNode());
            if (pt != null) {
                piTime = pt.getPiTime();
                execTime = pt.getExecTime();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Exception executing _updateTotalPlannedTime", e);
            piTime = 0;
            execTime = 0;
        }
        _w.totalPiTime.setText(TimeAmountFormatter.getHMSFormat(piTime));
        _w.totalExecTime.setText(TimeAmountFormatter.getHMSFormat(execTime));
    }

    // update the total planned time display
    private void _updateTotalUsedTime() {
        // Remaining Time (total of Allocated Time minus the sum of the Program Time
        // fields in the observations).
        // XXX TODO: Add elapsed and non-charged times?
        try {
            final ISPGroup group = getNode();
            final ObsTimes obsTimes = ObsTimesService.getCorrectedObsTimes(group);
//          String totalTimeStr = "00:00:00";
            String progTimeStr = "00:00:00";
            String partTimeStr = "00:00:00";
//          String nonChargedTimeStr  = "00:00:00";
            if (obsTimes != null) {
//              long totalTime = obsTimes.getTotalTime();
//              totalTimeStr = TimeAmountFormatter.getHMSFormat(totalTime);

                final ObsTimeCharges otc = obsTimes.getTimeCharges();
                final long progTime = otc.getTime(ChargeClass.PROGRAM);
                progTimeStr = TimeAmountFormatter.getHMSFormat(progTime);

                final long partTime = otc.getTime(ChargeClass.PARTNER);
                partTimeStr = TimeAmountFormatter.getHMSFormat(partTime);

//              long nonChargedTime = otc.getTime(ChargeClass.NONCHARGED);
//              nonChargedTimeStr = TimeAmountFormatter.getHMSFormat(nonChargedTime);
            }
            _w.partnerTime.setText(partTimeStr);
            _w.programTime.setText(progTimeStr);
        } catch (Exception e) {
            DialogUtil.error(e);
            _w.partnerTime.setText("00:00:00");
            _w.programTime.setText("00:00:00");
        }
    }

}

