package edu.gemini.ui.workspace.impl;

import java.awt.Component;
import java.awt.Container;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;

import edu.gemini.ui.workspace.BooleanStateAction;
import edu.gemini.ui.workspace.IActionManager;
import edu.gemini.ui.workspace.util.RetargetAction;

public class ActionManager implements IActionManager {

	private static final Logger LOGGER = Logger.getLogger(ActionManager.class.getName());
	private static final String KEY_ID = Shell.class.getName() + ".KEY_ID";
	
	private final JMenuBar bar;
	final Set<RetargetAction> retargetActions = new HashSet<RetargetAction>();
	
	public ActionManager(JMenuBar bar) {
		this.bar = bar;
	}
		
	public void addContainer(Relation rel, String path, String id, String caption) {		
		checkNull(rel, path, id, caption);
		if (path.equals("")) {
			addMenu(rel, id, caption);
		} else {
			addMenu(rel, id, caption, getMenuItem(path));
		}		
	}

	private void addMenu(Relation rel, String id, String caption) {		
		checkNull(rel, id, caption);

		JMenu menu = new JMenu(caption);
		menu.putClientProperty(KEY_ID, id);		
		
		switch (rel) {
		
		case FirstChildOf: 
			bar.add(menu, 0); 
			break;
			
		case LastChildOf: 
			bar.add(menu); 
			break;
			
		default: 
			throw new IllegalArgumentException(rel + " of the menu root makes no sense.");		
		
		}
		
	}

	public void addAction(Relation rel, String path, String id, final Action action) {		
		
		if (action instanceof RetargetAction)
			retargetActions.add((RetargetAction) action);
		
		checkNull(rel, path, id, action);		
		if (path.equals(""))
			throw new IllegalArgumentException("Can't add an action to the menu bar.");		
							
		// Parent is either a JMenu or a JMenuBar
		JMenuItem pathItem = getMenuItem(path);
		Container pathItemParent = pathItem.getParent();
		int index = -1;
		for (int i = 0; i < pathItemParent.getComponentCount(); i++) {
			if (pathItemParent.getComponent(i) == pathItem) {
				index = i;
				break;
			}
		}			
		assert index != -1;
		
		// JMenuItem to be added
		final JMenuItem menuItem;
		if (action instanceof BooleanStateAction) {		
			menuItem = new JCheckBoxMenuItem(action);
			((JCheckBoxMenuItem) menuItem).setState((Boolean) action.getValue(BooleanStateAction.BOOLEAN_STATE));
			action.addPropertyChangeListener(new PropertyChangeListener() {			
				public void propertyChange(PropertyChangeEvent evt) {
					if (BooleanStateAction.BOOLEAN_STATE.equals(evt.getPropertyName())) {
						((JCheckBoxMenuItem) menuItem).setState((Boolean) evt.getNewValue());
					}
				}				
			});
		} else {
			menuItem = new JMenuItem(action);
		}
		menuItem.putClientProperty(KEY_ID, id);

		switch (rel) {
		case FirstChildOf:
			if (pathItem instanceof JMenu) {
				pathItem.add(menuItem, 0);
				break;
			}
			
		case LastChildOf:
			if (pathItem instanceof JMenu) {
				pathItem.add(menuItem);
				break;
			}
			throw new IllegalArgumentException("Can't add action to non-menu: " + pathItem.getClientProperty(KEY_ID));			
			
		case NextSiblingOf:
			pathItemParent.add(menuItem, index + 1);
			break;			
			
		case PreviousSiblingOf:
			pathItemParent.add(menuItem, index);
			break;
			
		}
	}

//	public void addAction(Relation rel, String path, String id, String caption) {		
//		checkNull(rel, path, id, caption);
//		Action action = new RetargetAction(caption, null);
//		addAction(rel, path, id, action);		
//	}

