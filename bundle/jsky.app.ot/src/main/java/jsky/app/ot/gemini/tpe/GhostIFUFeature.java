package jsky.app.ot.gemini.tpe;

import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.spModel.gemini.ghost.GhostAsterism;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.target.SPCoordinates;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.WatchablePos;
import edu.gemini.spModel.target.env.GuideProbeTargets;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.env.TargetEnvironmentDiff;
import jsky.app.ot.tpe.*;
import jsky.app.ot.util.BasicPropertyList;
import jsky.app.ot.util.OtColor;
import jsky.app.ot.util.PropertyWatcher;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.Collections;

/**
 * Draws the GHOST IFU patrol fields and IFUs.
 */
public final class GhostIFUFeature extends TpeImageFeature implements PropertyWatcher, TpeModeSensitive, TpeDragSensitive {

    private AffineTransform trans;
    private boolean isEmpty;

    // Color for AO WFS limit.
    private static final Color AO_FOV_COLOR = Color.RED;
    private static final Color PROBE_RANGE_COLOR = OtColor.SALMON;

    // Composite used for drawing items that block the view
    private static final Composite BLOCKED = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5F);

    // Property used to control drawing of the probe ranges.
    private static final BasicPropertyList props = new BasicPropertyList(GhostIFUFeature.class.getName());
    private static final String PROP_SHOW_RANGES = "Show GHOST IFU anf patrol fields.";
    static {
        props.registerBooleanProperty(PROP_SHOW_RANGES, true);
    }

    // TODO: This should be dependent on the asterism type.
    private enum RangeDisplayMode {
        probe1(true, false), probe2(false, true), both(true, true);

        private final boolean show1;
        private final boolean show2;

        RangeDisplayMode(boolean show1, boolean show2) {
            this.show1 = show1;
            this.show2 = show2;
        }

        boolean show1() { return show1; }
        boolean show2() { return show2; }
    }

    private RangeDisplayMode rangeMode = RangeDisplayMode.both;


    /**
     * Construct the feature with its name and description.
     */
    public GhostIFUFeature() {
        super("GHOST", "Show GHOST IFU and patrol fields..");
    }

    /**
     * A property has changed.
     *
     * @see PropertyWatcher
     */
    public void propertyChange(String propName) {
        _iw.repaint();
    }

    /**
     * Override getProperties to return the properties supported by this
     * feature.
     */
    @Override
    public BasicPropertyList getProperties() {
        return props;
    }

    /**
     * Turn on/off the drawing of patrol fields.
     */
    public void setDrawPatrolFields(boolean draw) {
        props.setBoolean(PROP_SHOW_RANGES, draw);
    }

    /**
     * Gets the drawing of the patrol fields.
     */
    private boolean getDrawPatrolFields() {
        return props.getBoolean(PROP_SHOW_RANGES, true);
    }

    private PropertyChangeListener selListener = evt -> _redraw();

    /**
     * Reinitialize (recalculate the positions and redraw).
     */
    public void reinit(TpeImageWidget iw, TpeImageInfo tii) {
        _stopMonitorOffsetSelections(selListener);

        super.reinit(iw, tii);

        props.addWatcher(this);

        SPInstObsComp inst = _iw.getInstObsComp();
        if (inst == null) return;

        // arrange to be notified if telescope positions are added, removed, or selected
        _monitorPosList();

        // Monitor the selections of offset positions, since that affects the positions drawn
        _monitorOffsetSelections(selListener);

        final Point2D.Double base = tii.getBaseScreenPos();
        double ppa = tii.getPixelsPerArcsec();

        trans = new AffineTransform();
        trans.translate(base.x, base.y);
        // The model already used the position angle, so just rotate by the difference between north and up in the image
        trans.rotate(-tii.getTheta());
        trans.scale(ppa, ppa);
    }

    @Override public void unloaded() {
        props.deleteWatcher(this);
        super.unloaded();
    }

    /**
     * Implements the TelescopePosWatcher interface.
     */
    public void telescopePosLocationUpdate(WatchablePos tp) {
        _redraw();
    }

    /**
     * Implements the TelescopePosWatcher interface.
     */
    public void telescopePosGenericUpdate(WatchablePos tp) {
        _redraw();
    }

    @Override protected void handleTargetEnvironmentUpdate(TargetEnvironmentDiff diff) {
        _redraw();
    }

    /**
     * Schedule a redraw of the image feature.
     */
    private void _redraw() {
        if (_iw != null) _iw.repaint();
    }


    // Slant of the lines drawn in the probe 1 and 2 ranges.  Slant at a
    // 45 degree angle either falling (down) \ or rising (up) /
    private enum Orientation {
        vertical,
        horizontal,
    }

    // Creates a Paint that is used for filling IFU patrol fields 1 and 2.
    private static Paint createPatrolFieldPaint(Graphics2D g2d, Orientation s) {
        return createPatrolFieldPaint(g2d, s, 16, 0.16, 0.4);
    }

    private static Paint createPatrolFieldKeyPaint(Graphics2D g2d, Orientation s) {
        return createPatrolFieldPaint(g2d, s, 8, 0.32, 0.8);
    }

    private static Paint createPatrolFieldPaint(Graphics2D g2d, Orientation s, int skip, double alphaBg, double alphaLine) {
        final int size = 2 * skip;

        final Rectangle2D.Double rec = new Rectangle2D.Double(0, 0, size, size);

        // Get a buffered image capable of being transparent.
        final BufferedImage bim = g2d.getDeviceConfiguration().createCompatibleImage(size, size, Transparency.TRANSLUCENT);
        final Graphics2D bimg = bim.createGraphics();

        // Shade it with a light red color almost completely transparent.
        bimg.setColor(OtColor.makeTransparent(PROBE_RANGE_COLOR, alphaBg));
        bimg.setComposite(AlphaComposite.Src);
        bimg.fill(rec);

        // Now draw the slanting lines, which are also pretty transparent
        // though not quite as much as the background.
        bimg.setClip(0, 0, size, size);
        bimg.setColor(OtColor.makeTransparent(PROBE_RANGE_COLOR, alphaLine));

        if (s == Orientation.vertical) {
            for (int x=0; x<size; x += skip) {
                bimg.drawLine(x, 0, x, size);
            }
        } else {
            for (int y=0; y<size; y += skip) {
                bimg.drawLine(0, y, size, y);
            }
        }
        bimg.dispose();

        return new TexturePaint(bim, rec);
    }

    // If _flipRA is -1, flip the RA axis of the area
    private Area flipArea(Area a) {
        if (_flipRA == -1) {
            a = a.createTransformedArea(AffineTransform.getScaleInstance(_flipRA, 1.0));
        }
        return a;
    }

    /**
     * Draw the feature.
     */
    public void draw(Graphics g, TpeImageInfo tii) {
        if (!isEnabled(_iw.getContext())) return;
        if (trans == null) return;

        // Store the former color.
        final Graphics2D g2d = (Graphics2D) g;
        final Color c = g2d.getColor();

        final Option<ObsContext> ctxOpt = _iw.getObsContext();
        if (ctxOpt.isEmpty()) return;
        final ObsContext ctx = ctxOpt.getValue();

        // TODO: Draw the Arc2D. We no longer use Canopus probe range.
        // Draw the AO window itself.  A circle.
        Area a = Canopus.Wfs.cwfs3.probeRange(ctx);
        isEmpty = a.isEmpty();
        if (isEmpty) return;

        final Shape s = trans.createTransformedShape(flipArea(a));
        g2d.setColor(AO_FOV_COLOR);
        g2d.draw(s);

        // Draw the IFU1 and IFU2 patrol fields.
        if (getDrawPatrolFields()) {
            Area a1 = new Area(flipArea(Canopus.Wfs.cwfs1.probeRange(ctx))).createTransformedArea(trans);
            Area a2 = new Area(flipArea(Canopus.Wfs.cwfs2.probeRange(ctx))).createTransformedArea(trans);
            g2d.setColor(OtColor.makeTransparent(AO_FOV_COLOR, 0.3));

            if (rangeMode.show1()) g2d.draw(a1);
            if (rangeMode.show2()) g2d.draw(a2);

            Paint p = g2d.getPaint();
            if (rangeMode.show1()) {
                g2d.setPaint(createPatrolFieldKeyPaint(g2d, Orientation.horizontal));
                g2d.fill(a1);
            }
            if (rangeMode.show2()) {
                g2d.setPaint(createPatrolFieldKeyPaint(g2d, Orientation.vertical));
                g2d.fill(a2);
            }
            g2d.setPaint(p);
        }

        g2d.setColor(c);
    }

    @Override public boolean isEnabled(TpeContext ctx) {
        return super.isEnabled(ctx) && ctx.gems().isDefined();
    }

    private void setRangeDisplayMode(RangeDisplayMode mode) {
        if (rangeMode == mode) return;
        rangeMode = mode;
        _redraw();
    }


    private static boolean containsSRIFU1Target(GhostAsterism.SingleTarget gs, SPTarget target) {
        return gs.allSpTargetsJava().contains(target);
    }

    private static boolean containsSRIFU1Target(GhostAsterism.TargetPlusSky gts, SPTarget target) {
        return gts.allSpTargetsJava().contains(target);
    }

    private static boolean containsSRIFU1Coordinates(GhostAsterism.SkyPlusTarget gst, SPCoordinates coords) {
        return gst.allSpCoordinatesJava().contains(coords);
    }

    private static boolean containsSRIFU2Target(GhostAsterism.SingleTarget gs, SPTarget target) {
        return gs.allSpTargetsJava().contains(target);
    }

    private static boolean containsSRIFU2Target(GhostAsterism.TargetPlusSky gts, SPTarget target) {
        return gts.allSpTargetsJava().contains(target);
    }

    private static boolean containsSRIFU2Coordinates(TargetEnvironment, GhostAsterism.DualTarget gdt, SPCoordinates coords) {
        return gdt.allSpCoordinatesJava().contains(coords);
    }


    private static boolean containsTarget(TargetEnvironment env, GuideProbe guider, SPTarget target) {
        final Option<GuideProbeTargets> gtOpt = env.getPrimaryGuideProbeTargets(guider);
        return gtOpt.exists(gt -> gt.containsTarget(target));
    }

    public void handleDragStarted(Object dragObject, ObsContext context) {
        if (!(dragObject instanceof SPTarget)) return;

        SPTarget target = (SPTarget) dragObject;
        TargetEnvironment env = context.getTargets();
        if (env == null) {
            setRangeDisplayMode(RangeDisplayMode.both);
            return;
        }

        if (containsTarget(env, Canopus.Wfs.cwfs1, target)) {
            setRangeDisplayMode(RangeDisplayMode.probe1);
        } else if (containsTarget(env, Canopus.Wfs.cwfs2, target)) {
            setRangeDisplayMode(RangeDisplayMode.probe2);
        } else {
            setRangeDisplayMode(RangeDisplayMode.both);
        }
    }

    public void handleDragStopped(ObsContext context) {
        setRangeDisplayMode(RangeDisplayMode.both);
    }

    private static class ProbeRangeIcon implements Icon {
        private final Orientation[] slants;

        ProbeRangeIcon(Orientation... slants) {
            this.slants = slants;
        }

        public int getIconWidth()  { return 18; }
        public int getIconHeight() { return 18; }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setColor(Color.black);
            g2d.fill(new Rectangle2D.Double(1, 1, 16, 16));

            Paint origPaint = g2d.getPaint();
            for (Orientation slant : slants) {
                Paint p = createProbeRangeKeyPaint(g2d, slant);
                g2d.setPaint(p);
                g2d.fill(new Rectangle2D.Double(1, 1, 16, 16));
            }
            g2d.setPaint(origPaint);
        }
    }

    @Override
    public Option<Component> getKey() {
        JPanel pan = new JPanel(new GridBagLayout());

        pan.add(new JLabel("IFU1", new ProbeRangeIcon(Orientation.horizontal), JLabel.LEFT) {{ setForeground(Color.black); }},
                new GridBagConstraints() {{
                    gridx=0; gridy=0; anchor=WEST; fill=HORIZONTAL;
                }}
        );
        pan.add(new JLabel("IFU2", new ProbeRangeIcon(Orientation.vertical), JLabel.LEFT) {{ setForeground(Color.black); }},
                new GridBagConstraints() {{
                    gridx=1; gridy=0; anchor=WEST; fill=HORIZONTAL;
                }}
        );

        return new Some<>(pan);
    }

    public TpeImageFeatureCategory getCategory() {
        return TpeImageFeatureCategory.fieldOfView;
    }

    private static final TpeMessage WARNING = TpeMessage.warningMessage(
            "No valid region for GHOST IFU..");

    public Option<Collection<TpeMessage>> getMessages() {
        if (!isEmpty) return None.instance();
        return new Some<>(Collections.singletonList(WARNING));
    }

}