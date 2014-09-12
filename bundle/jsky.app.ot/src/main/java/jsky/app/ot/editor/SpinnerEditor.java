package jsky.app.ot.editor;

import edu.gemini.shared.gui.text.AbstractDocumentListener;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

/**
 * Help with tying the tricky spinner class to the various observe data objects.
 */
public final class SpinnerEditor {
    public interface Functions {
        int getValue();
        void setValue(int newValue);
    }

    private final JSpinner spinner;
    private final Functions functions;

    private final DocumentListener docListener = new AbstractDocumentListener() {
        @Override public void textChanged(DocumentEvent docEvent, String newText) {
            try {
                final int repeatCount = Integer.parseInt(newText);
                if (repeatCount > 0) functions.setValue(repeatCount);
            } catch (NumberFormatException ex) {
                // ignore
            }
        }
    };

    public SpinnerEditor(JSpinner spinner, Functions functions) {
        this.spinner   = spinner;
        this.functions = functions;
    }

    private void listenToSpinner() {
        final JTextComponent tc = ((JSpinner.NumberEditor) spinner.getEditor()).getTextField();
        tc.setText(String.valueOf(functions.getValue()));
        tc.getDocument().addDocumentListener(docListener);
    }

    private void deafToSpinner() {
        final JTextComponent tc = ((JSpinner.NumberEditor) spinner.getEditor()).getTextField();
        tc.getDocument().removeDocumentListener(docListener);
    }

    public void init() {
        deafToSpinner();
        spinner.setValue(functions.getValue());
        listenToSpinner();
    }

    public void cleanup() {
        deafToSpinner();
    }
}
