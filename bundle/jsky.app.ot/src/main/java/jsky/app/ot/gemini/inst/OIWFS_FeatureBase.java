package jsky.app.ot.gemini.inst;

import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.guide.GuideProbeUtil;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.target.SPSkyObject;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.WatchablePos;
import edu.gemini.spModel.target.env.GuideProbeTargets;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.env.TargetEnvironmentDiff;
import edu.gemini.spModel.target.offset.OffsetPosBase;
import edu.gemini.spModel.target.offset.OffsetPosList;
import edu.gemini.spModel.util.Angle;
import jsky.app.ot.gemini.tpe.PosMapOffsetEntry;
import jsky.app.ot.tpe.*;
import jsky.app.ot.util.BasicPropertyList;
import jsky.app.ot.util.PropertyWatcher;

import java.awt.*;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeListener;
import java.util.Set;

/**
 * Base class for the instrument specific OIWFS_Feature classes.
 * This class consolidates the common code for the instrument specific
 * subclasses.
 */
public abstract class OIWFS_FeatureBase extends WFS_FeatureBase
        implements PropertyWatcher {

    // Name of OIWFS.
    protected static final String OIWFS = "OIWFS";

    // The color to use to draw the OIWFS
    protected static final Color OIWFS_COLOR = Color.red.brighter();

    // The color to use to draw the obscured area
    protected static final Color OIWFS_OBSCURED_COLOR = Color.red.darker();

    // Used to draw thin lines
    protected static final Stroke OIWFS_STROKE = new BasicStroke(2.0F);

    // The base telescope position
    protected Point2D.Double _baseScreenPos;

    // Set to if _reinit should be called to recalculate the figures
    protected boolean _reinitPending = false;

    // Number of pixels per arcsec in screen coords
    protected double _pixelsPerArcsec;

    // The position angle in radians
    protected double _posAngle;

    // The cached property value
    protected boolean _withVignetting;

    // The cached property value
    protected boolean _fillObscuredArea;

    /**
     * Construct the feature with its name and description.
     */
    public OIWFS_FeatureBase(String name, String desc) {
        super(name, desc);
    }

    private final PropertyChangeListener selectionWatcher = evt -> _redraw();

    /**
     * Override reinit to start watching properties.
     */
    public void reinit(TpeImageWidget iw, TpeImageInfo tii) {
        _stopMonitorOffsetSelections(selectionWatcher);

        super.reinit(iw, tii);

        SPInstObsComp inst = _iw.getInstObsComp();
        if (inst == null) return;

        OIWFS_Feature.getProps().addWatcher(this);

        _baseScreenPos = tii.getBaseScreenPos();
        _pixelsPerArcsec = tii.getPixelsPerArcsec();
        _posAngle = tii.getCorrectedPosAngleRadians();
        _withVignetting = getWithVignetting();
        _fillObscuredArea = getFillObscuredArea();

        // arrange to be notified if telescope positions are added, removed, or selected
        _monitorPosList();

        // Monitor the selections of offset positions, since that affects the positions drawn
        _monitorOffsetSelections(selectionWatcher);

        _addOIWFSFigures();
    }

    private GuideProbe getOiwfsGuideProbe(TpeContext ctx) {
        if (ctx.targets().isEmpty()) return null;

        Set<GuideProbe> guiders = GuideProbeUtil.instance.getAvailableGuiders(ctx.obsShell().get());
        for (GuideProbe guider : guiders) {
            if (guider.getType() == GuideProbe.Type.OIWFS) return guider;
        }
        return null;
    }

    private Option<SPTarget> getPrimaryTarget(TpeContext ctx, GuideProbe guider) {
        TargetEnvironment env = ctx.targets().envOrNull();
        if (env == null) return None.instance();

        Option<GuideProbeTargets> gtOpt = env.getPrimaryGuideProbeTargets(guider);
        Option<SPTarget> none = None.instance();
        return (gtOpt.isEmpty()) ? none : gtOpt.getValue().getPrimary();
    }

    private void _addOIWFSFigures() {
        // Calculate the field position for OIWFS
        // These are changed when an offset position is selected
        double basePosX = _baseScreenPos.x, basePosY = _baseScreenPos.y;
        double offsetX = 0., offsetY = 0;
        boolean isFrozen = false;
        TpeContext ctx = _iw.getContext();
        if (ctx.isEmpty()) return;

        OffsetPosList<OffsetPosBase> selectedOffsetPosList = ctx.offsets().selectedPosListOrNull();
        OffsetPosBase selectedOffsetPos                    = ctx.offsets().selectedPosOrNull();

        // If an offset position is selected, use it as the base position
        GuideProbe guider = getOiwfsGuideProbe(ctx);


        if (selectedOffsetPos != null) {
            // Get offset from base pos in pixels and determine if the guide tags are set to frozen.
            // If frozen, shift the overlay with the offset, otherwise shift only the base position used.
            // Note that the offset positions rotate with the instrument.
            Point2D.Double p = new Point2D.Double(selectedOffsetPos.getXaxis() * _pixelsPerArcsec * _flipRA,
                                                  selectedOffsetPos.getYaxis() * _pixelsPerArcsec);
            Angle.rotatePoint(p, _posAngle);
            offsetX = p.x;
            offsetY = p.y;
            basePosX -= offsetX;
            basePosY -= offsetY;

            isFrozen = (guider != null) && selectedOffsetPos.isFrozen(guider);
        }

        // Get the guide star and offset positions to use.
        // The tricky part is when the guide tag is set to FROZEN, in which case we have to use
        // the angle calculated for the, previous (unfrozen) offset position and then translate everything
        // by the frozen offset (after translating back to the base pos).
        Option<SPTarget> none = None.instance();
        TpePositionMap pm = TpePositionMap.getMap(_iw);
        Option<SPTarget> primaryOiwfs = (guider == null) ? none : getPrimaryTarget(ctx, guider);
        PosMapOffsetEntry pmoe = PosMapOffsetEntry.getPosMapOffsetEntry(pm, selectedOffsetPosList,
                                       selectedOffsetPos, guider, primaryOiwfs);
        PosMapEntry<SPSkyObject> pme = pmoe.getPosMapEntry();


        if (pme == null) {
            // no OIWFS defined yet, but we can still draw the patrol area
            _updateFigureList(0., 0., basePosX, basePosY, 0., 0., _baseScreenPos.x, _baseScreenPos.y, false);
        } else {
            // An OIWFS position is defined
            Point2D.Double p = pme.screenPos;
            if (p != null) {
                OffsetPosBase offsetPos = pmoe.getOffsetPos();
                if (isFrozen && offsetPos != null) {
                    // guide set to frozen: Use pos and angle of previous offset, but translate to selected offset pos
                    Point2D.Double pp = new Point2D.Double(offsetPos.getXaxis() * _pixelsPerArcsec * _flipRA,
                                                           offsetPos.getYaxis() * _pixelsPerArcsec);
                    Angle.rotatePoint(pp, _posAngle);
                    double bx = _baseScreenPos.x - pp.x;
                    double by = _baseScreenPos.y - pp.y;
                    _updateFigureList(p.x, p.y, bx, by, pp.x - offsetX, pp.y - offsetY, _baseScreenPos.x, _baseScreenPos.y, true);
                } else {
                    // Normal case
                    _updateFigureList(p.x, p.y, basePosX, basePosY, 0., 0., _baseScreenPos.x, _baseScreenPos.y, true);
                }
            }
            pme.taggedPos.addWatcher(this);
        }
    }

    /**
     * Implements the TelescopePosWatcher interface.
     * @param tp
     */
    public void telescopePosLocationUpdate(WatchablePos tp) {
        _redraw();
    }

    /**
     * Implements the TelescopePosWatcher interface.
     * @param tp
     */
    public void telescopePosGenericUpdate(WatchablePos tp) {
        _redraw();
    }

    public void handleTargetEnvironmentUpdate(TargetEnvironmentDiff diff) {
        _redraw();
    }

    /**
     * Override unloaded to quit watching properties.
     */
    public void unloaded() {
        OIWFS_Feature.getProps().deleteWatcher(this);
        super.unloaded();
    }

    /**
     * A property has changed.
     *
     * @see jsky.app.ot.util.PropertyWatcher
     */
    public void propertyChange(String propName) {
        _iw.repaint();
    }

    /**
     * Override getProperties to return the properties supported by this
     * feature.
     */
    public BasicPropertyList getProperties() {
        return OIWFS_Feature.getProps();
    }

    /**
     * Turn on/off the consideration of vignetting.
     */
    public void setWithVignetting(boolean withVignetting) {
        OIWFS_Feature.getProps().registerBooleanProperty(OIWFS_Feature.PROP_WITH_VIG, withVignetting);
    }

    /**
     * Get the "with vignetting" property.
     */
    public boolean getWithVignetting() {
        return OIWFS_Feature.getProps().getBoolean(OIWFS_Feature.PROP_WITH_VIG, true);
    }

    /**
     * Turn on/off the filling of the obscured area.
     */
    public void setFillObscuredArea(boolean fill) {
        OIWFS_Feature.getProps().registerBooleanProperty(OIWFS_Feature.PROP_FILL_OBSCURED, fill);
    }

    /**
     * Get the "with vignetting" property.
     */
    public boolean getFillObscuredArea() {
        return OIWFS_Feature.getProps().getBoolean(OIWFS_Feature.PROP_FILL_OBSCURED, true);
    }

    /**
     * Update the list of figures to draw.
     *
     * @param guidePosX the X screen coordinate position for the OIWFS guide star
     * @param guidePosY the Y screen coordinate position for the OIWFS guide star
     * @param offsetPosX the X screen coordinate for the selected offset
     * @param offsetPosY the X screen coordinate for the selected offset
     * @param translateX translate resulting figure by this amount of pixels in X
     * @param translateY translate resulting figure by this amount of pixels in Y
     * @param basePosX the X screen coordinate for the base position
     * @param basePosY the Y screen coordinate for the base position
     * @param oiwfsDefined set to true if an OIWFS position is defined (otherwise
     *                     the xg and yg parameters are ignored)
     */
    protected abstract void _updateFigureList(double guidePosX, double guidePosY, double offsetPosX, double offsetPosY,
                                              double translateX, double translateY, double basePosX, double basePosY, boolean oiwfsDefined);


    /** Return true if the display needs to be updated because values changed. */
    protected boolean _needsUpdate(SPInstObsComp inst, TpeImageInfo tii) {
        double pixelsPerArcsec = tii.getPixelsPerArcsec();
        double flipRA = tii.flipRA();
        Point2D.Double baseScreenPos = tii.getBaseScreenPos();
        double posAngle = tii.getCorrectedPosAngleRadians();
        boolean withVignetting = getWithVignetting();
        boolean fillObscuredArea = getFillObscuredArea();

        return ((_figureList == null) ||
                (_pixelsPerArcsec != pixelsPerArcsec) ||
                (_flipRA != flipRA) ||
                (!_baseScreenPos.equals(baseScreenPos)) ||
                (_posAngle != posAngle) ||
                (_withVignetting != withVignetting) ||
                (_fillObscuredArea != fillObscuredArea));
    }

    /**
     * Schedule a redraw of the image feature.
     */
    protected void _redraw() {
        _reinitPending = true;
        _iw.repaint();
    }


    /**
     * Draw the feature.
     */
    public void draw(Graphics g, TpeImageInfo tii) {
        SPInstObsComp inst = _iw.getInstObsComp();
        if (inst == null)
            return;

        if (_reinitPending || _needsUpdate(inst, tii)) {
            reinit();
            _reinitPending = false;
        }

        Graphics2D g2d = (Graphics2D) g;
        drawFigures(g2d, getFillObscuredArea());
    }

    @Override public boolean isEnabled(TpeContext ctx) {
        if (!super.isEnabled(ctx)) return false;
        return ctx.instrument().isDefined() && ctx.instrument().get().hasOIWFS();
    }

    public TpeImageFeatureCategory getCategory() {
        return TpeImageFeatureCategory.fieldOfView;
    }

}


