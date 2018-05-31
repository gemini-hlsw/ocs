package jsky.app.ot.tpe;

import edu.gemini.spModel.target.SPCoordinates;
import edu.gemini.spModel.target.SPSkyObject;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.env.TargetEnvironmentDiff;
import edu.gemini.spModel.target.obsComp.TargetObsComp;

import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An auxiliary class used to maintain a mapping between telescope positions
 * and image widget locations.
 */
public final class TpePositionMap extends PosMap<SPSkyObject, SPSkyObject> {
    private static final Logger LOG = Logger.getLogger(TpePositionMap.class.getName());

    private static TpePositionMap _tpm;

    private TargetObsComp obsComp;

    private boolean _findAsterism = false;
    private boolean _findUserTargets = false;
    private boolean _findGuideStars = false;

    private final PropertyChangeListener targetEnvListener = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            final TargetObsComp oc = (TargetObsComp) evt.getSource();
            if (oc != obsComp) return;

            final TargetEnvironment oldEnv = (TargetEnvironment) evt.getOldValue();
            final TargetEnvironment newEnv = (TargetEnvironment) evt.getNewValue();

            final TargetEnvironmentDiff diff = TargetEnvironmentDiff.all(oldEnv, newEnv);

            final Collection<SPTarget> addedTargets   = diff.getAddedTargets();
            final Collection<SPTarget> removedTargets = diff.getRemovedTargets();
            final Collection<SPCoordinates> addedCoords   = diff.getAddedCoordinates();
            final Collection<SPCoordinates> removedCoords = diff.getRemovedCoordinates();

            final List<SPSkyObject> addedList = new ArrayList<>();
            addedList.addAll(addedTargets);
            addedList.addAll(addedCoords);

            final List<SPSkyObject> removedList = new ArrayList<>();
            removedList.addAll(removedTargets);
            removedList.addAll(removedCoords);

            if (!addedList.isEmpty()) handlePosListAddedPosition(addedList);
            if (!removedList.isEmpty()) handlePosListRemovedPosition(removedList);

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

    @Override
    public SPSkyObject getKey(SPSkyObject so) {
        return so;
    }

    @Override
    public boolean exists(SPSkyObject so) {
        if (obsComp == null) return false;
        final TargetEnvironment env = obsComp.getTargetEnvironment();
        if (so instanceof SPTarget)
            return env.getTargets().contains((SPTarget) so);
        if (so instanceof SPCoordinates)
            return env.getCoordinates().contains((SPCoordinates) so);
        return false;
    }

    @Override
    protected boolean posListAvailable() {
        return obsComp != null;
    }

    @Override
    public void free() {
        LOG.finest("free TpePositionMap for obsComp: " + obsComp);

        super.free();
        if (obsComp != null){
            obsComp.removePropertyChangeListener(TargetObsComp.TARGET_ENV_PROP, targetEnvListener);
        }
        obsComp = null;
    }

    @Override
    protected List<SPSkyObject> getAllPositions() {
        if (obsComp == null) return Collections.emptyList();
        final TargetEnvironment env = obsComp.getTargetEnvironment();
        final List<SPSkyObject> lst = new ArrayList<>();
        lst.addAll(env.getTargets().toList());
        lst.addAll(env.getCoordinates().toList());
        return Collections.unmodifiableList(lst);
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
    public PosMapEntry<SPSkyObject> locate(int x, int y) {
        Map<SPSkyObject, PosMapEntry<SPSkyObject>> posTable = getPosTable();
        if (posTable == null) return null;

        if (obsComp == null) return null;
        TargetEnvironment env = obsComp.getTargetEnvironment();

        for (PosMapEntry<SPSkyObject> pme : posTable.values()) {
            final Point2D.Double p = pme.screenPos;
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
            final SPSkyObject tp = pme.taggedPos;

            if (tp instanceof SPTarget) {
                final SPTarget t = (SPTarget) tp;
                if (env.getAsterism().allSpTargetsJava().contains(t)) {
                    if (_findAsterism) {
                        return pme;
                    } else {
                        continue;
                    }
                }
                if (env.isUserPosition(t)) {
                    if (_findUserTargets) {
                        return pme;
                    } else {
                        continue;
                    }
                }
                if (env.isGuidePosition(t)) {
                    if (_findGuideStars) {
                        return pme;
                    } else {
                        continue;
                    }
                }
            }

            if (tp instanceof SPCoordinates) {
                final SPCoordinates c = (SPCoordinates) tp;
                if (env.getCoordinates().contains(c)) {
                    if (_findAsterism) {
                        return pme;
                    } else {
                        //noinspection UnnecessaryContinue
                        continue;
                    }
                }
            }
        }
        return null;
    }
}

