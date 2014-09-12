package edu.gemini.pit.ui.util;

import javax.swing.*;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Insets;

@SuppressWarnings("serial")
public class SimpleToolbar extends JPanel {

	public SimpleToolbar() {
		super(new FlowLayout(FlowLayout.LEFT));
		setBackground(new Color(255, 255, 224));
		setOpaque(true);
		setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
	}
	
	public static class IconButton extends JButton {
		public IconButton(Icon icon, Icon disabled) {
			super(icon);
			setDisabledIcon(disabled);
			setBorder(null);
			setOpaque(false);
			setFocusable(false);
			setMargin(new Insets(0, 0, 0, 0));
		}
	}

	public static class StaticText extends JLabel {
		public StaticText(String text) {
			super(text);
			setForeground(Color.LIGHT_GRAY);
		}
	}
	
}
