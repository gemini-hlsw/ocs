// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: GMOS_SciAreaFeature.java 46768 2012-07-16 18:58:53Z rnorris $
//
package jsky.app.ot.gemini.gmos;

import diva.util.java2d.Polygon2D;
import edu.gemini.spModel.gemini.gmos.*;
import jsky.app.ot.gemini.inst.SciAreaFeatureBase;
import jsky.app.ot.tpe.TpeImageInfo;
import jsky.app.ot.tpe.TpeImageWidget;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import java.util.Iterator;


/**
 * Draws the Science Area, the detector or slit.
 */
public class GMOS_SciAreaFeature extends SciAreaFeatureBase {

    // The size of the imaging field of view in arcsec
    private static final double IMAGING_FOV_SIZE = 330.34;

    // The size of the imaging FOV before the cut-off corners, in arcsec
    private static final double IMAGING_FOV_INNER_SIZE = 131.33 * 2;

    // The size of the central imaging CCD, in arcsec
    private static final double IMAGING_CENTER_CCD_WIDTH = 2.76 * 60;

    // The size of the left and right imaging CCDs, in arcsec
    private static final double IMAGING_LR_CCD_WIDTH = 1.37 * 60;

    // The size of the gaps between imaging CCDs, in arcsec
    private static final double IMAGING_GAP_WIDTH = 3;

    // The size of the square MOS field of view in arcsec
    // (slightly smaller than the imaging FOV)
    private static final double MOS_FOV_SIZE = IMAGING_FOV_SIZE - 2 * 8.05;

    // The size of the imaging FOV before the cut-off corners, in arcsec
    private static final double MOS_FOV_INNER_SIZE = 139.38 * 2;


    // The height of the long slit FOV in arcsec (the width is selected by the user)
    private static final double LONG_SLIT_FOV_HEIGHT = 108.0;

    // The height of a long slit FOV bridge in arcsec
    // (There are one of these between each of the slits)
    private static final double LONG_SLIT_FOV_BRIDGE_HEIGHT = 3.2;


    // IFU visualisation in TPE
    //
    // Currently TPE shows the true focal plane geometry which has fixed IFU sub-fields
    // offset either side of the base position. Whilst this is factually correct, what users
    // will expect is that the larger of the two sub-fields be positioned on their target (as
    // defined by the target component). This is the first case in which the instrument
    // aperture is not symmetric about the pointing position (in this case it is offset by ~
    // 30 arcsec from it).
    //
    // {Phil: proposed solution}
    // The tricky aspect is that the base position displayed in TPE currently has two
    // meanings (a) it shows the position of the target RA,dec and (b) it also shows the
    // telescope pointing direction (essentially the direction in which the mount points) and
    // is used as the origin for drawing the PWFS and OIWFS patrol fields. These need not be
    // the same for an off-axis. (A similar case will occur with the bHROS fibre feed).
    //
    // There are several options, only one of which will avoid completely confusing the
    // users. The proposed solution is: firstly, the larger IFU sub-aperture should be drawn
    // at the base position. Secondly, the PWFS and OIWFS patrol fields must be offset (by
    // the opposite amount that the IFU field is off-axis). This 'pseudo' base position (we
    // would call it the pointing position) is the centre for drawing the PWFS and OIWFS
    // patrol fields. (The pointing position need not be displayed in TPE).
    //
    // [source: bmiller e-mail 19Dec01 and 14Jan 02, generalised by ppuxley]**********
    //
    // Allan: Note: For GMOS-S it is the smaller rect that is centered on the base position
    // (The display is reversed).
    //
    // The offset from the base position in arcsec
    private static final double IFU_FOV_OFFSET = 30.;

    // The offsets (from the base pos) and dimensions of the IFU FOV (in arcsec)
    private static final Rectangle2D.Double[] IFU_FOV = new Rectangle2D.Double[]{
        new Rectangle2D.Double(-30. - IFU_FOV_OFFSET, 0., 3.5, 5.),
        new Rectangle2D.Double(30. - IFU_FOV_OFFSET, 0., 7., 5.)
    };

