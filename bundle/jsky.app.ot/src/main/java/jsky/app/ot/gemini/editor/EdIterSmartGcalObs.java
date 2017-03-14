package jsky.app.ot.gemini.editor;

import edu.gemini.pot.client.SPDB;
import edu.gemini.pot.sp.*;
import edu.gemini.spModel.config.ConfigBridge;
import edu.gemini.spModel.config.MetaDataConfig;
import edu.gemini.spModel.config.map.ConfigValMapInstances;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.ConfigSequence;
import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.gemini.calunit.CalUnitParams;
import edu.gemini.spModel.gemini.seqcomp.SeqRepeatFlatObs;
import edu.gemini.spModel.gemini.seqcomp.SeqRepeatSmartGcalObs;
import edu.gemini.spModel.obsclass.ObsClass;
import edu.gemini.spModel.seqcomp.SeqRepeat;
import jsky.app.ot.editor.OtItemEditor;
import jsky.app.ot.editor.seq.SequenceTableUI;
import jsky.app.ot.nsp.SPTreeEditUtil;
import jsky.util.gui.DialogUtil;

import javax.swing.*;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is the editor for the Flat Observation iterator.
 */
public final class EdIterSmartGcalObs extends OtItemEditor<ISPSeqComponent, SeqRepeatSmartGcalObs> {
    private static final Logger LOG = Logger.getLogger(DialogUtil.class.getName());

    private static final String AUTOMATIC_OBS_CLASS = "Automatic";

    private static final Vector<Object> obsClassValues = new Stack<Object>() {{
        // add an automatic option to the obs class values in the obs class combo box
        for (ObsClass o : ObsClass.values()) {
            add(o);
        }
        add(AUTOMATIC_OBS_CLASS);
    }};

    private static final java.util.List<ItemKey> CALIBRATION_KEYS = new ArrayList<ItemKey>() {{
        add(new ItemKey("calibration:*"));
    }};

    private static class UI extends JPanel {
        final SequenceTableUI seq = new SequenceTableUI();
        final JComboBox<Object
                > obsClass = new JComboBox<>(obsClassValues);
        final JCheckBox showAll = new JCheckBox("Show full execution sequence") {{
            addActionListener(evt -> updateSequence());
            setToolTipText("Select to show all steps, not just those for the selected calibration node");
            setFocusable(false);
        }};
        final JButton convertToManual = new JButton("Configure Manually") {{
            setToolTipText("Replace this automatic calibration node with manually configurable calibration nodes.");
        }};

        UI() {
            super(new GridBagLayout());

            setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 1));

            add(convertToManual, new GridBagConstraints() {{
                gridx = 0;
                gridy = 0;
                insets = new Insets(0, 0, 0, 10);
            }});

            add(new JPanel(), new GridBagConstraints() {{
                gridx = 1;
                gridy = 0;
                fill = HORIZONTAL;
                weightx = 1.0;
            }});

            add(new JLabel("Class"), new GridBagConstraints() {{
                gridx = 2;
                gridy = 0;
                insets = new Insets(0, 0, 0, 10);
            }});

            add(obsClass, new GridBagConstraints() {{
                gridx = 3;
                gridy = 0;
            }});

            add(seq, new GridBagConstraints() {{
                gridx = 0;
                gridy = 1;
                gridwidth = 4;
                weightx = 1.0;
                weighty = 1.0;
                fill = BOTH;
                insets = new Insets(10, 0, 0, 0);
            }});

