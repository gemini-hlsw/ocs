package jsky.app.ot.gemini.gems;

import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.spModel.gemini.gems.CanopusWfs;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.target.WatchablePos;
import edu.gemini.spModel.target.env.TargetEnvironmentDiff;
import jsky.app.ot.tpe.*;
import jsky.app.ot.util.PropertyWatcher;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.Collections;

/**
 * Draws the Canopus AO field of view and probe ranges.
 */
public final class CanopusFeature extends TpeImageFeature implements PropertyWatcher {

    private AffineTransform trans;
    private boolean isEmpty;

    // Color for AO WFS limit.
    private static final Color AO_FOV_COLOR = Color.RED;

    /**
     * Construct the feature with its name and description.
     */
    public CanopusFeature() {
        super("Canopus", "Show the field of view of the Canopus WFS probes.");
    }

    /**
     * A property has changed.
     *
     * @see PropertyWatcher
     */
    @Override
    public void propertyChange(String propName) {
        _iw.repaint();
    }

    private final PropertyChangeListener selListener = evt -> _redraw();

    /**
     * Reinitialize (recalculate the positions and redraw).
     */
    @Override
    public void reinit(final TpeImageWidget iw, final TpeImageInfo tii) {
        _stopMonitorOffsetSelections(selListener);

        super.reinit(iw, tii);

        final SPInstObsComp inst = _iw.getInstObsComp();
        if (inst == null) return;

        // arrange to be notified if telescope positions are added, removed, or selected
        _monitorPosList();

        // Monitor the selections of offset positions, since that affects the positions drawn
        _monitorOffsetSelections(selListener);

        final Point2D.Double base = tii.getBaseScreenPos();
        final double ppa = tii.getPixelsPerArcsec();

        trans = new AffineTransform();
        trans.translate(base.x, base.y);
        // The model already used the position angle, so just rotate by the difference between north and up in the image
        trans.rotate(-tii.getTheta());
        trans.scale(ppa, ppa);
    }

    /**
     * Implements the TelescopePosWatcher interface.
     */
    public void telescopePosLocationUpdate(final WatchablePos tp) {
        _redraw();
    }

    /**
     * Implements the TelescopePosWatcher interface.
     */
    public void telescopePosGenericUpdate(final WatchablePos tp) {
        _redraw();
    }

    @Override
    protected void handleTargetEnvironmentUpdate(final TargetEnvironmentDiff diff) {
        _redraw();
    }

    /**
     * Schedule a redraw of the image feature.
     */
    private void _redraw() {
        if (_iw != null) _iw.repaint();
    }

    // If _flipRA is -1, flip the RA axis of the area
    private Area flipArea(final Area a) {
        return _flipRA == -1 ? a.createTransformedArea(AffineTransform.getScaleInstance(_flipRA, 1.0)) : a;
    }

    /**
     * Draw the feature.
     */
    @Override
    public void draw(final Graphics g, final TpeImageInfo tii) {
        if (!isEnabled(_iw.getContext())) return;
        if (trans == null) return;

        _iw.getObsContext().foreach(ctx -> {
            final Graphics2D g2d = (Graphics2D) g;
            final Color c = g2d.getColor();

            // Draw the AO window itself.  A circle.
            final Area a = CanopusWfs.probeRange(ctx);
            isEmpty = a.isEmpty();
            if (isEmpty) return;

            final Shape s = trans.createTransformedShape(flipArea(a));
            g2d.setColor(AO_FOV_COLOR);
            g2d.draw(s);

            g2d.setColor(c);
        });
    }

    @Override
    public boolean isEnabled(TpeContext ctx) {
        return super.isEnabled(ctx) && ctx.gems().isDefined();
    }

    @Override
    public TpeImageFeatureCategory getCategory() {
        return TpeImageFeatureCategory.fieldOfView;
    }

    private static final TpeMessage WARNING = TpeMessage.warningMessage(
        "No valid region for CWFS stars.  Check offset positions.");

    @Override
    public Option<Collection<TpeMessage>> getMessages() {
        if (!isEmpty) return None.instance();
        return new Some<>(Collections.singletonList(WARNING));
    }

}
