package edu.gemini.ui.gface;

import java.awt.Component;
import java.awt.Point;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import javax.swing.SwingUtilities;


/**
 * A viewer for a model M of element type E.
 * @author rnorris
 * @param <M>
 * @param <E>
 */
@SuppressWarnings("unchecked")
public abstract class GViewer<M, E> implements GSelectionBroker<E> {

	public static final String PROP_MODEL = "model";

	protected final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

	private final GViewerPlugin<M, E> controller;
	private final Component widget;
	private M model;
	
	// Plugins
	private GTranslator<M, E> translator;
	private GComparator<M, E> comparator;
	private GDecorator<M, E> decorator;
	private GFilter<M, E> filter;
	private GSelectionInterloper<M, E> interloper;
	
	public GViewer(GViewerPlugin<M, E> controller, Component control) {
		this.widget = control;
		this.controller = controller;
	}

	public GViewerPlugin<M, E> getController() {
		return controller;
	}

	public M getModel() {
		return model;
	}

	public void setModel(M model) {

		// Do nothing if model is the same.
		if (model == this.model || (model != null && model.equals(this.model))) 
			return;
		
		M prev = this.model;
		this.model = model;

		
		// Notify plugins
		GViewerPlugin[] plugins = new GViewerPlugin[] {
			translator, comparator, decorator, filter, interloper, controller // keep this last
		};
		for (GViewerPlugin<M, E> p: plugins) {
			if (p != null)
				p.modelChanged(this, prev, model);
		}
		
		pcs.firePropertyChange(PROP_MODEL, prev, model);
		refresh();
	}

	
	private GSelection<E> selection = GSelection.emptySelection();

	public final GSelection<E> getSelection() {
		return selection;
	}

	public final void setSelection(GSelection<?> newSelection) {
		assert newSelection != null;
		newSelection = newSelection.translate(getTranslator());			
		if (interloper != null) interloper.beforeSetSelection(newSelection);		
		Runnable selectionTask = getSelectionTask(newSelection); 
		if (selectionTask != null) {
			if (SwingUtilities.isEventDispatchThread()) {
				selectionTask.run();
			} else {
				SwingUtilities.invokeLater(selectionTask);
//				try {
//					SwingUtilities.invokeAndWait(selectionTask);
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				} catch (InvocationTargetException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
			}
		}
	}
	
//	public final void adjustSelection(GSelection<?> toAdd, GSelection<?> toRemove) {
//		assert toAdd != null;
//		assert toRemove != null;		
//		toAdd = toAdd.translate(getTranslator());
//		toRemove = toRemove.translate(getTranslator());	
//		setSelection(selection.minus(toRemove).plus(toAdd));
////		setSelection(toAdd.plus(selection).minus(toRemove));
//	}

	/**
	 * Returns a Runnable that will set the specified selection. This runnable will be executed
	 * on the Swing thread.
	 * @param newSelection
	 */
	protected abstract Runnable getSelectionTask(GSelection<?> newSelection);
	
	
	protected void setPulledSelection(GSelection<E> newSelection) {
		GSelection<E> previous = selection;
		selection = newSelection;		
		pcs.firePropertyChange(PROP_SELECTION, previous, selection);
	}
	
	
	/**
	 * Asks the viewer to refresh its content from the model. The existing selection
	 * will be preserved if possible.
	 */
	public abstract void refresh();

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

	public GTranslator<M, E> getTranslator() {
		return translator;
	}

	public void setTranslator(GTranslator<M, E> translator) {
		this.translator = translator;
		translator.modelChanged(this, null, model);
	}

	public GComparator<M, E> getComparator() {
		return comparator;
	}

	public void setComparator(GComparator<M, E> collator) {
		this.comparator = collator;
		comparator.modelChanged(this, null, model);
		refresh();
	}

	public GDecorator<M, E> getDecorator() {
		return decorator;
	}

	public void setDecorator(GDecorator<M, E> decorator) {
		this.decorator = decorator;
		decorator.modelChanged(this, null, model);
		widget.repaint(); // is this good enough?
	}

	public GFilter<M, E> getFilter() {
		return filter;
	}

	public void setFilter(GFilter<M, E> filter) {
		this.filter = filter;
		filter.modelChanged(this, null, model);
		refresh();
	}

	public GSelectionInterloper<M, E> getInterloper() {
		return interloper;
	}

	public void setInterloper(GSelectionInterloper<M, E> interloper) {
		this.interloper = interloper;
		interloper.modelChanged(this, null, model);
	}

	public Component getControl() {
		return widget;
	}
	
	public abstract E getElementAt(Point p);
	
	public E getElementAt(int x, int y) {
		return getElementAt(new Point(x, y));
	}
	

	// in order to integrate drag and drop, we need to do a few things:
	//
	// - if there is a drag gesture in the selection
	// 		- (abstract) ask what operations are allowed (MOVE, REMOVE, COPY)
	//		- (abstract) get the offset
	//		- if we're copying, empty the selection
	// - if there is a drag over the control
	//		- make sure it's a GSelection
	//		- translate it
	//		- (abstract) ask what operations are allowed (MOVE, REMOVE, COPY)
	//		- (abstract) get a dragTask(GSelection<E>, Point)
	//		- execute the task
	// - if there is a drop
	//		- make sure it's a GSelection
	//		- translate it
	//		- (abstract) ask what operations are allowed (MOVE, REMOVE, COPY)
	//		- (abstract) get a dropTask(GSelection<E>, Point, Point)
	//		- execute the task
	
	
}






