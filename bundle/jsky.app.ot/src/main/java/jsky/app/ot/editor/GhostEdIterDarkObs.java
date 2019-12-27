package jsky.app.ot.editor;

import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.spModel.obsclass.ObsClass;
import edu.gemini.spModel.seqcomp.GhostSeqRepeatDarkObs;
import jsky.util.gui.TextBoxWidget;
import jsky.util.gui.TextBoxWidgetWatcher;

import javax.swing.*;

public class GhostEdIterDarkObs extends OtItemEditor<ISPSeqComponent, GhostSeqRepeatDarkObs> {
    private final GhostIterDarkObsForm form;
    private final SpinnerEditor sped;
    private final SpinnerEditor rcSped;
    private final SpinnerEditor bcSped;

    public GhostEdIterDarkObs() {
        form = new GhostIterDarkObsForm();

        form.obsClass.setChoices(ObsClass.values());
        form.obsClass.addWatcher((ddlbw, index, val) -> getDataObject().setObsClass(ObsClass.values()[index]));

        sped = new SpinnerEditor(form.repeatSpinner, new SpinnerEditor.Functions() {
            @Override public int getValue() { return getDataObject().getStepCount(); }
            @Override public void setValue(int newValue) { getDataObject().setStepCount(newValue); }
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

    @Override
    public JPanel getWindow() {
        return form;
    }

    @Override
    public void init() {
        form.redExposureTime.setValue(getDataObject().getRedExposureTime());
        form.blueExposureTime.setValue(getDataObject().getBlueExposureTime());
        form.obsClass.setValue(getDataObject().getObsClass());
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
}
