package edu.gemini.ui.gface.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import edu.gemini.ui.gface.GSelection;
import edu.gemini.ui.gface.GSelectionBroker;

/**
 * A SelectionHub propogates selection changes between SelectionBrokers, keeping them
 * "in sync" as well as possible. It also provides methods allowing an external actor to push 
 * selection changes down to all member SelectionBrokers. A single SelectionBroker may be
 * designated as the primary broker, and its selection will be announced via property change
 * events.
 * @author rnorris
 */
@SuppressWarnings("unchecked")
public class SelectionHub implements GSelectionBroker {

	private static final Logger LOGGER = Logger.getLogger(SelectionHub.class.getName());
	private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
	private final Set<GSelectionBroker> brokers = new HashSet<GSelectionBroker>();
	private boolean broadcasting = false;
	private GSelectionBroker primaryBroker;
	
	private final PropertyChangeListener listener = new PropertyChangeListener() {
	
		public void propertyChange(PropertyChangeEvent evt) {
			synchronized (SelectionHub.this) {
				
				// Whose selection changed?
				GSelectionBroker source = (GSelectionBroker) evt.getSource();

				// we want to push a combination delta
				GSelection oldSelection = (GSelection) evt.getOldValue();
				GSelection newSelection = (GSelection) evt.getNewValue();

				// We only want to propogate changes if we're not broadcasting already.
				if (!broadcasting) {
					broadcasting = true;
					
//					// determine the adds and removes
//					GSelection toAdd = newSelection.minus(oldSelection);
//					GSelection toRemove = oldSelection.minus(newSelection);
//					
//					LOGGER.fine(source + ": selection change: + " + toAdd + ", - "+ toRemove);
					
					// Ok, do it.
					for (GSelectionBroker b: brokers)
						if (source != b) {
							LOGGER.finer("Informing " + b);
							b.setSelection(newSelection);
//							b.adjustSelection(toAdd, toRemove);
						}
				
					broadcasting = false;
				}
				
				// Whether we are broadcasting or not, we want to forward
				// changes in the primary selection.
				if (source == primaryBroker) {
					pcs.firePropertyChange(PROP_SELECTION, oldSelection, newSelection);
				}
				
			}
		}
	
	};
	
	public synchronized void setSelection(GSelection newSelection) {
		broadcasting = true;
		LOGGER.fine("External actor: selection set: + " + newSelection);
		for (GSelectionBroker b: brokers)
			b.setSelection(newSelection);
		if (primaryBroker == null)
			pcs.firePropertyChange(PROP_SELECTION, GSelection.emptySelection(), newSelection);
		broadcasting = false;
	}

//	public synchronized void adjustSelection(GSelection toAdd, GSelection toRemove) {
//		
//		// Should this actually be allowed?
//		
//		broadcasting = true;
//		LOGGER.fine("External actor: selection change: + " + toAdd + ", - "+ toRemove);
//		for (GSelectionBroker b: brokers)
//			b.adjustSelection(toAdd, toRemove);
//		if (primaryBroker == null)
//			pcs.firePropertyChange(PROP_SELECTION, GSelection.emptySelection(), toAdd); // ?
//		broadcasting = false;
//	}


	public synchronized void add(GSelectionBroker b) {
		brokers.add(b);		
		b.addPropertyChangeListener(GSelectionBroker.PROP_SELECTION, listener);
	}

	public synchronized boolean remove(GSelectionBroker b) {
		b.removePropertyChangeListener(GSelectionBroker.PROP_SELECTION, listener);
		return brokers.remove(b);
	}
	
	public GSelectionBroker getPrimaryBroker() {
		return primaryBroker;
	}

	public synchronized void setPrimaryBroker(GSelectionBroker newPrimaryBroker) {		
		// re-assert the primary guy's selection?

		broadcasting = true;
		GSelection prev = getSelection();
		primaryBroker = newPrimaryBroker;
		GSelection newSelection = getSelection();		
		LOGGER.fine("New Primary Broker: selection set: + " + newSelection);
		for (GSelectionBroker b: brokers)
			if (b != primaryBroker)
				b.setSelection(newSelection);
		pcs.firePropertyChange(PROP_SELECTION, prev, newSelection);
		broadcasting = false;
		
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		pcs.addPropertyChangeListener(listener);
	}

	public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		pcs.addPropertyChangeListener(propertyName, listener);
	}

	public PropertyChangeListener[] getPropertyChangeListeners() {
		return pcs.getPropertyChangeListeners();
	}

	public PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
		return pcs.getPropertyChangeListeners(propertyName);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		pcs.removePropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		pcs.removePropertyChangeListener(propertyName, listener);
	}

	public synchronized GSelection getSelection() {
		return primaryBroker != null ? primaryBroker.getSelection() : GSelection.emptySelection();
	}
	
}
