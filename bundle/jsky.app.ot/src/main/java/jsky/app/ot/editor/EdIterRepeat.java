// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: EdIterRepeat.java 47001 2012-07-26 19:40:02Z swalker $
//
package jsky.app.ot.editor;

import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.spModel.seqcomp.SeqRepeat;
import jsky.util.gui.TextBoxWidget;
import jsky.util.gui.TextBoxWidgetWatcher;

import javax.swing.JPanel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/**
 * This is the editor for Repeat iterator component.
 *
 * <p>
 * <em>Note</em> there is a bug in this class in that typing a repeat
 * count directly will not trigger an update to the attribute.
 */
public final class EdIterRepeat extends OtItemEditor<ISPSeqComponent, SeqRepeat>
        implements ChangeListener, TextBoxWidgetWatcher {

    /** the GUI layout panel */
    private final IterRepeatForm _w;

    /**
     * The constructor initializes the user interface.
     */
    public EdIterRepeat() {
        _w = new IterRepeatForm();

        _w.title.addWatcher(this);

        _w.repeatSpinner.setModel(new SpinnerNumberModel(1, 1, null, 1));
        _w.repeatSpinner.addChangeListener(this);
    }

    /** Return the window containing the editor */
    public JPanel getWindow() {
        return _w;
    }

    public void init() {
        String title = getDataObject().getTitle();
        if (title != null) {
            // The observe count is added automatically
            title = title.replaceAll(" [(][0-9]+X[)]\\z", "");
            _w.title.setText(title);
        }

        _w.repeatSpinner.setValue(getDataObject().getStepCount());
    }

    /**
     * Called when the value in the spinner is changed.
     */
    public void stateChanged(ChangeEvent evt) {
        final int i = (Integer) (_w.repeatSpinner.getValue());
        getDataObject().setStepCount(i);
    }


    /**
     * Watch changes to the title text box.
     * @see jsky.util.gui.TextBoxWidgetWatcher
     */
    public void textBoxKeyPress(TextBoxWidget tbwe) {
        getDataObject().setTitle(tbwe.getText().trim());
    }

    /**
     * Text box action, ignore.
     * @see jsky.util.gui.TextBoxWidgetWatcher
     */
    public void textBoxAction(TextBoxWidget tbwe) {
    }
}

