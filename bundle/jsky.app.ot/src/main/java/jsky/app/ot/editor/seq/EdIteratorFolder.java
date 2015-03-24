// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: EdIteratorFolder.java 47001 2012-07-26 19:40:02Z swalker $
//
package jsky.app.ot.editor.seq;

import edu.gemini.pot.sp.*;
import edu.gemini.spModel.seqcomp.SeqBase;
import jsky.app.ot.editor.OtItemEditor;
import jsky.util.gui.TextBoxWidget;
import jsky.util.gui.TextBoxWidgetWatcher;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;


/**
 * This is the editor for the "iterator folder" or "Sequence" item. It displays the following tabs:
 * <p>
 *  Sequence: a table into which the iteration sequence of contained
 * iterators is written, and
 * <p>
 * Timeline: which shows the sequence as a timeline.
 * <p>
 * ITC Imaging: All unique imaging configurations and their ITC calculation results (if applicable).
 * <p>
 * ITC Spectroscopy: All unique spectroscopy configurations and their ITC calculation results (if applicable).
 */
public final class EdIteratorFolder
        extends OtItemEditor<ISPSeqComponent, SeqBase>
        implements ChangeListener, jsky.util.gui.TextBoxWidgetWatcher {

    /** the GUI layout panel */
    private final IterFolderForm _w;

    /** ITC result panels */
    private final ItcImagingPanel itcImagingPanel;
    private final ItcSpectroscopyPanel itcSpectroscopyPanel;

    /** The data for the note */
    private final SequenceTab _sequenceTab;
    private final OrigSequenceTab _origSequenceTab;

    /**
     * The constructor initializes the user interface.
     */
    public EdIteratorFolder() {
        _w = new IterFolderForm();

        _w.title.addWatcher(this);

        _sequenceTab = new SequenceTab(this);

        // This will cause a state changed event and the stateChanged()
        // method to get called before the data object has been set.
        _w.tabbedPane.insertTab("Sequence", null, _sequenceTab, null, 0);
        _w.tabbedPane.setSelectedIndex(0);

        _origSequenceTab = new OrigSequenceTab(_w, this);

        _w.tabbedPane.addChangeListener(this);

        itcImagingPanel      = ItcPanel$.MODULE$.forImaging(this);
        itcSpectroscopyPanel = ItcPanel$.MODULE$.forSpectroscopy(this);

    }

    /** Return the window containing the editor */
    public JPanel getWindow() {
        return _w;
    }

    private final PropertyChangeListener obslogUpdater = new PropertyChangeListener() {
        @Override public void propertyChange(PropertyChangeEvent evt) {
            if ((evt.getSource() instanceof ISPObsExecLog) && SPUtil.getDataObjectPropertyName().equals(evt.getPropertyName())) {
                final int index = _w.tabbedPane.getSelectedIndex();
                if (index == 0) _sequenceTab.update();
            }
        }
    };


    /** Set the data object corresponding to this editor. */
    @Override public void init() {
        final ISPObservation obs = getContextObservation();
        // No context observation if this sequence component is in a conflict folder!
        if (obs != null) {
            obs.addCompositeChangeListener(obslogUpdater);
        }

        // show/hide ITC result panels depending on instrument
        final ISPObsComponent instrument = getContextInstrument();
        _w.tabbedPane.remove(itcImagingPanel.peer());
        _w.tabbedPane.remove(itcSpectroscopyPanel.peer());
        if (instrument != null) {
            final SPComponentType type = instrument.getType();
            if (itcImagingPanel.visibleFor(type))      _w.tabbedPane.add("ITC Imaging",      itcImagingPanel.peer());
            if (itcSpectroscopyPanel.visibleFor(type)) _w.tabbedPane.add("ITC Spectroscopy", itcSpectroscopyPanel.peer());
        }


        _origSequenceTab.init(this);

        final String title = getDataObject().getTitle();
        if (title != null) {
            _w.title.setText(title);
        } else {
            _w.title.setText("Sequence");
        }

        stateChanged(null);
    }

    @Override public void cleanup() {
        final ISPObservation obs = getContextObservation();
        if (obs != null) obs.removeCompositeChangeListener(obslogUpdater);
    }


    /**
     * Redefined from the parent class to do nothing, since there are no editable
     * components here.
     */
    protected void updateEnabledState(boolean enabled) {
    }


    /** Called whenever a tab is selected in the tabbed pane */
    public void stateChanged(ChangeEvent e) {
        final int index = _w.tabbedPane.getSelectedIndex();
        switch (index) {
            case 0:
                _sequenceTab.update();
                break;
            case 1:
                _origSequenceTab.update();
                break;
            default:
                // itc panels can be at different indices depending on instrument
                final Component c = _w.tabbedPane.getComponentAt(index);
                if (c == itcImagingPanel.peer())      itcImagingPanel.update();
                if (c == itcSpectroscopyPanel.peer()) itcSpectroscopyPanel.update();
        }
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
     * @see TextBoxWidgetWatcher
     */
    public void textBoxAction(TextBoxWidget tbwe) {
    }
}

