package edu.gemini.qpt.ui.view.visit.edit;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;

import javax.swing.AbstractAction;

import edu.gemini.qpt.core.Alloc;
import edu.gemini.qpt.core.Variant;
import edu.gemini.ui.gface.GSelection;
import edu.gemini.ui.gface.GViewer;

@SuppressWarnings("serial")
public class SelectAllAction extends AbstractAction implements PropertyChangeListener {
	
	private final GViewer<Variant, ?> viewer;
	
	public SelectAllAction(final GViewer<Variant, ?> viewer) {
		this.viewer = viewer;
		setEnabled(false);
		viewer.addPropertyChangeListener(GViewer.PROP_MODEL, this);
	}
	
	public void actionPerformed(ActionEvent e) {
		Collection<Alloc> all = viewer.getModel().getAllocs();
		viewer.setSelection(new GSelection<Alloc>(all.toArray(new Alloc[all.size()])));
	}

	public void propertyChange(PropertyChangeEvent evt) {
		setEnabled(viewer.getModel() != null);
	}	
	
}
