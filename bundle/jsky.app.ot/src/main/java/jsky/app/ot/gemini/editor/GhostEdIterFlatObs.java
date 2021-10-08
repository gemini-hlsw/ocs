package jsky.app.ot.gemini.editor;

import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.spModel.gemini.calunit.CalUnitParams;
import edu.gemini.spModel.gemini.seqcomp.GhostSeqRepeatFlatObs;
import edu.gemini.spModel.obsclass.ObsClass;
import jsky.app.ot.OTOptions;
import jsky.app.ot.editor.OtItemEditor;
import jsky.app.ot.editor.SpinnerEditor;
import jsky.app.ot.editor.type.SpTypeUIUtil;
import jsky.util.gui.DialogUtil;
import jsky.util.gui.TextBoxWidget;
import jsky.util.gui.TextBoxWidgetWatcher;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.util.*;

public class GhostEdIterFlatObs extends OtItemEditor<ISPSeqComponent, GhostSeqRepeatFlatObs> {
    private final GhostIterFlatObsForm form;
    private final SpinnerEditor sped;
    private final SpinnerEditor rcSped;
    private final SpinnerEditor bcSped;

    // If true, ignore action events
    private boolean ignoreActions = false;
    private static final String LAMP_PROPERTY = "Lamp";

    // Listeners
    private final ActionListener lampListener = e -> lampSelected();
    private final ItemListener arcListener = e -> arcSelected();

    /**
     * The constructor initializes the user interface.
     */
    public GhostEdIterFlatObs() {
        form = new GhostIterFlatObsForm();

        form.lamps[0].setText("high");
        form.lamps[0].putClientProperty(LAMP_PROPERTY, CalUnitParams.Lamp.IR_GREY_BODY_HIGH);
        form.lamps[1].setText("low");
        form.lamps[1].putClientProperty(LAMP_PROPERTY, CalUnitParams.Lamp.IR_GREY_BODY_LOW);
        form.lamps[2].setText("5W");
        form.lamps[2].putClientProperty(LAMP_PROPERTY, CalUnitParams.Lamp.QUARTZ_5W);
        form.lamps[3].setText("100W");
        form.lamps[3].putClientProperty(LAMP_PROPERTY, CalUnitParams.Lamp.QUARTZ_100W);

        Arrays.stream(form.lamps).sequential().forEach(b -> b.addActionListener(lampListener));


        final List<CalUnitParams.Lamp> arcLamps = CalUnitParams.Lamp.arcLamps();
        for (int i = 0; i < form.arcs.length; i++) {
            final CalUnitParams.Lamp l = arcLamps.get(i);
            form.arcs[i].putClientProperty(LAMP_PROPERTY, l);
            form.arcs[i].setText(l.displayValue());
            form.arcs[i].addItemListener(arcListener);
        }

        form.shutter.setChoices(CalUnitParams.Shutter.values());

        // Set up the filter editor.
        SpTypeUIUtil.initListBox(form.filter, CalUnitParams.Filter.class,
                e -> getDataObject().setFilter((CalUnitParams.Filter) Objects.requireNonNull(form.filter.getSelectedItem())));

        form.diffuser.setChoices(CalUnitParams.Diffuser.values());
        form.obsClass.setChoices(ObsClass.values());

        form.shutter.addWatcher((ddlbwe, index, val) -> {
            getDataObject().setShutter(CalUnitParams.Shutter.getShutterByIndex(index));
            updateEnabledStates();
        });

        form.diffuser.addWatcher((ddlbwe, index, val) -> {
            getDataObject().setDiffuser(CalUnitParams.Diffuser.getDiffuserByIndex(index));
            updateEnabledStates();
        });

        form.obsClass.addWatcher((ddlbwe, index, val) -> {
            getDataObject().setObsClass(ObsClass.values()[index]);
            updateEnabledStates();
        });

        sped = new SpinnerEditor(form.repeatSpinner, new SpinnerEditor.Functions() {
            @Override public int getValue() {
                return getDataObject().getStepCount();
            }
            @Override public void setValue(int newValue) {
                if (!ignoreActions) getDataObject().setStepCount(newValue);
            }
        });

        form.redExposureTime.addWatcher(new TextBoxWidgetWatcher() {
            @Override
            public void textBoxKeyPress(TextBoxWidget tbwe) {
                getDataObject().setRedExposureTime(tbwe.getDoubleValue(1.0));
            }
        });
        rcSped = new SpinnerEditor(form.redExposureCount, new SpinnerEditor.Functions() {
            @Override public int getValue() { return getDataObject().getRedExposureCount(); }
            @Override public void setValue(int newValue) { getDataObject().setRedExposureCount(newValue); }
        });

        form.blueExposureTime.addWatcher(new TextBoxWidgetWatcher() {
            @Override
            public void textBoxKeyPress(TextBoxWidget tbwe) {
                getDataObject().setBlueExposureTime(tbwe.getDoubleValue(1.0));
            }
        });
        bcSped = new SpinnerEditor(form.blueExposureCount, new SpinnerEditor.Functions() {
            @Override public int getValue() { return getDataObject().getBlueExposureCount(); }
            @Override public void setValue(int newValue) { getDataObject().setBlueExposureCount(newValue); }
        });
    }

