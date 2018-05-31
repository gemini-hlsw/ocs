package jsky.app.ot.gemini.tpe;

import edu.gemini.catalog.api.RadiusConstraint$;
import edu.gemini.pot.ModelConverters;
import edu.gemini.skycalc.Offset;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.spModel.gemini.gmos.GmosCommonType;
import edu.gemini.spModel.gemini.gmos.InstGmosCommon;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.guide.PatrolField;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.target.SPSkyObject;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.WatchablePos;
import edu.gemini.spModel.target.env.GuideProbeTargets;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.env.TargetEnvironmentDiff;
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe;
import edu.gemini.spModel.target.offset.OffsetPosBase;
import edu.gemini.spModel.target.offset.OffsetPosList;
import edu.gemini.spModel.util.Angle;
import jsky.app.ot.gemini.inst.WFS_FeatureBase;
import jsky.app.ot.gemini.inst.SciAreaFeature;
import jsky.app.ot.tpe.*;
import jsky.app.ot.util.BasicPropertyList;
import jsky.app.ot.util.PropertyWatcher;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * Draws the PWFS annulus.
 * <p/>
 * Note: This code is based on a Skycat plugin (tccSkyCatPlugin.tcl)
 * by D. Terrett & C. Mayer (CCLRC).
 * <p/>
 * This class is responsible for showing the shape and location of PWFS1 and PWFS2
 * in the image. The positions and angles are determined by the locations of the
 * PWFS1 and PWFS2 guide stars and the base position. In addition, if an offset
 * position is selected, it is used in place of the base position to determine the
 * PWFS position and angle.
 * <p/>
 * There are some special rules for dealing with selected offset positions.
 * In the normal case, if the offset position is set to use PWFS1, for example,
 * then selecting the offset position will cause the PWFS1 display to use that
 * position as the base position for calculations, which will change the angle
 * of the PWFS1 display.
 * <p/>
 * If the offset position has a PWFS set to "frozen", selecting it will cause the
 * previous, unfrozen offset position to be used for the angle, and the PWFS display will be
 * shifted by the selected offset position.
 *
 * @author Allan Brighton
 */
public class TpePWFSFeature extends WFS_FeatureBase implements PropertyWatcher {


    /* Drawing stuff */
    // colour for pwfs1
    private static final Color PWFS1_COLOR = Color.white;

    // colour for pwfs2
    private static final Color PWFS2_COLOR = Color.yellow;

    // Composite used for drawing items that partially block the view
    private static final Composite PARTIAL = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.15F);

    // Composite used for drawing items that block the view
    private static final Composite BLOCKED = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.50F);

   // Max number of points in a polygon used to draw a pwfs figure
    private static final int MAX_PWFS_POINTS = 21;

    /**
     * Set to if _reinit should be called to recalculate the figures.
     */
