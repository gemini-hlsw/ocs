package jsky.app.ot.tpe.feat;

import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import edu.gemini.spModel.target.obsComp.TargetSelection;
import edu.gemini.spModel.target.system.CoordinateParam;
import jsky.app.ot.tpe.*;

import java.awt.*;
import java.awt.geom.Point2D;

public class TpeBasePosFeature extends TpePositionFeature {

    /**
     * Construct the feature with its name and description.
     */
    public TpeBasePosFeature() {
        super("Base", "Show the location of the base position.");
    }

    public void reinit(TpeImageWidget iw, TpeImageInfo tii) {
        super.reinit(iw, tii);

        // Tell the position map that the base position is visible.
        TpePositionMap pm = TpePositionMap.getMap(iw);
        pm.setFindBase(true);
    }

    public void unloaded() {
        // Tell the position map that the base position is no longer visible.
        TpePositionMap pm = TpePositionMap.getExistingMap();
        if (pm != null) pm.setFindBase(false);

        super.unloaded();
    }

    /**
     */
    public boolean erase(TpeMouseEvent tme) {
        // You can't erase the base position
        return false;
    }

    /**
     * @see jsky.app.ot.tpe.TpeSelectableFeature
     */
    public Object select(TpeMouseEvent tme) {
        TpePositionMap pm = TpePositionMap.getMap(_iw);

        int x = tme.xWidget;
        int y = tme.yWidget;

        TargetObsComp obsComp = getTargetObsComp();
        if (obsComp != null) {
            PosMapEntry<SPTarget> pme = pm.getPositionMapEntry(obsComp.getBase());
            if ((pme != null) && (positionIsClose(pme, x, y)) && getContext().targets().shell().isDefined()) {
                TargetSelection.set(getContext().targets().envOrNull(), getContext().targets().shell().get(), pme.taggedPos);
                return pme.taggedPos;
            }
        }
        return null;
    }

    /**
     */
    public void draw(Graphics g, TpeImageInfo tii) {
        TpePositionMap pm = TpePositionMap.getMap(_iw);

        TargetEnvironment env = getTargetEnvironment();
        if (env == null) return;

        Point2D.Double base = pm.getLocationFromTag(env.getBase());
        if (base == null) return;

        int r = MARKER_SIZE;
        int d = 2 * r;

        // Draw crosshairs
        g.setColor(Color.yellow);
        g.drawOval((int) (base.x - r), (int) (base.y - r), d, d);
        g.drawLine((int) base.x, (int) (base.y - r), (int) base.x, (int) (base.y + r));
        g.drawLine((int) (base.x - r), (int) base.y, (int) (base.x + r), (int) base.y);
    }

    /**
     */
    public Option<Object> dragStart(TpeMouseEvent tme, TpeImageInfo tii) {
        TargetEnvironment env = getTargetEnvironment();
        if (env == null) return None.instance();

        TpePositionMap pm = TpePositionMap.getMap(_iw);
        PosMapEntry<SPTarget> pme = pm.getPositionMapEntry(env.getBase());
        if (pme != null && positionIsClose(pme, tme.xWidget, tme.yWidget)) {
            _dragObject = pme;
            return new Some<>(pme.taggedPos);
        }

        return None.instance();
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

            SPTarget tp = (SPTarget) _dragObject.taggedPos;
            tp.setRaDecDegrees(tme.pos.getRaDeg(), tme.pos.getDecDeg());
        }
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }
}

