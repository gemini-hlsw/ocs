package jsky.app.ot.gemini.obslog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.GeneralPath;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;

/**
 * A Container with an header and indented client area, which collapses when the header
 * is clicked. Default client area layout is a BoxLayout. The add/remove methods and layout 
 * property are delegated to the client area, so you can generally use this like any container.
 * @author rnorris
 * @version $Id: CollapsableContainer.java 6260 2005-06-01 14:20:49Z rnorris $
 */
public class CollapsableContainer extends Container {
		
	private static final int HEADER_INDENT = 16;
	
	private final Container root;		// Top-level container that needs to repaint on collapse.
	private final JComponent client;	// Component that gets removed on collapse

	private boolean expanded = false;

	/**
	 * Constructs a new CollapsableContainer with the specified root container, header
	 * text, and expanded state. The root container is a pointer to the top-level container
	 * that expects to be revalidated when the expanded state changes. In most cases this
	 * will be a JScrollPane that contains the top-level CollapsableContainer.
	 * @param root the topmost container that needs to be revalidated when the expanded state changes
	 * @param text header text
	 * @param expanded initial expanded state
	 */
	public CollapsableContainer(final Container root, final String text, boolean expanded) {		
		this.root = root;

		// The client is a box with a border on the right.
		client = new Box(BoxLayout.Y_AXIS);
		client.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));

		// Lay out the container
		super.setLayout(new BorderLayout());		
		super.add(new Header(text), BorderLayout.NORTH);
		super.add(Box.createHorizontalStrut(HEADER_INDENT), BorderLayout.WEST);

		// Initialize expanded state if needed.
		if (expanded)
			setExpanded(true);
				 
	}
						
// --Commented out by Inspection START (4/18/13 9:19 AM):
//	public boolean isExpanded() {
//		return expanded;
//	}
// --Commented out by Inspection STOP (4/18/13 9:19 AM)

	protected void setExpanded(boolean expanded) {
		this.expanded = expanded;
		if (expanded) {
			super.add(client, BorderLayout.CENTER);
		} else {
			super.remove(client);
		}
		root.validate(); // tell the top-level container that there was a structural change
	}

	// Components want to grow. This lets it grow horizontally but fixes the height
	public Dimension getMaximumSize() {
		final Dimension d = super.getMaximumSize();
		return new Dimension(d.width, getMinimumSize().height);
	}

	/*
	 * DELEGATE METHODS FROM HERE DOWN. ALL DELEGATE TO THE CLIENT AREA.
	 */
	
	public Component add(Component arg0) {
		return client.add(arg0);
	}

	public Component add(Component arg0, int arg1) {
		return client.add(arg0, arg1);
	}

	public void add(Component arg0, Object arg1) {
		client.add(arg0, arg1);
	}

	public void add(Component arg0, Object arg1, int arg2) {
		client.add(arg0, arg1, arg2);
	}

	public void remove(Component arg0) {
		client.remove(arg0);
	}

	public void removeAll() {
		client.removeAll();
	}

	public void setLayout(LayoutManager arg0) {
		client.setLayout(arg0);
	}
	
	public LayoutManager getLayout() {
		return client.getLayout();
	}

// RCN: this causes some crazy problems with popup menus being displayed at the same
// time as the CollapsableComponent for some reason. So don't do this.
//
//	public Component[] getComponents() {
//		return client.getComponents();
//	}
	
	// Header is a left-aligned custom-pained button
	private class Header extends JComponent {
		
		private final String text;
		
		private Header(String text) {
			this.text = text;
			setLayout(new BorderLayout());
			add(new HeaderButton(), BorderLayout.WEST);
		}

		private class HeaderButton extends JButton implements ActionListener {
			
			private HeaderButton() {
				setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				setFocusPainted(false);
				setText(text);
				setFont(getFont().deriveFont(Font.BOLD));
				setForeground(Color.DARK_GRAY);
				setBorder(BorderFactory.createEmptyBorder(1, 16, 2, 0));
				addActionListener(this);
			}

			public void actionPerformed(ActionEvent ae) {
				setExpanded(!expanded);
			}				
			
			public void paint(Graphics g) {
				super.paint(g);
				final Graphics2D g2d = (Graphics2D) g;
				g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);				
				g2d.setColor(Color.LIGHT_GRAY);
				final GeneralPath gp = new GeneralPath();
				if (expanded) {
					gp.moveTo(0, 3.5f);
					gp.lineTo(10, 3.5f);
					gp.lineTo(5, 12.5f);
					gp.closePath();
				} else {
					gp.moveTo(1, 2.5f);
					gp.lineTo(10, 7.5f);
					gp.lineTo(1, 12.5f);
					gp.closePath();
				}
				g2d.fill(gp);									
			}
		}

	}
	
}



