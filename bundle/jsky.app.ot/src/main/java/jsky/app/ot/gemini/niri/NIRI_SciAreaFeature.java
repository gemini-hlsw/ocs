package jsky.app.ot.gemini.niri;


import jsky.app.ot.gemini.inst.SciAreaFeatureBase;
import jsky.app.ot.tpe.TpeImageInfo;
import edu.gemini.spModel.gemini.niri.InstNIRI;
import edu.gemini.spModel.gemini.niri.Niri.*;

import java.awt.*;


/**
 * Draws the Science Area, the detector or slit.
 */
public class NIRI_SciAreaFeature extends SciAreaFeatureBase {

    // dotted line stroke
    private BasicStroke _dotted = new BasicStroke(2,
            BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_BEVEL,
            0,
            new float[]{2, 7},
            0);

    /**
     * Construct the feature
     */
    public NIRI_SciAreaFeature() {
    }

    /**
     * Draw the science area.
     */
    public void draw(Graphics g, TpeImageInfo tii) {
        super.draw(g, tii);

        InstNIRI niri = _iw.getContext().instrument().orNull(InstNIRI.SP_TYPE);

        Graphics2D g2d = (Graphics2D) g;
        if (niri != null && niri.getMask() == Mask.PINHOLE_MASK) {
            Polygon p = _sciAreaPD.getAWTPolygon();
            int[] x = p.xpoints;
            int[] y = p.ypoints;
            Stroke savedStroke = g2d.getStroke();
            g2d.setStroke(_dotted);
            // draw an dotted X through the science area
            g2d.drawLine(x[0], y[0], x[2], y[2]);
            g2d.drawLine(x[1], y[1], x[3], y[3]);

            g2d.setStroke(savedStroke);
        }
    }
}
