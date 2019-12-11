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

    public GhostEdIterDarkObs() {
        form = new GhostIterDarkObsForm();

        form.obsClass.addWatcher((ddlbw, index, val) -> getDataObject().setObsClass(ObsClass.values()[index]));

        sped = new SpinnerEditor(form.repeatSpinner, new SpinnerEditor.Functions() {
            @Override public int getValue() { return getDataObject().getStepCount(); }
            @Override public void setValue(int newValue) { getDataObject().setStepCount(newValue);}
        });

        form.redExposureTime.addWatcher(new TextBoxWidgetWatcher() {
            @Override
            public void textBoxKeyPress(TextBoxWidget tbwe) {
                getDataObject().setRedExposureTime(tbwe.getDoubleValue(1.0));
            }
        });

        form.blueExposureTime.addWatcher(new TextBoxWidgetWatcher() {
            @Override
            public void textBoxKeyPress(TextBoxWidget tbwe) {
                getDataObject().setBlueExposureTime(tbwe.getDoubleValue(1.0));
            }
        });
    }

    @Override
    public JPanel getWindow() {
        return form;
    }

    @Override
    public void cleanup() {
        sped.cleanup();
    }
}
