// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: GMOS_SciAreaFeature.java 46768 2012-07-16 18:58:53Z rnorris $
//
package jsky.app.ot.gemini.gmos;

import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.gemini.gmos.*;
import edu.gemini.spModel.inst.ScienceAreaGeometry;
import jsky.app.ot.gemini.inst.SciAreaFeatureBase;
import jsky.app.ot.tpe.TpeImageInfo;
import jsky.app.ot.tpe.TpeImageWidget;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Point2D;

import java.util.*;
import java.util.List;


/**
 * Draws the Science Area, the detector or slit.
 */
public class GMOS_SciAreaFeature extends SciAreaFeatureBase {

    // The size of the central imaging CCD, in arcsec
    private static final double IMAGING_CENTER_CCD_WIDTH = 2.76 * 60;

    // The size of the left and right imaging CCDs, in arcsec
    private static final double IMAGING_LR_CCD_WIDTH = 1.37 * 60;

    // The size of the gaps between imaging CCDs, in arcsec
    private static final double IMAGING_GAP_WIDTH = 3;

    // Holds information needed to display CCD gaps, if applicable
    private ImagingGaps _gaps;

    /**
     * Construct the feature
     */
    public GMOS_SciAreaFeature() {
    }

    /**
     * Override reinit to note if this is for Gemini south or north
     */
    public void reinit(final TpeImageWidget iw, final TpeImageInfo tii) {
        super.reinit(iw, tii);
    }

    private InstGmosCommon getInst() {
        final InstGmosCommon gmos = _iw.getContext().instrument().orNull(InstGmosNorth.SP_TYPE);
        if (gmos != null) return gmos;
        return _iw.getContext().instrument().orNull(InstGmosSouth.SP_TYPE);
    }

    private boolean isHamamatsu() {
        final InstGmosCommon inst = getInst();
        return inst != null && GmosCommonType.DetectorManufacturer.HAMAMATSU.equals(inst.getDetectorManufacturer());
    }


    /**
     * Return the offset from the base position in arcsec for drawing the
     * "tick mark" (used as a handle to rotate the science area). The
     * offset depends on the selected FP unit mode.
     */
    protected Point2D.Double _getTickMarkOffset() {
        final Point2D.Double offset = new Point2D.Double(_baseScreenPos.x, _baseScreenPos.y - _sciArea.getHeight() / 2.0);

        final InstGmosCommon instGMOS = getInst();
        if (instGMOS != null) {
            final GmosCommonType.FPUnitMode fpUnitMode = instGMOS.getFPUnitMode();
            if (fpUnitMode == GmosCommonType.FPUnitMode.BUILTIN) {
                if (instGMOS.isIFU()) {
                    offset.y = _baseScreenPos.y - 30.0;
                } else if (instGMOS.isNS()) {
                    offset.y = _baseScreenPos.y - GmosScienceAreaGeometry$.MODULE$.LongSlitFOVHeight() / 2.0 * _pixelsPerArcsec;
                }
            }
        }
        return offset;
    }

    /**
     * Update the list of FOV figures to draw.
     */
    @SuppressWarnings("unchecked")
    protected void _updateFigureList() {
        _figureList.clear();
        _gaps = null;

        InstGmosCommon instGMOS = getInst();
        if (instGMOS != null) {
            final ScienceAreaGeometry gmosScienceArea = new GmosScienceAreaGeometry(instGMOS);
            final Shape transformedArea = gmosScienceArea.scienceAreaAsJava(_baseScreenPos, _posAngle, _pixelsPerArcsec);
            if (transformedArea != null) {
                _figureList.add(transformedArea);
                // Create any necessary modifications.
                final GmosCommonType.FPUnitMode fpUnitMode = instGMOS.getFPUnitMode();
                if (fpUnitMode == GmosCommonType.FPUnitMode.BUILTIN && instGMOS.isImaging())
                    _gaps = new ImagingGaps(GmosScienceAreaGeometry$.MODULE$.ImagingFOVSize());
                else if (fpUnitMode == GmosCommonType.FPUnitMode.CUSTOM_MASK)
                    _gaps = new ImagingGaps(GmosScienceAreaGeometry$.MODULE$.MOSFOVSize());
            }
        }
    }

    @Override protected boolean _calc(final TpeImageInfo tii) {
        if (!super._calc(tii)) return false;
        _updateFigureList();
        return true;
    }

