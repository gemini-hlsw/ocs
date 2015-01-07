// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: GMOS_OIWFS_Feature.java 45719 2012-06-01 16:35:09Z swalker $
//
package jsky.app.ot.gemini.gmos;

import diva.util.java2d.Polygon2D;
import edu.gemini.skycalc.Angle;
import edu.gemini.skycalc.Offset;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.spModel.gemini.gmos.GmosCommonType;
import edu.gemini.spModel.gemini.gmos.GmosOiwfsGuideProbe;
import edu.gemini.spModel.gemini.gmos.GmosOiwfsProbeArm;
import edu.gemini.spModel.gemini.gmos.InstGmosCommon;
import edu.gemini.spModel.guide.PatrolField;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.target.offset.OffsetPosBase;
import edu.gemini.spModel.telescope.IssPort;
import jsky.app.ot.gemini.inst.OIWFS_FeatureBase;
import jsky.app.ot.tpe.TpeContext;
import jsky.app.ot.tpe.TpeImageInfo;
import jsky.app.ot.tpe.TpeMessage;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.List;


/**
 * Draws the OIWFS overlay for GMOS.
 */
public class GMOS_OIWFS_Feature extends OIWFS_FeatureBase {
    // The color to use to draw the OIWFS probe arm
    private static final Color PROBE_ARM_COLOR = Color.red;

    // Composite used for drawing items that block the view
    private static final Composite BLOCKED = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5F);

