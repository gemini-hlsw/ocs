package jsky.app.ot.tpe;

import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.env.TargetEnvironmentDiff;
import edu.gemini.spModel.target.obsComp.TargetObsComp;

import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An auxiliary class used to maintain a mapping between telescope positions
 * and image widget locations.
 */
public final class TpePositionMap extends PosMap<SPTarget, SPTarget> {
    private static final Logger LOG = Logger.getLogger(TpePositionMap.class.getName());

    private static TpePositionMap _tpm;

    private TargetObsComp obsComp;

    private boolean _findAsterism = false;
    private boolean _findUserTargets = false;
    private boolean _findGuideStars = false;

    private final PropertyChangeListener targetEnvListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
            TargetObsComp oc = (TargetObsComp) evt.getSource();
            if (oc != obsComp) return;

            TargetEnvironment oldEnv = (TargetEnvironment) evt.getOldValue();
            TargetEnvironment newEnv = (TargetEnvironment) evt.getNewValue();

            TargetEnvironmentDiff diff = TargetEnvironmentDiff.primaryGuideGroup(oldEnv, newEnv);

            Collection<SPTarget> added   = diff.getAddedTargets();
            Collection<SPTarget> removed = diff.getRemovedTargets();

            if (added.size() > 0) handlePosListAddedPosition(added);
            if (removed.size() > 0) handlePosListRemovedPosition(removed);

            _iw.repaint();
        }
    };

    /**
     * Get the position map associated with the given image widget, creating
     * it if necessary.
     */
    public static TpePositionMap getMap(TpeImageWidget iw) {
        if (_tpm == null) _tpm = new TpePositionMap(iw);
        return _tpm;
    }

    /**
     * Get the map only if it already exists.
     */
    public static TpePositionMap getExistingMap() { return _tpm; }

    /**
     * Construct with an image widget.
     */
    public TpePositionMap(TpeImageWidget iw) {
        super(iw);
    }

    public SPTarget getKey(SPTarget target) {
        return target;
    }

    public boolean exists(SPTarget target) {
        if (obsComp == null) return false;
        return obsComp.getTargetEnvironment().getTargets().contains(target);
    }

    protected boolean posListAvailable() {
        return obsComp != null;
    }

    public void free() {
        LOG.finest("free TpePositionMap for obsComp: " + obsComp);

        super.free();
        if (obsComp != null){
            obsComp.removePropertyChangeListener(TargetObsComp.TARGET_ENV_PROP, targetEnvListener);
        }
        obsComp = null;
    }

    protected List<SPTarget> getAllPositions() {
        if (obsComp == null) return Collections.emptyList();
        return obsComp.getTargetEnvironment().getTargets().toList();
    }

    public void reset(TpeContext ctx) {
        TargetObsComp obsComp = ctx.targets().orNull();
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("reset old=[" + this.obsComp + "], new=[" + obsComp + "]");
        }
        if (this.obsComp == obsComp) return;

        handlePreReset();

        if (this.obsComp != null) {
            this.obsComp.removePropertyChangeListener(TargetObsComp.TARGET_ENV_PROP, targetEnvListener);
        }
        this.obsComp = obsComp;
        if (this.obsComp != null) {
            this.obsComp.addPropertyChangeListener(TargetObsComp.TARGET_ENV_PROP, targetEnvListener);
        }

        handlePosListReset();
    }

    /** Turn on/off the ability to find asterism elements with a call to <tt>locate</tt>. */
    public void setFindAsterism(boolean find) {
        _findAsterism = find;
    }

    /**
     * Turn on/off the ability to find a guide star with a call to
     * <tt>locate</tt>.
     */
    public void setFindGuideStars(boolean find) {
        _findGuideStars = find;
    }

    /**
     * Turn on/off the ability to find a user position with a call to
     * <tt>locate</tt>.
     */
    public void setFindUserTarget(boolean find) {
        _findUserTargets = find;
    }


    /**
     * Find a (visible) position under the given x,y location.
     */
    public PosMapEntry<SPTarget> locate(int x, int y) {
        Map<SPTarget, PosMapEntry<SPTarget>> posTable = getPosTable();
        if (posTable == null) return null;

        if (obsComp == null) return null;
        TargetEnvironment env = obsComp.getTargetEnvironment();

        for (PosMapEntry<SPTarget> pme : posTable.values()) {
            Point2D.Double p = pme.screenPos;
            if (p == null) {
                continue;
            }

            // Is this position under the mouse indicator?
            double dx = Math.abs(p.x - x);
            if (dx > MARKER_SIZE) {
                continue;
            }
            double dy = Math.abs(p.y - y);
            if (dy > MARKER_SIZE) {
                continue;
            }

            // Is this position visible?
            SPTarget tp = pme.taggedPos;

            if (env.getAsterism().allSpTargetsJava().contains(tp)) {
                if (_findAsterism) {
                    return pme;
                } else {
                    continue;
                }
            }
            if (env.isUserPosition(tp)) {
                if (_findUserTargets) {
                    return pme;
                } else {
                    continue;
                }
            }
            if (env.isGuidePosition(tp)) {
                if (_findGuideStars) {
                    return pme;
                } else {
                    //noinspection UnnecessaryContinue
                    continue;
                }
            }
        }
        return null;
    }
}

