// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: EdIterObserve.java 47001 2012-07-26 19:40:02Z swalker $
//
package jsky.app.ot.editor;

import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.spModel.obsclass.ObsClass;
import edu.gemini.spModel.seqcomp.SeqRepeatObserve;
import jsky.util.gui.DropDownListBoxWidget;
import jsky.util.gui.DropDownListBoxWidgetWatcher;

import javax.swing.*;


/**
 * This is the editor for Observe iterator component.
 */
public final class EdIterObserve extends OtItemEditor<ISPSeqComponent, SeqRepeatObserve>
        implements DropDownListBoxWidgetWatcher {

    /** the GUI layout panel */
    private final IterObserveForm _w;
    private final SpinnerEditor sped;

    /**
     * The constructor initializes the user interface.
     */
    public EdIterObserve() {
        _w = new IterObserveForm();

        _w.repeatSpinner.setModel(new SpinnerNumberModel(1, 1, null, 1));

        _w.obsClass.setChoices(ObsClass.values());
        _w.obsClass.addWatcher(this);

        sped = new SpinnerEditor(_w.repeatSpinner, new SpinnerEditor.Functions() {
            @Override public int getValue() {
                return getDataObject().getStepCount();
            }

            @Override public void setValue(int newValue) {
                getDataObject().setStepCount(newValue);
            }
        });
    }

    /** Return the window containing the editor */
    public JPanel getWindow() {
        return _w;
    }

    /** Set the data object corresponding to this editor. */
    @Override public void init() {
        _w.obsClass.setValue(getDataObject().getObsClass());
        sped.init();
    }

    @Override public void cleanup() {
        sped.cleanup();
    }

    /**
     * Called when an item in a DropDownListBoxWidget is selected.
     */
    public void dropDownListBoxAction(DropDownListBoxWidget ddlbw, int index, String val) {
        if (ddlbw == _w.obsClass) {
            getDataObject().setObsClass(ObsClass.values()[index]);
        }
    }

}

