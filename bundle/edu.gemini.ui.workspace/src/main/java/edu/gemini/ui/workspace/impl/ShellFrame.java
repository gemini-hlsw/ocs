package edu.gemini.ui.workspace.impl;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JPanel;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.looks.BorderStyle;
import com.jgoodies.looks.plastic.PlasticLookAndFeel;

@SuppressWarnings("serial")
public class ShellFrame extends JFrame {

    public ShellFrame() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(Borders.DLU4_BORDER);
        panel.setSize(640, 480);

        JMenuBar bar = new JMenuBar();
        bar.putClientProperty(PlasticLookAndFeel.BORDER_STYLE_KEY, BorderStyle.SEPARATOR);
        bar.putClientProperty(PlasticLookAndFeel.IS_3D_KEY, Boolean.TRUE);

        setContentPane(panel);
        setJMenuBar(bar);
    }

}
