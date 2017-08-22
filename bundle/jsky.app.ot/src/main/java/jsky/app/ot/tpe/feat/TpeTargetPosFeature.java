package jsky.app.ot.tpe.feat;

import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.spModel.core.SiderealTarget;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.env.UserTarget;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import edu.gemini.spModel.target.obsComp.TargetSelection;
import jsky.app.ot.tpe.*;
import jsky.app.ot.util.BasicPropertyList;
import jsky.app.ot.util.PropertyWatcher;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.Iterator;

public class TpeTargetPosFeature extends TpePositionFeature
        implements TpeCreatableFeature, PropertyWatcher {

    private static final BasicPropertyList _props = new BasicPropertyList(TpeTargetPosFeature.class.getName());
    private static final String PROP_SHOW_TAGS = "Show Tags";
    static {
        _props.registerBooleanProperty(PROP_SHOW_TAGS, true);
    }

    private final class UserCreateableItem implements TpeCreatableItem {
        private final UserTarget.Type userTargetType;

        UserCreateableItem(UserTarget.Type t) {
            this.userTargetType = t;
        }

        public String getLabel() {
            return userTargetType.displayName;
        }

        public Type getType() {
            return Type.userTarget;
        }

        public boolean isEnabled(TpeContext ctx) {
            return ctx.targets().isDefined();
        }

        public void create(TpeMouseEvent tme, TpeImageInfo tii) {
            final TargetObsComp obsComp = getTargetObsComp();
            if (obsComp == null) return;

            final Option<SiderealTarget> targetOpt = tme.target;
            final SPTarget userPos = targetOpt.map(SPTarget::new).getOrElse(() -> {
                final double ra  = tme.pos.ra().toDegrees();
                final double dec = tme.pos.dec().toDegrees();
                // No SkyObject info is present so we use the old way of creating
                // a target from a mouse event using only the coordinates.
                return new SPTarget(ra, dec);
            });

            final TargetEnvironment env = obsComp.getTargetEnvironment();
            final UserTarget          u = new UserTarget(userTargetType, userPos);
            final ImList<UserTarget> us = env.getUserTargets().append(u);
            obsComp.setTargetEnvironment(env.setUserTargets(us));
            _iw.getContext().targets().commit();
        }
    }


    private final TpeCreatableItem[] createableItems = new TpeCreatableItem[UserTarget.Type.values().length];

    /**
     * Construct the feature with its name and description.
     */
    public TpeTargetPosFeature() {
        super("Target", "Show the locations of target positions.");

        final UserTarget.Type[] ts = UserTarget.Type.values();
        for (int i=0; i<ts.length; ++i) {
            createableItems[i] = new UserCreateableItem(ts[i]);
        }
    }


    public void reinit(final TpeImageWidget iw, final TpeImageInfo tii) {
        super.reinit(iw, tii);

        _props.addWatcher(this);

        // Tell the position map that the target positions are visible.
        final TpePositionMap pm = TpePositionMap.getMap(iw);
        pm.setFindUserTarget(true);
    }

    public void unloaded() {
        // Tell the position map that the target positions are not visible.
        final TpePositionMap pm = TpePositionMap.getExistingMap();
        if (pm != null) pm.setFindUserTarget(false);

        _props.deleteWatcher(this);

        super.unloaded();
    }

    /**
     * A property has changed.
     *
     * @see PropertyWatcher
     */
    public void propertyChange(final String propName) {
        _iw.repaint();
    }

    /**
     * Override getProperties to return the properties supported by this
     * feature.
     */
    public BasicPropertyList getProperties() {
        return _props;
    }

    /**
     * Turn on/off the drawing of position tags.
     */
    public void setDrawTags(final boolean drawTags) {
        _props.setBoolean(PROP_SHOW_TAGS, drawTags);
    }

    /**
     * Get the "draw position tags" property.
     */
    private boolean getDrawTags() {
        return _props.getBoolean(PROP_SHOW_TAGS, true);
    }


    /**
     */
    public TpeCreatableItem[] getCreatableItems() {
        return createableItems;
    }

    /**
     */
    public boolean erase(final TpeMouseEvent tme) {
        final TargetObsComp obsComp = getTargetObsComp();
        if (obsComp == null) return false;

        final TpePositionMap pm = TpePositionMap.getMap(_iw);

        final Iterator<PosMapEntry<SPTarget>> it = pm.getAllPositionMapEntries();
        while (it.hasNext()) {
            final PosMapEntry<SPTarget> pme = it.next();
            final SPTarget tp = pme.taggedPos;

            final TargetEnvironment env = obsComp.getTargetEnvironment();
            if (!env.isUserPosition(tp)) continue;

            if (positionIsClose(pme, tme.xWidget, tme.yWidget)) {
                ImList<UserTarget> us = env.getUserTargets().remove(t -> t.target.equals(tp));
                obsComp.setTargetEnvironment(env.setUserTargets(us));
                _iw.getContext().targets().commit();
                return true;
            }
        }
        return false;
    }

    /**
     * @see jsky.app.ot.tpe.TpeSelectableFeature
     */
    public Object select(final TpeMouseEvent tme) {
        final TargetObsComp obsComp = getTargetObsComp();
        if (obsComp == null) return null;

        final TpePositionMap pm = TpePositionMap.getMap(_iw);
        final SPTarget tp = (SPTarget) pm.locatePos(tme.xWidget, tme.yWidget);
        if (tp == null) return null;

        final TargetEnvironment env = obsComp.getTargetEnvironment();
        if (!env.isUserPosition(tp)) return null;

        TargetSelection.setTargetForNode(env, getContext().targets().shell().get(), tp);
        return tp;
    }

    /**
     */
    public void draw(final Graphics g, final TpeImageInfo tii) {
        final TargetObsComp obsComp = getTargetObsComp();
        if (obsComp == null) return;

        final TargetEnvironment env = obsComp.getTargetEnvironment();

        final TpePositionMap pm = TpePositionMap.getMap(_iw);

        g.setColor(Color.yellow);

        boolean drawTags = getDrawTags();
        if (drawTags) g.setFont(FONT);

        int index = 1;
        for (UserTarget u : env.getUserTargets()) {
            final PosMapEntry<SPTarget> pme = pm.getPositionMapEntry(u.target);
            if (pme == null) continue;

            final String tag = String.format("%s (%d)", u.type.displayName, index++);

            final Point2D.Double p = pme.screenPos;
            if (p == null) continue;

            g.drawLine((int) p.x, (int) (p.y - MARKER_SIZE), (int) p.x, (int) (p.y + MARKER_SIZE));
            g.drawLine((int) (p.x - MARKER_SIZE), (int) p.y, (int) (p.x + MARKER_SIZE), (int) p.y);

            if (drawTags) {
                // Draw the tag--should use font metrics to position the tag
                g.drawString(tag, (int) (p.x + MARKER_SIZE + 2), (int) (p.y + MARKER_SIZE * 2));
            }
        }
    }

    /**
     */
    public Option<Object> dragStart(TpeMouseEvent tme, TpeImageInfo tii) {
        final TargetObsComp obsComp = getTargetObsComp();
        if (obsComp == null) return None.instance();

        final TargetEnvironment env = obsComp.getTargetEnvironment();

        final TpePositionMap pm = TpePositionMap.getMap(_iw);

        final Iterator<PosMapEntry<SPTarget>> it = pm.getAllPositionMapEntries();
        while (it.hasNext()) {
            final PosMapEntry<SPTarget> pme = it.next();

            if (positionIsClose(pme, tme.xWidget, tme.yWidget) &&
                    env.isUserPosition(pme.taggedPos)) {

                _dragObject = pme;
                return new Some<>(pme.taggedPos);
            }
        }
        return None.instance();
    }
}