	public void addSeparator(Relation rel, String path) {		
		checkNull(rel, path);		
		if (path.equals(""))
			throw new IllegalArgumentException("Can't add a separator to the menu bar.");
			
		// Parent is either a JMenu or a JMenuBar		
		JMenuItem pathItem = getMenuItem(path);
		Container pathItemParent = pathItem.getParent();
		int index = -1;
		for (int i = 0; i < pathItemParent.getComponentCount(); i++) {
			if (pathItemParent.getComponent(i) == pathItem) {
				index = i;
				break;
			}
		}			
		assert index != -1;

		switch (rel) {
		case FirstChildOf:
			if (pathItem instanceof JMenu) {
				((JMenu) pathItem).add(new JSeparator(), 0);
				break;
			}
			
		case LastChildOf:
			if (pathItem instanceof JMenu) {
				((JMenu) pathItem).addSeparator();
				break;
			}
			throw new IllegalArgumentException("Can't add separator to non-menu: " + pathItem.getClientProperty(KEY_ID));			
			
		case NextSiblingOf:
			pathItemParent.add(new JSeparator(), index + 1);
			break;			
			
		case PreviousSiblingOf:
			pathItemParent.add(new JSeparator(), index);
			break;
			
		}

	}
	

	
	
	private void addMenu(Relation rel, String id, String caption, JMenuItem pathItem) {
		
		// New menu to add
		JMenu menu = new JMenu(caption);
		menu.putClientProperty(KEY_ID, id);

		// Parent is either a JMenu or a JMenuBar
		Container pathItemParent = pathItem.getParent();
		int index = -1;
		for (int i = 0; i < pathItemParent.getComponentCount(); i++) {
			if (pathItemParent.getComponent(i) == pathItem) {
				index = i;
				break;
			}
		}			
		assert index != -1;
		
		switch (rel) {
		case NextSiblingOf:
			pathItemParent.add(menu, index + 1);
			break;
			
		case PreviousSiblingOf:
			pathItemParent.add(menu, index);
			break;
			
		case FirstChildOf:
			if (pathItem instanceof JMenu) {
				pathItem.add(menu, 0);
				break;
			}
			
		case LastChildOf:
			if (pathItem instanceof JMenu) {
				pathItem.add(menu, pathItem);
				break;
			}
			throw new IllegalArgumentException("Can't add child to non-menu: " + pathItem.getClientProperty(KEY_ID));			
			
		}

	}
	
	
	
	private JMenuItem getMenuItem(String _path) {
		String[] path = _path.split("/");
		LOGGER.finer("Looking for " + path(path, 0));
		for (int i = 0; i < bar.getMenuCount(); i++) {
			JMenu menu = bar.getMenu(i);
			if (path[0].equals(menu.getClientProperty(KEY_ID))) {
				if (path.length == 1) {
					return menu; 
				}
				return getMenuItem(menu, path, 1);
			}
		}
		throw new NoSuchPathException(path, 0);
	}
	
	private JMenuItem getMenuItem(JMenu root, String[] path, int i) {
		LOGGER.finer("Looking for " + path(path, i));
		for (Component c: root.getMenuComponents()) {
			if (c instanceof JMenuItem) {
				JMenuItem item = (JMenuItem) c;
				if (path[i].equals(item.getClientProperty(KEY_ID))) {
					if (path.length == i + 1) {
						return item;
					} else if (item instanceof JMenu) {
						return getMenuItem((JMenu) item, path, i + 1);
					} else {
						throw new NoSuchPathException(path, i + 1);
					}
				}
			}
		}
		throw new NoSuchPathException(path, i);
	}
	
	@SuppressWarnings("serial")
	private static class NoSuchPathException extends NoSuchElementException {
		private NoSuchPathException(String[] path, int pos) {
			super("No such path: " + path(path, pos));
		}
	}

	private static String path(String[] path, int pos) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i <= pos; i++) {
			if (i > 0)
				buf.append("/");
			buf.append(path[i]);
		}
		return buf.toString();
	}
	
	private static void checkNull(Object... objects) {
		for (Object o: objects)
			if (o == null) throw new NullPointerException();		
	}
	
}
