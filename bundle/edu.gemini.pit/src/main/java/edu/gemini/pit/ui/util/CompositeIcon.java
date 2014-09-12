package edu.gemini.pit.ui.util;

import javax.swing.Icon;
import java.awt.Component;
import java.awt.Graphics;

/**
 * An Icon that composes a base image with an overlay that is drawn in the lower-right hand
 * corner. The overlay will be clipped if it is larger than the primary.
 * @author rnorris
 */
public class CompositeIcon implements Icon {
	
	private final Icon primary, overlay;

	public CompositeIcon(Icon primary, Icon overlay) {
		this.primary = primary;
		this.overlay = overlay;
	}

	public void paintIcon(Component c, Graphics g, int x, int y) {
		primary.paintIcon(c, g, x, y);
		overlay.paintIcon(c, g,
                x + primary.getIconWidth() - overlay.getIconWidth(),
				y + primary.getIconHeight() - overlay.getIconHeight() - 1);
	}

	public int getIconWidth() {
		return primary.getIconWidth();
	}

	public int getIconHeight() { 
		return primary.getIconHeight();
	}
	
	
}
