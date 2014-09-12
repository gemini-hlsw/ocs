package edu.gemini.qpt.ui.view.histo;

import java.awt.Point;

import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.core.Variant;
import edu.gemini.ui.gface.GSelection;
import edu.gemini.ui.gface.GViewer;

public class HistoViewer extends GViewer<Schedule, Variant> {

	public HistoViewer() {
		super(new HistoController(), new HistoPanel());
	}

	@Override
	public Variant getElementAt(Point p) {
		return null;
	}

	@Override
	protected Runnable getSelectionTask(GSelection<?> newSelection) {
		return null;
	}

	@Override
	public void refresh() {
		getControl().setVariant(getController().getVariant());
		getControl().repaint();
	}

	@Override
	public HistoController getController() {
		return (HistoController) super.getController();
	}
	
	@Override
	public HistoPanel getControl() {
		return (HistoPanel) super.getControl();
	}
	
}
