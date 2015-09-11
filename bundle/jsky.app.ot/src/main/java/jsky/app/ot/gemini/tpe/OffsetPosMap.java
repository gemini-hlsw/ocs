package jsky.app.ot.gemini.tpe;

import edu.gemini.spModel.target.offset.OffsetPosBase;
import edu.gemini.spModel.target.offset.OffsetPosList;
import edu.gemini.spModel.target.offset.OffsetPosListWatcher;
import jsky.app.ot.tpe.*;
import jsky.coords.WorldCoords;

import java.util.List;

/**
 * An auxiliary class used to maintain a mapping between telescope positions
 * and image widget locations.
 */
public final class OffsetPosMap extends PosMap<String, OffsetPosBase> implements TpeImageInfoObserver, OffsetPosListWatcher<OffsetPosBase> {

    /** The current position list */
    private final SingleOffsetListContext iterator;

    private WorldCoords _basePos;
    private double _posAngle;

    /**
     * Construct with an image widget.
     */
    public OffsetPosMap(TpeImageWidget iw, SingleOffsetListContext iterator) {
        super(iw);
        iw.addInfoObserver(this);
        this.iterator = iterator;
        iterator.posList().addWatcher(this);
    }

    public SingleOffsetListContext getIterator() { return iterator; }

    public String getKey(OffsetPosBase pos) {
        return pos.getTag();
    }

    public boolean exists(String key) {
        return (iterator.posList() == null) ? false : iterator.posList().exists(key);
    }

    protected boolean posListAvailable() {
        return true;
    }

    /**
     * Get the OffsetPosList currently associated with this map.
     */
    public OffsetPosList<OffsetPosBase> getTelescopePosList() {
        return iterator.posList();
    }

    /**
     * Free any resources held by this position map.
     */
    public void free() {
        if (_iw != null) _iw.deleteInfoObserver(this);
        super.free();
        iterator.posList().deleteWatcher(this);
    }

    protected List<OffsetPosBase> getAllPositions() {
        return iterator.posList().getAllPositions();
    }

    /**
     * The TpeImageInfo has been updated.
     */
    public void imageInfoUpdate(TpeImageWidget iw, TpeImageInfo tii) {
        WorldCoords basePos = tii.getBasePos();
        double posAngleDegrees = tii.getPosAngleDegrees();

        if (basePos != _basePos || posAngleDegrees != _posAngle) {
            _basePos = basePos;
            _posAngle = posAngleDegrees;

            try {
                _updateScreenLocations();
            } catch (Exception e) {
                e.printStackTrace(); // XXX problem when switching observations and offsets are on
            }
        }
    }

    /**
     * The list has been reset, or changed so much that the client should
     * start from scratch.
     */
    public void posListReset(OffsetPosList<OffsetPosBase> tpl) {
        if (tpl != getTelescopePosList()) return;
        handlePosListReset();
    }

    /**
     * A position has been added to the list.
     */
    public void posListAddedPosition(OffsetPosList<OffsetPosBase> tpl, List<OffsetPosBase> tpList) {
        if (tpl != getTelescopePosList()) return;
        handlePosListAddedPosition(tpList);
        _iw.repaint();
    }

    /**
     * A position has been removed from the list.
     */
    public void posListRemovedPosition(OffsetPosList<OffsetPosBase> tpl, List<OffsetPosBase> tpList) {
        if (tpl != getTelescopePosList()) return;
        handlePosListRemovedPosition(tpList);
        _iw.repaint();
    }

    public void posListPropertyUpdated(OffsetPosList<OffsetPosBase> tpl, String propertyName, Object oldValue, Object newValue) {
        // ignore, irrelevant
    }
}
