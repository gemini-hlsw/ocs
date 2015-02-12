// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: GMOS_OIWFS_Feature.java 45719 2012-06-01 16:35:09Z swalker $
//
package jsky.app.ot.gemini.gmos;

import edu.gemini.skycalc.Angle;
import edu.gemini.skycalc.Offset;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
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

    // Set to true if the offset constrained patrol field area is empty
    private boolean offsetConstrainedPatrolFieldIsEmpty = false;

    // The TPE warning message to return in there is no valid region.
    private static final TpeMessage WARNING = TpeMessage.warningMessage("No valid OIWFS region.  Check offset positions.");

    /**
     * Construct the feature with its name and description.
     */
    public GMOS_OIWFS_Feature() {
        super("GMOS OIWFS", "Show the GMOS OIWFS patrol field and arm.");
        setFillObscuredArea(true);
    }

    protected void addPatrolField(double xc, double yc) {
        // RCN: this won't work if there's no obs context
        for (final ObsContext ctx : _iw.getMinimalObsContext()) {
            for (final PatrolField patrolField : GmosOiwfsGuideProbe.instance.getCorrectedPatrolField(ctx)) {
                // rotation, scaling and transformation to match screen coordinates
                final Angle rotation = new Angle(-_posAngle, Angle.Unit.RADIANS);
                final Point2D.Double translation = new Point2D.Double(xc, yc);
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

        for (final ObsContext ctx : _iw.getMinimalObsContext()) {
            for (final PatrolField patrolField : GmosOiwfsGuideProbe.instance.getCorrectedPatrolField(ctx)) {
                offsetConstrainedPatrolFieldIsEmpty = patrolField.outerLimitOffsetIntersection(offsets).isEmpty() ? true : false;
                // rotation, scaling and transformation to match screen coordinates
                final Angle rotation = new Angle(-_posAngle, Angle.Unit.RADIANS);
                final Point2D.Double translation = new Point2D.Double(xc, yc);
                setTransformationToScreen(rotation, _pixelsPerArcsec, translation);
                addOffsetConstrainedPatrolField(patrolField, offsets);
            }
        }
    }

    /**
     * Get the offset that should be used as the "base" for drawing the probe
     * arm.  This is either the selected offset position (if any), otherwise
     * whatever offset position is considered the default one (the first), or
     * failing all else a 0 offset (which corresponds to the base).
     */
    private Offset getProbeArmOffset() {
        final TpeContext ctx = _iw.getContext();
        final OffsetPosBase selOffset = ctx.offsets().selectedPosOrNull();
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
        final InstGmosCommon inst = (InstGmosCommon) _iw.getInstObsComp();
        final ObsContext ctx = _iw.getMinimalObsContext().getOrNull();
        if (ctx != null && GmosOiwfsGuideProbe.instance.inRange(ctx, getProbeArmOffset())) {
            final Point2D guideStar = new Point2D.Double(xg, yg);
            final Point2D offset = new Point2D.Double(xc, yc);
            final Point2D translate = new Point2D.Double(xt, yt);
            final List<Shape> shapes = new GmosOiwfsProbeArm(inst).geometryForScreenAsJava(_posAngle, guideStar, offset, translate, flip, _flipRA, _pixelsPerArcsec);
            for (final Shape s : shapes)
                _figureList.add(new Figure(s, PROBE_ARM_COLOR, BLOCKED, OIWFS_STROKE));
        }
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
        final InstGmosCommon inst = (InstGmosCommon) _iw.getInstObsComp();
        final boolean flip = (inst.getIssPort() == IssPort.SIDE_LOOKING);

        _figureList.clear();
        addOffsetConstrainedPatrolField(basePosX, basePosY);
        addPatrolField(offsetPosX + translateX, offsetPosY + translateY);
        if (oiwfsDefined)
            _addProbeArm(guidePosX, guidePosY, offsetPosX, offsetPosY, translateX, translateY, flip);
    }


    /**
     * Return true if the display needs to be updated because values changed.
     */
    protected boolean _needsUpdate(SPInstObsComp inst, TpeImageInfo tii) {
        // Needs to take into account offset position list updates to work
        // as intended.  Unclear whether it is worth the effort to maintain
        // the old offset lists when the calculation isn't that slow anyway.
        return true;
    }

    @Override
    public Option<Collection<TpeMessage>> getMessages() {
        if (offsetConstrainedPatrolFieldIsEmpty) {
            return new Some<Collection<TpeMessage>>(Collections.singletonList(WARNING));
        } else {
            return None.instance();
        }
    }
}

