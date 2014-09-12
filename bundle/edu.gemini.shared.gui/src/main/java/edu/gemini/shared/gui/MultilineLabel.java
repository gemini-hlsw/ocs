package edu.gemini.shared.gui;

import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.LookAndFeel;

/**
 * A fake multiline label that is created by setting approriate properties
 * on a JTextArea.  It is <em>not</em> a JLabel sublcass and cannot display
 * images.
 *
 * @author David M. Geary (from the Graphic Java Swing Book)
 */
public class MultilineLabel extends JTextArea {

    /**
     * Default constructor.
     */
    public MultilineLabel() {
        _setupLineWrap();
    }

    /**
     * Constructs a new empty multiline label with the specified number of rows
     * and columns.
     */
    public MultilineLabel(int rows, int columns) {
        super(rows, columns);
        _setupLineWrap();
    }

    /**
     * Constructs with the text to display.
     */
    public MultilineLabel(String text) {
        super(text);
        _setupLineWrap();
    }

    /**
     * Constructs a new multiline label with the specified text and number of rows
     * and columns
     */
    public MultilineLabel(String text, int rows, int columns) {
        super(text, rows, columns);
        _setupLineWrap();
    }

    // private method to handle setting up of line wrap
    private void _setupLineWrap() {
        // Turn on wrapping and disable editing and highlighting
        setLineWrap(true);
        setWrapStyleWord(true);
        setHighlighter(null);
        setEditable(false);
    }

    /**
     * Overrides to give the text area a "label" like look and feel.
     */
    public void updateUI() {
        super.updateUI();

        // This call to _setupLineWrap may not be needed, but...
        _setupLineWrap();
        setOpaque(false);

        // Set the text area's border, colors and font to that of a label.
        LookAndFeel.installBorder(this, "Label.border");
        LookAndFeel.installColorsAndFont(this, "Label.background", "Label.foreground", "Label.font");
    }

    /**
     * A main for testing.
     */
    public static void main(String[] argv) {
        MultilineLabel ml = new MultilineLabel(12, 20);
        ml.setText("This is a very long piece of text" + " to see what will happen if I " + " keep typing.  I need the text" + " to wrap.");
        JFrame f = new JFrame("MultilineLabel Test");
        f.getContentPane().add(ml, BorderLayout.CENTER);
        f.pack();
        f.setVisible(true);
    }

}