    /**
     * Return the window containing the editor
     */
    @Override
    public JPanel getWindow() {
        return form;
    }

    @Override
    public void init() {
        form.redExposureTime.setValue(getDataObject().getRedExposureTime());
        form.blueExposureTime.setValue(getDataObject().getBlueExposureTime());
        update();
        sped.init();
        rcSped.init();
        bcSped.init();
    }

    @Override
    public void cleanup() {
        sped.cleanup();
        rcSped.cleanup();
        bcSped.cleanup();
    }

    // Update the widgets to reflect the model settings
    private void update() {
        ignoreActions = true;
        try {
            showLamps();
            form.shutter.setValue(getDataObject().getShutter());

            // Set the selected item directly on the model to allow for
            // obsolete types to be displayed. If set on the widget itself,
            // it will not be displayed in the combo box.
            form.filter.getModel().setSelectedItem(getDataObject().getFilter());

            form.diffuser.setValue(getDataObject().getDiffuser());
            form.obsClass.setValue(getDataObject().getObsClass());

            updateEnabledStates();
        } catch (Exception e) {
            DialogUtil.error(e);
        }
        ignoreActions = false;
    }

    // Update the lamp display to reflect the data object
    private void showLamps() {
        final Set<CalUnitParams.Lamp> lamps = getDataObject().getLamps();
        for (final JCheckBox b : form.arcs) {
            final CalUnitParams.Lamp l = (CalUnitParams.Lamp) b.getClientProperty(LAMP_PROPERTY);
            b.removeItemListener(arcListener);
            b.setSelected(lamps.contains(l));
            b.addItemListener(arcListener);
        }
        for (final JRadioButton b : form.lamps) {
            final CalUnitParams.Lamp l = (CalUnitParams.Lamp) b.getClientProperty(LAMP_PROPERTY);
            b.removeActionListener(lampListener);
            b.setSelected(lamps.contains(l));
            b.addActionListener(lampListener);
        }
    }

    private boolean isIrGreyBody() {
        final Set<CalUnitParams.Lamp> lamps = getDataObject().getLamps();
        return lamps.contains(CalUnitParams.Lamp.IR_GREY_BODY_HIGH) || lamps.contains(CalUnitParams.Lamp.IR_GREY_BODY_LOW);
    }

    /**
     * Update the enabled states of the widgets based on the current values.
     */
    private void updateEnabledStates() {
        // Disable lamp radio buttons if an arc was selected.
        final boolean isLamp = !getDataObject().isArc();
        for (final JRadioButton lampButton : form.lamps) {
            lampButton.setEnabled(isLamp);
        }

        final boolean editable = OTOptions.areRootAndCurrentObsIfAnyEditable(getProgram(), getContextObservation());
        form.shutter.setEnabled(isIrGreyBody() && editable);
    }

    // Called when one of the lamp radio buttons is selected.
    private void lampSelected() {
        if (ignoreActions) {
            return;
        }
        for (int i = 0; i < form.lamps.length; i++) {
            if (form.lamps[i].isSelected()) {
                final CalUnitParams.Lamp lamp = CalUnitParams.Lamp.flatLamps().get(i);
                getDataObject().setLamp(lamp);
                getDataObject().setDiffuser(
                    (lamp == CalUnitParams.Lamp.QUARTZ_5W) || (lamp == CalUnitParams.Lamp.QUARTZ_100W) ? CalUnitParams.Diffuser.VISIBLE : CalUnitParams.Diffuser.IR
                );
                final boolean b = (lamp == CalUnitParams.Lamp.IR_GREY_BODY_HIGH ||
                        lamp == CalUnitParams.Lamp.IR_GREY_BODY_LOW);
                if (b) {
                    getDataObject().setShutter(CalUnitParams.Shutter.OPEN);
                } else {
                    getDataObject().setShutter(CalUnitParams.Shutter.CLOSED);
                }
                getDataObject().setObsClass(getDataObject().getDefaultObsClass());
                update();
                break;
            }
        }
        update();
    }

    // Called when one of the arc checkboxes changes state
    private void arcSelected() {
        if (ignoreActions) {
            return;
        }
        final List<CalUnitParams.Lamp> arcs = new ArrayList<>(form.arcs.length);
        boolean foundCuAR = false;
        for (int i = 0; i < form.arcs.length; i++) {
            if (form.arcs[i].isSelected()) {
                final CalUnitParams.Lamp lamp = CalUnitParams.Lamp.arcLamps().get(i);
                arcs.add(lamp);
                foundCuAR |= lamp == CalUnitParams.Lamp.CUAR_ARC;
            }
        }
        if (arcs.size() != 0) {
            getDataObject().setLamps(arcs);
            getDataObject().setShutter(CalUnitParams.Shutter.CLOSED);
            getDataObject().setObsClass(getDataObject().getDefaultObsClass());
            getDataObject().setDiffuser(foundCuAR ? CalUnitParams.Diffuser.VISIBLE : CalUnitParams.Diffuser.IR); // See OT-426
        } else {
            lampSelected();
        }
        update();
    }
}
