package edu.gemini.spModel.target;

import java.io.Serializable;
import java.util.Set;
import java.util.HashSet;

public class WatchablePos implements Cloneable, Serializable {

    private transient Set<TelescopePosWatcher> _watchers;

    public final synchronized boolean addWatcher(final TelescopePosWatcher tpw) {
        if (_watchers == null) {
            _watchers = new HashSet<>();
        }
        return _watchers.add(tpw);
    }

    public final synchronized boolean deleteWatcher(final TelescopePosWatcher tpw) {
        return _watchers != null && _watchers.remove(tpw);
    }

    public final synchronized Set<TelescopePosWatcher> getWatchers() {
        return _watchers == null ? null : new HashSet<>(_watchers);
    }

    protected final void _notifyOfUpdate() {
        final Set<TelescopePosWatcher> v = getWatchers();
        if (v != null) {
            v.forEach(tpw -> tpw.telescopePosUpdate(this));
        }
    }

    // Discard watchers on clone()
    public Object clone() throws CloneNotSupportedException {
        final WatchablePos tp = (WatchablePos) super.clone();
        tp._watchers = null;
        return tp;
    }

}
