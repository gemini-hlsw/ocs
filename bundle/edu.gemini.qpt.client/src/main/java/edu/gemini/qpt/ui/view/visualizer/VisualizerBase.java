package edu.gemini.qpt.ui.view.visualizer;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JPanel;

import edu.gemini.qpt.core.Alloc;
import edu.gemini.qpt.core.Block;
import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.core.Variant;
import edu.gemini.qpt.shared.sp.Obs;
import edu.gemini.skycalc.TwilightBoundType;
import edu.gemini.skycalc.TwilightBoundedNight;
import edu.gemini.spModel.core.Site;
import edu.gemini.ui.gface.GSelection;

/**
 * Base class for the Visualizer panel. This slice handles the selection, dragging, and
 * stuff like that. The concrete Visualizer basically just does drawing.
 */
abstract class VisualizerBase extends JPanel {

	private static final Logger LOGGER = Logger.getLogger(VisualizerBase.class.getName());

	protected static final long PADDING = TimeUnit.MINUTES.toMillis(45);
	protected static final int SNAP_PIXELS = 5;
	protected static final Image DRAG_IMAGE = new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR);

	protected static final long MIN_DEG = 0;
	protected static final long MAX_DEG = 95;

	protected Variant model;
	protected long minTime, maxTime;
//	protected Point dragLocation;
	protected GSelection<Alloc> selection = GSelection.<Alloc>emptySelection();
	protected GSelection<Alloc> dragObject = GSelection.<Alloc>emptySelection();
	protected long dragDelta; // , dragEndTime, dragDuration;
	protected AffineTransform timeAlt2XY, alt2Y, time2X, sb2Y, timeSB2XY;

	protected Obs preview;

	@Override
	public void paint(Graphics g) {
		super.paint(g);
		if (model != null) {

			// We need to set up the model transforms each time we draw because the control
			// may have been resized. We could actually just do it in a resize handler, but
			// my instinct says this code will wind up in paint() eventually.

			// time2X transforms X from ms to horizontal pixels. Y is unaffected.
			time2X = AffineTransform.getScaleInstance(getSize().getWidth() / (maxTime - minTime), 1.0);
			time2X.translate(-minTime, 0);

			// alt2Y transforms altitude in degrees to vertical pixels. X is unaffected.
			alt2Y = AffineTransform.getScaleInstance(1, -1); // positive Y goes up
			alt2Y.translate(0, -getHeight()); // move Y origin down to the bottom
			alt2Y.scale(1.0, getSize().getHeight() / (MAX_DEG - MIN_DEG));
			alt2Y.translate(0.0, -MIN_DEG);

			// timeAlt2XY is simply a concatenation of time2X and at2Y that transforms a
			// point in (time, alt) space to (x, y) pixels.
			timeAlt2XY = (AffineTransform) time2X.clone();
			timeAlt2XY.concatenate(alt2Y);

			// sb2Y transforms sky brightness to vertical pixels. X is unaffected.
			sb2Y = AffineTransform.getTranslateInstance(0.0, 0.0); //-22);
			sb2Y.scale(1.0, getSize().getHeight() / (22-17));
			sb2Y.translate(0, -17);

			// timeSB2XY is simply a concatenation of time2X and at2Y that transforms a
			// point in (time, alt) space to (x, y) pixels.
			timeSB2XY = (AffineTransform) time2X.clone();
			timeSB2XY.concatenate(sb2Y);

		}
	}

	public void setDrag(Point xy, GSelection<Alloc> obj, boolean snap, Long offset) {

		// New drag object
		if (dragObject == null) {
			LOGGER.log(Level.WARNING, "Someone set a null drag object. Fix this.", new Exception("Stack Trace"));
			dragObject = GSelection.emptySelection();
		}
		dragObject = obj;
		if (dragObject.isEmpty()) return; // ?

		// Clear any selection.
		selection = GSelection.emptySelection();

		// Determine how far we're trying to drag, which is the mouse X position as an instant in
		// time, minus the drag offset, minus the start time of the drag object
		long dragTime = (long) clientToModel(xy).getX();

		// Determine the drag duration.
		obj.sort(); // be sure they're in ascending order (may not be due to ctrl+click)
		long dragDuration = obj.last().getEnd() - obj.first().getStart();
		dragDelta = dragTime - (offset != null? offset : (dragDuration / 2)) - obj.first().getStart();

		// If "snap" funcionality is requested, snap to the "outside" of the nearest Alloc
		// if it's within SNAP_PIXELS pixels, or to the "inside" of the nearest Block
		// boundary (again if it's within SNAP_PIXELS) if there is not a nearby Alloc. So
		// this may update the dragStartTime and dragEndTime members, or it may do nothing.
		long dragStartTime = obj.first().getStart() + dragDelta;
		long dragEndTime = dragStartTime + dragDuration;
		if (snap) {

			// Convert SNAP_PIXELS to ms so we can do the proximity calculations in model
			// units instead of converting everything to pixels.
			long snapMs = (long) (SNAP_PIXELS / time2X.getScaleX());
			boolean snapped = false;

			// First case, try to snap end-to-start to the outside of a nearby Alloc. As
			// soon as we snap, exit the loop. This means that in some cases with overlaps
			// we may not snap to the closest Alloc, but this is a corner case that's not
			// worth worrying about.
			for (Alloc a: model.getAllocs()) {

				long left = Math.abs(a.getEnd() - dragStartTime);
				long right = Math.abs(dragEndTime - a.getStart());

				if (left < snapMs && left < right) {

					dragStartTime = a.getEnd();
					dragEndTime = dragStartTime + dragDuration;
					snapped = true;
					break;

				} else if (right < snapMs) {

					dragEndTime = a.getStart();
					dragStartTime = dragEndTime - dragDuration;
					snapped = true;
					break;

				}


			}

			// Second case, try to snap start-to-start or end-to-end to a nearby Block.
			// Only do this if we haven't snapped already.
			if (!snapped) {
				for (Block b: model.getSchedule().getBlocks()) {

					long left = Math.abs(b.getStart() - dragStartTime);
					long right = Math.abs(b.getEnd() - dragEndTime);

					if (left < snapMs && left < right) {

						dragStartTime = b.getStart() + 1;
						dragEndTime = dragStartTime + dragDuration;
						snapped = true;
						break;

					} else if (right < snapMs) {

						dragEndTime = b.getEnd() - 1;
						dragStartTime = dragEndTime - dragDuration;
						snapped = true;
						break;

					}

				}
			}

		}

		dragDelta = dragStartTime - obj.first().getStart();


		// If any of the allocs is constrained by a predecessor or successor, we need to constraint
		// the dragDelta appropriately.
		for (Alloc a: dragObject) {
			Alloc pred = a.getPredecessor();
			Alloc succ = a.getSuccessor();
			if (pred != null) dragDelta = Math.max(dragDelta, pred.getEnd() - a.getStart());
			if (succ != null) dragDelta = Math.min(dragDelta, succ.getStart() - a.getEnd());
		}

		// Don't let the user drag such that stuff gets pushed off the side.
		if (obj.first().getStart() + dragDelta < minTime) dragDelta = minTime - obj.first().getStart(); // left side
		if (obj.last().getEnd()    + dragDelta > maxTime) dragDelta = maxTime - obj.last().getEnd(); // right side


		repaint();
	}

	public void clearDrag() {
		dragObject = GSelection.emptySelection();
		repaint();
	}

	public Variant getModel() {
		return model;
	}

	public void setModel(Variant newModel) {
		if (this.model != newModel) {
			this.model = newModel;
			if (this.model != null) {

				// Re-calculate boundaries and hook up listeners.
				// BUG: this should be done whenever the Blocks collection changes.
				final Schedule schedule = newModel.getSchedule();
				final Site site = schedule.getSite();
				TwilightBoundedNight tbn = new TwilightBoundedNight(TwilightBoundType.OFFICIAL, schedule.getStart(), site);
				minTime = Math.min(schedule.getStart(), tbn.getStartTime()) - PADDING;
				maxTime = Math.max(schedule.getEnd(), tbn.getEndTime()) + PADDING;

			}
// moved to subclass
//			repaint();
		}
	}

	public long getDragDelta() {
		return dragDelta;
	}