//    private boolean _reinitPending = false;

    // The position angle in radians
    private double _posAngle;

    // Addition Nod/Chop offset to use for the PWFS display.
    private Point2D.Double _nodChopOffset = new Point2D.Double(0., 0.);


    // Used to get the nod/chop offset
    private SciAreaFeature _sciAreaFeature;

    // Set to true if the offset constrained patrol field area is empty
    private boolean offsetConstrainedPatrolFieldIsEmpty = false;


    // nod/chop modes
    public static final int DEFAULT_NOD = 0;
    public static final int NOD_A_CHOP_B = 1;
    public static final int NOD_B_CHOP_A = 2;

    private static final BasicPropertyList _props = new BasicPropertyList(TpePWFSFeature.class.getName());
    public static final String PROP_DISPLAY_PWFS_AT = "Display at";
    public static final String PROP_FILL_OBSCURED = "Fill Obscured Area";
    static {
        _props.registerChoiceProperty(PROP_DISPLAY_PWFS_AT,
                new String[]{"Nod A Chop A, Nod B Chop B (default)",
                        "Nod A, Chop B",
                        "Nod B, Chop A"},
                0);
        _props.registerBooleanProperty(PROP_FILL_OBSCURED, true);
    }


    /**
     * Construct the feature with its name and description.
     */
    public TpePWFSFeature() {
        super("PWFS", "Show the field of view of the PWFS probes.");
        _props.addWatcher(this);
    }

    /**
     * Get the "with vignetting" property.
     */
    public boolean getFillObscuredArea() {
        return getProps().getBoolean(PROP_FILL_OBSCURED, true);
    }

    /**
     * If the current instrument is GMOS, return the x,y offsets for the patrol field,
     * based on the selected FP unit (in arcsec, rotated by the position angle),
     * otherwise return (0, 0).
     */
    private Point2D.Double _getPatrolFieldOffset(SPInstObsComp instr) {
        Point2D.Double offset = new Point2D.Double(0., 0.);
        if (instr instanceof InstGmosCommon) {
            InstGmosCommon inst = (InstGmosCommon) instr;
            GmosCommonType.FPUnitMode fpUnitMode = inst.getFPUnitMode();
            if (fpUnitMode == GmosCommonType.FPUnitMode.BUILTIN) {
                GmosCommonType.FPUnit fpUnit = (GmosCommonType.FPUnit) inst.getFPUnit();
                if (inst.isIFU()) {
                    offset.x = fpUnit.getWFSOffset();
                    if (_posAngle != 0.)
                        Angle.rotatePoint(offset, _posAngle);
                }
            }
        }
        return offset;
    }


    /**
     * Override getProperties to return the properties supported by this
     * feature.
     */
    public BasicPropertyList getProperties() {
        return _props;
    }

    /**
     * Static version of getProperties()
     */
    public static BasicPropertyList getProps() {
        return _props;
    }

    /**
     * Get the nod mode. One of the constants defined in SciAreaFeature:
     * DEFAULT_NOD, NOD_A_CHOP_B, NOD_B_CHOP_A.
     */
    public static int getNodMode() {
        return _props.getChoice(PROP_DISPLAY_PWFS_AT, DEFAULT_NOD);
    }


    /**
     * A property has changed (redefined from parent class to check for
     * WFS display change).
     *
     * @see PropertyWatcher
     */
    public void propertyChange(String propName) {
        redraw();
    }


    // Return the TpePWFSFeature, or null if none is defined yet.
    private SciAreaFeature _getSciAreaFeature() {
        TelescopePosEditor tpe = TpeManager.get();
        if (tpe != null) {
            return (SciAreaFeature) tpe.getFeature(SciAreaFeature.class);
        }
        return null;
    }

    private final PropertyChangeListener selListener = evt -> redraw();

    /**
     * Reinitialize (recalculate the positions and redraw).
     */
    public void reinit(TpeImageWidget iw, TpeImageInfo tii) {
        _stopMonitorOffsetSelections(selListener);

        super.reinit(iw, tii);

        offsetConstrainedPatrolFieldIsEmpty = false;

        // Get a reference to the TPE PWFS feature, used to draw the PWFS at the nod/chop offset
        if (_sciAreaFeature == null)
            _sciAreaFeature = _getSciAreaFeature();

        SPInstObsComp inst = _iw.getInstObsComp();
        _posAngle = tii.getCorrectedPosAngleRadians();

        // get any nod/chop offset
        _nodChopOffset = getNodChopOffset();

        // arrange to be notified if the PWFS targets are added, removed, or selected
        _monitorPosList();

        // Monitor the selections of offset positions, since that affects the positions drawn
        _monitorOffsetSelections(selListener);

        clearFigures();
        _addPWFSFigures(inst, tii);
    }

    private Option<SPTarget> getPrimaryTarget(TpeContext ctx, PwfsGuideProbe guider) {
        final Option<SPTarget> none = None.instance();

        TargetEnvironment env = ctx.targets().envOrNull();
        if (env == null) return none;
        Option<GuideProbeTargets> gtOpt = env.getPrimaryGuideProbeTargets(guider);
        return (gtOpt.isEmpty()) ? none : gtOpt.getValue().getPrimary();
    }


    // Add the figures representing the PWFS patrol area and probe arms to the display list
    private void _addPWFSFigures(SPInstObsComp inst, TpeImageInfo tii) {
        final double pixelsPerArcsec = tii.getPixelsPerArcsec();
        double radius = (int) (pixelsPerArcsec * PwfsGuideProbe.pwfs1.getRadius() + 0.5);
        final Point2D.Double baseScreenPos = tii.getBaseScreenPos();
        Point2D.Double _patrolFieldOffset = _getPatrolFieldOffset(inst);
        double pfXOffset = _patrolFieldOffset.x * pixelsPerArcsec;
        double pfYOffset = _patrolFieldOffset.y * pixelsPerArcsec;

        // These are changed when an offset position is selected
        double basePosX = baseScreenPos.x, basePosY = baseScreenPos.y;
        double offsetX = 0., offsetY = 0;
        boolean isFrozen1 = false, isFrozen2 = false;

        TpeContext ctx = _iw.getContext();
        if (ctx.isEmpty()) return;

        OffsetPosList<OffsetPosBase> selectedOffsetPosList = ctx.offsets().selectedPosListOrNull();
        OffsetPosBase selectedOffsetPos = ctx.offsets().selectedPosOrNull();

        // If an offset position is selected, use it as the base position
        if (selectedOffsetPos != null) {
            // Get offset from base pos in pixels and determine if the guide tags are set to frozen.
            // If frozen, shift the overlay with the offset, otherwise shift only the base position used.
            // Note that the offset positions rotate with the instrument.
            Point2D.Double p = new Point2D.Double(selectedOffsetPos.getXaxis() * pixelsPerArcsec,
                    selectedOffsetPos.getYaxis() * pixelsPerArcsec);
            Angle.rotatePoint(p, _posAngle);
            offsetX = p.x;
            offsetY = p.y;
            basePosX -= offsetX;
            basePosY -= offsetY;
            isFrozen1 = selectedOffsetPos.isFrozen(PwfsGuideProbe.pwfs1);
            isFrozen2 = selectedOffsetPos.isFrozen(PwfsGuideProbe.pwfs2);
        }

        final double x = baseScreenPos.x + _nodChopOffset.x + pfXOffset;
        final double y = baseScreenPos.y + _nodChopOffset.y + pfYOffset;
        edu.gemini.skycalc.Angle rotation = new edu.gemini.skycalc.Angle(-_posAngle, edu.gemini.skycalc.Angle.Unit.RADIANS);
        Point2D.Double translation = new Point2D.Double(x - offsetX, y - offsetY);
        Point2D.Double offsetTrans = new Point2D.Double(x, y);
        AffineTransform flipRATransform = AffineTransform.getScaleInstance(_flipRA, 1.0);
        setTransformationToScreen(rotation, pixelsPerArcsec, translation);
        for (ObsContext obsCtx : _iw.getMinimalObsContext()) {
            try {
                addRadiusLimits(RadiusConstraint$.MODULE$.between(ModelConverters.toNewAngle(PwfsGuideProbe.PWFS_RADIUS), ModelConverters.toNewAngle(PwfsGuideProbe.pwfs1.getVignettingClearance(obsCtx))), PWFS1_COLOR);
                addRadiusLimits(RadiusConstraint$.MODULE$.between(ModelConverters.toNewAngle(PwfsGuideProbe.PWFS_RADIUS), ModelConverters.toNewAngle(PwfsGuideProbe.pwfs2.getVignettingClearance(obsCtx))), PWFS2_COLOR);
            } catch(IllegalArgumentException e) {
                // ignore: some instruments don't support PWFS
                return;
            }
        }
        Area pwfs1 = PwfsGuideProbe.pwfs1.getPatrolField().getArea().createTransformedArea(flipRATransform).createTransformedArea(getTransformationToScreen());
        Area pwfs2 = PwfsGuideProbe.pwfs2.getPatrolField().getArea().createTransformedArea(flipRATransform).createTransformedArea(getTransformationToScreen());

        setTransformationToScreen(rotation, pixelsPerArcsec, offsetTrans);
        offsetConstrainedPatrolFieldIsEmpty = false;
        for (PwfsGuideProbe pwfs : new PwfsGuideProbe[]{PwfsGuideProbe.pwfs1, PwfsGuideProbe.pwfs2}) {
            PatrolField patrolField = pwfs.getPatrolField();
            if (!_iw.getMinimalObsContext().isEmpty() && !(_iw.getMinimalObsContext().getValue().getSciencePositions() == null)) {
                Set<Offset> offsets = _iw.getMinimalObsContext().getValue().getSciencePositions();
                offsetConstrainedPatrolFieldIsEmpty = offsetConstrainedPatrolFieldIsEmpty || patrolField.outerLimitOffsetIntersection(offsets).isEmpty();
                addOffsetConstrainedPatrolField(patrolField, offsets);
            }
        }

        // Derive conversion factor to go from mm in focal plane to pixels
        double mm2Pixels = radius * PwfsGuideProbe.getMmFactor() / PwfsGuideProbe.pwfs1.getRadius();

        // Get the guide star and offset positions to use.
        // The tricky part is when the guide tag is set to FROZEN, in which case we have to use
        // the angle calculated for the, previous (unfrozen) offset position and then translate everything
        // by the frozen offset (after translating back to the base pos).
        TpePositionMap pm = TpePositionMap.getMap(_iw);

        Option<SPTarget> primaryPwfs1 = getPrimaryTarget(ctx, PwfsGuideProbe.pwfs1);
        final PosMapOffsetEntry pmoe1 = PosMapOffsetEntry.getPosMapOffsetEntry(pm, selectedOffsetPosList, selectedOffsetPos, PwfsGuideProbe.pwfs1, primaryPwfs1);
        final PosMapEntry<SPSkyObject> pme1 = pmoe1.getPosMapEntry();
        final OffsetPosBase obp1 = pmoe1.getOffsetPos();
        if (pme1 != null) {
            calculateWFSForGuideProbe(pme1.screenPos, obp1, pixelsPerArcsec, baseScreenPos, pfXOffset, pfYOffset, basePosX, basePosY, offsetX, offsetY, isFrozen1, pwfs1, mm2Pixels, PwfsGuideProbe.pwfs1);
            pme1.taggedPos.addWatcher(this);
        }

        Option<SPTarget> primaryPwfs2 = getPrimaryTarget(ctx, PwfsGuideProbe.pwfs2);
        final PosMapOffsetEntry pmoe2 = PosMapOffsetEntry.getPosMapOffsetEntry(pm, selectedOffsetPosList, selectedOffsetPos, PwfsGuideProbe.pwfs2, primaryPwfs2);
        final PosMapEntry<SPSkyObject> pme2 = pmoe2.getPosMapEntry();
        final OffsetPosBase obp2 = pmoe1.getOffsetPos();
        if (pme2 != null) {
            calculateWFSForGuideProbe(pme2.screenPos, obp2, pixelsPerArcsec, baseScreenPos, pfXOffset, pfYOffset, basePosX, basePosY, offsetX, offsetY, isFrozen2, pwfs2, mm2Pixels, PwfsGuideProbe.pwfs2);
            pme2.taggedPos.addWatcher(this);
        }
    }

    /**
     * Calculate the projected shape of a WFS in the focal plane of the image
     * and add the necessary figures to the figure list.
     *
     * @param x      the X screen coordinate position for the guide star
     * @param y      the Y screen coordinate position for the guide star
     * @param xc     the X screen coordinate for the base position
     * @param yc     the Y screen coordinate for the base position
     * @param mm2Pix used to convert mm to pixels
     * @param tx     translate resulting figure by this amount of pixels in X
     * @param ty     translate resulting figure by this amount of pixels in Y
     */
    public void calcWfs(int wfs, double x, double y, double xc, double yc, double mm2Pix,
                               double tx, double ty, final Point2D.Double nodChopOffset, final Double flipRA) {
        tx += nodChopOffset.x;
        ty += nodChopOffset.y;

        // Set up parameters for appropriate wfs
        final boolean toggle = flipRA != -1; // allan: new default: 27-04-01
        final Color pwfsColor = wfs == 1 ? PWFS1_COLOR : PWFS2_COLOR;
        final PwfsGuideProbe probe = wfs == 1 ? PwfsGuideProbe.pwfs1 : PwfsGuideProbe.pwfs2;

        final double pwfsScale = probe.getScale();
        final double pwfsFplaneSep = probe.getFPlaneSep();
        final double pwfsMwidth = probe.getMWidth();
        final double pwfsMlength = probe.getMLength();
        final double pwfsAwidth = probe.getAWidth();

        // WFS position relative to field centre with allowance for height of wfs above focal plane.
        final double dx = (x - xc) * pwfsScale;
        final double dy = (y - yc) * pwfsScale;

        // Half the distance of the WFS from the field centre.
        final double s2 = Math.sqrt(dx * dx + dy * dy) / 2.;

        // Bearing of WFS.
        final double b = Math.atan2(-dy, dx);

        // Radius (in pixels) of the WFS table
        final double r = PwfsGuideProbe.getArmLength() * mm2Pix;

        // Angle between WFS bearing and arm pivot.
        final double d = Math.acos(s2 / r);

        // X/Y of pivot.
        final double xp = toggle ? xc + r * Math.cos(b + d) : xc + r * Math.cos(b - d);
        final double yp = toggle ? yc - r * Math.sin(b + d) : yc - r * Math.cos(b - d);

        // Set offset for determining the completely vignetted/unvignetted coords
        // of a point
        final double dr = PwfsGuideProbe.getM2Radius() * pwfsFplaneSep / PwfsGuideProbe.getM2FplaneSep();

        // Coords of corners of object for fully vignetted case when it is aligned
        // along the x axis with the end of the object furthest from the pivot
        // point.

        // Partially vignetted region
        final double[] xyList = new double[MAX_PWFS_POINTS * 2];

        // Continue with straight line segments. First compute how close to the main
        // arm you can get in the x direction
        final double step = (pwfsAwidth - pwfsMwidth) / 2.0;
        final double dxl = Math.sqrt(dr * dr - (dr - step) * (dr - step));

        //Imperative
        int i = 0; // index into xyList

        double x1 = (pwfsMlength / 2.0 + dr) * mm2Pix + r;
        double y1 = 0.0;
        xyList[i++] = x1;
        xyList[i++] = y1;

        double x2 = x1;
        double y2 = (pwfsMwidth / 2.0) * mm2Pix;
        xyList[i++] = x2;
        xyList[i++] = y2;

        // define a point at 45 degs
        double x3 = (pwfsMlength / 2.0 + dr / Math.sqrt(2.0)) * mm2Pix + r;
        double y3 = y2 + dr / Math.sqrt(2.0) * mm2Pix;
        xyList[i++] = x3;
        xyList[i++] = y3;

        // Now one at 90 degs
        double x4 = (pwfsMlength / 2.0) * mm2Pix + r;
        double y4 = y2 + dr * mm2Pix;
        xyList[i++] = x4;
        xyList[i++] = y4;

        double x5 = (-pwfsMlength / 2.0 + dxl) * mm2Pix + r;
        double y5 = y4;
        xyList[i++] = x5;
        xyList[i++] = y5;

        // Now reflect through the y axis
        xyList[i++] = x5;
        xyList[i++] = -y5;

        xyList[i++] = x4;
        xyList[i++] = -y4;

        xyList[i++] = x3;
        xyList[i++] = -y3;

        xyList[i++] = x2;
        xyList[i++] = -y2;

        xyList[i++] = x1;
        xyList[i++] = -y1;

        final Figure pwfsFigure = new Figure(PwfsGuideProbe.buildFigure(pwfsScale, xc, yc, xp, yp, dx, dy, xyList, i, tx, ty), pwfsColor, PARTIAL, new BasicStroke());

        // Now do the arm. It did not prove possible to draw a symmetric arm in one go.
        // Hence, the arm is drawn in two halves with the points in the list ordered
        // in the same way. This seems to be a consequnce of the way the create polygon
        // command with the "smooth" option works. Note also that P1 and P2 have
        // to be treated differently in the region where the mirror is attached to
        // the arm due to the different mechanical arrangements.

        i = 0; // reset list index

        x1 = x5;
        y1 = 0.0;
        xyList[i++] = x1;
        xyList[i++] = y1;

        x2 = x5;
        y2 = y5;
        xyList[i++] = x2;
        xyList[i++] = y2;

        x3 = (-pwfsMlength / 2.0) * mm2Pix + r;
        y3 = (pwfsAwidth / 2.0 + dr) * mm2Pix;

        // Add an extra point in for P2
        double x23 = 0.0, y23 = 0.0;
        if (wfs == 1) {
            xyList[i++] = x3;
            xyList[i++] = y3;
        } else {
            x23 = (x2 + x3) / 2.0;
            y23 = (y2 + y3) / 2.0;
            xyList[i++] = x23;
            xyList[i++] = y23;
            xyList[i++] = x3;
            xyList[i++] = y3;
        }

        // Add another point part way down the arm to get a smoother curve
        x4 = x3 - dr * mm2Pix;
        y4 = y3;
        xyList[i++] = x4;
        xyList[i++] = y4;

        x5 = 0.0;
        y5 = y3;
        xyList[i++] = x5;
        xyList[i++] = y5;

        double x6 = 0.0;
        double y6 = 0.0;
        xyList[i++] = x6;
        xyList[i++] = y6;

        double x7 = x1;
        double y7 = y1;
        xyList[i++] = x7;
        xyList[i++] = y7;

        final Figure halfArmFigure = new Figure(PwfsGuideProbe.buildFigure(pwfsScale, xc, yc, xp, yp, dx, dy, xyList, i, tx, ty), pwfsColor, PARTIAL, new BasicStroke());

        // Now reflect in the y axis to draw the other half of the arm

        i = 0; // reset list index
        xyList[i++] = x1;
        xyList[i++] = -y1;

        xyList[i++] = x2;
        xyList[i++] = -y2;

        if (wfs == 2) {
            xyList[i++] = x23;
            xyList[i++] = -y23;
        }
        xyList[i++] = x3;
        xyList[i++] = -y3;

        xyList[i++] = x4;
        xyList[i++] = -y4;

        xyList[i++] = x5;
        xyList[i++] = -y5;

        xyList[i++] = x6;
        xyList[i++] = -y6;

        xyList[i++] = x7;
        xyList[i++] = -y7;

        final Figure otherHalfArmFigure = new Figure(PwfsGuideProbe.buildFigure(pwfsScale, xc, yc, xp, yp, dx, dy, xyList, i, tx, ty), pwfsColor, PARTIAL, new BasicStroke());

        // Mirror

        x1 = (pwfsMlength / 2.0 - dr) * mm2Pix + r;
        y1 = (-pwfsMwidth / 2.0 + dr) * mm2Pix;

        x2 = x1;
        y2 = -y1;

        x3 = (-pwfsMlength / 2.0 + dr) * mm2Pix + r;
        y3 = y2;

        x4 = x3;
        y4 = y1;

        i = 0; // reset list index

        xyList[i++] = x1;
        xyList[i++] = y1;
        xyList[i++] = x2;
        xyList[i++] = y2;
        xyList[i++] = x3;
        xyList[i++] = y3;
        xyList[i++] = x4;
        xyList[i++] = y4;
        final Figure mirror = new Figure(PwfsGuideProbe.buildFigure(pwfsScale, xc, yc, xp, yp, dx, dy, xyList, i, tx, ty), pwfsColor, null, new BasicStroke());
        // Fully vignetted arm

        i = 0; // reset list index

        x1 = x4;
        y1 = 0.0;
        xyList[i++] = x1;
        xyList[i++] = y1;

        x2 = x3;
        y2 = y3;
        xyList[i++] = x2;
        xyList[i++] = y2;

        x3 = (-pwfsMlength / 2.0) * mm2Pix + r;
        y3 = y2;
        xyList[i++] = x3;
        xyList[i++] = y3;

        // Place a couple of points along the arc or line to ensure a smooth curve
        // Calculate the end points first
        x6 = x3 - dxl * mm2Pix;
        y6 = (pwfsAwidth / 2.0 - dr) * mm2Pix;

        if (wfs == 1) {
            double theta = Math.asin(dxl / dr) / 3.0;
            x4 = x3 - dr * Math.sin(theta) * mm2Pix;
            y4 = y3 + (dr * (1.0 - Math.cos(theta)) * mm2Pix);
            xyList[i++] = x4;
            xyList[i++] = y4;

            x5 = x3 - dr * Math.sin(2.0 * theta) * mm2Pix;
            y5 = y3 + (dr * (1.0 - Math.cos(2.0 * theta)) * mm2Pix);
            xyList[i++] = x5;
            xyList[i++] = y5;
        } else {
            x4 = x3 + (x6 - x3) / 3.0;
            y4 = y3 + (y6 - y3) / 3.0;
            x5 = x3 + 2.0 * (x6 - x3) / 3.0;
            y5 = y3 + 2.0 * (y6 - y3) / 3.0;
            xyList[i++] = x4;
            xyList[i++] = y4;
            xyList[i++] = x5;
            xyList[i++] = y5;
        }

        xyList[i++] = x6;
        xyList[i++] = y6;

        x7 = 0.0;
        y7 = y6;
        xyList[i++] = x7;
        xyList[i++] = y7;

        // Now reflect the points through the y axis
        xyList[i++] = x7;
        xyList[i++] = -y7;

        xyList[i++] = x6;
        xyList[i++] = -y6;

        xyList[i++] = x5;
        xyList[i++] = -y5;

        xyList[i++] = x4;
        xyList[i++] = -y4;

        xyList[i++] = x3;
        xyList[i++] = -y3;

        xyList[i++] = x2;
        xyList[i++] = -y2;

        xyList[i++] = x1;
        xyList[i++] = -y1;

        final Figure vignettedArm = new Figure(PwfsGuideProbe.buildFigure(pwfsScale, xc, yc, xp, yp, dx, dy, xyList, i, tx, ty), pwfsColor, BLOCKED, new BasicStroke());

        addFigure(pwfsFigure);
        addFigure(halfArmFigure);
        addFigure(otherHalfArmFigure);
        addFigure(mirror);
        addFigure(vignettedArm);
    }

    private void calculateWFSForGuideProbe(Point2D.Double screenPos, OffsetPosBase offsetPos, final double pixelsPerArcsec, final Point2D.Double baseScreenPos, final double pfXOffset, final double pfYOffset, final double basePosX, final double basePosY, final double offsetX, final double offsetY, final boolean isFrozen, Area patrolField, double mm2Pixels, PwfsGuideProbe guideProbe) {
        double bx, by;
        // only display if guide star is within the patrol area
        int probeIndex = (guideProbe == PwfsGuideProbe.pwfs1) ? 1 : 2;
        if (screenPos != null && patrolField.contains(screenPos.x, screenPos.y)) {
            if (isFrozen && offsetPos != null) {
                // Use pos and angle of previous offset, but translate to selected offset pos
                Point2D.Double pp = new Point2D.Double(offsetPos.getXaxis() * pixelsPerArcsec,
                        offsetPos.getYaxis() * pixelsPerArcsec);
                Angle.rotatePoint(pp, _posAngle);
                bx = baseScreenPos.x - pp.x + pfXOffset;
                by = baseScreenPos.y - pp.y + pfYOffset;
                calcWfs(probeIndex, screenPos.x, screenPos.y, bx, by, mm2Pixels, pp.x - offsetX, pp.y - offsetY, _nodChopOffset, _flipRA);
            } else {
                bx = basePosX + pfXOffset;
                by = basePosY + pfYOffset;
                calcWfs(probeIndex, screenPos.x, screenPos.y, bx, by, mm2Pixels, 0., 0., _nodChopOffset, _flipRA);
            }
        }
    }

    /**
     * Implements the TelescopePosWatcher interface.
     * @param tp
     */
    public void telescopePosLocationUpdate(WatchablePos tp) {
        redraw();
    }

    /**
     * Implements the TelescopePosWatcher interface.
     * @param tp
     */
    public void telescopePosGenericUpdate(WatchablePos tp) {
        redraw();
    }

    private boolean primaryUpdated(GuideProbe probe, TargetEnvironment oldEnv, TargetEnvironment newEnv) {
        Option<GuideProbeTargets> oldTargetsOpt = oldEnv.getPrimaryGuideProbeTargets(probe);
        Option<GuideProbeTargets> newTargetsOpt = newEnv.getPrimaryGuideProbeTargets(probe);
        if (oldTargetsOpt.isEmpty() || newTargetsOpt.isEmpty()) return false;

        Option<SPTarget> oldPrimary = oldTargetsOpt.getValue().getPrimary();
        Option<SPTarget> newPrimary = newTargetsOpt.getValue().getPrimary();

        if (oldPrimary.isEmpty()) {
            return !newPrimary.isEmpty();
        } else if (newPrimary.isEmpty()) {
            return true;
        }
        return oldPrimary.getValue() != newPrimary.getValue();
    }

    public void handleTargetEnvironmentUpdate(TargetEnvironmentDiff diff) {
        TargetEnvironmentDiff pwfs1, pwfs2;
        pwfs1 = TargetEnvironmentDiff.guideProbe(diff.getOldEnvironment(), diff.getNewEnvironment(), PwfsGuideProbe.pwfs1);
        pwfs2 = TargetEnvironmentDiff.guideProbe(diff.getOldEnvironment(), diff.getNewEnvironment(), PwfsGuideProbe.pwfs2);

        if ((pwfs1.getAddedTargets().size() > 0) ||
                (pwfs2.getAddedTargets().size() > 0) ||
                (pwfs1.getRemovedTargets().size() > 0) ||
                (pwfs2.getRemovedTargets().size() > 0)) {
            redraw();
        } else {
            TargetEnvironment newEnv = diff.getNewEnvironment();
            TargetEnvironment oldEnv = diff.getOldEnvironment();
            if (primaryUpdated(PwfsGuideProbe.pwfs1, oldEnv, newEnv) ||
                    primaryUpdated(PwfsGuideProbe.pwfs2, oldEnv, newEnv)) {
                redraw();
            }
        }
    }

    public TpeImageFeatureCategory getCategory() {
        return TpeImageFeatureCategory.fieldOfView;
    }

    /**
     * Return the Nod/Chop offset in screen pixels for the PWFS display.
     *
     * @see jsky.app.ot.gemini.trecs.TReCS_SciAreaFeature
     */
    public Point2D.Double getNodChopOffset() {
        return (_sciAreaFeature == null) ? new Point2D.Double() : _sciAreaFeature.getNodChopOffset();
    }


    /**
     * Schedule a redraw of the image feature.
     */
    public void redraw() { if (_iw != null) _iw.repaint(); }

    /**
     * Draw the feature.
     */
    public void draw(Graphics g, TpeImageInfo tii) {
        reinit();
        Graphics2D g2d = (Graphics2D) g;
        drawFigures(g2d, getFillObscuredArea());
    }


    private static final TpeMessage WARNING = TpeMessage.warningMessage(
            "No valid PWFS region.  Check offset positions.");

    @Override
    public Option<Collection<TpeMessage>> getMessages() {
        if (offsetConstrainedPatrolFieldIsEmpty) {
            return new Some<>(Collections.singletonList(WARNING));
        } else {
            return None.instance();
        }
    }

}

