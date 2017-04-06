package jsky.app.ot.gemini.nifs;

import diva.util.java2d.Polygon2D;
import edu.gemini.skycalc.Angle;
import edu.gemini.skycalc.Offset;
import edu.gemini.spModel.gemini.altair.AltairParams;
import edu.gemini.spModel.gemini.altair.InstAltair;
import edu.gemini.spModel.gemini.nifs.InstNIFS;
import edu.gemini.spModel.gemini.nifs.NifsOiwfsGuideProbe;
import edu.gemini.spModel.guide.PatrolField;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import jsky.app.ot.gemini.inst.OIWFS_FeatureBase;
import jsky.app.ot.tpe.TpeImageInfo;

import java.awt.*;
import java.awt.geom.*;
import java.util.Set;


/**
 * Draws the OIWFS overlay for NIFS.
 */
public final class NIFS_OIWFS_Feature extends OIWFS_FeatureBase {

    // Composite used for drawing items that partially block the view
    private static final Composite PARTIAL = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4F);

    // Composite used for drawing items that block the view
    private static final Composite BLOCKED = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6F);

    // Used to rotate figures by the position angle
    protected AffineTransform _posAngleTrans = new AffineTransform();

    /**
     * Construct the feature with its name and description.
     */
    public NIFS_OIWFS_Feature() {
        super("NIFS OIWFS FOV", "Show NIFS OIWFS.");
    }

    /** Return true if the display needs to be updated because values changed. */
    protected boolean _needsUpdate(final SPInstObsComp inst, final TpeImageInfo tii) {
        // Needs to take into account offset position list updates to work
        // as intended.  Unclear whether it is worth the effort to maintain
        // the old offset lists when the calculation isn't that slow anyway.
        return true;
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
     * @param basePosX the X screen coordinate for the base position (IGNORED)
     * @param basePosY the Y screen coordinate for the base position (IGNORED)
     * @param oiwfsDefined set to true if an OIWFS position is defined (otherwise
     *                     the xg and yg parameters are ignored)
     */
    protected void _updateFigureList(final double guidePosX, final double guidePosY, final double offsetPosX, final double offsetPosY,
                                     final double translateX, final double translateY, final double basePosX, final double basePosY, final boolean oiwfsDefined) {
        _figureList.clear();

        final InstNIFS inst = _iw.getContext().instrument().orNull(InstNIFS.SP_TYPE);
        if (inst == null) return;

        addOffsetConstrainedPatrolField(basePosX, basePosY);
        addPatrolField(offsetPosX + translateX, offsetPosY + translateY);

        _posAngleTrans.setToIdentity();
        _posAngleTrans.rotate(-_posAngle+Math.toRadians(90.), offsetPosX + translateX, offsetPosY + translateY);

        final Point2D.Double p = new Point2D.Double(offsetPosX + translateX, offsetPosY + translateY);
        _addGuideFields(p);
        _addPickoffProbe(p);
    }


    /**
     * Add the OIWFS patrol field to the list of figures to display.
     */
    protected void addPatrolField(final double xc, final double yc) {
        for (ObsContext ctx : _iw.getMinimalObsContext()) {
            // get scaled and offset f2 oiwfs patrol field
            for (PatrolField patrolField : NifsOiwfsGuideProbe.instance.getCorrectedPatrolField(ctx)) {
                // rotation, scaling and transformation to match screen coordinates
                final Angle rotation = new Angle(-_posAngle, Angle.Unit.RADIANS);
                final Point2D.Double translation = new Point2D.Double(xc, yc);
                setTransformationToScreen(rotation, _pixelsPerArcsec, translation);

                // set patrol field for in Range check (this should probably be done using the inRange check provided by the guide probe)
                final Area _patrolField = patrolField.getArea();
                transformToScreen(_patrolField);

                // draw patrol field
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
        for (ObsContext ctx : _iw.getMinimalObsContext()) {
            for (PatrolField patrolField : NifsOiwfsGuideProbe.instance.getCorrectedPatrolField(ctx)) {
                // rotation, scaling and transformation to match screen coordinates
                final Angle rotation = new Angle(-_posAngle, Angle.Unit.RADIANS);
                final Point2D.Double translation = new Point2D.Double(xc, yc);
                setTransformationToScreen(rotation, _pixelsPerArcsec, translation);

                final Set<Offset> offsets = _iw.getContext().offsets().scienceOffsetsJava();
                addOffsetConstrainedPatrolField(patrolField, offsets);
            }
        }
    }

    // draw the circles for the three guide fields at the given location
    private void _addGuideFields(Point2D.Double p) {
        if (getFieldLens() == AltairParams.FieldLens.OUT) {
            _addGuideFields(p, 120., Color.orange);
        } else {
            _addGuideFields(p, 60., Color.orange);
        }
    }

    // draw a guide field at the given location, with the given diameter in arcsec and color
    private void _addGuideFields(final Point2D.Double p, final double dd, final Color c) {
        final double d = dd * _pixelsPerArcsec;
        final Ellipse2D.Double oval = new Ellipse2D.Double(p.x - d / 2, p.y - d / 2, d, d);
        _figureList.add(new Figure(oval, c, null, OIWFS_STROKE));
    }

    private InstAltair extractAltair() {
        return _iw.getContext().altair().orNull();
    }

    private AltairParams.FieldLens getFieldLens() {
        InstAltair altair = extractAltair();
        return (altair == null) ? AltairParams.FieldLens.OUT : altair.getFieldLens();
    }

    // draw the NIFS pickoff probe at the given location
    private void _addPickoffProbe(final Point2D.Double p) {
        final boolean fill = getFillObscuredArea();
        _addPickoffProbeBlocked(p, fill);
        _addPickoffProbePartial(p, fill);
    }


    // draw the fully vignetted part of the NIFS pickoff probe at the given location
    private void _addPickoffProbeBlocked(final Point2D.Double p, final boolean fill) {
        final Composite composite = fill ? BLOCKED : null;

        final double h = 100 * _pixelsPerArcsec;
        final double w = 16.1 * _pixelsPerArcsec;
        final Rectangle2D.Double rect = new Rectangle2D.Double(p.x - w / 2, p.y, w, h);

        // draw an arc at top
        final double radius = w/2;
        final Arc2D.Double arc = new Arc2D.Double();
        arc.setArcByCenter(p.x, p.y, radius, -180, -180, Arc2D.OPEN);

        // Only display the outline, and rotate by the position angle
        final Area area = new Area(rect);
        area.add(new Area(arc));
        area.transform(_posAngleTrans);
        _figureList.add(new Figure(area, OIWFS_COLOR, composite, OIWFS_STROKE));
    }


    // draw the partially vignetted part of the NIFS pickoff probe at the given location
    private void _addPickoffProbePartial(final Point2D.Double p, final boolean fill) {
        final Composite composite = fill ? PARTIAL : null;

        // the outer shadow is no longer rectangular (Its wider at the outside)
        final double h = 100 * _pixelsPerArcsec;
        final double w1 = 26 * _pixelsPerArcsec;
        final double w2 = 30 * _pixelsPerArcsec;
        final double x0 = p.x - w1 / 2;
        final double y0 = p.y;
        final double x1 = p.x + w1 / 2;
        final double y1 = y0;
        final double x2 = p.x + w2 / 2;
        final double y2 = p.y + h;
        final double x3 = p.x - w2 / 2;
        final double y3 = y2;
        final double[] coords = new double[] {x0, y0, x1, y1, x2, y2, x3, y3};
        final Polygon2D.Double polygon = new Polygon2D.Double(coords);

        // draw an arc at top
        final double radius = w1/2;
        final Arc2D.Double arc = new Arc2D.Double();
        arc.setArcByCenter(p.x, p.y, radius, -180, -180, Arc2D.OPEN);

        // Only display the outline, and rotate by the position angle
        final Area area = new Area(polygon);
        area.add(new Area(arc));
        area.transform(_posAngleTrans);
        _figureList.add(new Figure(area, OIWFS_COLOR, composite, OIWFS_STROKE));
    }
}

