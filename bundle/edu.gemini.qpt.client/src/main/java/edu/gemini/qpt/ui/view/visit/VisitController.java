package edu.gemini.qpt.ui.view.visit;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Date;
import java.util.SortedSet;

import edu.gemini.qpt.core.Alloc;
import edu.gemini.qpt.core.Variant;
import edu.gemini.shared.util.DateTimeUtils;
import edu.gemini.ui.gface.GTableController;
import edu.gemini.ui.gface.GViewer;

public class VisitController implements GTableController<Variant, Alloc, VisitAttribute>, PropertyChangeListener {

	private static final Alloc[] NONE = new Alloc[0];
	
	private GViewer<Variant, Alloc> viewer;
	private Alloc[] allocs = NONE;
	
	public Object getSubElement(Alloc a, VisitAttribute subElement) {
		switch (subElement) {
//		case Group:
//			Group g = a.getObs().getGroup();
//			return g != null && g.getType() == GroupType.TYPE_SCHEDULING ? "\u00B7" : "";
		case Start: return new Date(a.getStart());
		case Dur: return DateTimeUtils.msToHHMM(a.getLength());
		case BG: return a.getSkyBrightnessBin(false);
		case Observation: return a.getObs();
		case Steps: return (a.getFirstStep() + 1) + "-" + (a.getLastStep() + 1);
		case Inst: return a.getObs().getInstrumentString();
        case Config: return a.getObs().getOptionsString();
		case WFS: return a.getObs().getWavefrontSensors();
		case Target: return a.getObs().getTargetName();			
		}
		return null;
	}

	public Alloc getElementAt(int row) {
		return allocs[row];
	}

	public synchronized int getElementCount() {
		return allocs.length;
	}

	public synchronized void modelChanged(GViewer<Variant, Alloc> viewer, Variant oldModel, Variant newModel) {
		
		// Move the listeners
		if (oldModel != null) oldModel.removePropertyChangeListener(Variant.PROP_ALLOCS, this);
		if (newModel != null) newModel.addPropertyChangeListener(Variant.PROP_ALLOCS, this);
		
		// Initialize the visits collection.
		allocs = newModel == null ? NONE : newModel.getAllocs().toArray(NONE);
		
		// Save the viewer so we can refresh when visits collection changes.
		this.viewer = viewer;
		
	}

	@SuppressWarnings("unchecked")
	public synchronized void propertyChange(PropertyChangeEvent evt) {
		if (Variant.PROP_ALLOCS.equals(evt.getPropertyName())) {
			allocs = ((SortedSet<Alloc>) evt.getNewValue()).toArray(NONE);
			viewer.refresh();
		}
	}

}
