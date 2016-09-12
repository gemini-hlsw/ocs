package jsky.app.ot.tpe;

import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.TelescopePosWatcher;
import edu.gemini.spModel.target.WatchablePos;
import edu.gemini.spModel.target.offset.OffsetPosBase;
import edu.gemini.spModel.telescope.IssPort;

import java.awt.geom.Point2D;
import java.util.*;

/**
 * An auxiliary class used to maintain a mapping between telescope positions
 * and image widget locations.
 */
public abstract class PosMap <K, T extends WatchablePos>
                               implements TpeViewObserver, TelescopePosWatcher {

    /** The size of a position marker */
    public static final int MARKER_SIZE = 4;

    /** The position editor image widget */
    protected TpeImageWidget _iw;


    /** Maps position tags to PositionMapEntries */
    protected Map<K, PosMapEntry<T>> _posTable = new Hashtable<>();

    /** Set to true if the positions in the map are valid */
    protected boolean _valid = false;


    /**
     * Construct with an image widget.
     */
    protected PosMap(TpeImageWidget iw) {
        _iw = iw;

        // Need to know when the view changes so that the position map
        // can be updated with the correct locations of the positions.
        _iw.addViewObserver(this);
    }


    /**
     * Free any resources held by this position map.
     */
    public void free() {
        _iw.deleteViewObserver(this);
        _stopObservingPosList();
        _posTable.clear();
        _iw = null;
    }

    /**
     * Quit observing any of the previous positions.
     */
    private void _stopObservingPosList() {
        List<T> tpA = getAllPositions();
        for (T tp : tpA) {
            tp.deleteWatcher(this);
        }
    }

    protected abstract List<T> getAllPositions();

    /**
     * Reset state to manage a new position list.
     */
    protected void handlePreReset() {
        _stopObservingPosList();
        _valid = false;
    }

    /**
     * Get the position table, initializing if necessary.
     */
    public Map<K, PosMapEntry<T>> getPosTable() {
        if (_valid) {
            return _posTable;
        }

        if (_iw.isImgInfoValid() && posListAvailable()) {
            _initPosTable();
            _valid = true;
            return _posTable;
        }
        return null;
    }

    protected abstract boolean posListAvailable();

    private IssPort getIssPort() {
        return _iw.getContext().instrument().issPortOrDefault();
    }

    /**
     * Update the given position map entry from the given event,
     */
    public void updatePosition(PosMapEntry<T> pme, TpeMouseEvent tme) {
        if (pme.screenPos == null) {
            pme.screenPos = new Point2D.Double(tme.xWidget, tme.yWidget);
        } else {
            pme.screenPos.x = tme.xWidget;
            pme.screenPos.y = tme.yWidget;
        }

        WatchablePos tp = pme.taggedPos;

        tp.deleteWatcher(this);

        if (tp instanceof OffsetPosBase) {
            OffsetPosBase pos = (OffsetPosBase) tp;
            pos.setXY(tme.xOffset, tme.yOffset, getIssPort());

            // NICI offset positions don't always go to just any p,q.
            // They will use the provided x,y offset and compute the
            // actual p,q.  So update the actual screen position.
            Point2D.Double p;
            try {
                p = _iw.taggedPosToScreenCoords(tp);
                pme.screenPos = p;
            } catch (Exception e) {
                // ignore
            }
        } else {
            ((SPTarget) tp).setRaDecDegrees(tme.pos.ra().toDegrees(), tme.pos.dec().toDegrees());
        }

        tp.addWatcher(this);

        _iw.repaint();
    }

    /**
     * Find a (visible) position under the given x,y location.
     */
    public PosMapEntry<T> locate(int x, int y) {
        Map<K, PosMapEntry<T>> posTable = getPosTable();
        if (posTable == null) return null;

        for (PosMapEntry<T> pme : posTable.values()) {
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

            return pme;
        }
        return null;
    }

    /**
     * Get the PosMapEntry corresponding with the telescope position
     * with the given tag.
     */
    public PosMapEntry<T> getPositionMapEntry(K key) {
        if (key == null) return null;
        Map<K, PosMapEntry<T>> posTable = getPosTable();
        if (posTable == null) return null;

        return posTable.get(key);
    }

    /**
     * Get an Enumeration of all the PositionMapEntries.
     */
    public final Iterator<PosMapEntry<T>> getAllPositionMapEntries() {
        Map<K, PosMapEntry<T>> posTable = getPosTable();
        if (posTable == null) {
            //noinspection unchecked
            return (Iterator<PosMapEntry<T>>) Collections.emptyMap().values();
        }
        return posTable.values().iterator();
    }

    /**
     * Find a TaggedPos under the given x,y location.
     */
    public WatchablePos locatePos(int x, int y) {
        PosMapEntry<T> pme = locate(x, y);
        if (pme == null) {
            return null;
        }
        return pme.taggedPos;
    }

    /**
     * Get the location of a position from its tag.
     */
    public Point2D.Double getLocationFromTag(K key) {
        Map<K, PosMapEntry<T>> posTable = getPosTable();
        if (posTable == null) {
            return null;
        }

        // Get the base position
        PosMapEntry<T> pme = posTable.get(key);
        if (pme == null) return null;

        return pme.screenPos;
    }



    /**
     * Initialize the mapping between TaggedPos objects and their
     * screen location.  Also become an observer of each position and
     * of the list as a whole in order to keep the mapping up-to-date.
     */
    private void _initPosTable() {

        _posTable.clear();

        List<T> tpA = getAllPositions();
        for (T tp : tpA) {
            tp.addWatcher(this);

            Point2D.Double p;
            try {
                p = _iw.taggedPosToScreenCoords(tp);
            } catch (Exception e) {
                p = null;
            }
            _posTable.put(getKey(tp), new PosMapEntry<>(p, tp));
        }
    }

    protected abstract K getKey(T pos);

    /**
     * Recalculate the screen positions of everything, because the view has
     * changed.
     */
    protected void _updateScreenLocations() {
        for (PosMapEntry<T> pme : _posTable.values()) {
            WatchablePos tp = pme.taggedPos;
            try {
                pme.screenPos = _iw.taggedPosToScreenCoords(tp);
            } catch (Exception e) {
                pme.screenPos = null;
            }
        }
    }


    /**
     * The list has been reset, or changed so much that the client should
     * start from scratch.
     */
    protected void handlePosListReset() {
        _updateMap(getAllPositions());
        _iw.repaint();
    }

    /**
     * A position has been added to the list.
     */
    public void handlePosListAddedPosition(Collection<T> tpList) {
        Map<K, PosMapEntry<T>> posTable = getPosTable();
        if (posTable == null) return;

        for (T tp : tpList) {
            tp.addWatcher(this);

            // New positions may have invalid WCS values...
            Point2D.Double p;
            try {
                p = _iw.taggedPosToScreenCoords(tp);
            } catch (Exception e) {
                p = null;
            }
            PosMapEntry<T> pme = new PosMapEntry<>(p, tp);
            posTable.put(getKey(tp), pme); // Replaces existing one if present
        }
    }

    /**
     * A position has been removed from the list.
     */
    public void handlePosListRemovedPosition(Collection<T> tpList) {
        Map<K, PosMapEntry<T>> posTable = getPosTable();
        if (posTable == null) return;

        for (T tp : tpList) {
            posTable.remove(getKey(tp));
            tp.deleteWatcher(this);
        }
    }

    protected abstract boolean exists(K key);

    /**
     * Re-sync the OffsetPosList and the posTable.
     */
    private void _updateMap(List<T> tpA) {
        Map<K, PosMapEntry<T>> posTable = getPosTable();
        if (posTable == null) return;

        // First remove anything from the table that doesn't exist anymore
        for (K tag : new HashSet<>(posTable.keySet())) {
            if (!exists(tag)) {
                PosMapEntry<T> pme = posTable.remove(tag);
                if (pme != null) { // And it really shouldn't be ...
                    pme.taggedPos.deleteWatcher(this);
                }
            }
        }

        // Now add anything from the list that isn't in the table, and make
        // sure that the PositionMaps that are there are still valid.
        for (T tp : tpA) {
            PosMapEntry<T> pme = posTable.get(getKey(tp));

            if (pme == null || pme.taggedPos != tp) {
                tp.addWatcher(this);
                Point2D.Double p;
                try {
                    p = _iw.taggedPosToScreenCoords(tp);
                } catch (Exception ex) {
                    p = null;
                }
                pme = new PosMapEntry<>(p, tp);
                posTable.put(getKey(tp), pme); // Replaces existing one if present
            }
        }
    }


    @SuppressWarnings("unchecked")
    public void telescopePosUpdate(WatchablePos tp) {
        Map<K, PosMapEntry<T>> posTable = getPosTable();
        if (posTable == null) return;

        PosMapEntry<T> pme = posTable.get(getKey((T) tp));

        if (pme != null) {
            // Was the position valid before the update?
            boolean wasValid = (pme.screenPos != null);

            try {
                pme.screenPos = _iw.taggedPosToScreenCoords(tp);
            } catch (Exception e) {
                //System.out.println("XXX PosMap.telescopePosLocationUpdate: couldn't get screenPos: " + e);
                pme.screenPos = null;
            }

            // Is the position valid now after the update?
            boolean isValid = (pme.screenPos != null);

            if (!wasValid && !isValid) {
                return;
            }
            _iw.repaint();
        }
    }

    /**
     * The TpeViewObserver interface.  The view changed in the image widget,
     * so update the locations of everything.
     *
     * @see TpeViewObserver
     */
    public void tpeViewChange(TpeImageWidget iw) {
        if (_valid) {
            _updateScreenLocations();
        } else {
            getPosTable();
        }
    }
}