//	public long getDragEndTime() {
//		return dragEndTime;
//	}

	public GSelection<Alloc> getSelection() {
		return selection;
	}

	public synchronized void setSelection(GSelection<Alloc> selection) {
		if (selection == null) {
			LOGGER.log(Level.WARNING, "Someone passed a null selection. Fix this.", new Exception("Stack Trace"));
			selection = GSelection.<Alloc>emptySelection();
		}
//		LOGGER.info("VISUALIZER BASE THINKS ITS SELECTION IS " + selection);
//		if (selection.size() == 1)
//		LOGGER.info("                                        " + System.identityHashCode(selection.first()));
		this.selection = selection;
		repaint();
	}

	/**
	 * Returns the Alloc at client coordinates (x, y) or null if there is no Alloc under
	 * that point. If there is more than one at that point, an arbitrary but deterministic
	 * choice will be made for you.
	 */
	protected Alloc getAllocAtPoint(int x, int y) {
		if (canMapClientToModel()) {
			Point2D timeAlt = clientToModel(x, y);
			for (Alloc a: model.getAllocs())
				if (a.contains((long) timeAlt.getX()))
					return a;
		}
		return null;
	}

	Point2D modelToClient(Point2D p) {
		return timeAlt2XY.transform(p, null);
	}

	Point2D modelToClient(int x, int y) {
		return modelToClient(new Point2D.Double(x, y));
	}

	Point2D clientToModel(int x, int y) {
		return clientToModel(new Point2D.Double(x, y));
	}

	Point2D clientToModel(Point2D p) {
		try {
			return timeAlt2XY.inverseTransform(p, null);
		} catch (NoninvertibleTransformException nte) {
			throw new Error(nte); // Never happens.
		}
	}

	private boolean canMapClientToModel() {
		return model != null && timeAlt2XY != null;
	}

	public GSelection<Alloc> getDragObject() {
		return dragObject;
	}

	public void setPreview(Obs obs) {
		preview = obs;
		repaint();
	}

}