    /**
     * Draw the feature.
     */
    public void draw(final Graphics g, final TpeImageInfo tii) {
        if (!_calc(tii)) return;

        final Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(FOV_COLOR);

        // draw the FOV
        for (final Shape s: _figureList)
            g2d.draw(s);
        if (_gaps != null)
            _gaps.draw(g2d);

        // Draw the drag item
        g2d.fill(_tickMarkPD.getPolygon2D());

        // Draw a string displaying the rotation angle
        final InstGmosCommon inst = getInst();
        if (_dragging && inst != null) {
            g2d.setFont(POS_ANGLE_FONT);

            // Draw a little above the mouse
            final int baseX = _dragX;
            final int baseY = _dragY - 10;
            final String s = "position angle = " + inst.getPosAngleDegreesStr() + " deg";
            g.drawString(s, baseX, baseY);
        }
    }


    /**
     * Draw the science area at the given x,y (screen coordinate) offset position.
     */
    @Override
    public void drawAtOffsetPos(final Graphics g, final TpeImageInfo tii, double x, double y) {
        if (!_calc(tii)) return;

        final Graphics2D g2d = (Graphics2D) g;
        final AffineTransform saveAT = g2d.getTransform();

        try {
            g2d.translate(x - _baseScreenPos.x, y - _baseScreenPos.y);
            for (final Shape s : _figureList)
                g2d.draw(s);
            if (_gaps != null)
                _gaps.draw(g2d);
        } finally {
            g2d.setTransform(saveAT);
        }
    }

    // Manages drawing the CCD gaps and associated labels
    private class ImagingGaps {
        // CCD gaps
        final Shape gap1;
        final Shape gap2;

        // label positions
        final Point2D.Double posCCD1;
        final Point2D.Double posCCD2;
        final Point2D.Double posCCD3;

        // label strings
        final String nameCCD1;
        final String nameCCD2;
        final String nameCCD3;

        /**
         * Initialize CCD gap for FOV of the given size
         *
         * @param size the basic size of the imaging square, in arcsec, centered at the base pos
         */
        ImagingGaps(double size) {
            final double height = size * _pixelsPerArcsec;
            final double width = IMAGING_CENTER_CCD_WIDTH * _pixelsPerArcsec;
            final double h2 = height / 2;
            final double w2 = width / 2;
            final double gapWidth = IMAGING_GAP_WIDTH * _pixelsPerArcsec;

            // XXX should the gaps be centered, inside or outside the center CCD?
            final double x1 = _baseScreenPos.x - w2 - gapWidth/2.;
            final double y1 = _baseScreenPos.y - h2;
            gap1 = makeGap(x1, y1, gapWidth, height);

            final double x2 = _baseScreenPos.x + w2 - gapWidth/2.;
            final double y2 = y1;
            gap2 = makeGap(x2, y2, gapWidth, height);

            final double lrWidth = IMAGING_LR_CCD_WIDTH * _pixelsPerArcsec;
            final double w3 = lrWidth / 2;
            final double y3 = y1 + height / 4;

            posCCD1 = new Point2D.Double(x1 - w3, y3);
            posCCD2 = new Point2D.Double(_baseScreenPos.x, y3);
            posCCD3 = new Point2D.Double(x2 + w3, y3);
            _posAngleTrans.transform(posCCD1, posCCD1);
            _posAngleTrans.transform(posCCD2, posCCD2);
            _posAngleTrans.transform(posCCD3, posCCD3);

            if (isHamamatsu()) {
                nameCCD1 = "CCDr";
                nameCCD2 = "CCDg";
                nameCCD3 = "CCDb";
            } else {
                nameCCD1 = "CCD1";
                nameCCD2 = "CCD2";
                nameCCD3 = "CCD3";
            }
        }

        // Returns a polygon for a gap with the given x,y origin, width and height (all in screen pixels)
        private Shape makeGap(double x, double y, double width, double height) {
            final List<Pair<Double,Double>> points = new ArrayList<>();
            points.add(new Pair<>(x        , y));
            points.add(new Pair<>(x + width, y));
            points.add(new Pair<>(x + width, y + height));
            points.add(new Pair<>(x        , y + height));

            // Create the polygon and rotate it by the PA.
            final ImPolygon p = ImPolygon$.MODULE$.apply(points);
            final Area a = new Area(p);
            a.transform(_posAngleTrans);
            return a;
        }

        // Draw the CCD gaps and associated labels
        void draw(Graphics2D g2d) {
            g2d.fill(gap1);
            g2d.fill(gap2);

            g2d.setFont(POS_ANGLE_FONT);
            FontMetrics fm = g2d.getFontMetrics();
            int w = fm.stringWidth("CCDx") / 2;
            g2d.drawString(nameCCD1, (int) posCCD1.x - w, (int) posCCD1.y);
            g2d.drawString(nameCCD2, (int) posCCD2.x - w, (int) posCCD2.y);
            g2d.drawString(nameCCD3, (int) posCCD3.x - w, (int) posCCD3.y);
        }
    }
}

