package edu.gemini.spModel.target;

import java.io.Serializable;
import java.util.Vector;

public class WatchablePos implements Cloneable, Serializable {

    private transient Vector<TelescopePosWatcher> _watchers;

    public final synchronized void addWatcher(final TelescopePosWatcher tpw) {
        if (_watchers == null) {
            _watchers = new Vector<>();
        } else if (_watchers.contains(tpw)) {
            return;
        }
        _watchers.addElement(tpw);
    }

    public final synchronized void deleteWatcher(final TelescopePosWatcher tpw) {
        if (_watchers == null) {
            return;
        }
        _watchers.removeElement(tpw);
    }

    protected final synchronized Vector _getWatchers() {
        if (_watchers == null) {
            return null;
        }

        return (Vector) _watchers.clone();
    }

    protected final void _notifyOfUpdate() {
        final Vector v = _getWatchers();
        if (v == null) return;
        for (int i = 0; i < v.size(); ++i) {
            final TelescopePosWatcher tpw;
            tpw = (TelescopePosWatcher) v.elementAt(i);
            tpw.telescopePosUpdate(this);
        }
    }

    // Discard watchers on clone()
    public Object clone() throws CloneNotSupportedException {
        final WatchablePos tp = (WatchablePos) super.clone();
        tp._watchers = null;
        return tp;
    }

}
