package edu.gemini.qpt.ui.view.visualizer;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JPanel;

import edu.gemini.qpt.core.Alloc;
import edu.gemini.qpt.core.Block;
import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.core.Variant;
import edu.gemini.qpt.shared.sp.Obs;
import edu.gemini.qpt.core.util.LocalSunriseSunset;
import edu.gemini.qpt.shared.util.TimeUtils;
import edu.gemini.shared.util.immutable.ImOption;
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

	protected static final long PADDING = 45 * TimeUtils.MS_PER_MINUTE;
	protected static final int SNAP_PIXELS = 5;

	protected static final long MIN_DEG = 0;
	protected static final long MAX_DEG = 95;

	protected Variant model;
	protected long minTime, maxTime;
	protected GSelection<Alloc> selection = GSelection.emptySelection();
	protected GSelection<Alloc> dragObject = GSelection.emptySelection();
	protected long dragDelta;
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

	public void setDrag(final Point xy, final GSelection<Alloc> obj, final boolean snap, final Long offset) {

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
		final long dragTime = (long) clientToModel(xy).getX();

		// Determine the drag duration.
		obj.sort(); // be sure they're in ascending order (may not be due to ctrl+click)
		final long dragDuration = obj.last().getEnd() - obj.first().getStart();
		dragDelta = dragTime - ImOption.apply(offset).getOrElse(dragDuration / 2) - obj.first().getStart();

		// If "snap" functionality is requested, snap to the "outside" of the nearest Alloc
		// if it's within SNAP_PIXELS pixels, or to the "inside" of the nearest Block
		// boundary (again if it's within SNAP_PIXELS) if there is not a nearby Alloc. So
		// this may update the dragStartTime and dragEndTime members, or it may do nothing.
		long dragStartTime = obj.first().getStart() + dragDelta;
		long dragEndTime = dragStartTime + dragDuration;

		if (snap) {
			// Convert SNAP_PIXELS to ms so we can do the proximity calculations in model
			// units instead of converting everything to pixels.
			final long snapMs = (long) (SNAP_PIXELS / time2X.getScaleX());
			boolean snapped = false;

			// First case, try to snap end-to-start to the outside of a nearby Alloc. As
			// soon as we snap, exit the loop. This means that in some cases with overlaps
			// we may not snap to the closest Alloc, but this is a corner case that's not
			// worth worrying about.
			for (final Alloc a: model.getAllocs()) {
				final long left = Math.abs(a.getEnd() - dragStartTime);
				final long right = Math.abs(dragEndTime - a.getStart());

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
				for (final Block b: model.getSchedule().getBlocks()) {
					final long left = Math.abs(b.getStart() - dragStartTime);
					final long right = Math.abs(b.getEnd() - dragEndTime);

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
		for (final Alloc a: dragObject) {
			final Alloc pred = a.getPredecessor();
			final Alloc succ = a.getSuccessor();
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

	public void setModel(final Variant newModel) {
	    // Repainting is done in subclass: don't worry about it here.
		if (this.model != newModel) {
			this.model = newModel;
			if (this.model != null) {

				// Re-calculate boundaries and hook up listeners.
				// BUG: this should be done whenever the Blocks collection changes.
				final Schedule schedule = newModel.getSchedule();
				final Site site = schedule.getSite();
				final TwilightBoundType tbt = LocalSunriseSunset.forSite(site);
				final TwilightBoundedNight tbn = new TwilightBoundedNight(tbt, schedule.getStart(), site);
				minTime = Math.min(schedule.getStart(), tbn.getStartTime()) - PADDING;
				maxTime = Math.max(schedule.getEnd(), tbn.getEndTime()) + PADDING;
			}
		}
	}

	public long getDragDelta() {
		return dragDelta;
	}

	public GSelection<Alloc> getSelection() {
		return selection;
	}

	public synchronized void setSelection(final GSelection<Alloc> selection) {
		if (selection == null) {
			LOGGER.log(Level.WARNING, "Someone passed a null selection. Fix this.", new Exception("Stack Trace"));
		}
        this.selection = ImOption.apply(selection).getOrElse(GSelection.emptySelection());
		repaint();
	}

	/**
	 * Returns the Alloc at client coordinates (x, y) or null if there is no Alloc under
	 * that point. If there is more than one at that point, an arbitrary but deterministic
	 * choice will be made for you.
	 */
	protected Alloc getAllocAtPoint(final int x, final int y) {
		if (canMapClientToModel()) {
			final long timeAltX = (long) clientToModel(x, y).getX();
			return model.getAllocs().stream().filter(a -> a.contains(timeAltX)).findFirst().orElse(null);
		}
		return null;
	}

	Point2D clientToModel(final int x, final int y) {
		return clientToModel(new Point2D.Double(x, y));
	}

	Point2D clientToModel(final Point2D p) {
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

	public void setPreview(final Obs obs) {
		preview = obs;
		repaint();
	}
}