    // Indexes for above array
    private static final int IFU_FOV_SMALLER_RECT_INDEX = 0;
    private static final int IFU_FOV_LARGER_RECT_INDEX = 1;

    // Set to true if the instrument is GMOS South
    private boolean _isSouth;

    // Holds infor needed to display CCD gaps, if applicable
    private ImagingGaps _gaps;

    /**
     * Construct the feature
     */
    public GMOS_SciAreaFeature() {
    }

    /**
     * Override reinit to note if this is for Gemini south or north
     */
    public void reinit(TpeImageWidget iw, TpeImageInfo tii) {
        super.reinit(iw, tii);
        _isSouth = iw.getContext().instrument().is(InstGmosSouth.SP_TYPE);
    }

    private InstGmosCommon getInst() {
        InstGmosCommon gmos = _iw.getContext().instrument().orNull(InstGmosNorth.SP_TYPE);
        if (gmos == null) gmos = _iw.getContext().instrument().orNull(InstGmosSouth.SP_TYPE);
        return gmos;
    }

    private boolean isHamamatsu() {
        final InstGmosCommon inst = getInst();
        return (inst == null) ? false : GmosCommonType.DetectorManufacturer.HAMAMATSU.equals(inst.getDetectorManufacturer());
    }

    /**
     * Add the FOV described by the arguments to the list of figures to display.
     * The shape drawn here is a square with cut-off corners.
     *
     * @param size the basic size of a square, in arcsec, centered at the base pos
     * @param innerSize the length of a side after cutting off the corners, in arcsec
     */
    private void _addFOV(double size, double innerSize) {
        // draw a square with the corners cut off
        double size1 = size * _pixelsPerArcsec;
        double size2 = innerSize * _pixelsPerArcsec;
        double d1 = size1 / 2;
        double d2 = size2 / 2;
        double cornerSize = d1 - d2;

        double x0 = _baseScreenPos.x - d1 + cornerSize;
        double y0 = _baseScreenPos.y - d1;
        Polygon2D.Double fov = new Polygon2D.Double(x0, y0);

        double x1 = x0 + size2;
        double y1 = y0;
        fov.lineTo(x1, y1);

        double x2 = x1 + cornerSize;
        double y2 = y1 + cornerSize;
        fov.lineTo(x2, y2);

        double x3 = x2;
        double y3 = y2 + size2;
        fov.lineTo(x3, y3);

        double x4 = x3 - cornerSize;
        double y4 = y3 + cornerSize;
        fov.lineTo(x4, y4);

        double x5 = x4 - size2;
        double y5 = y4;
        fov.lineTo(x5, y5);

        double x6 = x5 - cornerSize;
        double y6 = y5 - cornerSize;
        fov.lineTo(x6, y6);

        double x7 = x6;
        double y7 = y6 - size2;
        fov.lineTo(x7, y7);

        // rotate by position angle
        fov.transform(_posAngleTrans);

        _figureList.add(fov);
    }


    /** Add the imaging FOV to the list of figures to display. */
    private void _addImagingFOV() {
        _addFOV(IMAGING_FOV_SIZE, IMAGING_FOV_INNER_SIZE);
        _gaps = new ImagingGaps(IMAGING_FOV_SIZE);
    }

    /** Add the MOS field of view to the list of figures to display. */
    private void _addMOS_FOV() {
        _addFOV(MOS_FOV_SIZE, MOS_FOV_INNER_SIZE);
        _gaps = new ImagingGaps(MOS_FOV_SIZE);
    }

