package jsky.app.ot.tpe.feat;

import edu.gemini.spModel.target.SPSkyObject;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import jsky.app.ot.tpe.*;

import java.awt.geom.Point2D;

/**
 * A base class for all telescope positions.  The <tt>draw</tt> method
 * of TpeImageFeature and the <tt>dragStart</tt> method of
 * TpeDraggableFeature are left for subclasses.  This class
 * handles the common problem of dragging a position, once that position
 * has been located to start the drag.
 */
public abstract class TpePositionFeature extends TpeImageFeature
        implements TpeDraggableFeature, TpeEraseableFeature, TpeSelectableFeature {

    protected PosMapEntry<SPSkyObject> _dragObject;

    /**
     * Construct the feature with its name and description.
     */
    public TpePositionFeature(String name, String descr) {
        super(name, descr);
    }

    /**
     */
    public final TargetEnvironment getTargetEnvironment() {
        TargetObsComp obsComp = getTargetObsComp();
        if (obsComp == null) return null;
        return obsComp.getTargetEnvironment();
    }

    public final TargetObsComp getTargetObsComp() {
        return _iw.getContext().targets().orNull();
    }

    /**
     */
    public boolean positionIsClose(PosMapEntry<SPSkyObject> pme, int x, int y) {
        Point2D.Double p = pme.screenPos;
        if (p == null) {
            return false;
        }

        double dx = Math.abs(p.x - x);
        if (dx > MARKER_SIZE) {
            return false;
        }

        double dy = Math.abs(p.y - y);
        return dy <= MARKER_SIZE;
    }


    /**
     */
    public void drag(TpeMouseEvent tme) {
        if (_dragObject != null) {
            if (_dragObject.screenPos == null) {
                _dragObject.screenPos = new Point2D.Double(tme.xWidget, tme.yWidget);
            } else {
                _dragObject.screenPos.x = tme.xWidget;
                _dragObject.screenPos.y = tme.yWidget;
            }
        }

        _iw.repaint();
    }

    /**
     */
    public void dragStop(TpeMouseEvent tme) {
        if (_dragObject != null) {

            // Make sure to update the telescope position and let observers be
            // notified.

            TpePositionMap pm = TpePositionMap.getMap(_iw);
            pm.updatePosition(_dragObject, tme);
            _dragObject = null;

            _iw.getContext().targets().commit();
        }
    }


    /**
     * Return true if the mouse event is over the image feature drawing.
     */
    public boolean isMouseOver(TpeMouseEvent tme) {
        TpePositionMap pm = TpePositionMap.getMap(_iw);
        return (pm.locatePos(tme.xWidget, tme.yWidget) != null);
    }

    public TpeImageFeatureCategory getCategory() {
        return TpeImageFeatureCategory.target;
    }
}