            add(showAll, new GridBagConstraints() {{
                gridx = 0;
                gridy = 2;
                gridwidth = 3;
                fill = HORIZONTAL;
                insets = new Insets(5, 0, 0, 0);
            }});
        }

        boolean showAllSteps() {
            return showAll.isSelected();
        }

        void updateSequence() {
            seq.update(showAllSteps(), CALIBRATION_KEYS);
        }
    }

    // the GUI layout panel
    private final UI ui = new UI();

    private final ActionListener obsClassActionListener = evt -> {
        final SeqRepeatSmartGcalObs o = getDataObject();
        if (o == null) return;
        final Object oc = ui.obsClass.getSelectedItem();
        // check for the currently selected obs class - if it is not a obs class enum object
        // "Automatic" (represented by a string) was selected in the combo box
        if (oc instanceof ObsClass) {
            // manual obs class mode
            o.setObsClass((ObsClass) oc);
        } else {
            // automatic mode
            o.setObsClass(null);
        }
        //-- in order to update the GUI we need to set the data object (i.e. "save" the change)
        getNode().setDataObject(o);
        //-- done
        ui.updateSequence();
    };


    @SuppressWarnings("unchecked")
    private SeqRepeatFlatObs createFlatObsFromConfig(final Config c, final int observeCnt) {
        final SeqRepeatFlatObs newDataObject = new SeqRepeatFlatObs();
        final Set<CalUnitParams.Lamp> lamps = (Set<CalUnitParams.Lamp>) c.getItemValue(new ItemKey("calibration:lamp"));
        newDataObject.setDiffuser((CalUnitParams.Diffuser) c.getItemValue(new ItemKey("calibration:diffuser")));
        newDataObject.setFilter((CalUnitParams.Filter) c.getItemValue(new ItemKey("calibration:filter")));
        newDataObject.setShutter((CalUnitParams.Shutter) c.getItemValue(new ItemKey("calibration:shutter")));
        newDataObject.setExposureTime((Double) c.getItemValue(new ItemKey("calibration:exposureTime")));
        newDataObject.setCoaddsCount((Integer) c.getItemValue(new ItemKey("calibration:coadds")));
        newDataObject.setLamps(new ArrayList<>(lamps));
        newDataObject.setObsClass(ObsClass.parseType((String) c.getItemValue(new ItemKey("observe:class"))));
        newDataObject.setStepCount(observeCnt);
        return newDataObject;
    }

    private boolean compareConfig(final Config c1, final Config c2, final String parentName) {
        for (ItemKey key : c1.getKeys()) {
            final ItemKey parent = key.getParent();
            if (parent == null || !parent.getName().equals(parentName)) {
                continue;
            }
            // Read mode for GNIRS can be changed inside of a series of calibration images that belong
            // to the same calibration sequence -> that means that a change in the read mode does not
            // allow us to detect the boundaries between instrument sequence steps therefore we ignore it.
            // (I know this is a hack, sorry. I think there has to be a better way to do that in the future.)
            if (key.getName().equals("readMode")) {
                continue;
            }
            if (!c2.containsItem(key)) {
                return false;
            }
            if (!c1.getItemValue(key).equals(c2.getItemValue(key))) {
                return false;
            }
        }
        return true;
    }

    /**
     * The constructor initializes the user interface.
     */
    public EdIterSmartGcalObs() {
        ui.obsClass.addActionListener(obsClassActionListener);
        final ActionListener convertToManualActionListener = e -> {
            try {
                // get sequence ...
                final ISPObservation obs = getContextObservation();
                final ConfigSequence seq = ConfigBridge.extractSequence(obs, null, ConfigValMapInstances.IDENTITY_MAP, false)
                        .filter(new MetaDataConfig.NodeKeySequencePredicate(getNode().getNodeKey()));

                // get some general information about current node, parent etc
                final ISPNode smartNode = getNode();
                final ISPSeqComponent parent = (ISPSeqComponent) smartNode.getParent();
                int position = parent.getChildren().indexOf(smartNode);

                // check if user really wants to do that
                if (JOptionPane.showConfirmDialog(
                        null,
                        "Replace this automatically configured calibration with a manually configurable calibration.\nThis cannot be undone!",
                        "Configure Manually",
                        JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
                    return;
                }

                // UX-637: if no calibrations are available replace smart node with a default manual flat/arc
                if (seq.isEmpty() || (boolean) seq.getStep(0).getItemValue(new ItemKey("smartgcal:maperror"))) {
                    final ISPSeqComponent child = SPDB.get().getFactory().createSeqComponent(parent.getProgram(), SeqRepeatFlatObs.SP_TYPE, null);
                    child.setDataObject(new SeqRepeatFlatObs());
                    parent.addSeqComponent(position, child);
                    SPTreeEditUtil.removeNode(smartNode);
                    return;
                }

                // translate sequence of calibrations into corresponding manual nodes
                // (one dumb calibration for each step in smart calibration)
                // check for iterator, if this is repeated, then we need only the flats/arcs of the first iteration
                int lastStep = seq.size();
                if (parent.getDataObject() instanceof SeqRepeat) {
                    lastStep /= ((ISPSeqObject) parent.getDataObject()).getStepCount();
                }
                final SPComponentType type = SeqRepeatFlatObs.SP_TYPE;
                int observeCnt = 1;
                for (int i = 0; i < lastStep; i++) {
                    if (i == lastStep - 1 || !compareConfig(seq.getStep(i), seq.getStep(i + 1), "calibration")) {
                        final SeqRepeatFlatObs newDataObject = createFlatObsFromConfig(seq.getStep(i), observeCnt);
                        final ISPSeqComponent child = SPDB.get().getFactory().createSeqComponent(parent.getProgram(), type, null);
                        child.setDataObject(newDataObject);
                        parent.addSeqComponent(position++, child);
                        observeCnt = 1;

                        // if we are in an instrument iterator (instrument config changes) we only take
                        // the first one into account!
                        if (i < lastStep - 1 && !compareConfig(seq.getStep(i), seq.getStep(i + 1), "instrument")) {
                            break;
                        }
                    } else {
                        observeCnt++;
                    }
                }

                // remove the original smart node
                SPTreeEditUtil.removeNode(smartNode);

            } catch (final Exception ex) {
                LOG.log(Level.WARNING, "could not convert from smart to manual calibrations", ex);
            }
        };
        ui.convertToManual.addActionListener(convertToManualActionListener);
    }

    /**
     * Return the window containing the editor
     */
    @Override
    public JPanel getWindow() {
        return ui;
    }

    @Override
    public void init() {
        ui.seq.init(this);
        ui.obsClass.removeActionListener(obsClassActionListener);
        if (getDataObject().getObsClass() == null) {
            ui.obsClass.setSelectedItem(AUTOMATIC_OBS_CLASS);
        } else {
            ui.obsClass.setSelectedItem(getDataObject().getObsClass());
        }
        ui.obsClass.addActionListener(obsClassActionListener);
        ui.updateSequence();
    }

}