    /** Add the long slit field of view to the list of figures to display. */
    private void _addLongSlitFOV() {
        double slitWidth = _sciArea.getWidth();
        double slitHeight = LONG_SLIT_FOV_HEIGHT * _pixelsPerArcsec;
        double bridgeHeight = LONG_SLIT_FOV_BRIDGE_HEIGHT * _pixelsPerArcsec;

        // add the 3 slits along the y axis
        double x = _baseScreenPos.x - (slitWidth / 2);
        double d = slitHeight + bridgeHeight;
        for (int i = -1; i <= 1; i++) {
            double y = _baseScreenPos.y - (slitHeight / 2) + (i * d);
            Polygon2D.Double slit = new Polygon2D.Double(x, y);
            slit.lineTo(x + slitWidth, y);
            slit.lineTo(x + slitWidth, y + slitHeight);
            slit.lineTo(x, y + slitHeight);

            // rotate by position angle
            slit.transform(_posAngleTrans);

            _figureList.add(slit);
        }
    }

    /** Add the nod & shuffle field of view to the list of figures to display. */
    private void _addNS_FOV() {
        double slitWidth = _sciArea.getWidth();
        double slitHeight = LONG_SLIT_FOV_HEIGHT * _pixelsPerArcsec;
        //double bridgeHeight = LONG_SLIT_FOV_BRIDGE_HEIGHT * _pixelsPerArcsec;

        // add the slit along the y axis
        double x = _baseScreenPos.x - (slitWidth / 2);
        double y = _baseScreenPos.y - (slitHeight / 2);
        Polygon2D.Double slit = new Polygon2D.Double(x, y);
        slit.lineTo(x + slitWidth, y);
        slit.lineTo(x + slitWidth, y + slitHeight);
        slit.lineTo(x, y + slitHeight);

        // rotate by position angle
        slit.transform(_posAngleTrans);

        _figureList.add(slit);
    }


    // Add the IFU field of view to the list of figures to display.
    private void _addIFU_FOV(GmosCommonType.FPUnit fpUnit) {
        // The width values are different for these (see OT-419).
        // In these cases we need to subtract 2 from the width.
        int wx = 0;
        if (fpUnit == GmosSouthType.FPUnitSouth.IFU_N || fpUnit == GmosSouthType.FPUnitSouth.IFU_N_B
                || fpUnit == GmosSouthType.FPUnitSouth.IFU_N_R ) {
            wx = 2;
        }

        // These are used to center the right rect on the base pos
        double w = ((IFU_FOV[IFU_FOV_LARGER_RECT_INDEX].width - wx) * _pixelsPerArcsec) / 2.0;
        double h = (IFU_FOV[IFU_FOV_LARGER_RECT_INDEX].height * _pixelsPerArcsec) / 2.0;

        // Add the two slits
        for (int i = 0; i < IFU_FOV.length; i++) {
            if (wx != 0 && i != IFU_FOV_LARGER_RECT_INDEX) {
                continue; // only display the larger one for these (see OT-419)
            }
            double width = (IFU_FOV[i].width - wx) * _pixelsPerArcsec;
            double height = IFU_FOV[i].height * _pixelsPerArcsec;
            double x = _baseScreenPos.x + (IFU_FOV[i].x * _pixelsPerArcsec) - w;
            double y = _baseScreenPos.y + (IFU_FOV[i].y * _pixelsPerArcsec) - h;

            if (fpUnit == GmosNorthType.FPUnitNorth.IFU_2 || fpUnit == GmosNorthType.FPUnitNorth.IFU_3 ||
                fpUnit == GmosSouthType.FPUnitSouth.IFU_2 || fpUnit == GmosSouthType.FPUnitSouth.IFU_3 ||
                    fpUnit == GmosSouthType.FPUnitSouth.IFU_N_B || fpUnit == GmosSouthType.FPUnitSouth.IFU_N_R) {
                // left or right slit: show the left or right half of both slits, with the larger(north) or
                // smaller(south) of the two centered about the base position
                width /= 2.0;
                x += (w / 2.0);
            }

            // allan: 11/13/03: For GMOS-S, flip along the X axis about the base position
            // (OIWFS patrol field should be shifted as well: See JIRA log for #OT-10)
            if (_isSouth && i == IFU_FOV_SMALLER_RECT_INDEX) {
                x = _baseScreenPos.x + (_baseScreenPos.x - x) - width;
            }

            Polygon2D.Double fov = new Polygon2D.Double(x, y);
            fov.lineTo(x + width, y);
            fov.lineTo(x + width, y + height);
            fov.lineTo(x, y + height);

            // rotate by position angle
            fov.transform(_posAngleTrans);

            _figureList.add(fov);

        }
    }


