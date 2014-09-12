package edu.gemini.shared.gui.text;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

/**
 * A DocumentListener that makes it easier to listen for text changes on
 * JTextComponents.  Without this class, listening for text change events
 * involves implmenting DocumentListener and deal with the Document interface.
 */
public abstract class AbstractDocumentListener implements DocumentListener {

    /**
     * Receives notification that the text has changed in some way.  The
     * DocumentEvent and the new text are provided.
     */
    public abstract void textChanged(DocumentEvent docEvent, String newText);

    //
    // Strip the new text out of the document and call textChanged().
    //
    private void _handleEvent(DocumentEvent docEvent) {
        Document doc = docEvent.getDocument();
        int len = doc.getLength();
        try {
            textChanged(docEvent, doc.getText(0, len));
        } catch (javax.swing.text.BadLocationException ex) {
            // ignore
        }
    }

    /**
     * Implements a method from the DocumentListener interface.
     */
    public void changedUpdate(DocumentEvent docEvent) {
        _handleEvent(docEvent);
    }

    /**
     * Implements a method from the DocumentListener interface.
     */
    public void insertUpdate(DocumentEvent docEvent) {
        _handleEvent(docEvent);
    }

    /**
     * Implements a method from the DocumentListener interface.
     */
    public void removeUpdate(DocumentEvent docEvent) {
        _handleEvent(docEvent);
    }

}
