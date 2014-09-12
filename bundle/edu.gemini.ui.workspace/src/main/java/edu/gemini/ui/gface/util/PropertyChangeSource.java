package edu.gemini.ui.gface.util;

import java.beans.PropertyChangeListener;

public interface PropertyChangeSource {

	public void addPropertyChangeListener(PropertyChangeListener listener);

	public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener);

	public PropertyChangeListener[] getPropertyChangeListeners();

	public PropertyChangeListener[] getPropertyChangeListeners(String propertyName);

	public void removePropertyChangeListener(PropertyChangeListener listener);

	public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener);

}