//    // The offsets (from the base pos) and dimensions of the OIWFS patrol field (in arcsec)
//    private static final Shape PATROL_FIELD = GmosOiwfsGuideProbe.instance.getPatrolField().getArea();

    // The following values (in arcsec) are used to calculate the position of the OIWFS arm
    // and are described in the paper "Opto-Mechanical Design of the Gemini Multi-Object
    // Spectrograph On-Instrument Wavefront Sensor".
    private static final double TX = -427.52;  // Location of base stage in arcsec
    private static final double TZ = -101.84;

    private static final double BX = 124.89;   // Length of stage arm in arcsec
    private static final double MX = 358.46;   // Length of pick-off arm in arcsec


    // The size of the OIWFS probe arm pickoff mirror in arcsec
    private static final double PICKOFF_MIRROR_SIZE = 20.;

    // Set to true if the offset constrained patrol field area is empty
    private boolean offsetConstrainedPatrolFieldIsEmpty = false;

    /**
     * Construct the feature with its name and description.
     */
    public GMOS_OIWFS_Feature() {
        super("GMOS OIWFS", "Show the GMOS OIWFS patrol field and arm.");
        setFillObscuredArea(true);
    }

    protected void addPatrolField(double xc, double yc) {
        // RCN: this won't work if there's no obs context
        for (ObsContext ctx : _iw.getMinimalObsContext()) {
            for (PatrolField patrolField : GmosOiwfsGuideProbe.instance.getCorrectedPatrolField(ctx)) {
                // rotation, scaling and transformation to match screen coordinates
                Angle rotation = new Angle(-_posAngle, Angle.Unit.RADIANS);
                Point2D.Double translation = new Point2D.Double(xc, yc);
                setTransformationToScreen(rotation, _pixelsPerArcsec, translation);

                addPatrolField(patrolField);
            }
        }
    }

    /**
     * Add the OIWFS patrol field to the list of figures to display.
     *
     * @param xc the X screen coordinate for the base position to use
     * @param yc the Y screen coordinate for the base position to use
     */
    protected void addOffsetConstrainedPatrolField(final double xc, final double yc) {
        final Set<Offset> offsets = _iw.getContext().offsets().scienceOffsetsJava();

        for (ObsContext ctx : _iw.getMinimalObsContext()) {
            for (PatrolField patrolField : GmosOiwfsGuideProbe.instance.getCorrectedPatrolField(ctx)) {
                offsetConstrainedPatrolFieldIsEmpty = patrolField.outerLimitOffsetIntersection(offsets).isEmpty() ? true : false;
                // rotation, scaling and transformation to match screen coordinates
                final Angle rotation = new Angle(-_posAngle, Angle.Unit.RADIANS);
                final Point2D.Double translation = new Point2D.Double(xc, yc);
                setTransformationToScreen(rotation, _pixelsPerArcsec, translation);
                addOffsetConstrainedPatrolField(patrolField, offsets);
            }
        }
    }

    // Get the offset that should be used as the "base" for drawing the probe
    // arm.  This is either the selected offset position (if any), otherwise
    // whatever offset position is considered the default one (the first), or
    // failing all else a 0 offset (which corresponds to the base)..
    private Offset getProbeArmOffset() {
        TpeContext ctx = _iw.getContext();
        OffsetPosBase selOffset = ctx.offsets().selectedPosOrNull();

        // TPE REFACTOR - default offset?
//        if (selOffsetOpt.isEmpty()) selOffsetOpt = progData.getDefaultOffsetPos();

        return (selOffset == null) ? Offset.ZERO_OFFSET : selOffset.toSkycalcOffset();
    }

    /**
     * Add the OIWFS probe arm to the list of figures to display.
     * Calculates the OIWFS arm position (see eq. 21 - 28 in GMOS paper).
     *
     * @param xg the X screen coordinate position for the guide star
     * @param yg the Y screen coordinate position for the guide star
     * @param xc the X screen coordinate for the base position
     * @param yc the Y screen coordinate for the base position
     * @param xt translate resulting figure by this amount of pixels in X
     * @param yt translate resulting figure by this amount of pixels in Y
     * @param flip if true, flip the probe arm about the base position X axis
     */
    protected void _addProbeArm(double xg, double yg, double xc, double yc, double xt, double yt, boolean flip) {
        int sign = (flip ? -1 : 1);

        //get the selected offset, or the default offset(first one) if none is selected
        Option<ObsContext> obsCtxOpt=_iw.getMinimalObsContext();
        ObsContext ctx = obsCtxOpt.getOrNull();
        if(ctx==null)return;

        //Draw probe arm if guide star is inside the selected offset patrol field.
        Offset probeArmOffset = getProbeArmOffset();
        if (!GmosOiwfsGuideProbe.instance.inRange(ctx, probeArmOffset)) return;

        // get the additional (IFU) patrol field offset in screen coords, rotated by the position angle
        Point2D.Double ifuOffset = new Point2D.Double(_getPatrolFieldXOffset((InstGmosCommon) _iw.getInstObsComp()), 0);
        edu.gemini.spModel.util.Angle.rotatePoint(ifuOffset, _posAngle);
        xc += ifuOffset.x * _flipRA;
        yc += ifuOffset.y;

        // get the GMOS constants in screen coords
        Point2D.Double t = new Point2D.Double(TX * _pixelsPerArcsec * _flipRA,
                                              TZ * _pixelsPerArcsec * sign);
        edu.gemini.spModel.util.Angle.rotatePoint(t, _posAngle);
        double tx = t.x;
        double tz = t.y;

        // get the location of the OIWFS guide star in screen coordinates
        Point2D.Double tp = new Point2D.Double(xg, yg);

        // get x and y based on offset of tp to base pos
        double x = tx + tp.x - xc;
        double y = tz + tp.y - yc;

        double r = Math.sqrt(x * x + y * y);
        double mxx = MX * _pixelsPerArcsec;
        double mx = mxx * _flipRA;
        double mx2 = mx * mx;
        double bx = BX * _pixelsPerArcsec;
        double bx2 = bx * bx;

        // Calculate the relevant angles
        @SuppressWarnings({"SuspiciousNameCombination"}) double alpha = Math.atan2(x, y);
        double r2 = r * r;

        // Grim hack to handle an edge case.
        double acosArg = (r*r - (bx2 + mx2)) / (2 * bx * mx);
        if (acosArg > 1.0) {
            acosArg = 1.0;
        } else if (acosArg < -1.0) {
            acosArg = -1.0;
        }
        double phi = sign * Math.acos(acosArg);

        double theta = Math.asin((mx / r) * Math.sin(phi));
        if (mx2 > (r2 + bx2)) {
            theta = Math.PI - theta;
        }

        // Set up the transformation to rotate by the angles calculated above
        // (minus PI/2 to make up for the starting angle?)
        AffineTransform armTrans = new AffineTransform();
        double angle = phi - theta - alpha - Math.PI / 2.;
        armTrans.translate(xt, yt);
        armTrans.rotate(angle, tp.x, tp.y);

        // add the figures to the display list with required translation, reflection, and scaling
        armTrans.concatenate(AffineTransform.getTranslateInstance(tp.getX(), tp.getY()));
        armTrans.concatenate(AffineTransform.getScaleInstance(_flipRA, 1.0));
        armTrans.concatenate(AffineTransform.getScaleInstance(_pixelsPerArcsec, _pixelsPerArcsec));
        final List<Shape> shapes = GmosOiwfsProbeArm.transformedGeometryAsJava(armTrans);
        for (final Shape s: shapes)
            _figureList.add(new Figure(s, PROBE_ARM_COLOR, BLOCKED, OIWFS_STROKE));
    }


    /** Return the X offset for the patrol field based on the selected FP unit (in arcsec) */
    private double _getPatrolFieldXOffset(InstGmosCommon inst) {
        return ((GmosCommonType.FPUnit)inst.getFPUnit()).getWFSOffset();
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
    protected void _updateFigureList(double guidePosX, double guidePosY, double offsetPosX, double offsetPosY,
                                     double translateX, double translateY, double basePosX, double basePosY, boolean oiwfsDefined) {

        // need to flip the drawing about the X axis if the instrument is side-mounted
        InstGmosCommon inst = (InstGmosCommon) _iw.getInstObsComp();
        boolean flip = (inst.getIssPort() == IssPort.SIDE_LOOKING);

        _figureList.clear();
        addOffsetConstrainedPatrolField(basePosX, basePosY);
        addPatrolField(offsetPosX + translateX, offsetPosY + translateY);
        if (oiwfsDefined) {
            _addProbeArm(guidePosX, guidePosY, offsetPosX, offsetPosY, translateX, translateY, flip);
        }
    }


    /** Return true if the display needs to be updated because values changed. */
    protected boolean _needsUpdate(SPInstObsComp inst, TpeImageInfo tii) {
        // Needs to take into account offset position list updates to work
        // as intended.  Unclear whether it is worth the effort to maintain
        // the old offset lists when the calculation isn't that slow anyway.
        return true;
    }

    private static final TpeMessage WARNING = TpeMessage.warningMessage(
            "No valid OIWFS region.  Check offset positions.");

    @Override
    public Option<Collection<TpeMessage>> getMessages() {
        if (offsetConstrainedPatrolFieldIsEmpty) {
            return new Some<Collection<TpeMessage>>(Collections.singletonList(WARNING));
        } else {
            return None.instance();
        }
    }
}