    /**
     * Return the offset from the base position in arcsec for drawing the
     * "tick mark" (used as a handle to rotate the science area). The
     * offset depends on the selected FP unit mode.
     */
    protected Point2D.Double _getTickMarkOffset() {
        Point2D.Double offset = new Point2D.Double(_baseScreenPos.x,
                                                   _baseScreenPos.y - _sciArea.getHeight() / 2.0);

        InstGmosCommon instGMOS = getInst();
        if (instGMOS != null) {
            GmosCommonType.FPUnitMode fpUnitMode = instGMOS.getFPUnitMode();
            if (fpUnitMode == GmosCommonType.FPUnitMode.BUILTIN) {
                if (instGMOS.isIFU()) {
                    offset.y = _baseScreenPos.y - 30;
                } else if (instGMOS.isNS()) {
                    offset.y = _baseScreenPos.y - LONG_SLIT_FOV_HEIGHT / 2. * _pixelsPerArcsec;
                }
            }
        }
        return offset;
    }

    /**
     * Update the list of FOV figures to draw.
     */
    protected void _updateFigureList() {
        _figureList.clear();
        _gaps = null;

        InstGmosCommon instGMOS = getInst();
        if (instGMOS != null) {
            GmosCommonType.FPUnitMode fpUnitMode = instGMOS.getFPUnitMode();
            if (fpUnitMode == GmosCommonType.FPUnitMode.BUILTIN) {
                if (instGMOS.isImaging()) {
                    _addImagingFOV();
                } else if (instGMOS.isSpectroscopic()) {
                    _addLongSlitFOV();
                } else if (instGMOS.isIFU()) {
                    _addIFU_FOV((GmosCommonType.FPUnit) instGMOS.getFPUnit());
                } else if (instGMOS.isNS()) {
                    _addNS_FOV();
                }
            } else if (fpUnitMode == GmosCommonType.FPUnitMode.CUSTOM_MASK) {
                _addMOS_FOV();
            }
        }
    }

    @Override protected boolean _calc(TpeImageInfo tii) {
        if (!super._calc(tii)) return false;
        _updateFigureList();
        return true;
    }

    /**
     * Draw the feature.
     */
    public void draw(Graphics g, TpeImageInfo tii) {
        if (!_calc(tii)) return;

        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(FOV_COLOR);

        // draw the FOV
        Iterator it = _figureList.iterator();
        while (it.hasNext()) {
            g2d.draw((Shape) it.next());
        }
        if (_gaps != null) {
            _gaps.draw(g2d);
        }

        // Draw the drag item
        g2d.fill(_tickMarkPD.getPolygon2D());

        if (_dragging) {
            // Draw a little above the mouse
            int baseX = _dragX;
            int baseY = _dragY - 10;

            // Draw a string displaying the rotation angle
            g2d.setFont(POS_ANGLE_FONT);
            InstGmosCommon inst = getInst();
            if (inst != null) {
                String s = "position angle = " + inst.getPosAngleDegreesStr() + " deg";
                g.drawString(s, baseX, baseY);
            }
        }
    }


