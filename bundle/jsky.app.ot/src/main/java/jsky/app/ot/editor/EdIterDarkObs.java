// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: EdIterDarkObs.java 47001 2012-07-26 19:40:02Z swalker $
//
package jsky.app.ot.editor;

import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.shared.gui.text.AbstractDocumentListener;
import edu.gemini.spModel.obsclass.ObsClass;
import edu.gemini.spModel.seqcomp.SeqRepeatDarkObs;
import jsky.util.gui.DropDownListBoxWidget;
import jsky.util.gui.DropDownListBoxWidgetWatcher;
import jsky.util.gui.TextBoxWidget;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.text.JTextComponent;


/**
 * This is the editor for Dark Observe iterator component.
 */
public final class EdIterDarkObs extends OtItemEditor<ISPSeqComponent, SeqRepeatDarkObs>
        implements jsky.util.gui.TextBoxWidgetWatcher, DropDownListBoxWidgetWatcher {

    /**
     * the GUI layout panel
     */
    private final IterDarkObsForm _w;
    private final SpinnerEditor sped;

    /**
     * The constructor initializes the user interface.
     */
    public EdIterDarkObs() {
        _w = new IterDarkObsForm();

        _w.repeatSpinner.setModel(new SpinnerNumberModel(1, 1, null, 1));

        _w.obsClass.setChoices(ObsClass.values());
        _w.obsClass.addWatcher(this);

        // Exposure time
        _w.exposureTime.addWatcher(this);

        // Coadds
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

    /**
     * Return the window containing the editor
     */
    public JPanel getWindow() {
        return _w;
    }


    @Override public void init() {
        _w.exposureTime.setValue(getDataObject().getExposureTime());
        _w.coadds.setValue(getDataObject().getCoaddsCount());
        _w.obsClass.setValue(getDataObject().getObsClass());
        sped.init();
    }

    @Override public void cleanup() {
        sped.cleanup();
    }


    /**
     * Watch changes to text boxes
     */
    public void textBoxKeyPress(TextBoxWidget tbwe) {
        if (tbwe == _w.exposureTime) {
            getDataObject().setExposureTime(tbwe.getDoubleValue(1.));
        } else if (tbwe == _w.coadds) {
            getDataObject().setCoaddsCount(tbwe.getIntegerValue(1));
        }
    }

    /**
     * Text box action.
     */
    public void textBoxAction(TextBoxWidget tbwe) {
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

