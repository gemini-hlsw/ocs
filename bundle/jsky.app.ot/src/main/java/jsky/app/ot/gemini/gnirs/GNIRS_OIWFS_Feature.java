package jsky.app.ot.gemini.gnirs;

import edu.gemini.skycalc.Angle;
import edu.gemini.spModel.gemini.gnirs.GnirsOiwfsGuideProbe;
import edu.gemini.spModel.gemini.gnirs.InstGNIRS;
import edu.gemini.spModel.guide.PatrolField;
import edu.gemini.spModel.obs.context.ObsContext;
import jsky.app.ot.gemini.inst.OIWFS_FeatureBase;

import java.awt.*;
import java.awt.geom.Point2D;


/**
 * Draws the OIWFS overlay for GNIRS.
 */
public class GNIRS_OIWFS_Feature extends OIWFS_FeatureBase {

    // Composite used for drawing items that block the view
    private static final Composite BLOCKED = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4F);

    /**
     * Construct the feature with its name and description.
     */
    public GNIRS_OIWFS_Feature() {
        super("GNIRS OIWFS", "Show the GNIRS OIWFS.");
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
    protected void _updateFigureList(double guidePosX, double guidePosY, double offsetPosX, double offsetPosY,
                                     double translateX, double translateY, double basePosX, double basePosY, boolean oiwfsDefined) {
        _figureList.clear();

        InstGNIRS inst = _iw.getContext().instrument().orNull(InstGNIRS.SP_TYPE);
        if (inst == null) return;

        for (ObsContext ctx : _iw.getMinimalObsContext()) {
            for (PatrolField range : GnirsOiwfsGuideProbe.instance.getCorrectedPatrolField(ctx)) {
                final Angle angle = new Angle(-_posAngle, Angle.Unit.RADIANS);
                final Point2D.Double p1 = new Point2D.Double(offsetPosX + translateX, offsetPosY + translateY);
                setTransformationToScreen(angle, _pixelsPerArcsec, p1);

                // draw guide probe range
                final Composite composite = getFillObscuredArea() ? BLOCKED : null;
                addPatrolField(range, OIWFS_COLOR, OIWFS_STROKE, composite);

                // draw intersection of offset patrol fields
                final Point2D.Double p2 = new Point2D.Double(basePosX, basePosY);
                setTransformationToScreen(angle, _pixelsPerArcsec, p2);
                addOffsetConstrainedPatrolField(range, _iw.getContext().offsets().scienceOffsetsJava());
            }
        }
    }
}

