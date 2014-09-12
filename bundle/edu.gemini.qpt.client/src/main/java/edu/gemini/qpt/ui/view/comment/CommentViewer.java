package edu.gemini.qpt.ui.view.comment;

import java.awt.Point;

import edu.gemini.qpt.core.util.Commentable;
import edu.gemini.ui.gface.GSelection;
import edu.gemini.ui.gface.GViewer;

public class CommentViewer extends GViewer<Commentable, Commentable> {

	public static final String PROP_TARGET = "target";
	
	public CommentViewer() {
		super(null, new CommentEditor());
	}

	@Override
	public Commentable getElementAt(Point p) {
		return null;
	}

	@Override
	protected Runnable getSelectionTask(GSelection<?> newSelection) {
		Object sel = newSelection.size() == 1 ? newSelection.first() : null;
		return new SelectionTask(sel instanceof Commentable ? (Commentable) sel : null);
	}
	
	@Override
	public void refresh() {
	}

	private class SelectionTask implements Runnable {

		private final Commentable target;

		public SelectionTask(final Commentable target) {
			this.target = target;
		}

		public void run() {
			Commentable prev = getControl().getTarget();
			getControl().setTarget(target);
			pcs.firePropertyChange(PROP_TARGET, prev, getControl().getTarget());
			pullSelection();
		}

	}
	
	@Override
	public CommentEditor getControl() {
		return (CommentEditor) super.getControl();
	}

	private void pullSelection() {
		Commentable c = getControl().getTarget();
		setPulledSelection(c == null ? GSelection.<Commentable>emptySelection() :
			new GSelection<Commentable>(c));
	}
	

}
