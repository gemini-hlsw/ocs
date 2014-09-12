// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: EdIterBiasObs.java 47001 2012-07-26 19:40:02Z swalker $
//
package jsky.app.ot.editor;

import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.spModel.obsclass.ObsClass;
import edu.gemini.spModel.seqcomp.SeqRepeatBiasObs;
import jsky.util.gui.DropDownListBoxWidget;
import jsky.util.gui.DropDownListBoxWidgetWatcher;
import jsky.util.gui.TextBoxWidget;
import jsky.util.gui.TextBoxWidgetWatcher;

import javax.swing.*;

public final class EdIterBiasObs extends OtItemEditor<ISPSeqComponent, SeqRepeatBiasObs>
        implements TextBoxWidgetWatcher, DropDownListBoxWidgetWatcher {

    private final IterBiasObsForm _w;
    private final SpinnerEditor sped;

    public EdIterBiasObs() {
        _w = new IterBiasObsForm();

        _w.repeatSpinner.setModel(new SpinnerNumberModel(1, 1, null, 1));
        _w.obsClass.setChoices(ObsClass.values());
        _w.obsClass.addWatcher(this);
        _w.coadds.addWatcher(this);

        sped = new SpinnerEditor(_w.repeatSpinner, new SpinnerEditor.Functions() {
            @Override public int getValue() {
                return getDataObject().getStepCount();
            }
            @Override public void setValue(int newValue) {
                getDataObject().setStepCount(newValue);
            }
        });
    }

    public JPanel getWindow() {
        return _w;
    }

    @Override public void init() {
        _w.coadds.setValue(getDataObject().getCoaddsCount());
        _w.obsClass.setValue(getDataObject().getObsClass());
        sped.init();
    }

    @Override public void cleanup() {
        sped.cleanup();
    }

    public void textBoxKeyPress(TextBoxWidget tbwe) {
        if (tbwe == _w.coadds)
            getDataObject().setCoaddsCount(tbwe.getIntegerValue(1));
    }

    public void textBoxAction(TextBoxWidget tbwe) {
    }

    public void dropDownListBoxAction(DropDownListBoxWidget ddlbw, int index, String val) {
        if (ddlbw == _w.obsClass)
            getDataObject().setObsClass(ObsClass.values()[index]);
    }

}