    /**
     * Draw the science area at the given x,y (screen coordinate) offset position.
     */
    @Override
    public void drawAtOffsetPos(Graphics g, TpeImageInfo tii, double x, double y) {
        if (!_calc(tii)) return;

        Graphics2D g2d = (Graphics2D) g;
        AffineTransform saveAT = g2d.getTransform();

        try {
            g2d.translate(x - _baseScreenPos.x, y - _baseScreenPos.y);
            Iterator it = _figureList.iterator();
            while (it.hasNext()) {
                g2d.draw((Shape) it.next());
            }
            if (_gaps != null) {
                _gaps.draw(g2d);
            }
        } finally {
            g2d.setTransform(saveAT);
        }
    }

    // Manages drawing the CCD gaps and associated labels
    private class ImagingGaps {
        // CCD gaps
        Polygon2D.Double _gap1;
        Polygon2D.Double _gap2;

        // label positions
        Point2D.Double _posCCD1;
        Point2D.Double _posCCD2;
        Point2D.Double _posCCD3;

        // label strings
        String _nameCCD1;
        String _nameCCD2;
        String _nameCCD3;

        /**
         * Initialize CCD gap for FOV of the given size
         *
         * @param size the basic size of the imaging square, in arcsec, centered at the base pos
         */
        ImagingGaps(double size) {
            double height = size * _pixelsPerArcsec;
            double width = IMAGING_CENTER_CCD_WIDTH * _pixelsPerArcsec;
            double h2 = height / 2;
            double w2 = width / 2;
            double gapWidth = IMAGING_GAP_WIDTH * _pixelsPerArcsec;

            // XXX should the gaps be centered, inside or outside the center CCD?
            double x1 = _baseScreenPos.x - w2 - gapWidth/2.;
            double y1 = _baseScreenPos.y - h2;
            _gap1 = _makeGap(x1, y1, gapWidth, height);

            double x2 = _baseScreenPos.x + w2 - gapWidth/2.;
            double y2 = y1;
            _gap2 = _makeGap(x2, y2, gapWidth, height);

            double lrWidth = IMAGING_LR_CCD_WIDTH * _pixelsPerArcsec;
            double w3 = lrWidth / 2;
            double y3 = y1 + height / 4;

            _posCCD1 = new Point2D.Double(x1 - w3, y3);
            _posCCD2 = new Point2D.Double(_baseScreenPos.x, y3);
            _posCCD3 = new Point2D.Double(x2 + w3, y3);
            _posAngleTrans.transform(_posCCD1, _posCCD1);
            _posAngleTrans.transform(_posCCD2, _posCCD2);
            _posAngleTrans.transform(_posCCD3, _posCCD3);

            if (isHamamatsu()) {
                _nameCCD1 = "CCDr";
                _nameCCD2 = "CCDg";
                _nameCCD3 = "CCDb";
            } else {
                _nameCCD1 = "CCD1";
                _nameCCD2 = "CCD2";
                _nameCCD3 = "CCD3";
            }
        }

        // Returns a polygon for a gap with the given x,y origin, width and height (all in screen pixels)
        private Polygon2D.Double _makeGap(double x, double y, double width, double height) {
            Polygon2D.Double p = new Polygon2D.Double(x, y);
            p.lineTo(x + width, y);
            p.lineTo(x + width, y + height);
            p.lineTo(x, y + height);
            p.closePath();

            // rotate by position angle
            p.transform(_posAngleTrans);
            return p;
        }

        // Draw the CCD gaps and associated labels
        void draw(Graphics2D g2d) {
            g2d.fill(_gap1);
            g2d.fill(_gap2);

            g2d.setFont(POS_ANGLE_FONT);
            FontMetrics fm = g2d.getFontMetrics();
            int w = fm.stringWidth("CCDx") / 2;
            g2d.drawString(_nameCCD1, (int) _posCCD1.x - w, (int) _posCCD1.y);
            g2d.drawString(_nameCCD2, (int) _posCCD2.x - w, (int) _posCCD2.y);
            g2d.drawString(_nameCCD3, (int) _posCCD3.x - w, (int) _posCCD3.y);
        }
    }
}

