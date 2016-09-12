package jsky.app.ot.gemini.inst;

import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import jsky.app.ot.tpe.*;
import jsky.app.ot.util.BasicPropertyList;
import jsky.app.ot.util.PolygonD;
import jsky.app.ot.util.PropertyWatcher;
import jsky.app.ot.util.OtColor;
import jsky.util.gui.DrawUtil;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import java.util.LinkedList;


/**
 * Abstract base class for classes that display the science area in the telescope position editor.
 */
public abstract class SciAreaFeatureBase extends TpeImageFeature
        implements TpeDraggableFeature, PropertyWatcher {

    // The color to use to draw the field of view
    public static final Color FOV_COLOR = Color.cyan;

    // Describes the basic size of the science area
    protected TpeSciArea _sciArea = new TpeSciArea();

    // Polygon describing the basic science area
    protected PolygonD _sciAreaPD;

    // Polygon describing the little triangle used to rotate the science area
    protected PolygonD _tickMarkPD;

    /** Font used to draw text for position angle  */
    protected static final Font POS_ANGLE_FONT = new Font("dialog", Font.PLAIN, 12);

    // Used for rotating the science area
    protected boolean _dragging = false;
    protected int _dragX;
    protected int _dragY;

    // Number of pixels per arcsec in screen coords
    protected double _pixelsPerArcsec;

    // The base telescope position
    protected Point2D.Double _baseScreenPos;

    // The corrected position angle.
    private double _posAngle;

    // TODO: We can probably get rid of this.
    // Used to rotate figures by the position angle
    protected AffineTransform _posAngleTrans = new AffineTransform();

    // List of Figures to draw for the FOV.
    protected LinkedList<Shape> _figureList = new LinkedList<>();


    /**
     * Construct the feature with its name and description.
     */
    public SciAreaFeatureBase() {
        super("Sci Area", "Show the science area.");
    }

    protected Color getFovColor() {
        Color c = FOV_COLOR;
        return (_iw.isViewingOffsets()) ? OtColor.makeTransparent(c, 0.5) : c;
    }

    /**
     * Calculate the polygon describing the screen location of the science area.
     */
    protected boolean _calc(TpeImageInfo tii)  {
        SPInstObsComp inst = _iw.getInstObsComp();
        if (inst == null) return false;

        _sciArea.update(inst, tii);
        _baseScreenPos = tii.getBaseScreenPos();
        _sciAreaPD = _sciArea.getPolygonDAt(_baseScreenPos.x, _baseScreenPos.y);
        _pixelsPerArcsec = tii.getPixelsPerArcsec();
        _posAngle = tii.getCorrectedPosAngleRadians();
        _posAngleTrans.setToIdentity();
        _posAngleTrans.rotate(-_posAngle, _baseScreenPos.x, _baseScreenPos.y);

        // Init the _tickMarkPD
        if (_tickMarkPD == null) {
            double[] xpoints = new double[4];
            double[] ypoints = new double[4];
            _tickMarkPD = new PolygonD(xpoints, ypoints, 4);
        }

        Point2D.Double tickOffset = _getTickMarkOffset();

        _tickMarkPD.xpoints[0] = tickOffset.x;
        _tickMarkPD.ypoints[0] = tickOffset.y - MARKER_SIZE * 2;

        _tickMarkPD.xpoints[1] = tickOffset.x - MARKER_SIZE;
        _tickMarkPD.ypoints[1] = tickOffset.y - 2;

        _tickMarkPD.xpoints[2] = tickOffset.x + MARKER_SIZE;
        _tickMarkPD.ypoints[2] = tickOffset.y - 2;

        _tickMarkPD.xpoints[3] = _tickMarkPD.xpoints[0];
        _tickMarkPD.ypoints[3] = _tickMarkPD.ypoints[0];

        _iw.skyRotate(_tickMarkPD);

        return true;
    }


    /**
     * Return the offset from the base position in arcsec for drawing the
     * "tick mark" (used as a handle to rotate the science area). The
     * offset depends on the selected FP unit mode.
     */
    protected Point2D.Double _getTickMarkOffset() {
        return new Point2D.Double(_baseScreenPos.x, _baseScreenPos.y - _sciArea.getHeight() / 2.0);
    }

    /**
     * Update the list of FOV figures to draw (redefine as needed in derived class)
     */
    protected void _updateFigureList() {
        _figureList.clear();
    }

    /**
     * Draw the science area.
     */
    public void draw(Graphics g, TpeImageInfo tii) {
        final Graphics2D g2d = (Graphics2D) g;

        if (!_calc(tii)) return;
        _updateFigureList();

        g2d.setColor(FOV_COLOR);

        // draw the FOV
        _figureList.forEach(g2d::draw);

        // Draw the drag item and science area
        g2d.draw(_sciAreaPD.getPolygon2D());
        drawDragItem(g2d);
    }

    public TpeImageFeatureCategory getCategory() {
        return TpeImageFeatureCategory.fieldOfView;
    }

    protected void drawDragItem(Graphics2D g2d) {
        // Draw the drag item
        g2d.fill(_tickMarkPD.getPolygon2D());

        if (_dragging) {
            // Draw a little above the mouse
            int baseX = _dragX;
            int baseY = _dragY - 10;

            // Draw a string displaying the rotation angle
            if (_iw.getContext().instrument().isDefined()) {
                String pa = _iw.getContext().instrument().get().getPosAngleDegreesStr();
                String s = "position angle = " + pa + " deg";
                DrawUtil.drawString(g2d, s, FOV_COLOR, Color.black, baseX, baseY);
            }
        }
    }

    /**
     * Draw the science area at the given x,y (screen coordinate) offset position.
     */
    public void drawAtOffsetPos(Graphics g, TpeImageInfo tii, double x, double y) {
        g.drawPolygon(_sciArea.getPolygonAt(x, y));
    }


    /**
     * Start dragging the object.
     */
    public Option<Object> dragStart(TpeMouseEvent tme, TpeImageInfo tii) {
        if ((_sciAreaPD == null) || (_tickMarkPD == null)) {
            return None.instance();
        }

        _dragging = false;

        // See if dragging by the corner
        for (int i = 0; i < (_sciAreaPD.npoints - 1); ++i) {
            int cornerx = (int) (_sciAreaPD.xpoints[i] + 0.5);
            int cornery = (int) (_sciAreaPD.ypoints[i] + 0.5);

            int dx = Math.abs(cornerx - tme.xWidget);
            if (dx > MARKER_SIZE) {
                continue;
            }
            int dy = Math.abs(cornery - tme.yWidget);
            if (dy > MARKER_SIZE) {
                continue;
            }

            _dragging = true;
        }

        // See if dragging by the tick mark (give a couple extra pixels to make it
        // easier to grab)
        if (!_dragging) {
            int x = (int) (_tickMarkPD.xpoints[0] + 0.5);
            int dx = Math.abs(x - tme.xWidget);
            if (dx <= MARKER_SIZE + 2) {
                int y0 = (int) (_tickMarkPD.ypoints[0] + 0.5);
                int y1 = (int) (_tickMarkPD.ypoints[1] + 0.5);
                int y = (y0 + y1) / 2;
                int dy = Math.abs(y - tme.yWidget);
                if (dy <= MARKER_SIZE + 2) {
                    _dragging = true;
                }
            }
        }

        if (_dragging) {
            _dragX = tme.xWidget;
            _dragY = tme.yWidget;
        }

        return (_dragging) ? new Some<>(_iw.getContext().instrument()) : None.instance();
    }

     /**
      * Drag to a new location.
      */
     public void drag(TpeMouseEvent tme) {
         if (_dragX == tme.xWidget && _dragY == tme.yWidget) {
             _iw.repaint();
             return;
         }

         if (_dragging) {
             _dragX = tme.xWidget;
             _dragY = tme.yWidget;

             _iw.setPosAngle(Math.round(_tii.positionAngle(tme).toDegrees()));
             _iw.repaint();
         }
     }

    /**
     * Stop dragging.
     */
    public void dragStop(TpeMouseEvent tme) {
        if (_dragging) {
            drag(tme);
            _dragging = false;
            _iw.getContext().instrument().commit();
        }
    }


    /**
     * Return true if the mouse is over an active part of this image feature
     * (so that dragging can begin there).
     */
    public boolean isMouseOver(TpeMouseEvent tme) {
        if ((_sciAreaPD == null) || (_tickMarkPD == null)) {
            return false;
        }

        // See if by the tick mark (give a couple extra pixels to make it
        // easier to grab)
        int x = (int) (_tickMarkPD.xpoints[0] + 0.5);
        int dx = Math.abs(x - tme.xWidget);
        if (dx <= MARKER_SIZE + 2) {
            int y0 = (int) (_tickMarkPD.ypoints[0] + 0.5);
            int y1 = (int) (_tickMarkPD.ypoints[1] + 0.5);
            int y = (y0 + y1) / 2;
            int dy = Math.abs(y - tme.yWidget);
            if (dy <= MARKER_SIZE + 2) {
                return true;
            }
        }
        return false;
    }

    /**
     * Override getProperties to return the properties supported by this
     * feature.
     */
    public BasicPropertyList getProperties() {
        return SciAreaFeature.getProps();
    }

    /**
     * A property has changed.
     *
     * @see PropertyWatcher
     */
    public void propertyChange(String propName) {
        _iw.repaint();
    }


    /**
     * Return the current Nod/Chop offset in screen pixels.
     */
    public Point2D.Double getNodChopOffset() {
        return new Point2D.Double();
    }
}

