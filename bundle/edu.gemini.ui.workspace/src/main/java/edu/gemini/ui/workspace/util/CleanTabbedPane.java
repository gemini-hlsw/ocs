package edu.gemini.ui.workspace.util;

import java.awt.*;
import java.util.logging.Logger;

import javax.swing.*;
import javax.swing.border.Border;

import com.jgoodies.looks.Options;

import edu.gemini.ui.workspace.impl.Shell;

@SuppressWarnings("serial")
public class CleanTabbedPane extends JTabbedPane {

	private static final Logger LOGGER = Logger.getLogger(CleanTabbedPane.class.getName());
	private final Shell shell;
	
	public CleanTabbedPane(Shell shell, int tabPlacement) {
		super(tabPlacement);
		this.shell = shell;
		setFocusable(false);
//        Border b = null;
//        Border b = BorderFactory.createLineBorder(Color.BLACK);
        Border b = new SimpleInternalFrame.ShadowBorder();
		setBorder(b);
		putClientProperty(Options.EMBEDDED_TABS_KEY, Boolean.TRUE);
	}
	
	@Override
	public void addTab(String title, Component comp) {
		if (comp instanceof SimpleInternalFrame)
			((SimpleInternalFrame) comp).setBorder(BorderFactory.createEmptyBorder());
		super.addTab(title, comp);
	}
	
	@Override
	public void setSelectedIndex(final int next) {
		int prev = getSelectedIndex();
		if (prev != -1 && prev != next) {
			
			// If the currently selected tab contains the focused component (or its
			// parent), focus will bounce when we change tabs.
			Component current = getComponent(prev);
			Component focus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getPermanentFocusOwner();
			while (focus != current && focus != null)
				focus = focus.getParent();
			
			Component nextComponent = getComponentAt(next);
			if (focus == current) {
				LOGGER.fine("Changing from " + prev + " to " + next + "; guarding against focus bounce.");		
				shell.waitForFocus(nextComponent);
				
				// We also want to pre-select the view's border so it doesn't flicker,
				// if it's a SimpleInternalFrame.
				if (nextComponent instanceof SimpleInternalFrame)
					((SimpleInternalFrame) nextComponent).setSelected(true);
				
			}
			
		}		
		super.setSelectedIndex(next);

// Can we influence the way this stuff is painted? Probably not with internal tabs
// doesn't work, rats (NPE)
//        getTabComponentAt(next).setBackground(Color.RED);

	}
	
}
