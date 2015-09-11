package edu.gemini.ui.workspace.util;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;

@SuppressWarnings("serial")
public class RetargetAction implements Action {

	private final Object id;
	private final Action dummy;
	private Action target;
	
	public Object getId() {
		return id;
	}
	
	private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
	private final PropertyChangeListener listener = new PropertyChangeListener() {	
		public void propertyChange(PropertyChangeEvent pce) {
//			System.out.println(pce.getPropertyName() + ": " + pce.getOldValue() + " => " + pce.getNewValue());
			pcs.firePropertyChange(pce.getPropertyName(), pce.getOldValue(), pce.getNewValue());
		}
	};
	
	public RetargetAction(Object id, String text, final KeyStroke key) {
		this(id, new AbstractAction(text) {
			{
				setEnabled(false);
				putValue(ACCELERATOR_KEY, key); // , Platform.ACTION_MASK));
			}
			public void actionPerformed(ActionEvent ae) {
			}
		});
	}
	
	private RetargetAction(Object id, Action dummy) {
		this.id = id;
		this.dummy = dummy;
		this.target = dummy;
	}
	
	public  Action getTarget() {
		return target;
	}

	public  void setTarget(Action newTarget) {
		
		// Collect the previous state
		boolean prevEnabled = isEnabled();
								
		// Switch.
		target.removePropertyChangeListener(listener);
		target = newTarget != null ? newTarget : dummy;		
		target.addPropertyChangeListener(listener);

		// Update properties
		pcs.firePropertyChange("enabled", prevEnabled, isEnabled());
		
	}

	public  Object getValue(String key) {
		Object val = target.getValue(key);
		return (val != null) ? val : dummy.getValue(key);
	}

	public  void putValue(String key, Object val) {
		throw new UnsupportedOperationException("Don't do that.");
	}

	public  void setEnabled(boolean enabled) {
		throw new UnsupportedOperationException("Don't do that.");
	}

	public  boolean isEnabled() {
		return target.isEnabled();
	}

	public  void addPropertyChangeListener(PropertyChangeListener pcl) {
		pcs.addPropertyChangeListener(pcl);
	}

	public  void removePropertyChangeListener(PropertyChangeListener pcl) {
		pcs.removePropertyChangeListener(pcl);
	}

	public  void actionPerformed(ActionEvent ae) {
		target.actionPerformed(ae);
	}

	
	
}
