package edu.gemini.qpt.ui.util;

import java.awt.Color;

import javax.swing.JPanel;

/**
 * A trivial translucent JPanel. You can use this as your GlassPane if you want to darken
 * a window to make the user pay attention to a dialog, for example. 
 * @author rnorris
 */
@SuppressWarnings("serial")
public class SmokedGlassPane extends JPanel {

    public SmokedGlassPane() {
        setOpaque(true);
        setBackground(new Color(128, 128, 128, 32));
    }
    
}
