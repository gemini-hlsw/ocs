package edu.gemini.qpt.ui.view.property;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Map;
import java.util.logging.Logger;

import edu.gemini.qpt.core.Schedule;
import edu.gemini.ui.gface.GSelection;
import edu.gemini.ui.gface.GTableController;
import edu.gemini.ui.gface.GViewer;

@SuppressWarnings("unchecked")
public class PropertyController implements GTableController<GSelection<?>, Map.Entry<String, Object>, PropertyAttribute>, PropertyChangeListener {

	@SuppressWarnings("unused")
	private static final Logger LOGGER = Logger.getLogger(PropertyController.class.getName());
	
	private GViewer<GSelection<?>, Map.Entry<String, Object>> viewer;
	private Map.Entry<String, Object>[] entries = new Map.Entry[0];
	private Schedule schedule;
	
	public synchronized Object getSubElement(Map.Entry<String, Object> element, PropertyAttribute subElement) {
		switch (subElement) {
		case Name: return element.getKey();
		case Value: return element.getValue();
		default: throw new IllegalArgumentException(subElement.name());
		}
	}

	public synchronized Map.Entry<String, Object> getElementAt(int row) {
		return entries[row];
	}

	public synchronized int getElementCount() {
		return entries.length;
	}

	@SuppressWarnings("unchecked")
	public synchronized void modelChanged(GViewer<GSelection<?>, Map.Entry<String, Object>> viewer, GSelection<?> oldModel, GSelection<?> newModel) {
		this.viewer = viewer;
		Map<String, Object> table = PropertyTable.forObjects(schedule != null ? schedule.getCurrentVariant() : null, newModel);
		entries = new Map.Entry[table.size()];
		int i = 0;
		for (Map.Entry<String, Object> e: table.entrySet())
			entries[i++] = e;		
	}

	// The view advisor adds us as a model change listener.
	public void propertyChange(PropertyChangeEvent evt) {
		schedule = (Schedule) evt.getNewValue();
		if (schedule == null || viewer == null) entries = new Map.Entry[0];
		if (viewer != null) viewer.refresh();
	}
	
}
