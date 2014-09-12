/**
 * $Id: GmosFov.java 6526 2005-08-03 21:27:13Z brighton $
 */

package edu.gemini.mask;

/**
 * Extract the exact GMOS field of view from a given catalog table.
 */
public class GmosFov {

    // The values below define the GMOS field of view:
    //
    // This chops off where:
    //  x<XSTART || x>XEND
    //  y<YSTART || y>YEND and
    //
    //  This chops off the 4 corners. 4 Co-ord's are:
    //  TopLeft     A( XSTART,YEND-YCORNER2) to B(XSTART+XCORNER2,YEND)
    //			:slope,Ay-By/Ax-Bx, xo=By-slope*Bx
    //  TopRight    C(XEND-XCORNER1,YEND) to D(XEND, YEND-YCORNER2)
    //			:slope,(Cy-Dy)/(Cx-Dx),xo=Dy-slope*Dx
    //  BottomLeft  E(XSTART,YSTART+YCORNER1 ) to F(XSTART+XCORNER2, YSTART)
    //			:slope,(Ey-Fy)/(Ex-Fx),xo=Fy-slope*Fx-YDELTA
    //  BottomRight G(XEND-XCORNER1, YSTART) to H(XEND, YSTART+YCORNER1)
    //			:slope,(Gy-Hy)/(Gx-Hx),xo=Hy-slope*Hx-YDELTA
    //
    //           XCor2         XCor1
    //            |              |
    //               B        C
    //  YCor2-     /           \
    //           A               D
    //
    //           E               H
    //  YCor1-     \           /
    //               F        G
    //
    //
    // All values are in arc seconds.
    //

    private GmCoords _xVal;		// Contain FoV X coordinate values
    private GmCoords _yVal;		// Contain FoV Y coordinate values

    // Statistics about dropped rows
    private int _numDropped = 0;
    private int _numGuides = 0;
    private int _numP1 = 0;
    private int _numP2 = 0;
    private int _numP3 = 0;

    /**
     * Extract the exact GMOS field of view, and drop objects that are out of view.
     *
     * @param maskParams input table and parameters
     */
    public GmosFov(MaskParams maskParams) {
        double pixelScale = maskParams.getPixelScale();
        _xVal = GmCoords.getX(pixelScale);
        _yVal = GmCoords.getY(pixelScale);

        double Ax, Ay, Bx, By, Cx, Cy, Dx, Dy, m1, q1, m2, q2;
        double Ex, Ey, Fx, Fy, Gx, Gy, Hx, Hy, m3, q3, m4, q4;

        // Initialize slopes and x0's.
        m1 = m2 = m3 = m4 = 0;
        q1 = q2 = q3 = q4 = 0;

        /*
         *  WARNING, you will see this xoff value used, but since
         *  it is zero it nulls out some values.  I have left it in just
         *  incase someone wants to change the corners a bit.
         */
        double xoff = 0;

        /*
         *  Calculate the corner slopes.
         *  The Top Left Corner.
         */
        Ax = _xVal.getStart();
        Ay = _yVal.getEnd() - _yVal.getCorner2();
        Bx = _xVal.getStart() + _xVal.getCorner2();
        By = _yVal.getEnd();
        m1 = (Ay - By) / (Ax - Bx);
        q1 = (By - m1 * Bx) + m1 * xoff;

        /*
         *  Calculate the corner slopes.
         *  The Top Right Corner.
         */
        Cx = _xVal.getEnd() - _xVal.getCorner1();
        Cy = _yVal.getEnd();
        Dx = _xVal.getEnd();
        Dy = _yVal.getEnd() - _yVal.getCorner2();
        m2 = (Cy - Dy) / (Cx - Dx);
        q2 = (Dy - m2 * Dx) + m2 * xoff;

        /*
         *  Calculate the corner slopes.
         *  The Bottom Left Corner.
         */
        Ex = _xVal.getStart();
        Ey = _yVal.getStart() + _yVal.getCorner1();
        Fx = _xVal.getStart() + _xVal.getCorner2();
        Fy = _yVal.getStart();
        m3 = (Ey - Fy) / (Ex - Fx);
        q3 = (Fy - m3 * Fx) - _yVal.getDelta() + m3 * xoff;

        /*
         *  Calculate the corner slopes.
         *  The Bottom Right Corner.
         */
        Gx = _xVal.getEnd() - _xVal.getCorner1();
        Gy = _yVal.getStart();
        Hx = _xVal.getEnd();
        Hy = _yVal.getStart() + _yVal.getCorner1();
        m4 = (Gy - Hy) / (Gx - Hx);
        q4 = (Hy - m4 * Hx) - _yVal.getDelta() + m4 * xoff;

        ObjectTable table = maskParams.getTable();
        int numRows = table.getRowCount();
        for (int row = 0; row < numRows; row++) {
            double X = table.getXCcd(row);
            double Y = table.getYCcd(row);
            double pX = table.getSlitPosX(row);
            double pY = table.getSlitPosY(row);
            String priority = table.getPriority(row);

            /*
             *  Calculate the real slit position, x_ccd+(slitpos_x/pixelScale),
             *  and do the same for y.
             */
            X = X + (pX / pixelScale);
            Y = Y + (pY / pixelScale);

            /*
             *  If this item falls in one of the corners,
             *  then drop the item, otherwise put to output file.
             */
            if (Y <= m1 * X + q1
                    && Y <= m2 * X + q2
                    && Y >= _yVal.getStart()
                    && Y <= _yVal.getEnd() - _yVal.getDelta()
                    && X >= _xVal.getStart()
                    && X <= _xVal.getEnd()
                    && Y >= m3 * X + q3
                    && Y >= m4 * X + q4
                    && Y >= 1) {
                // keep this row
                continue;
            }
            // drop this row (then correct index for loop)
            // XXX change this method to create a new table?
            table.removeRow(row--);
            numRows--;
            _updateStatistics(priority);
        }

        System.out.println("Field of View Calculations:\n");
        System.out.println("   Num objects out of view: " + _numDropped);
        System.out.println("   Dropped Object Breakdown");
        System.out.println("   Guide objects:   " + _numGuides);
        System.out.println("   Priority 1 :     " + _numP1);
        System.out.println("   Priority 2 :     " + _numP2);
        System.out.println("   Priority 3 :     " + _numP3);
        System.out.println(
                "   Ignored objects:   "
                + (_numDropped - (_numGuides + _numP1 + _numP2 + _numP3)));
        System.out.println("----------------------------------------------------\n");
    }

    // Update the statistics for a discarded row
    private void _updateStatistics(String priority) {
        _numDropped++;
        if ("0".equals(priority)) {
            _numGuides++;
        } else if ("1".equals(priority)) {
            _numP1++;
        } else if ("2".equals(priority)) {
            _numP2++;
        } else if ("3".equals(priority)) {
            _numP3++;
        }
    }

    public int getNumDropped() {
        return _numDropped;
    }

    public int getNumGuides() {
        return _numGuides;
    }

    public int getNumP1() {
        return _numP1;
    }

    public int getNumP2() {
        return _numP2;
    }

    public int getNumP3() {
        return _numP3;
    }
}
