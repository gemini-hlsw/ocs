// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: EdNote.java 47001 2012-07-26 19:40:02Z swalker $
//
package jsky.app.ot.editor;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.spModel.obscomp.ProgramNote;
import edu.gemini.spModel.obscomp.SPNote;
import edu.gemini.spModel.obscomp.SchedNote;
import jsky.util.gui.Resources;

import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;


/**
 * This is the editor for Note item.
 */
public final class EdNote extends OtItemEditor<ISPObsComponent, SPNote> {
    private static final boolean showFileAttachments = false;

    // key listener sucks, couldn't find a simple way to get the text reliably
    private static abstract class DocListener implements DocumentListener {
        public void insertUpdate(DocumentEvent documentEvent) {
            handle(getText(documentEvent));
        }
        public void removeUpdate(DocumentEvent documentEvent) {
            handle(getText(documentEvent));
        }
        public void changedUpdate(DocumentEvent documentEvent) {
            handle(getText(documentEvent));
        }
        private String getText(DocumentEvent evt) {
            final Document doc = evt.getDocument();
            try {
                return doc.getText(0, doc.getLength());
            } catch (BadLocationException e) {
                throw new RuntimeException(e);
            }
        }
        protected abstract void handle(String text);
    }

    /** the GUI layout panel */
    private final NoteForm _w;

    /**
     * The constructor initializes the user interface.
     */
    public EdNote() {
        _w = new NoteForm(showFileAttachments);

        _w.title.getDocument().addDocumentListener(new DocListener() {
            protected void handle(String text) {
                getDataObject().setTitle(text);
            }
        });
        _w.note.getDocument().addDocumentListener(new DocListener() {
            protected void handle(String text) {
                getDataObject().setNote(text);
            }
        });

        //noinspection ConstantConditions
//        if (showFileAttachments) {
//
//        }
    }

    /** Return the window containing the editor */
    public JPanel getWindow() {
        return _w;
    }

    public void init() {

        // The title
        final String title = getDataObject().getTitle();
        if (title != null) {
            _w.title.setText(title);
        } else {
            _w.title.setText("");
        }

        // The Note
        final String noteText = getDataObject().getNote();
        if (noteText == null) {
            _w.note.setText("");
        } else {
            _w.note.setText(noteText);
            _w.note.setCaretPosition(0);
        }

        // The icon
        if (getDataObject() instanceof SchedNote) {
            _w.imageLabel.setIcon(Resources.getIcon("post-it-note-blue36.gif"));
        } else if (getDataObject() instanceof ProgramNote) {
            _w.imageLabel.setIcon(Resources.getIcon("post-it-note-red36.gif"));
        } else {
            _w.imageLabel.setIcon(Resources.getIcon("post-it-note36.gif"));
        }
    }

}

