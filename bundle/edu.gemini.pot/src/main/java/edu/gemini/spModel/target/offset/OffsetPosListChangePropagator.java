package edu.gemini.spModel.target.offset;

import edu.gemini.spModel.target.TelescopePosWatcher;
import edu.gemini.spModel.target.WatchablePos;

import java.util.List;

/**
 * Propagates changes to the position list or any of the positions it
 * contains to the containing data object's property change listeners.
 */
public final class OffsetPosListChangePropagator<P extends OffsetPosBase> implements OffsetPosListWatcher<P> {
    /**
     * Basically a function () => Unit which should fire the property change
     * event.
     */
    public interface Notifier {
        public void apply();
    }

    private final Notifier notifier;


    public OffsetPosListChangePropagator(Notifier notifier) {
        this.notifier = notifier;
    }

    public OffsetPosListChangePropagator(Notifier notifier, OffsetPosList<P> posList) {
        this.notifier = notifier;
        resetPosWatcher(posList.getAllPositions());
    }

    private final TelescopePosWatcher posWatcher = new TelescopePosWatcher() {
        @Override public void telescopePosUpdate(WatchablePos tp) {
            propagate();
        }
    };

    private void propagate() {
        notifier.apply();
//        dataObject.firePropertyChange(new PropertyChangeEvent(dataObject, SeqRepeatOffsetBase.OFFSET_POS_LIST_PROP, null, _posList));
    }

    private void resetPosWatcher(List<P> posList) {
        // The goal here is to make sure that all positions have one
        // instance of posWatcher.  Since we don't know which ones already
        // have it, just delete and add it to make sure it is there once.
        for (P p : posList) {
            p.deleteWatcher(posWatcher);
            p.addWatcher(posWatcher);
        }
    }

    @Override public void posListReset(OffsetPosList<P> opl) {
        resetPosWatcher(opl.getAllPositions());
        propagate();
    }

    @Override public void posListAddedPosition(OffsetPosList<P> opl, List<P> newPos) {
        resetPosWatcher(newPos);
        propagate();
    }

    @Override public void posListRemovedPosition(OffsetPosList<P> opl, List<P> rmPos) {
        for (P p : rmPos) p.deleteWatcher(posWatcher);
        propagate();
    }

    @Override public void posListPropertyUpdated(OffsetPosList<P> opl, String propertyName, Object oldValue, Object newValue) {
        propagate();
    }
}
