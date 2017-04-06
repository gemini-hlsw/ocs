package jsky.app.ot.gemini.gsaoi;

import edu.gemini.skycalc.Offset;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.shared.util.immutable.Tuple2;
import edu.gemini.spModel.gemini.gsaoi.Gsaoi;
import edu.gemini.spModel.gemini.gsaoi.GsaoiDetectorArray;
import edu.gemini.spModel.gemini.gsaoi.GsaoiDetectorArray.Quadrant;
import jsky.app.ot.gemini.inst.SciAreaFeatureBase;
import jsky.app.ot.tpe.*;

import java.awt.*;
import static java.awt.BasicStroke.CAP_BUTT;
import static java.awt.BasicStroke.JOIN_MITER;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * Shows the valid ranges for the ODGWs.  The valid range is the intersection
 * of the detector quadrants at all the offset positions.
 */
public final class GsaoiOdgwFeature extends TpeImageFeature {
    private boolean isEmpty;

    public GsaoiOdgwFeature() {
        super("ODGW", "Show ODGW ranges.");
    }

    private AffineTransform calc(TpeImageInfo tii) {
        double x = tii.getBaseScreenPos().getX();
        double y = tii.getBaseScreenPos().getY();
        double ppa = tii.getPixelsPerArcsec();
        double posAngle = tii.getCorrectedPosAngleRadians();

        AffineTransform trans = AffineTransform.getTranslateInstance(x, y);
        trans.rotate(-posAngle);
        trans.scale(ppa, ppa);
        return trans;
    }

    // A dotted line stroke.
    private static final Stroke STROKE = new BasicStroke(1f, CAP_BUTT, JOIN_MITER, 10f, new float[] {2f}, 1f);

    @Override
    public void draw(Graphics g, TpeImageInfo tii) {
        if (!isEnabled(_iw.getContext())) return;

        Graphics2D g2d = (Graphics2D) g;

        g2d.setColor(SciAreaFeatureBase.FOV_COLOR);
        Stroke origStroke = g2d.getStroke();
        g2d.setStroke(STROKE);

        Set<Offset> offsets = _iw.getContext().offsets().scienceOffsetsJava();

        AffineTransform trans = calc(tii);
        java.util.List<Tuple2<Quadrant, Shape>> tups;
        tups = GsaoiDetectorArray.instance.quadrantIntersection(offsets);

        boolean allEmpty = true;
        for (Tuple2<Quadrant, Shape> tup : tups) {
            Shape s = tup._2();
            Rectangle2D r = s.getBounds2D();
            if ((r.getWidth() > 0) && (r.getHeight() > 0)) {
                allEmpty = false;
            }
            // Flip the shape if needed
            if (_flipRA == -1) {
                s = new Area(s).createTransformedArea(AffineTransform.getScaleInstance(_flipRA, 1.0));
            }
            g2d.draw(trans.createTransformedShape(s));
        }
        isEmpty = allEmpty;

        g2d.setStroke(origStroke);
    }

    public TpeImageFeatureCategory getCategory() {
        return TpeImageFeatureCategory.fieldOfView;
    }

    @Override public boolean isEnabled(TpeContext ctx) {
        if (!super.isEnabled(ctx)) return false;
        return ctx.instrument().is(Gsaoi.SP_TYPE);
    }

    private static final TpeMessage WARNING = TpeMessage.warningMessage(
            "No valid ODGW region.  Check offset positions.");

    public Option<Collection<TpeMessage>> getMessages() {
        if (!isEmpty) return None.instance();
        return new Some<Collection<TpeMessage>>(Collections.singletonList(WARNING));
    }
}
