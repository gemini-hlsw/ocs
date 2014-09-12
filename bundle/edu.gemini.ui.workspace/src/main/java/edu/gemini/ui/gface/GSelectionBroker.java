package edu.gemini.ui.gface;

import edu.gemini.ui.gface.util.PropertyChangeSource;

public interface GSelectionBroker<E> extends PropertyChangeSource {

	public static final String PROP_SELECTION = "selection";

	GSelection<E> getSelection();

	void setSelection(GSelection<?> newSelection);

//	void adjustSelection(GSelection<?> toAdd, GSelection<?> toRemove);
	
}
