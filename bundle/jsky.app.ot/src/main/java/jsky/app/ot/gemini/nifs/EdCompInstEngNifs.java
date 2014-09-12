/**
 * $Id: EdCompInstEngNifs.java 7063 2006-05-25 16:17:10Z anunez $
 */

package jsky.app.ot.gemini.nifs;

import edu.gemini.spModel.gemini.nifs.InstEngNifs;
import edu.gemini.spModel.gemini.nifs.NIFSParams.EngReadMode;
import edu.gemini.spModel.type.SpTypeUtil;
import jsky.app.ot.gemini.editor.EdCompInstEngBase;
import jsky.util.gui.DropDownListBoxWidget;
import jsky.util.gui.DropDownListBoxWidgetWatcher;
import jsky.util.gui.TextBoxWidget;

import javax.swing.JPanel;

public final class EdCompInstEngNifs extends EdCompInstEngBase<InstEngNifs>
        implements jsky.util.gui.TextBoxWidgetWatcher, DropDownListBoxWidgetWatcher {

    // The GUI layout panel
    private final EngNifsForm _w;

    /**
     * The constructor initializes the user interface.
     */
    public EdCompInstEngNifs() {
        _w = new EngNifsForm();
        final String[] engReadModes = SpTypeUtil.getFormattedDisplayValueAndDescriptions(EngReadMode.class);
        _w.engReadMode.setChoices(engReadModes);
        _w.engReadMode.addWatcher(this);
        _w.numberOfSamples.addWatcher(this);
        _w.period.addWatcher(this);
        _w.numberOfPeriods.addWatcher(this);
        _w.numberOfResets.addWatcher(this);
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
        _w.engReadMode.setValue(getDataObject().getEngineeringReadMode().ordinal());
        _w.numberOfSamples.setValue(getDataObject().getNumberOfSamples());
        _w.period.setValue(getDataObject().getPeriod());
        _w.numberOfPeriods.setValue(getDataObject().getNumberOfPeriods());
        _w.numberOfResets.setValue(getDataObject().getNumberOfResets());
    }

    public void textBoxKeyPress(TextBoxWidget tbw) {
        if (_w.numberOfSamples == tbw) {
            getDataObject().setNumberOfSamples(tbw.getIntegerValue(getDataObject().getNumberOfSamples()));
        } else if (_w.period == tbw) {
            getDataObject().setPeriod(tbw.getIntegerValue(getDataObject().getPeriod()));
        } else if (_w.numberOfPeriods == tbw) {
            getDataObject().setNumberOfPeriods(tbw.getIntegerValue(getDataObject().getNumberOfPeriods()));
        } else if (_w.numberOfResets == tbw) {
            getDataObject().setNumberOfResets(tbw.getIntegerValue(getDataObject().getNumberOfResets()));
        }
    }

    public void textBoxAction(TextBoxWidget tbw) {
    }

    public void dropDownListBoxAction(DropDownListBoxWidget ddlbw, int index, String val) {
        if (ddlbw == _w.engReadMode) {
            final EngReadMode engReadMode = EngReadMode.getReadModeByIndex(index);
            getDataObject().setEngineeringReadMode(engReadMode);
        }
    }
}
